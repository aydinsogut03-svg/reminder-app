package com.aydinsogut.reminder.share

import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IcsBuilderTest {
    private val start = Instant.parse("2026-10-29T07:00:00Z").toEpochMilli()

    @Test fun buildsEventWithAlarmAndRepeat() {
        val ics = IcsBuilder.build(
            listOf(Reminder(id = 7, title = "Dişçi, kontrol", note = "Satır1\nSatır2", triggerAt = start, repeat = RepeatRule.WEEKLY, createdAt = 1)),
            now = Instant.parse("2026-10-02T00:00:00Z"),
        )
        val lines = ics.split("\r\n")
        assertEquals("BEGIN:VCALENDAR", lines.first())
        assertTrue("DTSTART:20261029T070000Z" in lines)
        assertTrue("DTEND:20261029T073000Z" in lines)
        assertTrue("SUMMARY:Dişçi\\, kontrol" in lines)
        assertTrue("DESCRIPTION:Satır1\\nSatır2" in lines)
        assertTrue("RRULE:FREQ=WEEKLY" in lines)
        assertTrue("BEGIN:VALARM" in lines)
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }

    @Test fun foldsLongLines() {
        val ics = IcsBuilder.build(listOf(Reminder(title = "ş".repeat(80), triggerAt = start)))
        ics.split("\r\n").forEach { assertTrue(it.toByteArray().size <= 75) }
    }
}
