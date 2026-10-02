package com.aydinsogut.reminder.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.ui.components.EmptyState
import com.aydinsogut.reminder.ui.components.SwipeableReminderCard
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.capitalizeTr
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ReminderListScreen(
    onOpen: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val repository = LocalContext.current.appContainer.repository
    val viewModel = viewModel { ReminderListViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(reminder: Reminder) {
        viewModel.delete(reminder)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "\"${reminder.title}\" silindi",
                actionLabel = "Geri al",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(reminder)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { Greeting() }
        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(Icons.Rounded.Today, state.todayCount, "Bugün", ReminderPalette.color(0), Modifier.weight(1f))
                StatCard(Icons.Rounded.AlarmOn, state.upcomingCount, "Yaklaşan", ReminderPalette.color(1), Modifier.weight(1f))
                StatCard(Icons.Rounded.TaskAlt, state.doneCount, "Biten", ReminderPalette.color(2), Modifier.weight(1f))
            }
        }

        if (state.loaded && state.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Henüz hatırlatıcı yok",
                    message = "Aşağıdaki \"Yeni\" düğmesiyle ilk hatırlatıcını ekle. Takvimden bir güne not da düşebilirsin.",
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }

        state.sections.forEach { section ->
            item(key = "section-${section.key}") {
                SectionHeader(section.title, section.items.size, section.isWarning)
            }
            items(section.items, key = { it.id }) { reminder ->
                SwipeableReminderCard(
                    reminder = reminder,
                    onClick = { onOpen(reminder.id) },
                    onToggleDone = { viewModel.toggleDone(reminder) },
                    onDelete = { deleteWithUndo(reminder) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun Greeting() {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
    Column(Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)) {
        Text(
            text = LocalDate.now().format(TimeFormats.dayTitle).capitalizeTr(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = greeting, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun StatCard(icon: ImageVector, count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.13f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, isWarning: Boolean) {
    val color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        ) {
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
