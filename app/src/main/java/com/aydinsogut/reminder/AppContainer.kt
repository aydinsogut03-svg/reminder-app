package com.aydinsogut.reminder

import android.content.Context
import androidx.room.Room
import com.aydinsogut.reminder.ai.GeminiNano
import com.aydinsogut.reminder.ai.HandwritingRecognizer
import com.aydinsogut.reminder.ai.ReminderUnderstanding
import com.aydinsogut.reminder.alarm.AlarmScheduler
import com.aydinsogut.reminder.alarm.NotificationHelper
import com.aydinsogut.reminder.alarm.ReminderSpeaker
import com.aydinsogut.reminder.data.AppDatabase
import com.aydinsogut.reminder.data.ReminderRepository
import com.aydinsogut.reminder.data.SettingsRepository

/** Basit bağımlılık kabı. Uygulama büyüdüğünde Hilt'e geçiş için tek nokta burasıdır. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "reminders.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    val scheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }

    val notifications: NotificationHelper by lazy { NotificationHelper(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val repository: ReminderRepository by lazy {
        ReminderRepository(appContext, database.reminderDao(), scheduler, notifications, settings, speaker)
    }

    val speaker: ReminderSpeaker by lazy { ReminderSpeaker(appContext) }

    val gemini: GeminiNano by lazy { GeminiNano() }

    val handwriting: HandwritingRecognizer by lazy { HandwritingRecognizer() }

    val understanding: ReminderUnderstanding by lazy { ReminderUnderstanding(gemini, settings) }
}
