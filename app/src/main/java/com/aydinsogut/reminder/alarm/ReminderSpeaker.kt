package com.aydinsogut.reminder.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.util.TurkishLocale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Hatırlatmaları Türkçe sesli okur. Telefon sessiz ya da titreşimdeyse okumaz. */
class ReminderSpeaker(private val context: Context) {

    fun canSpeakNow(): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    suspend fun speak(reminder: Reminder): Boolean {
        val text = buildString {
            append("Hatırlatma. ")
            append(reminder.title.trim().trimEnd('.'))
            append('.')
            if (reminder.note.isNotBlank()) {
                append(' ')
                append(reminder.note.trim().take(200))
            }
        }
        return speak(text)
    }

    suspend fun speak(text: String): Boolean = withTimeoutOrNull(30_000L) {
        suspendCancellableCoroutine { cont ->
            var engine: TextToSpeech? = null
            fun finish(result: Boolean) {
                engine?.shutdown()
                if (cont.isActive) cont.resume(result)
            }
            engine = TextToSpeech(context.applicationContext) { status ->
                val tts = engine
                if (status != TextToSpeech.SUCCESS || tts == null) {
                    finish(false)
                    return@TextToSpeech
                }
                tts.language = TurkishLocale
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = finish(true)

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = finish(false)
                    override fun onError(utteranceId: String?, errorCode: Int) = finish(false)
                })
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reminder")
            }
            cont.invokeOnCancellation { engine?.shutdown() }
        }
    } ?: false
}
