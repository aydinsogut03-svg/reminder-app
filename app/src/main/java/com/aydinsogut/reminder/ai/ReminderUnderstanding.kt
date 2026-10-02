package com.aydinsogut.reminder.ai

import com.aydinsogut.reminder.data.SettingsRepository
import com.aydinsogut.reminder.util.ParsedReminder
import com.aydinsogut.reminder.util.TurkishReminderParser
import java.time.LocalDateTime

/** Ses, kalem veya paylaşımdan gelen metni hatırlatıcılara çevirir: önce Gemini Nano, olmazsa yerel ayrıştırıcı. */
class ReminderUnderstanding(
    private val nano: GeminiNano,
    private val settings: SettingsRepository,
) {
    data class Result(val reminders: List<ParsedReminder>, val usedGemini: Boolean)

    suspend fun understand(text: String): Result {
        val prefs = settings.current()
        val now = LocalDateTime.now()
        if (prefs.useGemini) {
            val fromNano = nano.extractReminders(text, now)
                ?.map { r ->
                    // Saatsiz tarihlerde kullanıcının varsayılan saati, geçmişte kalan bugünkü saatlerde yarın.
                    val dt = r.dateTime
                    when {
                        dt == null -> r
                        !dt.isAfter(now) && dt.toLocalDate() == now.toLocalDate() -> r.copy(dateTime = dt.plusDays(1))
                        else -> r
                    }
                }
            if (!fromNano.isNullOrEmpty()) return Result(fromNano, usedGemini = true)
        }
        val local = TurkishReminderParser.parse(text, now, prefs.defaultTime)
        return Result(listOf(local), usedGemini = false)
    }
}
