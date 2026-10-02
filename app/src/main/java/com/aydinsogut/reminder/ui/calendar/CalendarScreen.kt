package com.aydinsogut.reminder.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.lerp
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.util.toLocalDateTime
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.ui.components.EmptyState
import com.aydinsogut.reminder.ui.components.SwipeableReminderCard
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishLocale
import com.aydinsogut.reminder.util.capitalizeTr
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

private val WeekDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@Composable
fun CalendarScreen(
    onAddForDate: (LocalDate) -> Unit,
    onOpen: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel = viewModel { CalendarViewModel(container.repository, container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val isToday = state.selected == LocalDate.now()
    val scope = rememberCoroutineScope()

    fun saveInk(text: String) {
        val date = state.selected
        scope.launch {
            val saved = viewModel.addFromInk(text, date) ?: return@launch
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "\"${saved.title}\" · ${saved.triggerAt.toLocalDateTime().format(TimeFormats.short)}",
                actionLabel = "Geri al",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoAdd(saved)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Takvim", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (!isToday || YearMonth.from(state.selected) != state.month) {
                    AssistChip(
                        onClick = viewModel::goToToday,
                        label = { Text("Bugün") },
                        leadingIcon = {
                            Icon(Icons.Rounded.EventAvailable, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                        },
                    )
                }
            }
        }

        item(key = "calendar") {
            MonthCard(
                month = state.month,
                selected = state.selected,
                colorsByDate = state.colorsByDate,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onSelect = viewModel::select,
            )
        }

        item(key = "ink") {
            DayInkCard(
                date = state.selected,
                defaultTime = settings.defaultTime,
                onSubmit = ::saveInk,
            )
        }

        item(key = "day-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isToday) "Bugün" else state.selected.dayOfWeek.getDisplayName(TextStyle.FULL, TurkishLocale).capitalizeTr(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = state.selected.format(TimeFormats.dayMonthYear),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                TextButton(onClick = { onAddForDate(state.selected) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ayrıntılı ekle")
                }
            }
        }

        if (state.selectedItems.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "Bu gün için henüz bir şey yok. Yukarıya yazman yeterli.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
                )
            }
        } else {
            items(state.selectedItems, key = { it.id }) { reminder ->
                SwipeableReminderCard(
                    reminder = reminder,
                    onClick = { onOpen(reminder.id) },
                    onToggleDone = { viewModel.toggleDone(reminder) },
                    onDelete = { viewModel.delete(reminder) },
                    showDate = false,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    selected: LocalDate,
    colorsByDate: Map<LocalDate, List<Int>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.045f),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = month.month.getDisplayName(TextStyle.FULL_STANDALONE, TurkishLocale).capitalizeTr(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = month.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = onPrevious, colors = navButtonColors()) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Önceki ay")
                }
                FilledTonalIconButton(onClick = onNext, colors = navButtonColors()) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Sonraki ay")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                WeekDays.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (index >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                label = "month",
            ) { shownMonth ->
                MonthGrid(shownMonth, selected, colorsByDate, onSelect)
            }
        }
    }
}

@Composable
private fun navButtonColors() = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    colorsByDate: Map<LocalDate, List<Int>>,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val offset = month.atDay(1).dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val weeks = (offset + daysInMonth + 6) / 7

    Column {
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - offset + 1
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        DayCell(
                            date = date,
                            isSelected = date == selected,
                            isToday = date == today,
                            dotColors = colorsByDate[date].orEmpty().distinct().take(3),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    dotColors: List<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.primary
        isWeekend -> colors.onSurfaceVariant
        else -> colors.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1.18f)
            .padding(2.dp)
            .clip(shape)
            .background(if (isSelected) colors.primary else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                dotColors.forEach { index ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.onPrimary else ReminderPalette.color(index)),
                    )
                }
            }
        }
    }
}
