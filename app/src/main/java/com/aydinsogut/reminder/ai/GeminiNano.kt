package com.aydinsogut.reminder.ai

import com.aydinsogut.reminder.util.ParsedReminder
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishLocale
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class NanoStatus(val label: String) {
    AVAILABLE("Hazır, cihaz içinde çalışıyor"),
    DOWNLOADABLE("Model indirilebilir"),
    DOWNLOADING("Model indiriliyor…"),
    UNAVAILABLE("Bu cihazda kullanılamıyor"),
}

/**
 * Gemini Nano (ML Kit GenAI Prompt API) ile serbest metinden hatırlatıcı çıkarır.
 * Tamamen cihaz içinde çalışır, internet veya API anahtarı gerekmez.
 */
class GeminiNano {
    private val model: GenerativeModel? by lazy { runCatching { Generation.getClient() }.getOrNull() }

    suspend fun status(): NanoStatus {
        val client = model ?: return NanoStatus.UNAVAILABLE
        return runCatching {
            when (client.checkStatus()) {
                FeatureStatus.AVAILABLE -> NanoStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> NanoStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> NanoStatus.DOWNLOADING
                else -> NanoStatus.UNAVAILABLE
            }
        }.getOrDefault(NanoStatus.UNAVAILABLE)
    }

    /** Modeli indirir ve son durumu döndürür. */
    suspend fun download(): NanoStatus {
        val client = model ?: return NanoStatus.UNAVAILABLE
        runCatching { client.download().collect { } }
        return status()
    }

    /** Metindeki hatırlatıcıları çıkarır; model hazır değilse ya da cevap anlaşılmazsa null. */
    suspend fun extractReminders(text: String, now: LocalDateTime = LocalDateTime.now()): List<ParsedReminder>? {
        val client = model ?: return null
        if (status() != NanoStatus.AVAILABLE) return null
        val prompt = buildPrompt(text, now)
        val output = runCatching { client.generateContent(prompt).candidates.firstOrNull()?.text }.getOrNull()
            ?: return null
        return parseOutput(output).takeIf { it.isNotEmpty() }
    }

    companion object {
        private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
        private val dayName = DateTimeFormatter.ofPattern("EEEE", TurkishLocale)

        fun buildPrompt(text: String, now: LocalDateTime): String = """
            Sen bir hatırlatıcı asistanısın. Bugün ${now.format(isoDate)} ${now.format(dayName)}, saat ${now.format(TimeFormats.time)}.
            Aşağıdaki metindeki her yapılacak işi ayrı bir hatırlatıcı olarak çıkar.
            Her hatırlatıcı için yalnızca şu biçimde tek bir satır yaz:
            BAŞLIK | YYYY-AA-GG | SS:DD
            Başlık kısa ve Türkçe olsun, tarih ve saat ifadelerini başlığa yazma.
            Metinde tarih yoksa tarih alanını, saat yoksa saat alanını boş bırak.
            Başka hiçbir açıklama yazma.

            Metin: $text
        """.trimIndent()

        /** "Başlık | 2026-10-03 | 09:00" satırlarını okur. */
        fun parseOutput(output: String): List<ParsedReminder> =
            output.lineSequence()
                .map { it.trim().trimStart('-', '*', '•', ' ') }
                .filter { it.contains('|') }
                .mapNotNull { line ->
                    val parts = line.split('|').map { it.trim() }
                    val title = parts.getOrNull(0)?.trim('"', ' ').orEmpty()
                    if (title.isBlank() || title.equals("BAŞLIK", ignoreCase = true)) return@mapNotNull null
                    val date = parts.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val time = parts.getOrNull(2)?.let { raw ->
                        Regex("(\\d{1,2})[:.](\\d{2})").find(raw)?.let { m ->
                            val h = m.groupValues[1].toInt()
                            val min = m.groupValues[2].toInt()
                            if (h in 0..23 && min in 0..59) LocalTime.of(h, min) else null
                        }
                    }
                    val dateTime = when {
                        date != null -> LocalDateTime.of(date, time ?: LocalTime.of(9, 0))
                        time != null -> LocalDateTime.of(LocalDate.now(), time)
                        else -> null
                    }
                    ParsedReminder(title.replaceFirstChar { it.titlecase(TurkishLocale) }, dateTime)
                }
                .toList()
    }
}
