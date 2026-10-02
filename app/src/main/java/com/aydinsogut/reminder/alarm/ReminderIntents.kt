package com.aydinsogut.reminder.alarm

object ReminderIntents {
    const val EXTRA_ID = "com.aydinsogut.reminder.extra.ID"

    const val ACTION_FIRE = "com.aydinsogut.reminder.action.FIRE"
    const val ACTION_SNOOZE_FIRE = "com.aydinsogut.reminder.action.SNOOZE_FIRE"
    const val ACTION_DONE = "com.aydinsogut.reminder.action.DONE"
    const val ACTION_SNOOZE = "com.aydinsogut.reminder.action.SNOOZE"

    /** Uygulamayı yeni hatırlatıcı ekranıyla açar (widget'tan). */
    const val ACTION_ADD = "com.aydinsogut.reminder.action.ADD"

    /** Uygulamayı belirli bir hatırlatıcıyla açar (bildirim ve widget'tan). */
    const val ACTION_OPEN = "com.aydinsogut.reminder.action.OPEN"

    const val SNOOZE_MINUTES = 10L
}
