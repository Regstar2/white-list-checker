package com.whitelistchecker.domain.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class LocalNotificationSender(
    private val context: Context,
    private val channelManager: LocalNotificationChannelManager,
    private val permissionChecker: LocalNotificationPermissionChecker,
    private val formatter: LocalNotificationFormatter,
) {

    fun sendStateChangeNotification(
        event: WhitelistStateChangeEvent,
        checkResult: NetworkCheckResult,
    ): LocalNotificationResult {
        if (event.type != WhitelistStateChangeType.WHITELIST_TURNED_ON &&
            event.type != WhitelistStateChangeType.WHITELIST_TURNED_OFF
        ) {
            return LocalNotificationResult.Disabled
        }
        if (!permissionChecker.areNotificationsAllowed()) {
            return LocalNotificationResult.PermissionNotGranted
        }
        return try {
            channelManager.ensureChannelsCreated()
            val title = formatter.titleFor(event)
            val text = formatter.textFor(event, checkResult)
            val notificationId = when (event.type) {
                WhitelistStateChangeType.WHITELIST_TURNED_ON -> WHITELIST_TURNED_ON_NOTIFICATION_ID
                WhitelistStateChangeType.WHITELIST_TURNED_OFF -> WHITELIST_TURNED_OFF_NOTIFICATION_ID
                else -> WHITELIST_TURNED_ON_NOTIFICATION_ID
            }
            val notification = NotificationCompat.Builder(context, LocalNotificationChannelManager.WHITELIST_EVENTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_whitelist)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            LocalNotificationResult.Success
        } catch (exception: Exception) {
            LocalNotificationResult.Failure(
                reason = exception.message ?: exception.javaClass.simpleName,
            )
        }
    }

    fun sendTestCheckNotification(checkResult: NetworkCheckResult): LocalNotificationResult {
        if (!permissionChecker.areNotificationsAllowed()) {
            return LocalNotificationResult.PermissionNotGranted
        }
        return try {
            channelManager.ensureChannelsCreated()
            val title = formatter.conclusionFor(checkResult.state)
            val text = formatter.textForCheckResult(checkResult)
            val notification = NotificationCompat.Builder(
                context,
                LocalNotificationChannelManager.WHITELIST_EVENTS_CHANNEL_ID,
            )
                .setSmallIcon(R.drawable.ic_stat_whitelist)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(TEST_CHECK_NOTIFICATION_ID, notification)
            LocalNotificationResult.Success
        } catch (exception: Exception) {
            LocalNotificationResult.Failure(
                reason = exception.message ?: exception.javaClass.simpleName,
            )
        }
    }

    companion object {
        private const val WHITELIST_TURNED_ON_NOTIFICATION_ID = 1001
        private const val WHITELIST_TURNED_OFF_NOTIFICATION_ID = 1002
        private const val TEST_CHECK_NOTIFICATION_ID = 1003
    }
}
