package com.aydinsogut.reminder.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

object TimeFormats {
    val date: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEE", TurkishLocale)
    val dayTitle: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", TurkishLocale)
    val dayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TurkishLocale)
    val dateShort: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, EEE", TurkishLocale)
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)
    val month: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", TurkishLocale)
    val short: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM EEE, HH:mm", TurkishLocale)
}

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.toLocalDate(): LocalDate = toLocalDateTime().toLocalDate()

fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.startOfDayMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun String.capitalizeTr(): String = replaceFirstChar { it.titlecase(TurkishLocale) }

/** "Bugün 14:00", "Yarın 09:30" ya da "5 Eki Paz, 10:00" biçiminde. */
fun formatReminderTime(millis: Long): String {
    val dateTime = millis.toLocalDateTime()
    val today = LocalDate.now()
    val time = dateTime.format(TimeFormats.time)
    return when (dateTime.toLocalDate()) {
        today -> "Bugün $time"
        today.plusDays(1) -> "Yarın $time"
        today.minusDays(1) -> "Dün $time"
        else -> dateTime.format(TimeFormats.short)
    }
}
