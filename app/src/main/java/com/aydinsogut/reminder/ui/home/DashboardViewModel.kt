package com.aydinsogut.reminder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.util.toLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReminderSection(
    val key: String,
    val title: String,
    val items: List<Reminder>,
    val isWarning: Boolean = false,
)

data class DashboardUiState(
    val loaded: Boolean = false,
    val next: Reminder? = null,
    val upcoming: List<ReminderSection> = emptyList(),
    val past: List<ReminderSection> = emptyList(),
    val todayCount: Int = 0,
    val upcomingCount: Int = 0,
    val missedCount: Int = 0,
    val doneCount: Int = 0,
)

class DashboardViewModel(
    private val repository: ReminderRepository,
    settings: SettingsRepository,
) : ViewModel() {
    /** "Kaçırıldı" ve geri sayım güncel kalsın diye dakikada bir tetiklenir. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val state: StateFlow<DashboardUiState> = combine(repository.observeAll(), settings.settings, ticker) { all, prefs, now ->
        buildDashboard(all, now, LocalDate.now(), prefs.showCompleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

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

private fun buildDashboard(all: List<Reminder>, now: Long, today: LocalDate, showCompleted: Boolean): DashboardUiState {
    val missed = mutableListOf<Reminder>()
    val done = mutableListOf<Reminder>()
    val todayItems = mutableListOf<Reminder>()
    val tomorrow = mutableListOf<Reminder>()
    val thisWeek = mutableListOf<Reminder>()
    val later = mutableListOf<Reminder>()

    for (reminder in all) {
        val date = reminder.triggerAt.toLocalDate()
        when {
            reminder.isDone -> done += reminder
            reminder.repeat == RepeatRule.NONE && reminder.triggerAt < now -> missed += reminder
            date <= today -> todayItems += reminder
            date == today.plusDays(1) -> tomorrow += reminder
            date.isBefore(today.plusDays(7)) -> thisWeek += reminder
            else -> later += reminder
        }
    }

    val upcoming = buildList {
        if (todayItems.isNotEmpty()) add(ReminderSection("today", "Bugün", todayItems.sortedBy { it.triggerAt }))
        if (tomorrow.isNotEmpty()) add(ReminderSection("tomorrow", "Yarın", tomorrow.sortedBy { it.triggerAt }))
        if (thisWeek.isNotEmpty()) add(ReminderSection("week", "Bu hafta", thisWeek.sortedBy { it.triggerAt }))
        if (later.isNotEmpty()) add(ReminderSection("later", "Daha sonra", later.sortedBy { it.triggerAt }))
    }
    val past = buildList {
        if (missed.isNotEmpty()) add(ReminderSection("missed", "Kaçırılan", missed.sortedByDescending { it.triggerAt }, isWarning = true))
        if (showCompleted && done.isNotEmpty()) add(ReminderSection("done", "Tamamlanan", done.sortedByDescending { it.triggerAt }))
    }
    val active = todayItems + tomorrow + thisWeek + later

    return DashboardUiState(
        loaded = true,
        next = active.filter { it.triggerAt >= now }.minByOrNull { it.triggerAt },
        upcoming = upcoming,
        past = past,
        todayCount = todayItems.size,
        upcomingCount = active.size,
        missedCount = missed.size,
        doneCount = done.size,
    )
}
