package com.aydinsogut.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem"),
    LIGHT("Açık"),
    DARK("Koyu"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val snoozeMinutes: Int = 10,
    /** Takvimden ya da sesle saatsiz eklenen hatırlatıcıların saati. */
    val defaultHour: Int = 9,
    val showCompleted: Boolean = true,
    /** Sesle hızlı eklemede zaman anlaşıldıysa düzenleme ekranını atlayıp direkt kaydet. */
    val voiceAutoSave: Boolean = true,
) {
    val defaultTime: LocalTime get() = LocalTime.of(defaultHour, 0)

    companion object {
        val SnoozeOptions = listOf(5, 10, 15, 30, 60)
    }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                themeMode = prefs[THEME]?.let { name -> ThemeMode.entries.find { it.name == name } } ?: defaults.themeMode,
                snoozeMinutes = prefs[SNOOZE] ?: defaults.snoozeMinutes,
                defaultHour = prefs[DEFAULT_HOUR] ?: defaults.defaultHour,
                showCompleted = prefs[SHOW_COMPLETED] ?: defaults.showCompleted,
                voiceAutoSave = prefs[VOICE_AUTO_SAVE] ?: defaults.voiceAutoSave,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[THEME] = value.name }

    suspend fun setSnoozeMinutes(value: Int) = dataStore.edit { it[SNOOZE] = value }

    suspend fun setDefaultHour(value: Int) = dataStore.edit { it[DEFAULT_HOUR] = value.coerceIn(0, 23) }

    suspend fun setShowCompleted(value: Boolean) = dataStore.edit { it[SHOW_COMPLETED] = value }

    suspend fun setVoiceAutoSave(value: Boolean) = dataStore.edit { it[VOICE_AUTO_SAVE] = value }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val SNOOZE = intPreferencesKey("snooze_minutes")
        val DEFAULT_HOUR = intPreferencesKey("default_hour")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
        val VOICE_AUTO_SAVE = booleanPreferencesKey("voice_auto_save")
    }
}
