package com.aydinsogut.reminder.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Hatırlatma zamanı, epoch milisaniye. */
    val triggerAt: Long,
    val repeat: RepeatRule = RepeatRule.NONE,
    /** [com.aydinsogut.reminder.ui.theme.ReminderPalette] içindeki renk sırası. */
    @ColumnInfo(defaultValue = "0") val colorIndex: Int = 0,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
