package jp.kinniku.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jp.kinniku.app.data.alarm.AlarmGateway
import jp.kinniku.app.data.csv.CsvExporter
import javax.inject.Inject
import kotlinx.coroutines.*

@HiltAndroidApp
class KinnikuApplication : Application() {
    @Inject lateinit var alarms: AlarmGateway
    @Inject lateinit var csv: CsvExporter
    override fun onCreate() {
        super.onCreate()
        alarms.createChannel()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { csv.reap() }
    }
}
