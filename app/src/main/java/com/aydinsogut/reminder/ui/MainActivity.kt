package com.aydinsogut.reminder.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aydinsogut.reminder.alarm.ReminderIntents
import com.aydinsogut.reminder.ui.theme.ReminderTheme

sealed interface LaunchRequest {
    data object NewReminder : LaunchRequest
    data class OpenReminder(val id: Long) : LaunchRequest
}

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchRequest = parse(intent)
        setContent {
            ReminderTheme {
                ReminderNavHost(
                    launchRequest = launchRequest,
                    onLaunchRequestHandled = { launchRequest = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchRequest = parse(intent)
    }

    private fun parse(intent: Intent?): LaunchRequest? = when (intent?.action) {
        ReminderIntents.ACTION_ADD -> LaunchRequest.NewReminder
        ReminderIntents.ACTION_OPEN -> intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L)
            .takeIf { it > 0 }
            ?.let { LaunchRequest.OpenReminder(it) }
        else -> null
    }
}
