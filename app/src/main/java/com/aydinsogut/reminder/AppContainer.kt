package com.aydinsogut.reminder

import android.content.Context
import androidx.room.Room
import com.aydinsogut.reminder.alarm.AlarmScheduler
import com.aydinsogut.reminder.alarm.NotificationHelper
import com.aydinsogut.reminder.data.AppDatabase
import com.aydinsogut.reminder.data.ReminderRepository

/** Basit bağımlılık kabı. Uygulama büyüdüğünde Hilt'e geçiş için tek nokta burasıdır. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "reminders.db").build()
    }

    val scheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }

    val notifications: NotificationHelper by lazy { NotificationHelper(appContext) }

    val repository: ReminderRepository by lazy {
        ReminderRepository(appContext, database.reminderDao(), scheduler, notifications)
    }
}
