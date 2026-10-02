package com.aydinsogut.reminder.share

import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.RepeatRule
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Hatırlatıcıları takvim uygulamalarının ve e-postanın anladığı iCalendar (.ics) metnine çevirir. */
object IcsBuilder {
    private val utc: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    const val EVENT_MINUTES = 30L

    fun build(reminders: List<Reminder>, now: Instant = Instant.now()): String {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Hatirlatici//TR",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH",
        )
        for (r in reminders) {
            val start = Instant.ofEpochMilli(r.triggerAt)
            lines += "BEGIN:VEVENT"
            lines += "UID:hatirlatici-${r.id}-${r.createdAt}@aydinsogut.reminder"
            lines += "DTSTAMP:${utc.format(now)}"
            lines += "DTSTART:${utc.format(start)}"
            lines += "DTEND:${utc.format(start.plusSeconds(EVENT_MINUTES * 60))}"
            lines += "SUMMARY:${escape(r.title)}"
            if (r.note.isNotBlank()) lines += "DESCRIPTION:${escape(r.note)}"
            rrule(r.repeat)?.let { lines += "RRULE:$it" }
            lines += "BEGIN:VALARM"
            lines += "ACTION:DISPLAY"
            lines += "DESCRIPTION:${escape(r.title)}"
            lines += "TRIGGER:PT0M"
            lines += "END:VALARM"
            lines += "END:VEVENT"
        }
        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n", postfix = "\r\n") { fold(it) }
    }

    fun rrule(repeat: RepeatRule): String? = when (repeat) {
        RepeatRule.NONE -> null
        RepeatRule.DAILY -> "FREQ=DAILY"
        RepeatRule.WEEKLY -> "FREQ=WEEKLY"
        RepeatRule.MONTHLY -> "FREQ=MONTHLY"
        RepeatRule.YEARLY -> "FREQ=YEARLY"
    }

    fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

    /** RFC 5545: 75 bayttan uzun satırlar, başında boşluk olan devam satırlarına bölünür. */
    private fun fold(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= 75) return line
        val out = StringBuilder()
        var count = 0
        var limit = 75
        for (codePoint in line.codePoints().toArray()) {
            val ch = String(Character.toChars(codePoint))
            val size = ch.toByteArray(Charsets.UTF_8).size
            if (count + size > limit) {
                out.append("\r\n ")
                count = 0
                limit = 74
            }
            out.append(ch)
            count += size
        }
        return out.toString()
    }
}
