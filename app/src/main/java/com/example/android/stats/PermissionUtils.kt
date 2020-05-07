package com.example.android.stats

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Checks a mode permission, coming from AppOpsManager
 */
fun checkModePermission(context: Context, permission: String): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(permission, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageAccess(context: Context) {
    MaterialAlertDialogBuilder(context)
        .setMessage("This view requires usage stats permission. Grant it in next view and click <back> to come back!")
        .setPositiveButton("OK") { _, _ -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        .show()
}