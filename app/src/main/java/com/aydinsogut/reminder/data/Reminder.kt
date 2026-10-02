package com.aydinsogut.reminder.data

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
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
