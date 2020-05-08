package com.example.android.stats

import android.view.View
import java.time.LocalDateTime

interface StatsProvider<T> {
    fun getPageTitle(): String

    // Permissions
    fun checkRuntimePermissions(): Boolean
    fun requestPermissions()
    fun onRuntimePermissionsUpdated(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean

    // Range selection
    suspend fun getDataForRange(range: Pair<LocalDateTime, LocalDateTime>): List<T>

    // Total
    fun getTotalText(): String
    fun getTotalIcon(): Int
    fun computeTotal(data: List<T>): String

    // Stats
    fun getStatsText(): String
    fun getXValues(data: List<T>): List<String>
    fun dataToX(): (data: T) -> String
    fun dataToY(): (data: T) -> Float
    fun formatY(x: Float): String

    // Detailed stats
    fun getDetailedStatsLayout(): Int
    fun showDetailedStats(v: View, selected: T)
}