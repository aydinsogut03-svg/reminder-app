package com.aydinsogut.reminder.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class GeminiNanoOutputTest {
    @Test fun parsesMultipleLines() {
        val output = """
            Annemi ara | 2026-10-03 | 09:00
            - Ekmek al | 2026-10-02 | 19:30
        """.trimIndent()
        val result = GeminiNano.parseOutput(output)
        assertEquals(2, result.size)
        assertEquals("Annemi ara", result[0].title)
        assertEquals(LocalDateTime.of(2026, 10, 3, 9, 0), result[0].dateTime)
        assertEquals("Ekmek al", result[1].title)
    }

    @Test fun missingDateAndTimeGivesNull() {
        val result = GeminiNano.parseOutput("süt al |  | ")
        assertEquals("Süt al", result.single().title)
        assertNull(result.single().dateTime)
    }

    @Test fun ignoresHeaderAndChatter() {
        val result = GeminiNano.parseOutput("İşte hatırlatıcılar:\nBAŞLIK | YYYY-AA-GG | SS:DD\nDişçi | 2026-10-15 | 14.30")
        assertEquals(1, result.size)
        assertEquals(LocalDateTime.of(2026, 10, 15, 14, 30), result.single().dateTime)
    }
}
