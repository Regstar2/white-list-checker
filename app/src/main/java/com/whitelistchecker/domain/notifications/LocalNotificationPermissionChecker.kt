package com.whitelistchecker.domain.notifications

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

class LocalNotificationPermissionChecker(
    private val context: Context,
) {

    fun areNotificationsAllowed(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun requiresRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
