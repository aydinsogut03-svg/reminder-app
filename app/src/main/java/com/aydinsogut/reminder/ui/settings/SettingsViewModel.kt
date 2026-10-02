package com.aydinsogut.reminder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(value: ThemeMode) = launch { repository.setThemeMode(value) }

    fun setSnoozeMinutes(value: Int) = launch { repository.setSnoozeMinutes(value) }

    fun setDefaultHour(value: Int) = launch { repository.setDefaultHour(value) }

    fun setShowCompleted(value: Boolean) = launch { repository.setShowCompleted(value) }

    fun setVoiceAutoSave(value: Boolean) = launch { repository.setVoiceAutoSave(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
