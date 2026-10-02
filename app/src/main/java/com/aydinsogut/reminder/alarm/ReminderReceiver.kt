package com.aydinsogut.reminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aydinsogut.reminder.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarm çalması ve bildirim düğmeleri (tamamlandı, ertele) buraya gelir. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L)
        if (id < 0) return
        val repository = context.appContainer.repository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderIntents.ACTION_FIRE,
                    ReminderIntents.ACTION_SNOOZE_FIRE -> repository.onAlarmFired(id)
                    ReminderIntents.ACTION_DONE -> repository.completeFromNotification(id)
                    ReminderIntents.ACTION_SNOOZE -> repository.snooze(id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
