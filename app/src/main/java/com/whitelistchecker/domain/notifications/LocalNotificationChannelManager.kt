package com.whitelistchecker.domain.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class LocalNotificationChannelManager(
    private val context: Context,
) {

    fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = notificationManager.getNotificationChannel(WHITELIST_EVENTS_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            WHITELIST_EVENTS_CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val WHITELIST_EVENTS_CHANNEL_ID = "whitelist_events"
        private const val CHANNEL_NAME = "События белых списков"
        private const val CHANNEL_DESCRIPTION = "Уведомления о включении и выключении белых списков"
    }
}
