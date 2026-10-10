package jp.kinniku.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("settings")
data class Settings(val restSeconds: Int = 90, val notificationAsked: Boolean = false)
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val seconds = intPreferencesKey("rest_seconds")
    private val asked = booleanPreferencesKey("notification_asked")
    val values = context.settings.data.map { Settings(it[seconds] ?: 90, it[asked] ?: false) }
    suspend fun saveSeconds(value: Int) { require(value in 1..3600); context.settings.edit { it[seconds] = value } }
    suspend fun markAsked() { context.settings.edit { it[asked] = true } }
}
