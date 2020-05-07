package com.example.android.stats.appnetwork

import android.annotation.SuppressLint
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.example.android.stats.atMidnight
import com.example.android.stats.toDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

data class NetworkStats(val rxBytes: Long, val txBytes: Long)
data class AppNetwork(val appName: String, val appIcon: Drawable?, val mobileStats: NetworkStats?, val wifiStats: NetworkStats?)

@SuppressLint("MissingPermission", "HardwareIds")
fun getNetworkStats(context: Context, startDate: LocalDateTime = now().atMidnight(), endDate: LocalDateTime = now()): List<AppNetwork> {
    val nsm = context.getSystemService(NetworkStatsManager::class.java) ?: throw IllegalStateException("Could not find NetworkStatsManager")
    val tm = context.getSystemService(TelephonyManager::class.java) ?: throw IllegalStateException("Could not find TelephonyManager")

    val startTime = startDate.toDate().time
    val endTime = endDate.toDate().time

    // Mobile data
    val uidToMobileStats = hashMapOf<Int, NetworkStats>()
    val mobileSummary = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, tm.subscriberId, startTime, endTime)
    while (mobileSummary.hasNextBucket()) {
        val bucket = android.app.usage.NetworkStats.Bucket()
        mobileSummary.getNextBucket(bucket)
        uidToMobileStats[bucket.uid] = NetworkStats(bucket.rxBytes, bucket.txBytes)
    }

    // Wifi data
    val uidToWifiStats = hashMapOf<Int, NetworkStats>()
    val wifiSummary = nsm.querySummary(ConnectivityManager.TYPE_WIFI, "", startTime, endTime)
    while (wifiSummary.hasNextBucket()) {
        val bucket = android.app.usage.NetworkStats.Bucket()
        wifiSummary.getNextBucket(bucket)
        uidToWifiStats[bucket.uid] = NetworkStats(bucket.rxBytes, bucket.txBytes)
    }

    val packageManager = context.packageManager
    return (uidToMobileStats.keys + uidToWifiStats.keys)
        .mapNotNull {
            val info = try {
                packageManager.getNameForUid(it)?.let { name -> packageManager.getApplicationInfo(name, PackageManager.GET_SHARED_LIBRARY_FILES) }
            } catch (ex: Exception) {
                null
            }
            info?.let { i -> Triple(i, uidToMobileStats[it], uidToWifiStats[it]) }
        }
        .filter { it.first.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps
        .map {
            AppNetwork(
                packageManager.getApplicationLabel(it.first).toString(),
                packageManager.getApplicationIcon(it.first),
                it.second,
                it.third
            )
        }
        .toList()
}