package com.example.android.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDateTime.ofInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM")

fun toDayMonth(time: LocalDateTime): String {
    return time.format(dayMonthFormatter)
}

fun toLocalDateTime(millis: Long): LocalDateTime {
    return ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}