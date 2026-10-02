package com.aydinsogut.reminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.share.ReminderSharing
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.ui.theme.soft
import com.aydinsogut.reminder.util.formatReminderTime

private val WhatsAppGreen = Color(0xFF25D366)

/** Karta basılı tutunca açılan menü: paylaş (WhatsApp, e-posta, diğer), takvime ekle, düzenle, sil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderActionsSheet(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val settings by context.appContainer.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val accent = ReminderPalette.color(reminder.colorIndex)

    fun run(action: () -> Unit) {
        action()
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatReminderTime(reminder.triggerAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Paylaş",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShareTarget(Icons.AutoMirrored.Rounded.Chat, "WhatsApp", WhatsAppGreen) {
                    run { ReminderSharing.shareWhatsApp(context, reminder, settings.shareSignature) }
                }
                ShareTarget(Icons.Rounded.Email, "E-posta", ReminderPalette.color(5)) {
                    run { ReminderSharing.shareEmail(context, reminder, settings.shareSignature, settings.shareEmail) }
                }
                ShareTarget(Icons.Rounded.EventAvailable, "Takvime", ReminderPalette.color(1)) {
                    run { ReminderSharing.addToCalendar(context, reminder) }
                }
                ShareTarget(Icons.Rounded.Share, "Diğer", ReminderPalette.color(4)) {
                    run { ReminderSharing.shareAny(context, reminder, settings.shareSignature) }
                }
            }
            if (onEdit != null || onDelete != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
            }
            if (onEdit != null) {
                SheetRow(Icons.Rounded.Edit, "Düzenle", MaterialTheme.colorScheme.onSurface) { run(onEdit) }
            }
            if (onDelete != null) {
                SheetRow(Icons.Rounded.Delete, "Sil", MaterialTheme.colorScheme.error) { run(onDelete) }
            }
        }
    }
}

@Composable
private fun ShareTarget(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape).background(color.soft(0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
