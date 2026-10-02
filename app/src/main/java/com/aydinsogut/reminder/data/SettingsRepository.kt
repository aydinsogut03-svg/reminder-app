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
    /** Zamanı gelen hatırlatmayı Türkçe sesli okur. */
    val speakReminders: Boolean = true,
    /** Uygunsa cümleleri Gemini Nano ile (cihaz içinde) anlar. */
    val useGemini: Boolean = true,
    /** Kalemle yazdıktan sonra kendiliğinden kaydetmeden önce beklenen saniye; 0 = kapalı (✓ ile kaydet). */
    val inkAutoSaveSeconds: Int = 2,
    /** Paylaşılan metnin sonuna "Hatırlatıcı ile paylaşıldı" eklenir. */
    val shareSignature: Boolean = true,
    /** E-postayla paylaşırken alıcı olarak önceden yazılan adres (boşsa sorulur). */
    val shareEmail: String = "",
) {
    val defaultTime: LocalTime get() = LocalTime.of(defaultHour, 0)

    companion object {
        val SnoozeOptions = listOf(5, 10, 15, 30, 60)
        val InkAutoSaveOptions = listOf(0, 1, 2, 3, 5)
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
                speakReminders = prefs[SPEAK_REMINDERS] ?: defaults.speakReminders,
                useGemini = prefs[USE_GEMINI] ?: defaults.useGemini,
                inkAutoSaveSeconds = prefs[INK_AUTO_SAVE] ?: defaults.inkAutoSaveSeconds,
                shareSignature = prefs[SHARE_SIGNATURE] ?: defaults.shareSignature,
                shareEmail = prefs[SHARE_EMAIL] ?: defaults.shareEmail,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[THEME] = value.name }

    suspend fun setSnoozeMinutes(value: Int) = dataStore.edit { it[SNOOZE] = value }

    suspend fun setDefaultHour(value: Int) = dataStore.edit { it[DEFAULT_HOUR] = value.coerceIn(0, 23) }

    suspend fun setShowCompleted(value: Boolean) = dataStore.edit { it[SHOW_COMPLETED] = value }

    suspend fun setVoiceAutoSave(value: Boolean) = dataStore.edit { it[VOICE_AUTO_SAVE] = value }

    suspend fun setSpeakReminders(value: Boolean) = dataStore.edit { it[SPEAK_REMINDERS] = value }

    suspend fun setUseGemini(value: Boolean) = dataStore.edit { it[USE_GEMINI] = value }

    suspend fun setInkAutoSaveSeconds(value: Int) = dataStore.edit { it[INK_AUTO_SAVE] = value.coerceIn(0, 10) }

    suspend fun setShareSignature(value: Boolean) = dataStore.edit { it[SHARE_SIGNATURE] = value }

    suspend fun setShareEmail(value: String) = dataStore.edit { it[SHARE_EMAIL] = value.trim() }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val SNOOZE = intPreferencesKey("snooze_minutes")
        val DEFAULT_HOUR = intPreferencesKey("default_hour")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
        val VOICE_AUTO_SAVE = booleanPreferencesKey("voice_auto_save")
        val SPEAK_REMINDERS = booleanPreferencesKey("speak_reminders")
        val USE_GEMINI = booleanPreferencesKey("use_gemini")
        val INK_AUTO_SAVE = intPreferencesKey("ink_auto_save_seconds")
        val SHARE_SIGNATURE = booleanPreferencesKey("share_signature")
        val SHARE_EMAIL = stringPreferencesKey("share_email")
    }
}
