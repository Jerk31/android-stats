package com.example.android.stats.calls

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog.Calls
import com.example.android.stats.atMidnight
import com.example.android.stats.toDate
import com.example.android.stats.toLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

data class CallLogInfo(val name: String?, val number: String, val callType: Int, val date: Long, val duration: Long)
data class IndividualCallStats(var name: String, var totalTime: Long = 0L, val calls: MutableList<CallLogInfo> = mutableListOf()) {
    fun addCall(call: CallLogInfo): IndividualCallStats {
        this.calls.add(call)
        this.totalTime += call.duration
        return this
    }
}

data class CallStats(val totalTime: Long, val individualCalls: List<IndividualCallStats>)

fun getTimeRangeFromString(string: String): Pair<LocalDateTime, LocalDateTime> {
    val match = "From (.+) to (.+)".toRegex().find(string)
    if (match == null || match.groupValues.size < 2) {
        return getTodayTimeRange()
    }
    return Pair(toLocalDateTime(match.groupValues[1]), toLocalDateTime(match.groupValues[2]))
}

fun getTodayTimeRange(): Pair<LocalDateTime, LocalDateTime> = Pair(
    now().atMidnight(),
    now()
)

fun getFirstDayOfWeek(fromDate: LocalDateTime = now()): LocalDateTime = fromDate
    .with(TemporalAdjusters.previousOrSame(WeekFields.ISO.firstDayOfWeek))
    .atMidnight()

fun getLastDayOfWeek(fromDate: LocalDateTime = now()): LocalDateTime = fromDate
    .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(((WeekFields.ISO.firstDayOfWeek.value + 5) % DayOfWeek.values().size) + 1)))
    .atMidnight()
    .minusMinutes(1)

fun getThisWeekTimeRange(): Pair<LocalDateTime, LocalDateTime> = now().let {
    Pair(
        getFirstDayOfWeek(it),
        getLastDayOfWeek(it)
    )
}

fun getLastWeekTimeRange(): Pair<LocalDateTime, LocalDateTime> = now().minus(1, ChronoUnit.WEEKS).let {
    Pair(
        getFirstDayOfWeek(it),
        getLastDayOfWeek(it)
    )
}

@SuppressLint("MissingPermission")
fun getCallLogs(context: Context, startDate: LocalDateTime? = null, endDate: LocalDateTime? = null): List<CallLogInfo> {
    val contentResolver = context.applicationContext.contentResolver
    val projection = arrayOf(Calls.NUMBER, Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME)

    val callLogs = mutableListOf<CallLogInfo>()

    // build select
    val selectBuilder = StringBuilder()
    val args = mutableListOf<String>()
    if (startDate != null) {
        selectBuilder.append(Calls.DATE + " >?")
        args.add(startDate.toDate().time.toString())
        if (endDate != null) {
            selectBuilder.append(" AND ")
        }
    }
    if (endDate != null) {
        selectBuilder.append(Calls.DATE + " <?")
        args.add(endDate.toDate().time.toString())
    }
    val select = if (selectBuilder.isEmpty()) null else selectBuilder.toString()

    // run query & get results
    contentResolver.query(Calls.CONTENT_URI, projection, select, args.toTypedArray(), Calls.DEFAULT_SORT_ORDER)?.use {
        val nameColumn = it.getColumnIndex(Calls.CACHED_NAME)
        val numberColumn = it.getColumnIndex(Calls.NUMBER)
        val typeColumn = it.getColumnIndex(Calls.TYPE)
        val dateColumn = it.getColumnIndex(Calls.DATE)
        val durationColumn = it.getColumnIndex(Calls.DURATION)

        while (it.moveToNext()) {
            callLogs.add(
                CallLogInfo(
                    it.getString(nameColumn),
                    it.getString(numberColumn),
                    it.getInt(typeColumn),
                    it.getLong(dateColumn),
                    it.getLong(durationColumn)
                )
            )
        }
    }
    return callLogs
}

fun generateStats(callLogs: List<CallLogInfo>): CallStats {
    val totalTime = callLogs.map { it.duration }.sum()
    val individualStats = callLogs
        .groupingBy { it.name ?: it.number }
        .fold({ k, e -> IndividualCallStats(k).addCall(e) }) { _, acc, e -> acc.addCall(e) }
        .values
        .toList()
    return CallStats(totalTime, individualStats)
}

