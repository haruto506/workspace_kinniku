package jp.kinniku.app.data.alarm

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.kinniku.app.MainActivity
import jp.kinniku.app.R
import jp.kinniku.app.data.WorkoutRepository
import jp.kinniku.app.data.db.RestTimer
import jp.kinniku.app.domain.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

@Singleton
class AndroidTime @Inject constructor(@ApplicationContext private val context: Context) : TimeProvider {
    override fun today(): LocalDate = LocalDate.now()
    override fun wallMillis() = System.currentTimeMillis()
    override fun elapsedMillis() = SystemClock.elapsedRealtime()
    override fun bootCount() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
}

@Singleton
class AlarmGateway @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager get() = context.getSystemService(AlarmManager::class.java)
    private fun pending(timer: RestTimer, flags: Int): PendingIntent? = PendingIntent.getBroadcast(context, 1,
        Intent(context, RestAlarmReceiver::class.java).setAction("jp.kinniku.REST")
            .setData(android.net.Uri.parse("kinniku://rest/${timer.token}/${timer.setId}"))
            .putExtra("token", timer.token).putExtra("set", timer.setId), flags or PendingIntent.FLAG_IMMUTABLE)
    fun exactAvailable() = manager.canScheduleExactAlarms()
    fun schedule(timer: RestTimer) {
        val intent = pending(timer, PendingIntent.FLAG_UPDATE_CURRENT)!!
        val at = timer.startedElapsed + timer.target * 1000L
        try {
            if (exactAvailable()) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent)
        } catch (_: SecurityException) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent) }
        } catch (_: RuntimeException) { /* DB remains authoritative; recovered on next foreground. */ }
    }
    fun cancel(timer: RestTimer) { pending(timer, PendingIntent.FLAG_NO_CREATE)?.let { manager.cancel(it); it.cancel() } }
    fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("rest", context.getString(R.string.rest_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
            })
    }
    fun notifyFinished() {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "rest")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.rest_finished))
            .setContentText(context.getString(R.string.rest_next)).setContentIntent(intent)
            .setSilent(true).setAutoCancel(true).build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(1, notification) }
    }
}

@AndroidEntryPoint
class RestAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: WorkoutRepository
    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra("token") || !intent.hasExtra("set")) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8000) { repository.completeTimer(intent.getLongExtra("token", 0), intent.getLongExtra("set", 0), true) } }
            finally { result.finish() }
        }
    }
}
