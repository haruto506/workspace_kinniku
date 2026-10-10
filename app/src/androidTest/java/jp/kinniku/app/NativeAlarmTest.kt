package jp.kinniku.app

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kinniku.app.data.WorkoutRepository
import jp.kinniku.app.data.CommitInput
import jp.kinniku.app.ui.AppViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeAlarmTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun backgroundAlarmRecordsRestAndPostsSilentNotification() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        lateinit var repository: WorkoutRepository
        compose.activityRule.scenario.onActivity { repository = ViewModelProvider(it)[AppViewModel::class.java].repository }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val original = repository.settings.values.first().restSeconds
        val day = LocalDate.now().toEpochDay()
        var sessionId: Long? = null
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            manager.cancelAll()
            repository.settings.saveSeconds(3)
            repository.saveSession(day, null, "アラーム検証")
            sessionId = requireNotNull(repository.dao.lastSession(day)).id
            val exercise = requireNotNull(repository.dao.exercise(1))
            repository.commit(CommitInput(day, sessionId, null, exercise, "60", "10", "", ""))
            val timer = requireNotNull(repository.dao.timer())
            withTimeout(60_000) { repository.timer.first { it == null } }
            assertEquals(3, repository.dao.set(timer.setId)?.rest)
            withTimeout(30_000) { while (manager.activeNotifications.isEmpty()) delay(100) }
            val channel = manager.getNotificationChannel("rest")
            assertNull(channel.sound)
            assertFalse(channel.shouldVibrate())
            assertEquals(1, manager.activeNotifications.size)
        } finally {
            repository.stopTimer(true)
            sessionId?.let { repository.delete("session", it) }
            repository.settings.saveSeconds(original)
            manager.cancelAll()
        }
    }
}
