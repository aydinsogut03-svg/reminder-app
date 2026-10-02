package com.aydinsogut.reminder.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aydinsogut.reminder.alarm.ReminderIntents
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.ThemeMode
import com.aydinsogut.reminder.ui.theme.ReminderTheme

sealed interface LaunchRequest {
    /** Yeni hatırlatıcı; [text] paylaşılan metin, [voice] açılır açılmaz mikrofonu başlatır. */
    data class NewReminder(val text: String? = null, val voice: Boolean = false) : LaunchRequest
    data class OpenReminder(val id: Long) : LaunchRequest
}

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchRequest = parse(intent)
        val settingsFlow = appContainer.settings.settings
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            ReminderTheme(darkTheme = dark) {
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
        ReminderIntents.ACTION_ADD -> LaunchRequest.NewReminder()
        ReminderIntents.ACTION_VOICE -> LaunchRequest.NewReminder(voice = true)
        ReminderIntents.ACTION_OPEN -> intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L)
            .takeIf { it > 0 }
            ?.let { LaunchRequest.OpenReminder(it) }
        Intent.ACTION_SEND -> {
            val text = listOfNotNull(
                intent.getStringExtra(Intent.EXTRA_SUBJECT),
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            ).filter { it.isNotBlank() }.distinct().joinToString("\n")
            text.takeIf { it.isNotBlank() }?.let { LaunchRequest.NewReminder(text = it) }
        }
        else -> null
    }
}
