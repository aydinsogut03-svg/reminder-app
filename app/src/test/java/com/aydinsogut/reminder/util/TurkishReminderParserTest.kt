package com.aydinsogut.reminder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TurkishReminderParserTest {
    // 2 Ekim 2026 Cuma, 11:00
    private val now = LocalDateTime.of(2026, 10, 2, 11, 0)

    private fun parse(text: String) = TurkishReminderParser.parse(text, now)

    @Test fun tomorrowWithHour() {
        val r = parse("Yarın saat 9'da annemi aramayı hatırlat")
        assertEquals("Annemi aramayı", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 3, 9, 0), r.dateTime)
    }

    @Test fun eveningPeriodMakesPm() {
        val r = parse("cuma akşam 8'de toplantı")
        assertEquals("Toplantı", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 9, 20, 0), r.dateTime)
    }

    @Test fun relativeHours() {
        val r = parse("2 saat sonra ilaç iç")
        assertEquals("İlaç iç", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 2, 13, 0), r.dateTime)
    }

    @Test fun relativeMinutesInWords() {
        val r = parse("on beş dakika sonra çamaşırları as")
        assertEquals("Çamaşırları as", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 2, 11, 15), r.dateTime)
    }

    @Test fun halfHour() {
        assertEquals(LocalDateTime.of(2026, 10, 2, 11, 30), parse("yarım saat sonra fırını kapat").dateTime)
    }

    @Test fun monthDateWithClock() {
        val r = parse("15 Ekim 14:30 diş hekimi")
        assertEquals("Diş hekimi", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 15, 14, 30), r.dateTime)
    }

    @Test fun pastMonthDateRollsToNextYear() {
        assertEquals(LocalDateTime.of(2027, 3, 1, 9, 0), parse("1 mart kira").dateTime)
    }

    @Test fun timeOnlyAlreadyPassedMovesToTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 3, 10, 0), parse("saat 10 spor").dateTime)
    }

    @Test fun bareAfternoonHour() {
        val r = parse("3'te Ahmet'i ara")
        assertEquals("Ahmet'i ara", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 2, 15, 0), r.dateTime)
    }

    @Test fun halfPast() {
        assertEquals(LocalDateTime.of(2026, 10, 3, 9, 30), parse("yarın sabah saat dokuz buçukta otobüs").dateTime)
    }

    @Test fun thisEvening() {
        val r = parse("bu akşam ekmek al")
        assertEquals("Ekmek al", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 2, 20, 0), r.dateTime)
    }

    @Test fun nextWeekWeekday() {
        assertEquals(LocalDateTime.of(2026, 10, 7, 9, 0), parse("haftaya çarşamba fatura öde").dateTime)
    }

    @Test fun mondayWithSuffix() {
        assertEquals(LocalDateTime.of(2026, 10, 5, 9, 0), parse("pazartesiye rapor hazırla").dateTime)
    }

    @Test fun noTimeKeepsTitle() {
        val r = parse("Süt al")
        assertEquals("Süt al", r.title)
        assertNull(r.dateTime)
    }

    @Test fun numericDate() {
        assertEquals(LocalDateTime.of(2026, 12, 24, 9, 0), parse("24.12.2026 hediye al").dateTime)
    }

    @Test fun dayInkWithTime() {
        val r = TurkishReminderParser.parseForDay("dişçi 10'da", LocalDate.of(2026, 10, 29))
        assertEquals("Dişçi", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 29, 10, 0), r.dateTime)
    }

    @Test fun dayInkWithoutTimeUsesDefault() {
        val r = TurkishReminderParser.parseForDay("Annemi ara", LocalDate.of(2026, 10, 29), LocalTime.of(8, 0))
        assertEquals("Annemi ara", r.title)
        assertEquals(LocalDateTime.of(2026, 10, 29, 8, 0), r.dateTime)
    }

    @Test fun dayInkAfternoonHour() {
        assertEquals(LocalDateTime.of(2026, 10, 29, 15, 0), TurkishReminderParser.parseForDay("3'te toplantı", LocalDate.of(2026, 10, 29)).dateTime)
    }
}
