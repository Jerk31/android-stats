package com.example.android.stats.usage

import android.app.AppOpsManager
import android.content.Context
import android.view.View
import com.example.android.stats.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.MILLISECONDS

class AppUsageStats(private val context: Context) : StatsProvider<AppUsage> {
    override fun getPageTitle(): String {
        return context.getString(R.string.usage_page_title)
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

    override suspend fun getDataForRange(range: Pair<LocalDateTime, LocalDateTime>): List<AppUsage> {
        if (!checkRuntimePermissions()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val appUsage = getAppUsage(context, range.first, range.second).filter { it.value.totalTimeInForeground > 0L }
            return@withContext generateStats(context, appUsage)
                .filter { MILLISECONDS.toMinutes(it.totalForegroundMs) > 5 } // Keep only applications whose usage is > 5min
                .sortedBy { it.totalForegroundMs }
                .nLast(15) { left, right ->
                    AppUsage("Other", null, left.totalForegroundMs + right.totalForegroundMs)
                }
        }
    }

    override fun getTotalText(): String {
        return context.getString(R.string.usage_total)
    }

    override fun getTotalIcon(): Int {
        return R.drawable.ic_access_time_black_30dp
    }

    override fun computeTotal(data: List<AppUsage>): String {
        return data.map { MILLISECONDS.toSeconds(it.totalForegroundMs) }.sum().toPrettyDuration()
    }

    override fun getStatsText(): String {
        return context.getString(R.string.usage_stats_title)
    }

    override fun getXValues(data: List<AppUsage>): List<String> {
        return data.map(AppUsage::appName)
    }

    override fun dataToX(): (data: AppUsage) -> String = AppUsage::appName
    override fun dataToY(): (data: AppUsage) -> Float = { MILLISECONDS.toSeconds(it.totalForegroundMs).toFloat() }

    override fun formatY(x: Float): String {
        return x.toLong().toPrettyDuration(false)
    }

    override fun getDetailedStatsLayout(): Int {
        return R.layout.usage_detailed_stats
    }

    override fun showDetailedStats(v: View, selected: AppUsage) {
        v.findImageView(R.id.app_icon).setImageDrawable(selected.appIcon ?: context.getDrawable(android.R.drawable.sym_def_app_icon))
        v.findTextView(R.id.detailed_stats_label).text = context.getString(R.string.detailed_stats_for).format(selected.appName)
        v.findTextView(R.id.screen_time_duration).text = MILLISECONDS.toSeconds(selected.totalForegroundMs).toPrettyDuration()
    }
}