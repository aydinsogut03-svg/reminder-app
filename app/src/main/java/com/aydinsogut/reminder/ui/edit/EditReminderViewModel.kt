package com.aydinsogut.reminder.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.util.TurkishReminderParser
import com.aydinsogut.reminder.util.formatReminderTime
import com.aydinsogut.reminder.util.toEpochMillis
import com.aydinsogut.reminder.util.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class EditUiState(
    val id: Long = 0L,
    val title: String = "",
    val note: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val repeat: RepeatRule = RepeatRule.NONE,
    val colorIndex: Int = 0,
    val createdAt: Long? = null,
) {
    val isNew: Boolean get() = id == 0L
    val triggerAt: Long get() = LocalDateTime.of(date, time).toEpochMillis()
    val isInPast: Boolean get() = repeat == RepeatRule.NONE && triggerAt <= System.currentTimeMillis()
    val canSave: Boolean get() = title.isNotBlank()
}

class EditReminderViewModel(
    private val repository: ReminderRepository,
    private val settings: SettingsRepository,
    private val id: Long,
    initialDate: LocalDate?,
    initialText: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditUiState(
            date = initialDate ?: LocalDate.now(),
            time = if (initialDate != null) LocalTime.of(9, 0) else defaultTime(),
        ),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (id == 0L) {
            viewModelScope.launch {
                val defaults = settings.current()
                if (initialDate != null) _state.update { it.copy(time = defaults.defaultTime) }
                if (!initialText.isNullOrBlank()) applyText(initialText, defaults)
            }
        }
        if (id != 0L) {
            viewModelScope.launch {
                repository.get(id)?.let { reminder ->
                    val dateTime = reminder.triggerAt.toLocalDateTime()
                    _state.value = EditUiState(
                        id = reminder.id,
                        title = reminder.title,
                        note = reminder.note,
                        date = dateTime.toLocalDate(),
                        time = dateTime.toLocalTime().withSecond(0).withNano(0),
                        repeat = reminder.repeat,
                        colorIndex = reminder.colorIndex,
                        createdAt = reminder.createdAt,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }

    fun onDateChange(value: LocalDate) = _state.update { it.copy(date = value) }

    fun onTimeChange(value: LocalTime) = _state.update { it.copy(time = value) }

    fun onRepeatChange(value: RepeatRule) = _state.update { it.copy(repeat = value) }

    fun onColorChange(value: Int) = _state.update { it.copy(colorIndex = value) }

    fun onDateTimeChange(value: LocalDateTime) = _state.update {
        it.copy(date = value.toLocalDate(), time = value.toLocalTime().withSecond(0).withNano(0))
    }

    /**
     * Sesle söylenen ya da paylaşılan metni başlık, not ve zamana çevirir.
     * [quickSave] açıksa ve zaman anlaşıldıysa direkt kaydeder ve [onAutoSaved] çağrılır.
     */
    fun onSpokenText(text: String, quickSave: Boolean, onAutoSaved: (String) -> Unit) {
        viewModelScope.launch {
            val defaults = settings.current()
            val understoodTime = applyText(text, defaults)
            if (quickSave && defaults.voiceAutoSave && understoodTime && _state.value.canSave) {
                val saved = persist()
                onAutoSaved("Kaydedildi: ${saved.title}, ${formatReminderTime(saved.triggerAt)}")
            }
        }
    }

    /** Metni forma uygular; zaman bulunduysa true döner. */
    private fun applyText(text: String, defaults: AppSettings): Boolean {
        val parsed = TurkishReminderParser.parse(text.trim(), defaultTime = defaults.defaultTime)
        val title = parsed.title.ifBlank { text.trim() }
        val longText = title.length > 60 || text.contains('\n')
        _state.update { current ->
            current.copy(
                title = if (longText) title.lineSequence().first().take(60).trim() else title,
                note = if (longText) text.trim() else current.note,
                date = parsed.dateTime?.toLocalDate() ?: current.date,
                time = parsed.dateTime?.toLocalTime() ?: current.time,
            )
        }
        return parsed.dateTime != null
    }

    fun save(onSaved: () -> Unit) {
        if (!_state.value.canSave) return
        viewModelScope.launch {
            persist()
            onSaved()
        }
    }

    private suspend fun persist(): Reminder {
        val s = _state.value
        val reminder = Reminder(
            id = s.id,
            title = s.title.trim(),
            note = s.note.trim(),
            triggerAt = s.triggerAt,
            repeat = s.repeat,
            colorIndex = s.colorIndex,
            isDone = false,
            createdAt = s.createdAt ?: System.currentTimeMillis(),
        )
        val savedId = repository.save(reminder)
        return reminder.copy(id = savedId)
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.get(id)?.let { repository.delete(it) }
            onDeleted()
        }
    }

    private companion object {
        fun defaultTime(): LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
}
