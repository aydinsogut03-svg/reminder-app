package com.aydinsogut.reminder.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.ai.ReminderUnderstanding
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.util.ParsedReminder
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
    /** Metin anlaşılırken (Gemini Nano çalışırken) true. */
    val isThinking: Boolean = false,
    val usedGemini: Boolean = false,
    /** Aynı cümleden çıkan diğer hatırlatıcılar; kaydedince bunlar da eklenir. */
    val extraReminders: List<ParsedReminder> = emptyList(),
) {
    val isNew: Boolean get() = id == 0L
    val triggerAt: Long get() = LocalDateTime.of(date, time).toEpochMillis()
    val isInPast: Boolean get() = repeat == RepeatRule.NONE && triggerAt <= System.currentTimeMillis()
    val canSave: Boolean get() = title.isNotBlank()
}

class EditReminderViewModel(
    private val repository: ReminderRepository,
    private val settings: SettingsRepository,
    private val understanding: ReminderUnderstanding,
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
                if (!initialText.isNullOrBlank()) onSpokenText(initialText, quickSave = false) { }
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
     * Sesle söylenen, kalemle yazılan ya da paylaşılan metni anlayıp forma uygular.
     * [quickSave] açıksa ve tüm zamanlar anlaşıldıysa direkt kaydeder ve [onAutoSaved] çağrılır.
     */
    fun onSpokenText(text: String, quickSave: Boolean, onAutoSaved: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isThinking = true) }
            val defaults = settings.current()
            val result = runCatching { understanding.understand(text) }.getOrNull()
            val reminders = result?.reminders.orEmpty().ifEmpty { listOf(ParsedReminder(text.trim(), null)) }
            applyParsed(text, reminders.first())
            _state.update {
                it.copy(
                    isThinking = false,
                    usedGemini = result?.usedGemini == true,
                    extraReminders = reminders.drop(1).filter { r -> r.dateTime != null },
                )
            }
            val allTimed = reminders.all { it.dateTime != null }
            if (quickSave && defaults.voiceAutoSave && allTimed && _state.value.canSave) {
                val saved = persist()
                val count = 1 + _state.value.extraReminders.size
                onAutoSaved(
                    if (count > 1) "$count hatırlatıcı kaydedildi"
                    else "Kaydedildi: ${saved.title}, ${formatReminderTime(saved.triggerAt)}",
                )
            }
        }
    }

    fun removeExtra(index: Int) = _state.update {
        it.copy(extraReminders = it.extraReminders.filterIndexed { i, _ -> i != index })
    }

    private fun applyParsed(text: String, parsed: ParsedReminder) {
        val title = parsed.title.ifBlank { text.trim() }
        val longText = title.length > 60 || text.trim().contains('\n')
        _state.update { current ->
            current.copy(
                title = if (longText) title.take(60).trim() else title,
                note = if (longText) text.trim() else current.note,
                date = parsed.dateTime?.toLocalDate() ?: current.date,
                time = parsed.dateTime?.toLocalTime() ?: current.time,
            )
        }
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
        s.extraReminders.forEach { extra ->
            val dateTime = extra.dateTime ?: return@forEach
            repository.save(Reminder(title = extra.title, triggerAt = dateTime.toEpochMillis(), colorIndex = s.colorIndex))
        }
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
