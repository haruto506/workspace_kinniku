package jp.kinniku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.kinniku.app.data.db.*
import jp.kinniku.app.domain.*
import java.time.LocalDate

@Composable
fun RecordScreen(model: AppViewModel, state: UiState, onPick: (Long?) -> Unit, onTemplate: (Long?) -> Unit) {
    val editable = state.day == state.today.toEpochDay()
    var sessionDialog by remember { mutableStateOf<Pair<Long?, String>?>(null) }
    var confirm by remember { mutableStateOf<Triple<String, Long, String>?>(null) }
    var editSet by remember { mutableStateOf<Pair<ExerciseRecord, WorkoutSet>?>(null) }
    var editCardio by remember { mutableStateOf<ExerciseRecord?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Pine), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(LocalDate.ofEpochDay(state.day).format(dateHeading), color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f))
                    Text(if (editable) "今日の積み重ね" else "トレーニング記録", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Rules.kg(state.dailyVolume, true), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("  kg", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("総ボリューム", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (!editable) item { Text("過去の記録は閲覧・削除のみできます", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (editable && state.sessions.isEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ onPick(null) }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text("種目を追加") }
                OutlinedButton({ onTemplate(null) }, enabled = !state.busy) { Text("テンプレート") }
            }
        }
        if (state.sessions.isEmpty() && state.drafts.isEmpty() && !state.loading) item {
            EmptyState("まだ記録がありません", if (editable) "種目を選んで、最初のセットを記録しましょう" else "この日の記録はありません")
        }
        state.sessions.forEachIndexed { sessionIndex, session ->
            item(key = "session-${session.session.id}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.session.name ?: "セッション${sessionIndex + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (editable) IconButton({ sessionDialog = session.session.id to session.session.name.orEmpty() }, enabled = !state.busy) { Icon(Icons.Outlined.Edit, "セッション名を編集") }
                    IconButton({ confirm = Triple("session", session.session.id, "セッションと配下のすべての記録を削除します。") }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "セッションを削除") }
                }
            }
            session.sortedRecords.forEachIndexed { recordIndex, record ->
                item(key = "record-${record.record.id}") {
                    RecordCard(record, editable, state.busy, recordIndex > 0, recordIndex < session.records.lastIndex,
                        onMove = { delta -> model.action { model.repository.move("record", record.record.id, delta) } },
                        onDelete = { confirm = Triple("record", record.record.id, "「${record.record.name}」と配下のすべてのセットを削除します。") },
                        onEditCardio = { editCardio = record.record }, onAdd = { model.addSet(record.record) },
                        onEditSet = { editSet = record.record to it }, onDeleteSet = { model.delete("set", it) },
                        onMoveSet = { id, delta -> model.action { model.repository.move("set", id, delta) } })
                }
            }
            items(state.drafts.filter { it.sessionId == session.session.id }, key = { it.key }) { DraftCard(model, state, it) }
            if (editable) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onPick(session.session.id) }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Add, null); Text("種目を追加") }
                    TextButton({ onTemplate(session.session.id) }, enabled = !state.busy) { Text("テンプレート") }
                }
            }
        }
        items(state.drafts.filter { it.sessionId == null }, key = { it.key }) { DraftCard(model, state, it) }
        if (editable) item {
            TextButton({ sessionDialog = null to "" }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Text("セッションを追加") }
        }
    }
    sessionDialog?.let { (id, name) -> NameDialog(if (id == null) "セッションを追加" else "セッション名", name, state.busy,
        { sessionDialog = null }) { value -> model.action(after = { sessionDialog = null }) { model.repository.saveSession(state.day, id, value) } } }
    confirm?.let { (kind, id, message) -> ConfirmDelete("記録を削除しますか？", message, { confirm = null }) { model.delete(kind, id) } }
    editSet?.let { (record, set) ->
        var weight by remember(set.id) { mutableStateOf(set.weight?.let { Rules.kg(it.toLong()) }.orEmpty()) }
        var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
        AlertDialog(onDismissRequest = { if (!state.busy) editSet = null }, title = { Text("セットを編集") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!record.bodyweight) NumberField(weight, { weight = it }, "重量 (kg)", decimal = true)
                NumberField(reps, { reps = it }, "回数")
            } }, confirmButton = { TextButton({ model.action(after = { editSet = null }) { model.repository.editSet(set.id, weight, reps) } }, enabled = !state.busy) { Text("保存") } },
            dismissButton = { TextButton({ editSet = null }, enabled = !state.busy) { Text("キャンセル") } })
    }
    editCardio?.let { record ->
        var duration by remember(record.id) { mutableStateOf(record.duration.toString()) }
        AlertDialog(onDismissRequest = { if (!state.busy) editCardio = null }, title = { Text("実施時間を編集") },
            text = { NumberField(duration, { duration = it }, "実施時間 (分)") },
            confirmButton = { TextButton({ model.action(after = { editCardio = null }) { model.repository.editCardio(record.id, duration) } }, enabled = !state.busy) { Text("保存") } },
            dismissButton = { TextButton({ editCardio = null }, enabled = !state.busy) { Text("キャンセル") } })
    }
}

@Composable
private fun RecordCard(row: RecordWithSets, editable: Boolean, busy: Boolean, canUp: Boolean, canDown: Boolean,
    onMove: (Int) -> Unit, onDelete: () -> Unit, onEditCardio: () -> Unit, onAdd: () -> Unit,
    onEditSet: (WorkoutSet) -> Unit, onDeleteSet: (Long) -> Unit, onMoveSet: (Long, Int) -> Unit) {
    val record = row.record
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(BodyPart.from(record.bodyPart).label + if (record.bodyweight) " · 自重" else "", color = Pine, style = MaterialTheme.typography.labelMedium)
            Text(record.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (editable) MoveButtons(canUp, canDown, !busy, onMove)
                IconButton(onDelete, enabled = !busy) { Icon(Icons.Outlined.DeleteOutline, "種目記録を削除", Modifier.size(20.dp)) }
            }
            if (record.category == "cardio") Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${record.duration} 分", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                if (editable) IconButton(onEditCardio, enabled = !busy) { Icon(Icons.Outlined.Edit, "実施時間を編集") }
            } else {
                row.sortedSets.forEachIndexed { index, set ->
                    HorizontalDivider(color = Paper)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(if (record.bodyweight) "${set.reps} 回" else "${Rules.kg(requireNotNull(set.weight).toLong())} kg × ${set.reps} 回", fontWeight = FontWeight.Medium)
                            set.rest?.let { Text("休憩 ${Rules.clock(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        if (editable) IconButton({ onEditSet(set) }, enabled = !busy) { Icon(Icons.Outlined.Edit, "セット${index + 1}を編集", Modifier.size(18.dp)) }
                        IconButton({ onDeleteSet(set.id) }, enabled = !busy) { Icon(Icons.Outlined.Close, "セット${index + 1}を削除", Modifier.size(18.dp)) }
                    }
                    set.memo?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 28.dp)) }
                    if (editable && row.sets.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        MoveButtons(index > 0, index < row.sets.lastIndex, !busy) { onMoveSet(set.id, it) }
                    }
                }
                if (row.sets.isEmpty()) Text("セットがありません", style = MaterialTheme.typography.bodySmall)
                if (editable) TextButton(onAdd, enabled = !busy) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("セットを追加") }
            }
        }
    }
}

@Composable
private fun DraftCard(model: AppViewModel, state: UiState, draft: Draft) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("入力中 · 完了するまで保存されません", color = Pine, style = MaterialTheme.typography.labelSmall)
                    Text(draft.exercise.name, style = MaterialTheme.typography.titleMedium)
                }
                IconButton({ model.removeDraft(draft.key) }, enabled = !state.busy) { Icon(Icons.Outlined.Close, "下書きを削除") }
            }
            if (draft.exercise.category == "cardio") NumberField(draft.duration, { text -> model.updateDraft(draft.key) { it.copy(duration = text) } }, "実施時間 (分)", Modifier.fillMaxWidth())
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!draft.exercise.bodyweight) NumberField(draft.weight, { text -> model.updateDraft(draft.key) { it.copy(weight = text) } }, "重量 (kg)", Modifier.weight(1f), decimal = true)
                    NumberField(draft.reps, { text -> model.updateDraft(draft.key) { it.copy(reps = text) } }, "回数", Modifier.weight(1f))
                }
                OutlinedTextField(draft.memo, { text -> model.updateDraft(draft.key) { it.copy(memo = text) } }, label = { Text("メモ（任意）") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
            }
            if (draft.remaining > 1) Text("残り ${draft.remaining} セット", style = MaterialTheme.typography.labelMedium)
            Button({ model.commit(draft.key) }, enabled = !state.busy && draft.day == state.today.toEpochDay(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("完了")
            }
        }
    }
}
