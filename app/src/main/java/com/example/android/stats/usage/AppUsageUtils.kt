package com.example.android.stats.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES
import android.graphics.drawable.Drawable
import com.example.android.stats.atMidnight
import com.example.android.stats.toDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now


data class AppUsage(val appName: String, val appIcon: Drawable?, val totalForegroundMs: Long)

fun getAppUsage(context: Context, startDate: LocalDateTime = now().atMidnight(), endDate: LocalDateTime = now()): Map<String, UsageStats> {
    val usm = context.getSystemService(UsageStatsManager::class.java) ?: throw IllegalStateException("Could not find UsageStatsManager")
    return usm.queryAndAggregateUsageStats(startDate.toDate().time, endDate.toDate().time)
}

fun generateStats(context: Context, appUsageStats: Map<String, UsageStats>): List<AppUsage> {
    val packageManager = context.packageManager
    return appUsageStats
        .mapNotNull {
            val info = try {
                packageManager.getApplicationInfo(it.key, GET_SHARED_LIBRARY_FILES)
            } catch (ex: Exception) {
                null
            }
            info?.let { i -> i to it.value }
        }
        .filter { it.first.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps
        .map { AppUsage(packageManager.getApplicationLabel(it.first).toString(), packageManager.getApplicationIcon(it.first), it.second.totalTimeInForeground) }
        .toList()
}