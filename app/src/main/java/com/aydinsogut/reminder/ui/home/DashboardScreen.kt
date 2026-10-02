package com.aydinsogut.reminder.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.ui.components.SwipeableReminderCard
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.ui.theme.soft
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishLocale
import com.aydinsogut.reminder.util.capitalizeTr
import com.aydinsogut.reminder.util.formatReminderTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private const val TAB_UPCOMING = 0
private const val TAB_PAST = 1

/** Ana sayfa: sıradaki uyarı, hızlı ekleme ve yaklaşan / geçmiş uyarılar. */
@Composable
fun DashboardScreen(
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onVoice: () -> Unit,
    onInk: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel = viewModel { DashboardViewModel(container.repository, container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_UPCOMING) }
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

    val sections = if (tab == TAB_UPCOMING) state.upcoming else state.past

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { Greeting() }
        item(key = "next") { NextCard(state.next, state.todayCount, onOpen) }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
                QuickAction(Icons.Rounded.Draw, "Kalemle", ReminderPalette.color(2), onInk, Modifier.weight(1f))
                QuickAction(Icons.Rounded.Mic, "Sesle", ReminderPalette.color(1), onVoice, Modifier.weight(1f))
                QuickAction(Icons.Rounded.Add, "Yeni", ReminderPalette.color(0), onAdd, Modifier.weight(1f))
            }
        }
        item(key = "tabs") {
            Segmented(
                selected = tab,
                upcomingCount = state.upcomingCount,
                pastCount = state.past.sumOf { it.items.size },
                missedCount = state.missedCount,
                onSelect = { tab = it },
            )
        }
        if (tab == TAB_UPCOMING && state.missedCount > 0) {
            item(key = "missed-banner") {
                MissedBanner(state.missedCount) { tab = TAB_PAST }
            }
        }
        if (state.loaded && sections.isEmpty()) {
            item(key = "empty-$tab") {
                Text(
                    text = if (tab == TAB_UPCOMING) {
                        "Yaklaşan uyarı yok. Yukarıdan kalemle, sesle ya da elle ekleyebilirsin."
                    } else {
                        "Henüz geçmiş uyarı yok."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }
        }
        sections.forEach { section ->
            item(key = "section-$tab-${section.key}") { SectionHeader(section) }
            items(section.items, key = { "$tab-${it.id}" }) { reminder ->
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
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
    Column(Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
        Text(
            text = LocalDate.now().format(TimeFormats.dayTitle).capitalizeTr(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = greeting, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun NextCard(next: Reminder?, todayCount: Int, onOpen: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = next?.let { ReminderPalette.color(it.colorIndex) } ?: colors.primary
    val brush = Brush.linearGradient(
        listOf(
            lerp(colors.surface, accent, 0.16f),
            lerp(colors.surface, ReminderPalette.color(4), 0.08f),
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .then(if (next != null) Modifier.clickable { onOpen(next.id) } else Modifier),
    ) {
        // Köşede yumuşak dekor daireleri.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(accent.soft(0.10f)),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-50).dp, y = 30.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(ReminderPalette.color(1).soft(0.10f)),
        )
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sıradaki", style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Spacer(Modifier.height(12.dp))
            if (next == null) {
                Text("Yaklaşan uyarı yok", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text("Keyfine bak", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                Text(
                    next.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${formatReminderTime(next.triggerAt)} · ${timeUntil(next.triggerAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (todayCount > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Bugün $todayCount uyarın var",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = color.soft(0.10f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.soft(0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Segmented(
    selected: Int,
    upcomingCount: Int,
    pastCount: Int,
    missedCount: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        SegmentItem("Yaklaşan", upcomingCount, Icons.Rounded.NotificationsActive, selected == TAB_UPCOMING, false, Modifier.weight(1f)) {
            onSelect(TAB_UPCOMING)
        }
        SegmentItem("Geçmiş", pastCount, Icons.Rounded.History, selected == TAB_PAST, missedCount > 0, Modifier.weight(1f)) {
            onSelect(TAB_PAST)
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    alert: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) colors.surface else Color.Transparent, label = "segment")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        val badge = if (alert) colors.error else colors.primary
        Box(
            Modifier
                .clip(CircleShape)
                .background(badge.soft(if (selected || alert) 0.14f else 0.08f))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = badge)
        }
    }
}

@Composable
private fun MissedBanner(count: Int, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(error.soft(0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = error, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "$count kaçırılan uyarı var",
            style = MaterialTheme.typography.bodyMedium,
            color = error,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = error)
    }
}

@Composable
private fun SectionHeader(section: ReminderSection) {
    val color = if (section.isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(section.title.uppercase(TurkishLocale), style = MaterialTheme.typography.labelMedium, color = color)
        Spacer(Modifier.width(6.dp))
        Text("· ${section.items.size}", style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
    }
}

private fun timeUntil(triggerAt: Long): String {
    val minutes = ((triggerAt - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "şimdi"
        minutes < 60 -> "$minutes dk sonra"
        minutes < 24 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "$h sa sonra" else "$h sa $m dk sonra"
        }
        else -> "${minutes / (24 * 60)} gün sonra"
    }
}
