package com.example.android.stats

import android.content.Context
import android.text.format.Formatter.formatShortFileSize
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun View.findImageView(id: Int): ImageView {
    return findViewById(id)
}

fun View.findTextView(id: Int): TextView {
    return findViewById(id)
}

fun LocalDateTime.toDate(): Date {
    return Date.from(atZone(ZoneId.systemDefault()).toInstant())
}

fun LocalDateTime.atMidnight(): LocalDateTime {
    return withHour(0).withMinute(0).withSecond(0)
}

fun Long.toPrettyDuration(withSeconds: Boolean = true): String {
    val duration = Duration.ofSeconds(this)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()

    val timeParts = mutableListOf<String>()
    if (hours > 0L) {
        timeParts.add("${hours}h")
    }
    if (minutes > 0L) {
        timeParts.add("${minutes}m")
    }
    if (withSeconds) {
        val seconds = duration.seconds % 60
        if (seconds > 0L) {
            timeParts.add("${seconds}s")
        }
    }
    return timeParts.joinToString(separator = " ")
}

fun Long.toPrettyByteSize(context: Context): String {
    return formatShortFileSize(context, this)
}