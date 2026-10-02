package com.aydinsogut.reminder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.ui.theme.Success
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.formatReminderTime
import com.aydinsogut.reminder.util.toLocalDateTime

private val CardShape = RoundedCornerShape(22.dp)

/** Sağa kaydır: tamamla / geri al. Sola kaydır: sil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f })

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onToggleDone()
                state.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val direction = state.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Success
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipe-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardShape)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                val icon = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                    SwipeToDismissBoxValue.StartToEnd -> if (reminder.isDone) Icons.Rounded.Undo else Icons.Rounded.Check
                    SwipeToDismissBoxValue.Settled -> null
                }
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White)
            }
        },
    ) {
        ReminderCard(reminder, onClick, onToggleDone, showDate = showDate, onLongClick = onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val accent = ReminderPalette.color(reminder.colorIndex)
    val overdue = !reminder.isDone &&
        reminder.repeat == RepeatRule.NONE &&
        reminder.triggerAt < System.currentTimeMillis()
    val timeTint = if (overdue) MaterialTheme.colorScheme.error else accent
    val timeText = if (showDate) {
        formatReminderTime(reminder.triggerAt)
    } else {
        reminder.triggerAt.toLocalDateTime().format(TimeFormats.time)
    }

    val container = if (reminder.isDone) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        lerp(MaterialTheme.colorScheme.surface, accent, 0.08f)
    }

    Surface(
        shape = CardShape,
        color = container,
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
                .alpha(if (reminder.isDone) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DoneToggle(done = reminder.isDone, accent = accent, onToggle = onToggleDone)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (reminder.isDone) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(Icons.Rounded.Schedule, if (overdue) "Kaçırıldı · $timeText" else timeText, timeTint)
                    if (reminder.repeat != RepeatRule.NONE) {
                        InfoChip(Icons.Rounded.Repeat, reminder.repeat.label, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneToggle(done: Boolean, accent: Color, onToggle: () -> Unit) {
    val fill by animateColorAsState(if (done) accent else Color.Transparent, label = "done-fill")
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, accent, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Rounded.Check, contentDescription = "Tamamlandı", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}
