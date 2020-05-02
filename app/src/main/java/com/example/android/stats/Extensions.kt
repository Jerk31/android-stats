package com.example.android.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import java.time.*
import java.time.LocalDateTime.ofInstant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.*

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Duration.toPrettyString(): String {
    return toString()
        .substring(2)
        .replace(Regex("(\\d[HMS])(?!$)"), "$1 ")
        .toLowerCase(Locale.ROOT);
}

fun LocalDateTime.toDate(): Date {
    return Date.from(atZone(ZoneId.systemDefault()).toInstant())
}

fun LocalDateTime.atMidnight(): LocalDateTime {
    return withHour(0).withMinute(0).withSecond(0)
}

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
    return ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}