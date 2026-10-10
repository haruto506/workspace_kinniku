package jp.kinniku.app.data

import androidx.room.withTransaction
import jp.kinniku.app.data.alarm.AlarmGateway
import jp.kinniku.app.data.db.*
import jp.kinniku.app.domain.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

data class CommitInput(val day: Long, val sessionId: Long?, val recordId: Long?, val exercise: Exercise,
    val weight: String, val reps: String, val duration: String, val memo: String, val beforeRecordId: Long? = null)
data class Committed(val sessionId: Long, val recordId: Long)
data class VolumePoint(val day: Long, val exerciseId: Long, val part: String, val value: Long)

@Singleton
class WorkoutRepository @Inject constructor(val db: AppDatabase, private val clock: TimeProvider,
    val settings: SettingsStore, val alarms: AlarmGateway) {
    val dao = db.dao()
    // Serializes DB transitions AND OS effects, so an old cancellation cannot race a new start.
    private val mutations = Mutex()
    val exercises = dao.exercises()
    val templates = dao.templates()
    val timer = dao.observeTimer()
    fun day(day: Long) = dao.day(day)
    fun volumes(start: Long, end: Long): Flow<List<VolumePoint>> = combine(dao.volumes(start, end), dao.averageWeights(start, end)) { rows, daily ->
        val weights = daily.associate { it.day to BigDecimal(it.total).divide(BigDecimal(it.count), 0, RoundingMode.HALF_UP).toInt() }
        rows.map { VolumePoint(it.day, it.exerciseId, it.bodyPart,
            if (it.bodyweight) Rules.resolveWeight(it.day, weights).toLong() * it.reps else it.weighted) }
    }
    private suspend fun checkSession(id: Long): Session = requireNotNull(dao.session(id)) { "セッションが削除されています" }
        .also { Rules.editable(it.day, clock.today()) }
    private suspend fun checkRecord(id: Long): ExerciseRecord = requireNotNull(dao.record(id)) { "種目記録が削除されています" }
        .also { checkSession(it.sessionId) }
    private suspend fun finishRow(timer: RestTimer, reset: Boolean = false) {
        val reboot = Rules.rebooted(timer.startedElapsed, clock.elapsedMillis(), timer.boot, clock.bootCount())
        if (!reset && !reboot) dao.rest(timer.setId, Rules.rest(timer.startedElapsed, clock.elapsedMillis(), timer.target))
        dao.deleteTimer(timer.token, timer.setId)
    }
    suspend fun commit(input: CommitInput): Committed = mutations.withLock {
        val targetSeconds = settings.values.first().restSeconds
        var old: RestTimer? = null
        var next: RestTimer? = null
        val result = db.withTransaction {
            Rules.editable(input.day, clock.today())
            val existing = input.recordId?.let { checkRecord(it) }
            val master = requireNotNull(dao.exercise(input.exercise.id)) { "種目が見つかりません" }
            if (existing == null) {
                require(!master.deleted) { "種目が削除されています。選び直してください" }
                require(master.name == input.exercise.name && master.bodyPart == input.exercise.bodyPart && master.category == input.exercise.category && master.bodyweight == input.exercise.bodyweight) { "種目が変更されています。選び直してください" }
            }
            val cardio = (existing?.category ?: master.category) == "cardio"
            val bw = existing?.bodyweight ?: master.bodyweight
            val weight = if (!cardio && !bw) Rules.tenth(input.weight) else null
            val reps = if (!cardio) Rules.integer(input.reps) else 0
            val duration = if (cardio) Rules.integer(input.duration) else null
            val memo = Rules.memo(input.memo)
            val sessionId = existing?.sessionId ?: input.sessionId?.let { checkSession(it).id }
                ?: dao.lastSession(input.day)?.id ?: dao.insertSession(Session(day = input.day, createdAt = clock.wallMillis()))
            val recordId = existing?.id ?: dao.insertRecord(ExerciseRecord(sessionId = sessionId,
                exerciseId = master.id, order = dao.records(sessionId).size, name = master.name,
                bodyPart = master.bodyPart, category = master.category, bodyweight = master.bodyweight,
                duration = duration, createdAt = clock.wallMillis()))
            if (existing == null && input.beforeRecordId != null) {
                val ordered = dao.records(sessionId).toMutableList()
                val before = ordered.indexOfFirst { it.id == input.beforeRecordId }
                if (before >= 0) {
                    val created = ordered.removeAt(ordered.indexOfFirst { it.id == recordId })
                    ordered.add(before, created)
                    ordered.forEachIndexed { index, record -> dao.updateRecord(record.copy(order = index)) }
                }
            }
            if (cardio) {
                if (existing != null) dao.updateRecord(existing.copy(duration = duration))
            } else {
                val setId = dao.insertSet(WorkoutSet(recordId = recordId, order = dao.sets(recordId).size,
                    weight = weight, reps = reps, memo = memo, createdAt = clock.wallMillis()))
                old = dao.timer()
                old?.let { finishRow(it) }
                next = RestTimer(token = Random.nextLong(), setId = setId, startedElapsed = clock.elapsedMillis(),
                    boot = clock.bootCount(), startedAt = clock.wallMillis(), target = targetSeconds)
                dao.insertTimer(next!!)
            }
            Committed(sessionId, recordId)
        }
        old?.let(alarms::cancel)
        next?.let(alarms::schedule)
        result
    }
    suspend fun completeTimer(token: Long, setId: Long, notify: Boolean) = mutations.withLock {
        val completed = db.withTransaction {
            val timer = dao.timer() ?: return@withTransaction null
            if (timer.token != token || timer.setId != setId) return@withTransaction null
            if (Rules.rebooted(timer.startedElapsed, clock.elapsedMillis(), timer.boot, clock.bootCount())) {
                finishRow(timer, true); return@withTransaction timer to false
            }
            if (Rules.rest(timer.startedElapsed, clock.elapsedMillis(), timer.target) < timer.target) return@withTransaction null
            finishRow(timer)
            timer to true
        }
        completed?.let { (timer, reached) -> alarms.cancel(timer); if (notify && reached) alarms.notifyFinished() }
    }
    suspend fun reconcileTimer() = mutations.withLock {
        val old = db.withTransaction {
            val timer = dao.timer() ?: return@withTransaction null
            if (Rules.rebooted(timer.startedElapsed, clock.elapsedMillis(), timer.boot, clock.bootCount()) ||
                Rules.rest(timer.startedElapsed, clock.elapsedMillis(), timer.target) >= timer.target) {
                finishRow(timer); timer to false
            } else timer to true
        }
        old?.let { (timer, running) -> if (running) alarms.schedule(timer) else alarms.cancel(timer) }
    }
    suspend fun stopTimer(reset: Boolean) = mutations.withLock {
        db.withTransaction { dao.timer()?.also { finishRow(it, reset) } }?.let(alarms::cancel)
    }
    suspend fun saveSession(day: Long, id: Long?, name: String) = mutations.withLock {
        db.withTransaction {
            Rules.editable(day, clock.today())
            val clean = Rules.name(name, false).ifEmpty { null }
            if (id == null) dao.insertSession(Session(day = day, name = clean, createdAt = clock.wallMillis()))
            else dao.updateSession(checkSession(id).copy(name = clean))
        }
    }
    suspend fun editSet(id: Long, weight: String, reps: String) = mutations.withLock {
        db.withTransaction {
            val set = requireNotNull(dao.set(id)) { "セットが削除されています" }
            val record = checkRecord(set.recordId)
            dao.updateSet(set.copy(weight = if (record.bodyweight) null else Rules.tenth(weight), reps = Rules.integer(reps)))
        }
    }
    suspend fun editCardio(id: Long, duration: String) = mutations.withLock {
        db.withTransaction { val record = checkRecord(id); require(record.category == "cardio"); dao.updateRecord(record.copy(duration = Rules.integer(duration))) }
    }
    suspend fun delete(kind: String, id: Long) = mutations.withLock {
        val removed = db.withTransaction {
            val timer = dao.timer()
            when (kind) {
                "set" -> dao.set(id)?.let { dao.deleteSet(id); dao.sets(it.recordId).forEachIndexed { i, row -> dao.updateSet(row.copy(order = i)) } }
                "record" -> dao.record(id)?.let { dao.deleteRecord(id); dao.records(it.sessionId).forEachIndexed { i, row -> dao.updateRecord(row.copy(order = i)) } }
                "session" -> dao.deleteSession(id)
                "weight" -> dao.deleteWeight(id)
                "template" -> dao.deleteTemplate(id)
            }
            timer?.takeIf { dao.timer() == null }
        }
        removed?.let(alarms::cancel)
    }
    suspend fun move(kind: String, id: Long, delta: Int) = mutations.withLock {
        db.withTransaction {
            if (kind == "record") {
                val record = checkRecord(id)
                val rows = dao.records(record.sessionId).toMutableList()
                val from = rows.indexOfFirst { it.id == id }; val to = from + delta
                if (to in rows.indices) { rows.add(to, rows.removeAt(from)); rows.forEachIndexed { i, row -> dao.updateRecord(row.copy(order = i)) } }
            } else {
                val set = requireNotNull(dao.set(id)); checkRecord(set.recordId)
                val rows = dao.sets(set.recordId).toMutableList()
                val from = rows.indexOfFirst { it.id == id }; val to = from + delta
                if (to in rows.indices) { rows.add(to, rows.removeAt(from)); rows.forEachIndexed { i, row -> dao.updateSet(row.copy(order = i)) } }
            }
        }
    }
    suspend fun saveExercise(id: Long?, name: String, part: String, cardio: Boolean, bodyweight: Boolean) = mutations.withLock {
        db.withTransaction {
            val clean = Rules.name(name); val key = Rules.nameKey(clean)
            require(dao.duplicate(key, id ?: 0) == 0) { "同じ名前の種目が存在します" }
            require(BodyPart.entries.any { it.code == part } && (part == "cardio") == cardio) { "区分と部位が一致していません" }
            val old = id?.let { requireNotNull(dao.exercise(it)) }
            require(old?.deleted != true) { "種目が削除されています" }
            val value = Exercise(id ?: 0, clean, key, part, if (cardio) "cardio" else "strength", !cardio && bodyweight,
                false, old?.createdAt ?: clock.wallMillis(), clock.wallMillis())
            if (old == null) dao.insertExercise(value) else {
                dao.updateExercise(value)
                if (old.category != value.category) removeTemplateReferences(old.id)
            }
        }
    }
    private suspend fun removeTemplateReferences(id: Long) {
        val templates = dao.affectedTemplates(id)
        dao.removeExerciseFromTemplates(id)
        templates.forEach { template -> dao.template(template)?.items?.sortedBy { it.order }?.forEachIndexed { i, item -> dao.updateItem(item.copy(order = i)) } }
    }
    suspend fun deleteExercise(id: Long) = mutations.withLock {
        db.withTransaction {
            dao.exercise(id)?.let { dao.updateExercise(it.copy(deleted = true, updatedAt = clock.wallMillis())); removeTemplateReferences(id) }
        }
    }
    suspend fun saveWeight(id: Long?, day: Long, text: String) = mutations.withLock {
        db.withTransaction {
            Rules.notFuture(day, clock.today()); val amount = Rules.tenth(text, true)
            if (id == null) dao.insertWeight(BodyWeight(day = day, weight = amount, createdAt = clock.wallMillis()))
            else { val old = requireNotNull(dao.weight(id)); Rules.editable(old.day, clock.today()); require(day == old.day); dao.updateWeight(old.copy(weight = amount)) }
        }
    }
    suspend fun saveTemplate(id: Long?, name: String, items: List<Pair<Long, Int?>>) = mutations.withLock {
        db.withTransaction {
            val clean = Rules.name(name)
            require(id != null || items.isNotEmpty()) { "種目を1件以上追加してください" }
            items.forEach { (exerciseId, count) ->
                val exercise = requireNotNull(dao.exercise(exerciseId))
                require(!exercise.deleted) { "削除された種目が含まれています" }
                require(if (exercise.category == "cardio") count == null else count != null && count in 1..20) { "種目の区分またはセット数を確認してください" }
            }
            val templateId = if (id == null) dao.insertTemplate(Template(name = clean, createdAt = clock.wallMillis())) else {
                val old = requireNotNull(dao.template(id)); dao.updateTemplate(old.template.copy(name = clean)); dao.clearItems(id); id
            }
            dao.insertItems(items.mapIndexed { index, (exercise, count) -> TemplateItem(templateId = templateId, exerciseId = exercise, order = index, setCount = count) })
        }
    }
}
