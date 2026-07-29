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

        if (notificationManager.getNotificationChannel(WHITELIST_EVENTS_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                WHITELIST_EVENTS_CHANNEL_ID,
                EVENTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = EVENTS_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (notificationManager.getNotificationChannel(ACTIVE_MONITORING_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ACTIVE_MONITORING_CHANNEL_ID,
                ACTIVE_MONITORING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = ACTIVE_MONITORING_CHANNEL_DESCRIPTION
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WHITELIST_EVENTS_CHANNEL_ID = "whitelist_events"
        const val ACTIVE_MONITORING_CHANNEL_ID = "active_monitoring"
        private const val EVENTS_CHANNEL_NAME = "События белых списков"
        private const val EVENTS_CHANNEL_DESCRIPTION =
            "Уведомления о включении и выключении белых списков"
        private const val ACTIVE_MONITORING_CHANNEL_NAME = "Активный мониторинг"
        private const val ACTIVE_MONITORING_CHANNEL_DESCRIPTION =
            "Постоянное уведомление активного мониторинга"
    }
}
