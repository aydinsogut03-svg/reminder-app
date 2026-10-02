package com.aydinsogut.reminder.alarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationManagerCompat
import com.aydinsogut.reminder.R
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.ui.MainActivity
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.formatReminderTime

class NotificationHelper(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hatırlatıcılar",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Zamanı gelen hatırlatıcı bildirimleri"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(reminder: Reminder) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val requestCode = reminder.id.toInt()
        val openIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .setAction(ReminderIntents.ACTION_OPEN)
                .putExtra(ReminderIntents.EXTRA_ID, reminder.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = reminder.note.ifBlank { formatReminderTime(reminder.triggerAt) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ReminderPalette.color(reminder.colorIndex).toArgb())
            .setContentTitle(reminder.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Tamamlandı", actionIntent(reminder.id, ReminderIntents.ACTION_DONE))
            .addAction(
                0,
                "${ReminderIntents.SNOOZE_MINUTES} dk ertele",
                actionIntent(reminder.id, ReminderIntents.ACTION_SNOOZE),
            )
            .build()

        try {
            manager.notify(requestCode, notification)
        } catch (e: SecurityException) {
            // İzin bildirim gösterilirken geri alınmış olabilir.
        }
    }

    fun cancel(id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    private fun actionIntent(id: Long, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderIntents.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}
