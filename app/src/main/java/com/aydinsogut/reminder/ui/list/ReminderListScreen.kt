package com.aydinsogut.reminder.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.ui.components.ReminderRow

@Composable
fun ReminderListScreen(
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = LocalContext.current.appContainer.repository
    val viewModel = viewModel { ReminderListViewModel(repository) }
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    if (reminders.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Henüz hatırlatıcı yok", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sağ alttaki + düğmesiyle ilk hatırlatıcını ekle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    val now = System.currentTimeMillis()
    val (upcoming, past) = reminders.partition {
        !it.isDone && (it.triggerAt >= now || it.repeat != RepeatRule.NONE)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (upcoming.isNotEmpty()) {
            item(key = "header-upcoming") { SectionHeader("Yaklaşan") }
            items(upcoming, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onClick = { onOpen(reminder.id) },
                    onToggleDone = { viewModel.toggleDone(reminder) },
                )
            }
        }
        if (past.isNotEmpty()) {
            item(key = "header-past") { SectionHeader("Geçmiş ve tamamlanan") }
            items(past.sortedByDescending { it.triggerAt }, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onClick = { onOpen(reminder.id) },
                    onToggleDone = { viewModel.toggleDone(reminder) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}
