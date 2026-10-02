package com.aydinsogut.reminder.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReminderSection(
    val key: String,
    val title: String,
    val items: List<Reminder>,
    val isWarning: Boolean = false,
)

data class ListUiState(
    val loaded: Boolean = false,
    val sections: List<ReminderSection> = emptyList(),
    val todayCount: Int = 0,
    val upcomingCount: Int = 0,
    val doneCount: Int = 0,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

class ReminderListViewModel(private val repository: ReminderRepository) : ViewModel() {
    val state: StateFlow<ListUiState> = repository.observeAll()
        .map { buildState(it, System.currentTimeMillis(), LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    fun toggleDone(reminder: Reminder) {
        viewModelScope.launch { repository.setDone(reminder.id, !reminder.isDone) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch { repository.delete(reminder) }
    }

    fun restore(reminder: Reminder) {
        viewModelScope.launch { repository.restore(reminder) }
    }
}

private fun buildState(all: List<Reminder>, now: Long, today: LocalDate): ListUiState {
    val missed = mutableListOf<Reminder>()
    val todayItems = mutableListOf<Reminder>()
    val tomorrow = mutableListOf<Reminder>()
    val thisWeek = mutableListOf<Reminder>()
    val later = mutableListOf<Reminder>()
    val done = mutableListOf<Reminder>()

    for (reminder in all) {
        val date = reminder.triggerAt.toLocalDate()
        when {
            reminder.isDone -> done += reminder
            reminder.repeat == RepeatRule.NONE && reminder.triggerAt < now -> missed += reminder
            date == today -> todayItems += reminder
            date == today.plusDays(1) -> tomorrow += reminder
            date.isBefore(today.plusDays(7)) -> thisWeek += reminder
            else -> later += reminder
        }
    }

    val sections = buildList {
        if (missed.isNotEmpty()) add(ReminderSection("missed", "Kaçırılan", missed.sortedByDescending { it.triggerAt }, isWarning = true))
        if (todayItems.isNotEmpty()) add(ReminderSection("today", "Bugün", todayItems))
        if (tomorrow.isNotEmpty()) add(ReminderSection("tomorrow", "Yarın", tomorrow))
        if (thisWeek.isNotEmpty()) add(ReminderSection("week", "Bu hafta", thisWeek))
        if (later.isNotEmpty()) add(ReminderSection("later", "Daha sonra", later))
        if (done.isNotEmpty()) add(ReminderSection("done", "Tamamlanan", done.sortedByDescending { it.triggerAt }))
    }

    return ListUiState(
        loaded = true,
        sections = sections,
        todayCount = all.count { !it.isDone && it.triggerAt.toLocalDate() == today },
        upcomingCount = all.count { !it.isDone && it.triggerAt >= now },
        doneCount = done.size,
    )
}
