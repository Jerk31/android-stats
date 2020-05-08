package com.example.android.stats.network

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.example.android.stats.*
import com.example.android.stats.calls.CallsStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AppNetworkStats(private val context: Context) : StatsProvider<AppNetwork> {
    companion object {
        const val PERMISSIONS_CODE = 123
    }

    override fun getPageTitle(): String {
        return context.getString(R.string.network_page_title)
    }

    private fun modeOk(): Boolean {
        return checkModePermission(context, AppOpsManager.OPSTR_GET_USAGE_STATS)
    }

    private fun permissionsOk(): Boolean {
        return PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED
    }

    override fun checkRuntimePermissions(): Boolean {
        return modeOk() && permissionsOk()
    }

    override fun requestPermissions() {
        if (!permissionsOk()) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.READ_PHONE_STATE), CallsStats.PERMISSIONS_CODE)
        }
        if (!modeOk()) {
            requestUsageAccess(context)
        }
    }

    override fun onRuntimePermissionsUpdated(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        return requestCode == PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    override suspend fun getDataForRange(range: Pair<LocalDateTime, LocalDateTime>): List<AppNetwork> {
        if (!checkRuntimePermissions()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            var sortedStats = getNetworkStats(context, range.first, range.second)
                .filter { getReceivedBytes(it) > 0L }
                .sortedBy { getReceivedBytes(it) }
            if (sortedStats.size > 10) {
                sortedStats = sortedStats.filter { getReceivedBytes(it) > 1024 * 1024 } // > 1Mo
            }
            return@withContext sortedStats
        }
    }

    override fun getTotalText(): String {
        return context.getString(R.string.network_total)
    }

    override fun getTotalIcon(): Int {
        return R.drawable.ic_import_export_black_24dp
    }

    override fun computeTotal(data: List<AppNetwork>): String {
        val totalReceived = data.fold(0L) { acc, it -> acc + (it.mobileStats?.rxBytes ?: 0L) + (it.wifiStats?.rxBytes ?: 0L) }
        val totalSent = data.fold(0L) { acc, it -> acc + (it.mobileStats?.txBytes ?: 0L) + (it.wifiStats?.txBytes ?: 0L) }
        return (totalReceived + totalSent).toPrettyByteSize(context)
    }

    override fun getStatsText(): String {
        return context.getString(R.string.network_stats_title)
    }

    override fun getXValues(data: List<AppNetwork>): List<String> {
        return data.map(AppNetwork::appName)
    }

    override fun dataToX(): (data: AppNetwork) -> String = AppNetwork::appName
    override fun dataToY(): (data: AppNetwork) -> Float = {
        getReceivedBytes(it).toFloat()
    }

    override fun formatY(x: Float): String {
        return x.toLong().toPrettyByteSize(context)
    }

    override fun getDetailedStatsLayout(): Int {
        return R.layout.network_detailed_stats
    }

    override fun showDetailedStats(v: View, selected: AppNetwork) {
        v.findImageView(R.id.app_icon).setImageDrawable(selected.appIcon ?: context.getDrawable(android.R.drawable.sym_def_app_icon))
        v.findTextView(R.id.detailed_stats_label).text = context.getString(R.string.detailed_stats_for).format(selected.appName)

        v.findTextView(R.id.wifi_data_received).text = selected.wifiStats?.rxBytes?.toPrettyByteSize(context) ?: "0"
        v.findTextView(R.id.wifi_data_sent).text = selected.wifiStats?.txBytes?.toPrettyByteSize(context) ?: "0"

        v.findTextView(R.id.network_data_received).text = selected.mobileStats?.rxBytes?.toPrettyByteSize(context) ?: "0"
        v.findTextView(R.id.network_data_sent).text = selected.mobileStats?.txBytes?.toPrettyByteSize(context) ?: "0"
    }

    private fun getReceivedBytes(data: AppNetwork): Long {
        return (data.mobileStats?.rxBytes ?: 0L) + (data.wifiStats?.rxBytes ?: 0L)
    }
}