package com.example.android.stats

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
private val dayMonthFormatter = DateTimeFormatterBuilder()
    .appendPattern("dd/MM")
    .parseDefaulting(ChronoField.YEAR, Year.now().value.toLong())
    .toFormatter()

fun toDayMonth(time: LocalDateTime): String {
    if (time.year != Year.now().value) {
        return time.format(dayMonthYearFormatter)
    }
    return time.format(dayMonthFormatter)
}

fun toLocalDateTime(string: String): LocalDateTime {
    return try {
        LocalDate.parse(string, dayMonthFormatter).atStartOfDay()
    } catch (ex: DateTimeParseException) {
        LocalDate.parse(string, dayMonthYearFormatter).atStartOfDay()
    }
}

fun toLocalDateTime(millis: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}

fun getTimeRangeFromString(string: String): Pair<LocalDateTime, LocalDateTime> {
    val match = "From (.+) to (.+)".toRegex().find(string)
    if (match == null || match.groupValues.size < 2) {
        return getTodayTimeRange()
    }
    return try {
        var startTime = toLocalDateTime(match.groupValues[1])
        var endTime = toLocalDateTime(match.groupValues[2])
        if (startTime == endTime) {
            startTime = startTime.atMidnight()
            endTime = endTime.plusDays(1).atMidnight().minusMinutes(1)
        }
        Pair(startTime, endTime)
    } catch (ex: DateTimeParseException) {
        getTodayTimeRange()
    }
}

fun getTodayTimeRange(): Pair<LocalDateTime, LocalDateTime> = Pair(
    LocalDateTime.now().atMidnight(),
    LocalDateTime.now()
)

fun getFirstDayOfWeek(fromDate: LocalDateTime = LocalDateTime.now()): LocalDateTime = fromDate
    .with(TemporalAdjusters.previousOrSame(WeekFields.ISO.firstDayOfWeek))
    .atMidnight()

fun getLastDayOfWeek(fromDate: LocalDateTime = LocalDateTime.now()): LocalDateTime = fromDate
    .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(((WeekFields.ISO.firstDayOfWeek.value + 5) % DayOfWeek.values().size) + 1)))
    .atMidnight()
    .minusMinutes(1)

fun getThisWeekTimeRange(): Pair<LocalDateTime, LocalDateTime> = LocalDateTime.now().let {
    Pair(
        getFirstDayOfWeek(it),
        getLastDayOfWeek(it)
    )
}

fun getLastWeekTimeRange(): Pair<LocalDateTime, LocalDateTime> = LocalDateTime.now().minus(1, ChronoUnit.WEEKS).let {
    Pair(
        getFirstDayOfWeek(it),
        getLastDayOfWeek(it)
    )
}

fun timeRangeToContentProviderSelect(dateField: String, start: LocalDateTime?, end: LocalDateTime?): Pair<String?, List<String>> {
    val selectBuilder = StringBuilder()
    val args = mutableListOf<String>()
    if (start != null) {
        selectBuilder.append("$dateField >?")
        args.add(start.toDate().time.toString())
        if (end != null) {
            selectBuilder.append(" AND ")
        }
    }
    if (end != null) {
        selectBuilder.append("$dateField <?")
        args.add(end.toDate().time.toString())
    }
    val select = if (selectBuilder.isEmpty()) null else selectBuilder.toString()
    return Pair(select, args)
}