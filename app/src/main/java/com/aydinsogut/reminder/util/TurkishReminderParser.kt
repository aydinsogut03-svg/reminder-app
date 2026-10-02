package com.aydinsogut.reminder.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

/** Serbest Türkçe cümleden çıkarılan hatırlatıcı. [dateTime] cümlede zaman yoksa null. */
data class ParsedReminder(
    val title: String,
    val dateTime: LocalDateTime?,
)

/**
 * "yarın saat 9'da annemi ara", "cuma akşam 8'de toplantı", "2 saat sonra ilaç" gibi
 * cümlelerden başlık ve zamanı çıkaran, internetsiz çalışan basit bir ayrıştırıcı.
 */
object TurkishReminderParser {

    private const val START = "(?<![\\p{L}\\d])"
    private const val END = "(?![\\p{L}\\d])"
    private const val SUFFIX = "(?:['’]?\\p{L}{0,4})"

    private val numberWords: Map<String, Int> = buildMap {
        val units = listOf("", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
        val tens = listOf("", "on", "yirmi", "otuz", "kırk", "elli")
        for (t in tens.indices) for (u in units.indices) {
            val n = t * 10 + u
            if (n > 0) put("${tens[t]} ${units[u]}".trim(), n)
        }
    }

    private val numberPattern: String =
        (listOf("\\d{1,3}") + numberWords.keys.sortedByDescending { it.length }).joinToString("|")

    private val months = mapOf(
        "ocak" to Month.JANUARY, "şubat" to Month.FEBRUARY, "mart" to Month.MARCH,
        "nisan" to Month.APRIL, "mayıs" to Month.MAY, "haziran" to Month.JUNE,
        "temmuz" to Month.JULY, "ağustos" to Month.AUGUST, "eylül" to Month.SEPTEMBER,
        "ekim" to Month.OCTOBER, "kasım" to Month.NOVEMBER, "aralık" to Month.DECEMBER,
    )

    private val weekdays = linkedMapOf(
        "pazartesi" to DayOfWeek.MONDAY, "salı" to DayOfWeek.TUESDAY,
        "çarşamba" to DayOfWeek.WEDNESDAY, "perşembe" to DayOfWeek.THURSDAY,
        "cumartesi" to DayOfWeek.SATURDAY, "cuma" to DayOfWeek.FRIDAY,
        "pazar" to DayOfWeek.SUNDAY,
    )

    private enum class Period { MORNING, NOON, AFTERNOON, EVENING, NIGHT }

    private val relativeRegex =
        Regex("$START(yarım|$numberPattern)\\s*(dakika|dk|saat|gün|hafta)\\s+sonra\\p{L}*$END")
    private val dayAfterTomorrowRegex = Regex("${START}(?:yarından sonra|öbür gün|ertesi gün)\\p{L}*$END")
    private val tomorrowRegex = Regex("${START}yarın$SUFFIX$END")
    private val todayRegex = Regex("${START}bugün$SUFFIX$END")
    private val nextWeekRegex = Regex("${START}(?:haftaya|gelecek hafta|önümüzdeki hafta)$SUFFIX$END")
    private val weekdayRegex =
        Regex("$START(${weekdays.keys.joinToString("|")})$SUFFIX(?:\\s+gün\\p{L}{0,3})?$END")
    private val monthDateRegex =
        Regex("$START(\\d{1,2})\\s+(${months.keys.joinToString("|")})$SUFFIX(?:\\s+(\\d{4})$SUFFIX)?$END")
    private val numericDateRegex = Regex("$START(\\d{1,2})[./](\\d{1,2})[./](\\d{2,4})$SUFFIX$END")
    private val periodRegex =
        Regex("$START(?:bu\\s+)?(sabah|öğleden sonra|öğlen?|akşam|gece)$SUFFIX$END")
    private val clockRegex = Regex("$START(?:saat\\s+)?(\\d{1,2})[:.](\\d{2})$SUFFIX$END")
    private val hourRegex = Regex("${START}saat\\s+($numberPattern)(\\s+buçuk)?$SUFFIX$END")
    private val bareHourRegex = Regex("$START(\\d{1,2})(\\s+buçuk)?['’](?:da|de|ta|te)$END")
    private val fillerRegex = Regex(
        "$START(?:bana\\s+)?(?:hatırlat\\p{L}*|hatırlatıcı\\p{L}*|not\\s+(?:al|ekle|et)\\p{L}*|(?:lütfen))(?:\\s+mısın|\\s+misin)?$END",
    )

    fun parse(
        text: String,
        now: LocalDateTime = LocalDateTime.now(),
        defaultTime: LocalTime = LocalTime.of(9, 0),
    ): ParsedReminder {
        val lower = text.lowercase(TurkishLocale)
        // Türkçe küçültme uzunluğu korur; korumazsa başlığı küçük harfli metinden üretiriz.
        val original = if (lower.length == text.length) text else lower
        val work = Work(lower, original)
        val today = now.toLocalDate()

        var date: LocalDate? = null
        var time: LocalTime? = null
        var period: Period? = null

        work.find(relativeRegex)?.let { m ->
            val unit = m.groupValues[2]
            val amount = m.groupValues[1]
            val minutes = when (unit) {
                "dakika", "dk" -> toNumber(amount)?.toLong()
                "saat" -> if (amount == "yarım") 30L else toNumber(amount)?.let { it * 60L }
                else -> null
            }
            when {
                minutes != null -> {
                    val target = now.plusMinutes(minutes).withSecond(0).withNano(0)
                    date = target.toLocalDate()
                    time = target.toLocalTime()
                }
                unit == "gün" -> date = toNumber(amount)?.let { today.plusDays(it.toLong()) }
                unit == "hafta" -> date = toNumber(amount)?.let { today.plusWeeks(it.toLong()) }
            }
        }

        if (date == null) {
            work.find(monthDateRegex)?.let { m ->
                val day = m.groupValues[1].toInt()
                val month = months.getValue(m.groupValues[2])
                val year = m.groupValues[3].toIntOrNull()
                date = safeDate(year ?: today.year, month.value, day)?.let {
                    if (year == null && it.isBefore(today)) it.plusYears(1) else it
                }
            }
        }
        if (date == null) {
            work.find(numericDateRegex)?.let { m ->
                val rawYear = m.groupValues[3].toInt()
                val year = if (rawYear < 100) 2000 + rawYear else rawYear
                date = safeDate(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }
        }
        if (date == null) {
            when {
                work.find(dayAfterTomorrowRegex) != null -> date = today.plusDays(2)
                work.find(tomorrowRegex) != null -> date = today.plusDays(1)
                work.find(todayRegex) != null -> date = today
            }
        }
        val nextWeek = work.find(nextWeekRegex) != null
        if (date == null) {
            val weekday = work.find(weekdayRegex)?.let { weekdays.getValue(it.groupValues[1]) }
            date = when {
                weekday != null && nextWeek ->
                    today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(weekday))
                weekday != null -> today.with(TemporalAdjusters.next(weekday))
                nextWeek -> today.plusWeeks(1)
                else -> null
            }
        }

        work.find(periodRegex)?.let { m ->
            period = when (m.groupValues[1]) {
                "sabah" -> Period.MORNING
                "öğle", "öğlen" -> Period.NOON
                "öğleden sonra" -> Period.AFTERNOON
                "akşam" -> Period.EVENING
                else -> Period.NIGHT
            }
            if (date == null && m.value.startsWith("bu")) date = today
        }

        if (time == null) {
            work.find(clockRegex)?.let { m ->
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toInt()
                if (h in 0..23 && min in 0..59) time = LocalTime.of(h, min)
            }
        }
        if (time == null) {
            (work.find(hourRegex) ?: work.find(bareHourRegex))?.let { m ->
                val h = toNumber(m.groupValues[1])
                val half = m.groupValues[2].isNotBlank()
                if (h != null && h in 0..23) time = LocalTime.of(h, if (half) 30 else 0)
            }
        }

        time = adjustForPeriod(time, period)

        work.find(fillerRegex)

        val title = cleanTitle(work.remaining())

        if (date == null && time == null) return ParsedReminder(title, null)

        val resolvedTime = time ?: defaultTime
        var dateTime = LocalDateTime.of(date ?: today, resolvedTime)
        if (date == null && !dateTime.isAfter(now)) dateTime = dateTime.plusDays(1)
        return ParsedReminder(title, dateTime)
    }

    private fun adjustForPeriod(time: LocalTime?, period: Period?): LocalTime? {
        if (time == null) {
            return when (period) {
                Period.MORNING -> LocalTime.of(9, 0)
                Period.NOON -> LocalTime.of(12, 0)
                Period.AFTERNOON -> LocalTime.of(15, 0)
                Period.EVENING -> LocalTime.of(20, 0)
                Period.NIGHT -> LocalTime.of(22, 0)
                null -> null
            }
        }
        val h = time.hour
        val pm = when (period) {
            Period.AFTERNOON, Period.EVENING -> h in 1..11
            Period.NIGHT -> h in 6..11
            Period.NOON -> h in 1..5
            Period.MORNING -> false
            // "saat 3'te" konuşmada genelde öğleden sonra demektir.
            null -> h in 1..5
        }
        return if (pm) time.plusHours(12) else time
    }

    private fun toNumber(token: String): Int? = token.toIntOrNull() ?: numberWords[token]

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()

    private fun cleanTitle(raw: String): String {
        val collapsed = raw.replace(Regex("\\s+"), " ").trim()
            .trim(',', '.', '-', ';', ':', '!', '?', ' ')
            .replace(Regex("^(ve|için)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(ve|için)$", RegexOption.IGNORE_CASE), "")
            .trim()
        return collapsed.replaceFirstChar { it.titlecase(TurkishLocale) }
    }

    /** Eşleşen parçaları boşlukla silerek ilerleyen çalışma metni; indeksler iki kopyada aynı kalır. */
    private class Work(lower: String, original: String) {
        private val lowerChars = lower.toCharArray()
        private val originalChars = original.toCharArray()

        fun find(regex: Regex): MatchResult? {
            val match = regex.find(String(lowerChars)) ?: return null
            for (i in match.range) {
                lowerChars[i] = ' '
                originalChars[i] = ' '
            }
            return match
        }

        fun remaining(): String = String(originalChars)
    }
}
