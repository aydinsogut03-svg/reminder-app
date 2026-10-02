package com.aydinsogut.reminder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.ai.GeminiNano
import com.aydinsogut.reminder.ai.NanoStatus
import com.aydinsogut.reminder.alarm.ReminderSpeaker
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val reminders: ReminderRepository,
    private val gemini: GeminiNano,
    private val speaker: ReminderSpeaker,
) : ViewModel() {
    private val _nanoStatus = MutableStateFlow<NanoStatus?>(null)
    val nanoStatus: StateFlow<NanoStatus?> = _nanoStatus.asStateFlow()

    init {
        viewModelScope.launch { _nanoStatus.value = gemini.status() }
    }

    fun downloadGemini() {
        viewModelScope.launch {
            _nanoStatus.value = NanoStatus.DOWNLOADING
            _nanoStatus.value = gemini.download()
        }
    }

    fun testSpeech() {
        viewModelScope.launch { speaker.speak("Hatırlatma. Bu bir deneme. Saat dokuzda annemi ara.") }
    }

    fun setSpeakReminders(value: Boolean) = launch { repository.setSpeakReminders(value) }

    fun setUseGemini(value: Boolean) = launch { repository.setUseGemini(value) }

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(value: ThemeMode) = launch { repository.setThemeMode(value) }

    fun setSnoozeMinutes(value: Int) = launch { repository.setSnoozeMinutes(value) }

    fun setDefaultHour(value: Int) = launch { repository.setDefaultHour(value) }

    fun setShowCompleted(value: Boolean) = launch { repository.setShowCompleted(value) }

    fun setVoiceAutoSave(value: Boolean) = launch { repository.setVoiceAutoSave(value) }

    fun setInkAutoSaveSeconds(value: Int) = launch { repository.setInkAutoSaveSeconds(value) }

    fun setFingerDrawing(value: Boolean) = launch { repository.setFingerDrawing(value) }

    fun setShareSignature(value: Boolean) = launch { repository.setShareSignature(value) }

    fun setShareEmail(value: String) = launch { repository.setShareEmail(value) }

    fun exportAll(onReady: (List<Reminder>) -> Unit) {
        viewModelScope.launch { onReady(reminders.all()) }
    }

    fun clearCompleted(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(reminders.deleteCompleted()) }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
