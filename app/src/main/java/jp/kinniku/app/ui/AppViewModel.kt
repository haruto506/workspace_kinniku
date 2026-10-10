package jp.kinniku.app.ui

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.kinniku.app.data.*
import jp.kinniku.app.data.db.*
import jp.kinniku.app.data.csv.CsvExporter
import jp.kinniku.app.domain.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.util.UUID

data class Draft(val key: String = UUID.randomUUID().toString(), val day: Long, val sessionId: Long?, val recordId: Long?,
    val exercise: Exercise, val remaining: Int = 1, val weight: String = "", val reps: String = "", val duration: String = "", val memo: String = "", val group: String = key, val beforeRecordId: Long? = null)
data class UiState(val today: LocalDate = LocalDate.now(), val day: Long = LocalDate.now().toEpochDay(),
    val sessions: List<SessionWithRecords> = emptyList(), val exercises: List<ExerciseUsage> = emptyList(),
    val templates: List<TemplateWithItems> = emptyList(), val drafts: List<Draft> = emptyList(),
    val timer: RestTimer? = null, val elapsed: Int = 0, val settings: Settings = Settings(),
    val dailyVolume: Long = 0, val busy: Boolean = false, val exporting: Boolean = false,
    val exportProgress: String = "", val exportResults: List<String> = emptyList(),
    val notificationPrompt: Boolean = false, val loading: Boolean = true, val error: String? = null, val foregroundVersion: Int = 0)

@HiltViewModel
class AppViewModel @Inject constructor(val repository: WorkoutRepository, private val clock: TimeProvider,
    private val csv: CsvExporter, private val saved: SavedStateHandle) : ViewModel() {
    private val selectedDay = MutableStateFlow(clock.today().toEpochDay())
    private val state = MutableStateFlow(UiState(today = clock.today(), day = clock.today().toEpochDay()))
    val ui: StateFlow<UiState> = state.asStateFlow()
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private var exportJob: Job? = null
    private var foreground = false
    private var reconciled = false
    init {
        restoreDrafts()
        viewModelScope.launch { repository.exercises.collect { rows -> state.update { it.copy(exercises = rows) } } }
        viewModelScope.launch { repository.templates.collect { rows -> state.update { it.copy(templates = rows) } } }
        viewModelScope.launch { repository.timer.collect { row -> state.update { it.copy(timer = row, elapsed = row?.let { t -> Rules.rest(t.startedElapsed, clock.elapsedMillis(), t.target) } ?: 0) } } }
        viewModelScope.launch { repository.settings.values.collect { value -> state.update { it.copy(settings = value) } } }
        viewModelScope.launch { selectedDay.collectLatest { day -> repository.day(day).collect { rows -> state.update { it.copy(sessions = rows, loading = false) } } } }
        viewModelScope.launch { selectedDay.collectLatest { day -> repository.volumes(day, day).collect { rows -> state.update { it.copy(dailyVolume = rows.sumOf { p -> p.value }) } } } }
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val today = clock.today()
                state.update { it.copy(today = today) }
                val timer = state.value.timer
                if (foreground && reconciled && timer != null) {
                    val elapsed = Rules.rest(timer.startedElapsed, clock.elapsedMillis(), timer.target)
                    state.update { it.copy(elapsed = elapsed) }
                    if (elapsed >= timer.target) runCatching { repository.completeTimer(timer.token, timer.setId, true) }
                }
            }
        }
    }
    fun foreground(value: Boolean) {
        foreground = value
        if (value) {
            state.update { it.copy(foregroundVersion = it.foregroundVersion + 1) }
            reconciled = false
            viewModelScope.launch { try { repository.reconcileTimer() } finally { reconciled = true } }
        }
    }
    fun selectDay(day: Long) {
        if (state.value.exporting) return
        discardDrafts()
        if (selectedDay.value != day) { state.update { it.copy(day = day, loading = true) }; selectedDay.value = day }
    }
    fun today() = selectDay(clock.today().toEpochDay())
    fun discardDrafts() { state.update { it.copy(drafts = emptyList()) }; persistDrafts() }
    private fun mutateDrafts(block: (List<Draft>) -> List<Draft>) { state.update { it.copy(drafts = block(it.drafts)) }; persistDrafts() }
    fun updateDraft(key: String, transform: (Draft) -> Draft) = mutateDrafts { rows -> rows.map { if (it.key == key) transform(it) else it } }
    fun removeDraft(key: String) = mutateDrafts { rows -> rows.filterNot { it.key == key } }
    fun addDraft(exercise: Exercise, sessionId: Long?, record: ExerciseRecord? = null) {
        if (state.value.day != clock.today().toEpochDay()) { messages.tryEmit("当日の記録のみ追加できます"); return }
        mutateDrafts { it + Draft(day = state.value.day, sessionId = sessionId, recordId = record?.id, exercise = exercise) }
    }
    fun addSet(record: ExerciseRecord) {
        val snapshot = Exercise(id = record.exerciseId, name = record.name, nameKey = "", bodyPart = record.bodyPart,
            category = record.category, bodyweight = record.bodyweight, createdAt = 0)
        addDraft(snapshot, record.sessionId, record)
    }
    fun applyTemplate(id: Long, sessionId: Long?) = action {
        val template = requireNotNull(repository.dao.template(id)) { "テンプレートが削除されています" }
        require(template.items.isNotEmpty()) { "種目が0件のため適用できません" }
        val day = clock.today().toEpochDay()
        val target = sessionId ?: repository.dao.lastSession(day)?.id
        val drafts = template.items.sortedBy { it.order }.flatMap { item ->
            val exercise = requireNotNull(repository.dao.exercise(item.exerciseId))
            require(!exercise.deleted)
            val group = UUID.randomUUID().toString()
            List(item.setCount ?: 1) { Draft(day = day, sessionId = target, recordId = null, exercise = exercise, group = group) }
        }
        if (state.value.day != day) { selectedDay.value = day; state.update { it.copy(day = day) } }
        mutateDrafts { it + drafts }
        messages.emit("テンプレートを追加しました。入力後に完了してください")
    }
    fun commit(key: String) = action {
        val draft = state.value.drafts.firstOrNull { it.key == key } ?: return@action
        val result = repository.commit(CommitInput(draft.day, draft.sessionId, draft.recordId, draft.exercise, draft.weight, draft.reps, draft.duration, draft.memo, draft.beforeRecordId))
        mutateDrafts { rows -> rows.mapIndexedNotNull { index, row ->
            when {
                row.key == key -> null
                row.group == draft.group -> row.copy(sessionId = result.sessionId, recordId = result.recordId)
                index < rows.indexOfFirst { it.key == key } && row.recordId == null && (row.beforeRecordId == null || row.beforeRecordId == draft.beforeRecordId) &&
                    (row.sessionId == draft.sessionId || row.sessionId == result.sessionId) -> row.copy(sessionId = result.sessionId, beforeRecordId = result.recordId)
                row.sessionId == null && row.day == draft.day -> row.copy(sessionId = result.sessionId)
                else -> row
            }
        } }
        if (draft.exercise.category == "strength" && !repository.settings.values.first().notificationAsked) {
            repository.settings.markAsked()
            state.update { it.copy(notificationPrompt = true) }
        }
    }
    fun dismissNotificationPrompt() { state.update { it.copy(notificationPrompt = false) } }
    fun dismissError() { state.update { it.copy(error = null) } }
    fun action(after: () -> Unit = {}, block: suspend () -> Unit) {
        if (state.value.busy || state.value.exporting) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block(); after() }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { state.update { it.copy(error = error.message ?: "保存できませんでした。もう一度お試しください") } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    fun delete(kind: String, id: Long) = action {
        repository.delete(kind, id)
        if (kind == "session") mutateDrafts { it.filterNot { d -> d.sessionId == id } }
        if (kind == "record") mutateDrafts { it.filterNot { d -> d.recordId == id } }
    }
    fun export(start: Long, end: Long) {
        if (state.value.exporting || state.value.busy) return
        if (start > end) { messages.tryEmit("開始日は終了日以前にしてください"); return }
        state.update { it.copy(exporting = true, exportResults = emptyList()) }
        exportJob = viewModelScope.launch {
            try {
                val results = csv.export(start, end) { progress -> state.update { it.copy(exportProgress = progress) } }
                state.update { it.copy(exportResults = results) }
                messages.emit(if (results.all { it.startsWith("保存済み") }) "3件のファイルをダウンロードに保存しました" else "一部の書き出しに失敗しました。結果を確認してください")
            } catch (cancel: CancellationException) { state.update { it.copy(exportResults = listOf("中止しました。完了済みのファイルはダウンロードに残ります")) } }
            catch (_: Exception) { messages.emit("書き出しに失敗しました。保存先を確認してください") }
            finally { state.update { it.copy(exporting = false) } }
        }
    }
    fun cancelExport() { exportJob?.cancel() }
    fun reapCsv() { viewModelScope.launch { csv.reap() } }
    private fun persistDrafts() {
        saved["drafts"] = ArrayList(state.value.drafts.map { d -> Bundle().apply {
            putString("key", d.key); putString("group", d.group); putLong("before", d.beforeRecordId ?: 0); putLong("day", d.day); putLong("session", d.sessionId ?: 0); putLong("record", d.recordId ?: 0)
            putLong("exercise", d.exercise.id); putString("name", d.exercise.name); putString("part", d.exercise.bodyPart)
            putString("category", d.exercise.category); putBoolean("bw", d.exercise.bodyweight); putInt("remaining", d.remaining)
            putString("weight", d.weight); putString("reps", d.reps); putString("duration", d.duration); putString("memo", d.memo)
        } })
    }
    private fun restoreDrafts() {
        val bundles = saved.get<ArrayList<Bundle>>("drafts") ?: return
        val drafts = bundles.mapNotNull { b -> runCatching {
            Draft(b.getString("key")!!, b.getLong("day"), b.getLong("session").takeIf { it != 0L }, b.getLong("record").takeIf { it != 0L },
                Exercise(b.getLong("exercise"), b.getString("name")!!, "", b.getString("part")!!, b.getString("category")!!, b.getBoolean("bw"), createdAt = 0),
                b.getInt("remaining"), b.getString("weight").orEmpty(), b.getString("reps").orEmpty(), b.getString("duration").orEmpty(), b.getString("memo").orEmpty(), b.getString("group") ?: b.getString("key")!!, b.getLong("before").takeIf { it > 0 })
        }.getOrNull() }.filter { it.day == clock.today().toEpochDay() }
        state.update { it.copy(drafts = drafts) }
    }
}
