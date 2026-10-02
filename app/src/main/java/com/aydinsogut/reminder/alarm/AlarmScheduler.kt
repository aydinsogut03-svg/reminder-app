package com.aydinsogut.reminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aydinsogut.reminder.data.Reminder

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(reminder: Reminder) {
        if (reminder.isDone || reminder.triggerAt <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent(reminder.id, ReminderIntents.ACTION_FIRE))
            return
        }
        setAlarm(reminder.triggerAt, pendingIntent(reminder.id, ReminderIntents.ACTION_FIRE))
    }

    /** Ertelenen alarm ayrı bir PendingIntent kullanır, tekrarlayan serinin alarmını ezmez. */
    fun scheduleSnooze(id: Long, at: Long) {
        setAlarm(at, pendingIntent(id, ReminderIntents.ACTION_SNOOZE_FIRE))
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntent(id, ReminderIntents.ACTION_FIRE))
        alarmManager.cancel(pendingIntent(id, ReminderIntents.ACTION_SNOOZE_FIRE))
    }

    private fun setAlarm(at: Long, pendingIntent: PendingIntent) {
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }

    private fun pendingIntent(id: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderIntents.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
