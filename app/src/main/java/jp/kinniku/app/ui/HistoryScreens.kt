package jp.kinniku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.kinniku.app.data.db.*
import jp.kinniku.app.domain.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(model: AppViewModel, state: UiState, onDay: (Long) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(state.today)) }
    val earliest by model.repository.dao.earliestSession().collectAsStateWithLifecycle(null)
    val flow = remember(month) { model.repository.dao.activeDays(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()) }
    val days by flow.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.verticalScroll(rememberScrollState())) { PageColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ month = month.minusMonths(1) }, enabled = earliest?.let { month.atDay(1).toEpochDay() > it } == true) { Icon(Icons.Outlined.ChevronLeft, "前の月") }
            Text("${month.year}年${month.monthValue}月", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton({ month = month.plusMonths(1) }, enabled = month < YearMonth.from(state.today)) { Icon(Icons.Outlined.ChevronRight, "次の月") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { listOf("日", "月", "火", "水", "木", "金", "土").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                val offset = month.atDay(1).dayOfWeek.value % 7
                val weeks = (offset + month.lengthOfMonth() + 6) / 7
                repeat(weeks) { week -> Row {
                    repeat(7) { dow ->
                        val number = week * 7 + dow - offset + 1
                        val date = if (number in 1..month.lengthOfMonth()) month.atDay(number) else null
                        val active = date?.toEpochDay() in days
                        Box(Modifier.weight(1f).height(58.dp).then(if (date != null) Modifier
                            .semantics { contentDescription = "${date.format(dateHeading)} ${if (active) "記録あり" else "記録なし"}" }
                            .clickable(enabled = date <= state.today) { onDay(date.toEpochDay()) } else Modifier), contentAlignment = Alignment.Center) {
                            if (date != null) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(number.toString(), fontWeight = if (date == state.today) FontWeight.Bold else FontWeight.Normal,
                                    color = if (date > state.today) MaterialTheme.colorScheme.outline.copy(alpha = .45f) else if (date == state.today) Pine else Ink)
                                Box(Modifier.size(5.dp).background(if (active) Pine else androidx.compose.ui.graphics.Color.Transparent, CircleShape))
                            }
                        }
                    }
                } }
            }
        }
        Text("● 記録のある日  ·  日付をタップして確認", color = Pine, style = MaterialTheme.typography.bodySmall)
        if (days.isEmpty()) EmptyState("この月の記録はありません")
    } }
}

@Composable
fun GraphScreen(model: AppViewModel, state: UiState) {
    var exerciseId by remember { mutableStateOf<Long?>(null) }
    var period by remember { mutableIntStateOf(0) }
    var choosing by remember { mutableStateOf(false) }
    val choices = state.exercises.filter { it.uses > 0 }
    val selected = choices.firstOrNull { it.exercise.id == exerciseId } ?: choices.firstOrNull()
    val start = Rules.graphStart(state.today, period).toEpochDay()
    val flow = remember(start, state.today) { model.repository.volumes(start, state.today.toEpochDay()) }
    val volumes by flow.collectAsStateWithLifecycle(emptyList())
    val points = volumes.filter { it.exerciseId == selected?.exercise?.id }.groupBy { it.day }.map { (day, rows) -> day to rows.sumOf { it.value } }.sortedBy { it.first }
    Column(Modifier.verticalScroll(rememberScrollState())) { PageColumn {
        SectionTitle("積み重ねを、見える形に", "同じ種目の総ボリュームを日ごとに表示します")
        OutlinedButton({ choosing = true }, Modifier.fillMaxWidth(), enabled = choices.isNotEmpty()) {
            Text(selected?.exercise?.let { it.name + if (it.deleted) "（削除済み）" else "" } ?: "記録のある種目がありません", Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, null)
        }
        ChoiceRow(listOf("1ヶ月", "3ヶ月", "1年", "全期間"), period) { period = it }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp)) { LineChart(points) } }
        if (selected?.exercise?.category == "cardio") Text("有酸素の記録は総ボリュームの集計対象外です", style = MaterialTheme.typography.bodySmall)
    } }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("種目を選ぶ") },
        text = { LazyColumn { items(choices, key = { it.exercise.id }) { row ->
            TextButton({ exerciseId = row.exercise.id; choosing = false }) { Text(row.exercise.name + if (row.exercise.deleted) "（削除済み）" else "") }
        } } }, confirmButton = { TextButton({ choosing = false }) { Text("閉じる") } })
}

@Composable
fun WeightScreen(model: AppViewModel, state: UiState) {
    var period by remember { mutableIntStateOf(0) }
    var display by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyWeight?>(null) }
    val start = Rules.graphStart(state.today, period).toEpochDay()
    val flow = remember(start, state.today) { model.repository.dao.weights(start, state.today.toEpochDay()) }
    val weights by flow.collectAsStateWithLifecycle(emptyList())
    val points = weights.groupBy { it.day }.map { (day, rows) -> day to Rules.average(rows.map { it.weight }).toLong() }.sortedBy { it.first }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("体の変化を記録", "同じ日の記録は平均してグラフに表示します") }
        item { Button({ adding = true }, Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Outlined.Add, null); Text("体重を記録") } }
        item { ChoiceRow(listOf("グラフ", "一覧"), display) { display = it } }
        item { ChoiceRow(listOf("1ヶ月", "3ヶ月", "1年", "全期間"), period) { period = it } }
        if (display == 0) item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp)) { LineChart(points, true) } } }
        else {
            if (weights.isEmpty()) item { EmptyState("体重の記録がありません") }
            items(weights, key = { it.id }) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(LocalDate.ofEpochDay(row.day).format(dateHeading), style = MaterialTheme.typography.labelMedium)
                            Text("${Rules.kg(row.weight.toLong(), true)} kg", style = MaterialTheme.typography.titleLarge)
                        }
                        if (row.day == state.today.toEpochDay()) IconButton({ editing = row }, enabled = !state.busy) { Icon(Icons.Outlined.Edit, "体重を編集") }
                        IconButton({ model.delete("weight", row.id) }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "体重を削除") }
                    }
                }
            }
        }
    }
    if (adding || editing != null) {
        var date by remember { mutableStateOf(editing?.let { LocalDate.ofEpochDay(it.day) } ?: state.today) }
        var weight by remember { mutableStateOf(editing?.let { Rules.kg(it.weight.toLong(), true) }.orEmpty()) }
        AlertDialog(onDismissRequest = { if (!state.busy) { adding = false; editing = null } }, title = { Text("体重を記録") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateButton(date, state.today, enabled = editing == null) { date = it }
                NumberField(weight, { weight = it }, "体重 (kg)", decimal = true)
                if (date != state.today) Text("過去日の記録は保存後に編集できません。訂正は削除して入れ直してください。", style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton({ model.action(after = { adding = false; editing = null }) { model.repository.saveWeight(editing?.id, date.toEpochDay(), weight) } }, enabled = !state.busy) { Text("保存") } },
            dismissButton = { TextButton({ adding = false; editing = null }, enabled = !state.busy) { Text("キャンセル") } })
    }
}

@Composable
fun SummaryScreen(model: AppViewModel, state: UiState) {
    var monthly by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(state.today) }
    val start = if (monthly) anchor.withDayOfMonth(1) else Rules.weekStart(anchor)
    val end = if (monthly) YearMonth.from(anchor).atEndOfMonth() else start.plusDays(6)
    val earliest by model.repository.dao.earliestSession().collectAsStateWithLifecycle(null)
    val flow = remember(start, end) { model.repository.volumes(start.toEpochDay(), end.toEpochDay()) }
    val points by flow.collectAsStateWithLifecycle(emptyList())
    val total = points.sumOf { it.value }
    Column(Modifier.verticalScroll(rememberScrollState())) { PageColumn {
        ChoiceRow(listOf("週", "月"), if (monthly) 1 else 0) { monthly = it == 1; anchor = state.today }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ anchor = if (monthly) anchor.minusMonths(1) else anchor.minusWeeks(1) }, enabled = earliest?.let { start.toEpochDay() > it } == true) { Icon(Icons.Outlined.ChevronLeft, "前の期間") }
            Text("${start.format(shortDate)} 〜 ${end.format(shortDate)}\n${start.year}年", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton({ anchor = if (monthly) anchor.plusMonths(1) else anchor.plusWeeks(1) }, enabled = end < state.today) { Icon(Icons.Outlined.ChevronRight, "次の期間") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("総ボリューム合計", style = MaterialTheme.typography.labelLarge)
                Text("${Rules.kg(total, true)} kg", style = MaterialTheme.typography.headlineLarge)
            }
        }
        SectionTitle("部位別ボリューム")
        BodyPart.entries.take(6).forEach { part ->
            val value = points.filter { it.part == part.code }.sumOf { it.value }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { Text(part.label, Modifier.weight(1f)); Text("${Rules.kg(value, true)} kg") }
                LinearProgressIndicator(progress = { if (total > 0) (value.toDouble() / total).toFloat() else 0f }, modifier = Modifier.fillMaxWidth(), color = Pine, trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
        }
        if (points.isEmpty()) Text("対象期間に記録がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}
