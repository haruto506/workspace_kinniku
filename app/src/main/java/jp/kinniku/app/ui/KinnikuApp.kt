package jp.kinniku.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KinnikuApp(model: AppViewModel) {
    val state by model.ui.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "today"
    val root = route in listOf("today", "calendar", "graph", "weight")
    var menu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { model.messages.collect { snackbar.showSnackbar(it) } }
    fun go(destination: String, clearDrafts: Boolean = true) {
        if (state.exporting || state.busy) return
        if (clearDrafts) model.discardDrafts()
        nav.navigate(destination) { launchSingleTop = true }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Pine, onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFDCEEE3), onPrimaryContainer = Ink,
        background = Paper, surface = androidx.compose.ui.graphics.Color.White, onSurface = Ink,
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9EEE8))) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
            TopAppBar(title = { Text(when {
                route == "today" -> "筋トレノート"
                route == "calendar" -> "カレンダー"
                route == "graph" -> "種目別グラフ"
                route == "weight" -> "体重"
                route.startsWith("picker") -> "種目を選ぶ"
                route.startsWith("templates") -> "テンプレート"
                route == "exercises" -> "種目マスタ"
                route == "summary" -> "期間サマリー"
                route == "csv" -> "CSV 書き出し"
                route == "settings" -> "設定"
                else -> "過去の記録"
            }) }, navigationIcon = {
                if (!root) IconButton({ if (!state.exporting && !state.busy) nav.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, "戻る") }
            }, actions = {
                if (route == "today") {
                    IconButton({ menu = true }, enabled = !state.exporting && !state.busy) { Icon(Icons.Outlined.MoreVert, "メニュー") }
                    DropdownMenu(menu, { menu = false }) {
                        listOf("種目マスタ" to "exercises", "テンプレート" to "templates/0", "期間サマリー" to "summary", "CSV 書き出し" to "csv", "設定" to "settings").forEach { (name, destination) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { menu = false; go(destination) })
                        }
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper))
        }, bottomBar = {
            Column {
                state.timer?.let { timer ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(20.dp))
                            Text("休憩  ${jp.kinniku.app.domain.Rules.clock(state.elapsed)} / ${jp.kinniku.app.domain.Rules.clock(timer.target)}", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                            TextButton({ model.action { model.repository.stopTimer(false) } }, enabled = !state.busy && !state.exporting) { Text("停止") }
                            IconButton({ model.action { model.repository.stopTimer(true) } }, enabled = !state.busy && !state.exporting) { Icon(Icons.Outlined.RestartAlt, "リセット") }
                        }
                    }
                }
                if (root) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(Triple("today", "今日", Icons.Outlined.FitnessCenter), Triple("calendar", "カレンダー", Icons.Outlined.CalendarMonth),
                        Triple("graph", "グラフ", Icons.Outlined.ShowChart), Triple("weight", "体重", Icons.Outlined.MonitorWeight)).forEach { (destination, label, icon) ->
                        NavigationBarItem(selected = route == destination, onClick = {
                            if (!state.busy && !state.exporting) {
                                model.discardDrafts()
                                if (destination == "today") model.today()
                                nav.navigate(destination) { popUpTo("today"); launchSingleTop = true }
                            }
                        }, enabled = !state.busy && !state.exporting, icon = { Icon(icon, label) }, label = { Text(label) })
                    }
                }
            }
        }) { padding ->
            NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding).fillMaxSize()) {
                composable("today") {
                    LaunchedEffect(state.today) { if (state.day != state.today.toEpochDay()) model.today() }
                    RecordScreen(model, state, onPick = { go("picker/${it ?: 0}", false) }, onTemplate = { go("templates/${it ?: 0}", false) })
                }
                composable("record") { RecordScreen(model, state, { }, { }) }
                composable("picker/{session}") { entry ->
                    val session = entry.arguments?.getString("session")?.toLongOrNull()?.takeIf { it > 0 }
                    ExercisePicker(state, onSelect = { model.addDraft(it, session); nav.popBackStack() })
                }
                composable("calendar") { CalendarScreen(model, state) { day ->
                    model.selectDay(day)
                    if (day == state.today.toEpochDay()) { nav.navigate("today") { popUpTo("today"); launchSingleTop = true } }
                    else go("record")
                } }
                composable("graph") { GraphScreen(model, state) }
                composable("weight") { WeightScreen(model, state) }
                composable("exercises") { ExercisesScreen(model, state) }
                composable("templates/{session}") { entry ->
                    TemplatesScreen(model, state, entry.arguments?.getString("session")?.toLongOrNull()?.takeIf { it > 0 }) {
                        nav.navigate("today") { popUpTo("today"); launchSingleTop = true }
                    }
                }
                composable("summary") { SummaryScreen(model, state) }
                composable("csv") { CsvScreen(model, state) }
                composable("settings") { SettingsScreen(model, state) }
            }
        }
        if (state.notificationPrompt) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                LaunchedEffect(Unit) { model.dismissNotificationPrompt() }
            } else AlertDialog(onDismissRequest = { model.dismissNotificationPrompt() }, title = { Text("休憩の終了を通知") },
                text = { Text("音や振動なしで、通知バーに休憩の終了を表示します。許可しなくても計測と記録は続きます。") },
                confirmButton = { TextButton({ model.dismissNotificationPrompt(); permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("続ける") } },
                dismissButton = { TextButton({ model.dismissNotificationPrompt() }) { Text("今はしない") } })
        }
        state.error?.let { error -> AlertDialog(onDismissRequest = { model.dismissError() }, title = { Text("入力内容を確認してください") },
            text = { Text(error) }, confirmButton = { TextButton({ model.dismissError() }) { Text("閉じる") } }) }
        if (state.exporting) {
            BackHandler { }
            AlertDialog(onDismissRequest = {}, title = { Text("CSV を書き出しています") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(state.exportProgress) } },
                confirmButton = { TextButton({ model.cancelExport() }) { Text("中止") } })
        }
    }
}
