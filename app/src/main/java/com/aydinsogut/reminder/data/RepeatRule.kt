package com.aydinsogut.reminder.data

import com.aydinsogut.reminder.util.toEpochMillis
import com.aydinsogut.reminder.util.toLocalDateTime
import java.time.LocalDateTime

enum class RepeatRule(val label: String) {
    NONE("Tekrar yok"),
    DAILY("Her gün"),
    WEEKLY("Her hafta"),
    MONTHLY("Her ay"),
    YEARLY("Her yıl");

    fun next(from: LocalDateTime): LocalDateTime = when (this) {
        NONE -> from
        DAILY -> from.plusDays(1)
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        YEARLY -> from.plusYears(1)
    }
}

/** Tekrarlayan bir hatırlatıcının [now] sonrasındaki ilk zamanı. */
fun Reminder.nextTriggerAfter(now: Long): Long {
    if (repeat == RepeatRule.NONE) return triggerAt
    var next = triggerAt.toLocalDateTime()
    while (next.toEpochMillis() <= now) {
        next = repeat.next(next)
    }
    return next.toEpochMillis()
}
