package jp.kinniku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jp.kinniku.app.data.db.*
import jp.kinniku.app.domain.*

@Composable
fun ExercisePicker(state: UiState, onSelect: (Exercise) -> Unit, strengthOnly: Boolean = false) {
    var search by remember { mutableStateOf("") }
    var part by remember { mutableIntStateOf(0) }
    val parts = BodyPart.entries.filter { !strengthOnly || it != BodyPart.CARDIO }
    val query = if (search.isBlank()) "" else Rules.nameKey(search.take(50))
    val rows = state.exercises.filter { !it.exercise.deleted && (!strengthOnly || it.exercise.category == "strength") &&
        (part == 0 || it.exercise.bodyPart == parts[part - 1].code) && (query.isEmpty() || it.exercise.nameKey.contains(query)) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("種目名で検索") }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) })
            ChoiceRow(listOf("すべて") + parts.map { it.label }, part) { part = it }
            Text("よく使う種目から表示", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rows.isEmpty()) item { EmptyState("該当する種目がありません", "種目マスタから新しい種目を追加できます") }
            items(rows, key = { it.exercise.id }) { row ->
                Card(onClick = { onSelect(row.exercise) }, enabled = !state.busy, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(BodyPart.from(row.exercise.bodyPart).label + if (row.exercise.bodyweight) " · 自重" else "", style = MaterialTheme.typography.labelMedium, color = Pine)
                        }
                        Icon(Icons.Outlined.Add, "選択", tint = Pine)
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisesScreen(model: AppViewModel, state: UiState) {
    var editor by remember { mutableStateOf<Exercise?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Exercise?>(null) }
    var search by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("自分のトレーニングを", "種目を変更しても、過去の記録はそのまま残ります") }
        item { Button({ adding = true }, Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Outlined.Add, null); Text("種目を追加") } }
        item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("種目名で検索") }, singleLine = true) }
        items(state.exercises.filter { !it.exercise.deleted && it.exercise.name.contains(search, ignoreCase = true) }.sortedBy { it.exercise.id }, key = { it.exercise.id }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.exercise.name, style = MaterialTheme.typography.titleSmall)
                        Text(BodyPart.from(row.exercise.bodyPart).label + if (row.exercise.bodyweight) " · 自重" else "", color = Pine, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton({ editor = row.exercise }, enabled = !state.busy) { Icon(Icons.Outlined.Edit, "種目を編集") }
                    IconButton({ deleting = row.exercise }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "種目を削除") }
                }
            }
        }
    }
    if (adding || editor != null) ExerciseEditor(model, state, editor) { adding = false; editor = null }
    deleting?.let { row -> ConfirmDelete("種目を削除しますか？", "「${row.name}」を種目一覧とすべてのテンプレートから取り除きます。過去の記録は残ります。", { deleting = null }) {
        model.action { model.repository.deleteExercise(row.id) }
    } }
}

@Composable
private fun ExerciseEditor(model: AppViewModel, state: UiState, initial: Exercise?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var cardio by remember { mutableStateOf(initial?.category == "cardio") }
    var part by remember { mutableStateOf(initial?.bodyPart?.takeIf { it != "cardio" } ?: "chest") }
    var bw by remember { mutableStateOf(initial?.bodyweight ?: false) }
    var confirmCategory by remember { mutableStateOf(false) }
    fun save() = model.action(after = onDismiss) { model.repository.saveExercise(initial?.id, name, if (cardio) "cardio" else part, cardio, bw) }
    AlertDialog(onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text(if (initial == null) "種目を追加" else "種目を編集") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("種目名") }, singleLine = true)
            ChoiceRow(listOf("筋トレ", "有酸素"), if (cardio) 1 else 0) { cardio = it == 1 }
            if (!cardio) {
                Text("部位", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(BodyPart.entries.take(6).map { it.label }, BodyPart.entries.indexOfFirst { it.code == part }) { part = BodyPart.entries[it].code }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(bw, { bw = it }); Text("自重種目（重量を入力しない）") }
            } else Text("部位は「有酸素」になります")
        } },
        confirmButton = { TextButton({ if (initial != null && (initial.category == "cardio") != cardio) confirmCategory = true else save() }, enabled = !state.busy) { Text("保存") } },
        dismissButton = { TextButton(onDismiss, enabled = !state.busy) { Text("キャンセル") } })
    if (confirmCategory) AlertDialog(onDismissRequest = { confirmCategory = false }, title = { Text("区分を変更しますか？") },
        text = { Text("この種目を含むすべてのテンプレートから取り除かれます。過去の記録は変わりません。") },
        confirmButton = { TextButton({ confirmCategory = false; save() }, enabled = !state.busy) { Text("変更して保存") } },
        dismissButton = { TextButton({ confirmCategory = false }) { Text("戻る") } })
}

@Composable
fun TemplatesScreen(model: AppViewModel, state: UiState, sessionId: Long?, onApplied: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<TemplateWithItems?>(null) }
    var deleting by remember { mutableStateOf<TemplateWithItems?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("いつものメニューを、すぐに", "種目・順番・セット数をまとめて保存できます") }
        item { Button({ adding = true }, Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Outlined.Add, null); Text("テンプレートを作成") } }
        if (state.templates.isEmpty()) item { EmptyState("テンプレートがありません") }
        items(state.templates, key = { it.template.id }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.template.name, style = MaterialTheme.typography.titleLarge)
                    Text("${row.items.size} 種目", color = Pine, style = MaterialTheme.typography.labelLarge)
                    row.items.sortedBy { it.order }.forEach { item ->
                        Text((state.exercises.firstOrNull { it.exercise.id == item.exerciseId }?.exercise?.name ?: "削除済み") + (item.setCount?.let { " · ${it}セット" } ?: " · 有酸素"), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Button({ model.applyTemplate(row.template.id, sessionId); onApplied() }, enabled = row.items.isNotEmpty() && !state.busy) { Text("今日に適用") }
                        Spacer(Modifier.weight(1f))
                        IconButton({ editor = row }, enabled = !state.busy) { Icon(Icons.Outlined.Edit, "テンプレートを編集") }
                        IconButton({ deleting = row }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "テンプレートを削除") }
                    }
                }
            }
        }
    }
    if (adding || editor != null) TemplateEditor(model, state, editor) { adding = false; editor = null }
    deleting?.let { row -> ConfirmDelete("テンプレートを削除しますか？", "「${row.template.name}」を削除します。記録には影響しません。", { deleting = null }) { model.delete("template", row.template.id) } }
}

private data class TemplateDraftItem(val exerciseId: Long, val count: String, val cardio: Boolean)
@Composable
private fun TemplateEditor(model: AppViewModel, state: UiState, initial: TemplateWithItems?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.template?.name.orEmpty()) }
    var rows by remember { mutableStateOf<List<TemplateDraftItem>>(initial?.items?.sortedBy { it.order }?.map { TemplateDraftItem(it.exerciseId, it.setCount?.toString().orEmpty(), it.setCount == null) } ?: emptyList()) }
    var picking by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Column {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ if (picking) picking = false else onDismiss() }, enabled = !state.busy) { Icon(Icons.Outlined.Close, "閉じる") }
                    Text(if (picking) "種目を追加" else "テンプレートを編集", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    if (!picking) TextButton({ model.action(after = onDismiss) {
                        model.repository.saveTemplate(initial?.template?.id, name, rows.map { it.exerciseId to if (it.cardio) null else Rules.integer(it.count, 20) })
                    } }, enabled = !state.busy) { Text("保存") }
                }
                if (picking) ExercisePicker(state, onSelect = { rows = rows + TemplateDraftItem(it.id, if (it.category == "cardio") "" else "3", it.category == "cardio"); picking = false })
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("テンプレート名") }, singleLine = true) }
                    rows.forEachIndexed { index, item -> item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(state.exercises.firstOrNull { it.exercise.id == item.exerciseId }?.exercise?.name ?: "削除済み", style = MaterialTheme.typography.titleMedium)
                                if (!item.cardio) NumberField(item.count, { count -> rows = rows.mapIndexed { i, row -> if (i == index) row.copy(count = count) else row } }, "セット数（1〜20）")
                                else Text("有酸素 · 実施時間は記録時に入力")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    MoveButtons(index > 0, index < rows.lastIndex, !state.busy) { delta -> rows = rows.toMutableList().apply { add(index + delta, removeAt(index)) } }
                                    IconButton({ rows = rows.filterIndexed { i, _ -> i != index } }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "テンプレートから種目を削除") }
                                }
                            }
                        }
                    } }
                    item { OutlinedButton({ picking = true }, Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Outlined.Add, null); Text("種目を追加") } }
                }
            }
        }
    }
}
