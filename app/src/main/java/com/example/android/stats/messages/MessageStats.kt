package com.example.android.stats.messages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.example.android.stats.R
import com.example.android.stats.StatsProvider
import com.example.android.stats.nLast
import java.time.LocalDateTime

class MessageStats(private val context: Context) : StatsProvider<IndividualMessageStats> {
    companion object {
        const val PERMISSIONS_CODE = 123
    }

    override fun getPageTitle(): String {
        return context.getString(R.string.messages_page_title)
    }

    override fun checkRuntimePermissions(): Boolean {
        return PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_SMS) == PermissionChecker.PERMISSION_GRANTED
    }

    override fun requestPermissions() {
        ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.READ_SMS), PERMISSIONS_CODE)
    }

    override fun onRuntimePermissionsUpdated(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        return requestCode == PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    override fun getMissingPermissionsMessage(): String {
        return context.getString(R.string.missing_permissions).format("READ_SMS")
    }

    override suspend fun getDataForRange(range: Pair<LocalDateTime, LocalDateTime>): List<IndividualMessageStats> {
        return getMessages(context, range.first, range.second)
            .sortedBy { it.totalSent + it.totalReceived }
            .nLast(15) { left, right ->
                IndividualMessageStats("Other", left.totalReceived + right.totalReceived, left.totalSent + right.totalSent, left.messages + right.messages)
            }
    }

    override fun getTotalText(): String {
        return context.getString(R.string.messages_total)
    }

    override fun getTotalIcon(): Int {
        return R.drawable.ic_message_black_24dp
    }

    override fun computeTotal(data: List<IndividualMessageStats>): String {
        return data.map { it.totalReceived + it.totalSent }.sum().toString()
    }

    override fun getStatsText(): String {
        return context.getString(R.string.messages_stats_title)
    }

    override fun getXValues(data: List<IndividualMessageStats>): List<String> {
        return data.map(IndividualMessageStats::name)
    }

    override fun dataToX(): (data: IndividualMessageStats) -> String = IndividualMessageStats::name
    override fun dataToY(): (data: IndividualMessageStats) -> Float = { it.totalReceived.toFloat() }

    override fun formatY(x: Float): String {
        return x.toString()
    }

    override fun getDetailedStatsLayout(): Int {
        return R.layout.messages_detailed_stats
    }

    override fun showDetailedStats(v: View, selected: IndividualMessageStats) {
        TODO("Not yet implemented")
    }
}