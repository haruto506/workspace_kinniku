package jp.kinniku.app.ui

import android.app.DatePickerDialog
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import jp.kinniku.app.domain.Rules
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

val Pine = Color(0xFF216953)
val Paper = Color(0xFFF6F8F4)
val Ink = Color(0xFF192E27)
val dateHeading: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPAN)
val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d", Locale.JAPAN)

@Composable
fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable
fun EmptyState(message: String, hint: String = "") {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.FitnessCenter, null, Modifier.size(40.dp), tint = Pine.copy(alpha = .55f))
        Text(message, style = MaterialTheme.typography.titleMedium)
        if (hint.isNotEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
fun SectionTitle(text: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable
fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label -> FilterChip(selected == index, { onSelect(index) }, label = { Text(label) }) }
    }
}
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, decimal: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, modifier, label = { Text(label) }, singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number))
}
@Composable
fun DateButton(date: LocalDate, today: LocalDate, enabled: Boolean = true, label: String = "日付", onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth)
            .apply { datePicker.maxDate = today.atTime(23,59,59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.show()
    }, enabled = enabled) { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("$label: ${date.format(dateHeading)}") }
}
@Composable
fun ConfirmDelete(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("削除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("キャンセル") } })
}
@Composable
fun NameDialog(title: String, initial: String = "", busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("名前（50文字まで）") }, singleLine = true) },
        confirmButton = { TextButton({ onSave(name) }, enabled = !busy) { Text("保存") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("キャンセル") } })
}
@Composable
fun MoveButtons(canUp: Boolean, canDown: Boolean, enabled: Boolean, onMove: (Int) -> Unit) {
    IconButton({ onMove(-1) }, enabled = enabled && canUp) { Icon(Icons.Outlined.ArrowUpward, "上へ移動", Modifier.size(20.dp)) }
    IconButton({ onMove(1) }, enabled = enabled && canDown) { Icon(Icons.Outlined.ArrowDownward, "下へ移動", Modifier.size(20.dp)) }
}

@Composable
fun LineChart(points: List<Pair<Long, Long>>, weight: Boolean = false) {
    if (points.isEmpty()) { EmptyState("対象期間に記録がありません"); return }
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val maxValue = points.maxOf { it.second }.toDouble() / 10
    val step = if (maxValue <= 0) 1.0 else 10.0.pow(floor(log10(maxValue / 5))).let { ceil(maxValue / 5 / it) * it }.coerceAtLeast(1.0)
    val top = step * 5
    val lowDay = points.first().first
    val span = (points.last().first - lowDay).coerceAtLeast(1)
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(98, 116, 107) } }
    val description = points.joinToString("、") { "${LocalDate.ofEpochDay(it.first).format(shortDate)} ${Rules.kg(it.second, weight)} kg" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${LocalDate.ofEpochDay(points[selected].first).format(dateHeading)}  ·  ${Rules.kg(points[selected].second, true)} kg",
            style = MaterialTheme.typography.titleSmall, color = Pine)
        Canvas(Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = description }
            .pointerInput(points) { detectTapGestures { tap ->
                val left = 62.dp.toPx(); val width = size.width - left - 16.dp.toPx()
                selected = points.indices.minBy { abs(left + (points[it].first - lowDay).toFloat() / span * width - tap.x) }
            } }) {
            val left = 62.dp.toPx(); val right = size.width - 16.dp.toPx()
            val bottom = size.height - 30.dp.toPx(); val upper = 14.dp.toPx()
            paint.textSize = 11.dp.toPx()
            for (i in 0..5) {
                val y = bottom - (bottom - upper) * i / 5
                drawLine(Color(0xFFDDE5DE), Offset(left, y), Offset(right, y), 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText((step * i).toLong().toString(), 0f, y + 4.dp.toPx(), paint)
            }
            drawContext.canvas.nativeCanvas.drawText("kg", 0f, 10.dp.toPx(), paint)
            val offsets = points.map { (day, value) -> Offset(if (points.size == 1) (left + right) / 2 else left + (day - lowDay).toFloat() / span * (right - left),
                bottom - (value / 10.0 / top * (bottom - upper)).toFloat()) }
            offsets.zipWithNext().forEach { (a, b) -> drawLine(Pine, a, b, 2.5.dp.toPx()) }
            offsets.forEachIndexed { i, point -> drawCircle(Pine, (if (i == selected) 6 else 3).dp.toPx(), point) }
            drawContext.canvas.nativeCanvas.drawText(LocalDate.ofEpochDay(points.first().first).format(shortDate), left, size.height - 6.dp.toPx(), paint)
            if (points.size > 1) drawContext.canvas.nativeCanvas.drawText(LocalDate.ofEpochDay(points.last().first).format(shortDate), right - 36.dp.toPx(), size.height - 6.dp.toPx(), paint)
        }
        Text("グラフをタップして値を確認", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
