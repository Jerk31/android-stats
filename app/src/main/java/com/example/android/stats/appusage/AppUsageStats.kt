package com.example.android.stats.appusage

import android.app.AppOpsManager
import android.content.Context
import android.view.View
import com.example.android.stats.*
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.MILLISECONDS

class AppUsageStats(private val context: Context) : StatsProvider<AppUsage> {
    override fun getPageTitle(): String {
        return context.getString(R.string.appusage_page_title)
    }

    override fun checkRuntimePermissions(): Boolean {
        return checkModePermission(context, AppOpsManager.OPSTR_GET_USAGE_STATS)
    }

    override fun requestPermissions() {
        requestUsageAccess(context)
    }

    override fun onRuntimePermissionsUpdated(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        return true
    }

    override fun getDataForRange(range: Pair<LocalDateTime, LocalDateTime>): List<AppUsage> {
        if (!checkRuntimePermissions()) {
            return emptyList()
        }
        val appUsage = getAppUsage(context, range.first, range.second).filter { it.value.totalTimeInForeground > 0L }
        var sortedStats = generateStats(context, appUsage).sortedBy { it.stats.totalTimeInForeground }
        if (appUsage.keys.size > 10) {
            sortedStats = sortedStats.filter { MILLISECONDS.toMinutes(it.stats.totalTimeInForeground) > 5 }
        }
        return sortedStats
    }

    override fun getTotalText(): String {
        return context.getString(R.string.appusage_total)
    }

    override fun getTotalIcon(): Int {
        return android.R.drawable.sym_def_app_icon
    }

    override fun computeTotal(data: List<AppUsage>): String {
        return data.map { MILLISECONDS.toSeconds(it.stats.totalTimeInForeground) }.sum().toPrettyDuration()
    }

    override fun getStatsText(): String {
        return context.getString(R.string.appusage_stats_title)
    }

    override fun getXValues(data: List<AppUsage>): List<String> {
        return data.map(AppUsage::appName)
    }

    override fun dataToX(): (data: AppUsage) -> String = AppUsage::appName
    override fun dataToY(): (data: AppUsage) -> Float = { MILLISECONDS.toSeconds(it.stats.totalTimeInForeground).toFloat() }

    override fun formatY(x: Float): String {
        return x.toLong().toPrettyDuration(false)
    }

    override fun getDetailedStatsLayout(): Int {
        return R.layout.appusage_detailed_stats
    }

    override fun showDetailedStats(v: View, selected: AppUsage) {
        v.findImageView(R.id.app_icon).setImageDrawable(selected.appIcon ?: context.getDrawable(android.R.drawable.sym_def_app_icon))
        v.findTextView(R.id.detailed_stats_label).text = context.getString(R.string.detailed_stats_for).format(selected.appName)
        v.findTextView(R.id.screen_time_duration).text = MILLISECONDS.toSeconds(selected.stats.totalTimeInForeground).toPrettyDuration()
    }
}