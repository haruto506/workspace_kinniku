package jp.kinniku.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.kinniku.app.domain.Rules
import java.time.LocalDate

@Composable
fun SettingsScreen(model: AppViewModel, state: UiState) {
    var seconds by remember(state.settings.restSeconds) { mutableStateOf(state.settings.restSeconds.toString()) }
    val context = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState())) { PageColumn {
        SectionTitle("休憩タイマー", "セット完了時に、自動で計測を始めます")
        NumberField(seconds, { seconds = it }, "目安の秒数（1〜3600秒）", Modifier.fillMaxWidth())
        Button({ model.action {
            model.repository.settings.saveSeconds(Rules.integer(seconds, 3600)); model.messages.emit("次回のタイマーから適用します")
        } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("設定を保存") }
        Text("変更は次のセット完了から反映されます。計測中のタイマーは変わりません。", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        SectionTitle("通知")
        Text(if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "通知は許可されています。音や振動は使用しません。" else "通知が許可されていません。計測と休憩時間の記録は通常どおり行われます。")
        OutlinedButton({
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            runCatching { context.startActivity(intent) }.onFailure {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
            }
        }) { Text("端末の通知設定を開く") }
        if (!model.repository.alarms.exactAvailable()) Text("正確なアラームが利用できないため、終了通知が遅れる場合があります。")
        HorizontalDivider()
        Text("筋トレノート 1.0.0", style = MaterialTheme.typography.titleSmall)
        Text("記録はこの端末内に保存されます。\n機種変更やアンインストールの前に、CSV を書き出してください。CSV の読み込み機能はありません。", style = MaterialTheme.typography.bodySmall)
    } }
}

@Composable
fun CsvScreen(model: AppViewModel, state: UiState) {
    var ranged by remember { mutableStateOf(false) }
    val earliestSession by model.repository.dao.earliestSession().collectAsStateWithLifecycle(null)
    val earliestWeight by model.repository.dao.earliestWeight().collectAsStateWithLifecycle(null)
    val earliest = listOfNotNull(earliestSession, earliestWeight).minOrNull() ?: state.today.toEpochDay()
    var start by remember(earliest) { mutableStateOf(LocalDate.ofEpochDay(earliest)) }
    var end by remember { mutableStateOf(state.today) }
    LaunchedEffect(Unit) { model.reapCsv() }
    Column(Modifier.verticalScroll(rememberScrollState())) { PageColumn {
        SectionTitle("記録を手元に残す", "筋トレ・有酸素・体重を3つの CSV に書き出します")
        ChoiceRow(listOf("全データ", "期間を指定"), if (ranged) 1 else 0) { ranged = it == 1 }
        if (ranged) {
            DateButton(start, state.today, label = "開始日") { start = it }
            DateButton(end, state.today, label = "終了日") { end = it }
            if (start > end) Text("開始日は終了日以前にしてください", color = MaterialTheme.colorScheme.error)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("保存先: ダウンロード", style = MaterialTheme.typography.titleMedium)
                Text("UTF-8（BOM付き）・CSV形式\nデータがない種類もヘッダーのみのファイルを保存します。")
            }
        }
        Button({ model.export(if (ranged) start.toEpochDay() else Long.MIN_VALUE, if (ranged) end.toEpochDay() else Long.MAX_VALUE) },
            enabled = !state.busy && !state.exporting && (!ranged || start <= end), modifier = Modifier.fillMaxWidth()) { Text("CSV を書き出す") }
        state.exportResults.forEach { Text(it, color = if (it.startsWith("失敗")) MaterialTheme.colorScheme.error else Pine, style = MaterialTheme.typography.bodyMedium) }
    } }
}
