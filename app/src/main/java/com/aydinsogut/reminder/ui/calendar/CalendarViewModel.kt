package com.aydinsogut.reminder.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.util.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    /** Her gün için o günkü hatırlatıcıların renk sıraları (nokta göstergesi için). */
    val colorsByDate: Map<LocalDate, List<Int>> = emptyMap(),
    val selectedItems: List<Reminder> = emptyList(),
)

class CalendarViewModel(private val repository: ReminderRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    val state: StateFlow<CalendarUiState> =
        combine(month, selected, repository.observeAll()) { month, selected, all ->
            val byDate = all.groupBy { it.triggerAt.toLocalDate() }
            CalendarUiState(
                month = month,
                selected = selected,
                colorsByDate = byDate.mapValues { (_, items) -> items.map { it.colorIndex } },
                selectedItems = byDate[selected].orEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun select(date: LocalDate) {
        selected.value = date
        month.value = YearMonth.from(date)
    }

    fun goToToday() = select(LocalDate.now())

    fun delete(reminder: Reminder) {
        viewModelScope.launch { repository.delete(reminder) }
    }

    /** Kalemle yazılan metni seçili güne hatırlatıcı olarak kaydeder; kaydedileni döndürür (geri almak için). */
    suspend fun addFromInk(text: String, date: LocalDate): Reminder? = repository.addFromText(text, date)

    fun restore(reminder: Reminder) {
        viewModelScope.launch { repository.restore(reminder) }
    }

    fun undoAdd(reminder: Reminder) {
        viewModelScope.launch { repository.delete(reminder) }
    }

    fun toggleDone(reminder: Reminder) {
        viewModelScope.launch { repository.setDone(reminder.id, !reminder.isDone) }
    }
}
