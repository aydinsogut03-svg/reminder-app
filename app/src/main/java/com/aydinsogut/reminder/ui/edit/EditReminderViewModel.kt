package com.aydinsogut.reminder.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.RepeatRule
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
    val createdAt: Long? = null,
) {
    val isNew: Boolean get() = id == 0L
    val triggerAt: Long get() = LocalDateTime.of(date, time).toEpochMillis()
    val isInPast: Boolean get() = repeat == RepeatRule.NONE && triggerAt <= System.currentTimeMillis()
    val canSave: Boolean get() = title.isNotBlank()
}

class EditReminderViewModel(
    private val repository: ReminderRepository,
    private val id: Long,
    initialDate: LocalDate?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditUiState(
            date = initialDate ?: LocalDate.now(),
            time = if (initialDate != null) LocalTime.of(9, 0) else defaultTime(),
        ),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
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

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.save(
                Reminder(
                    id = s.id,
                    title = s.title.trim(),
                    note = s.note.trim(),
                    triggerAt = s.triggerAt,
                    repeat = s.repeat,
                    isDone = false,
                    createdAt = s.createdAt ?: System.currentTimeMillis(),
                ),
            )
            onSaved()
        }
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
