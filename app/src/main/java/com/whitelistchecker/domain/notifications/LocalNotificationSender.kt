package com.whitelistchecker.domain.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.whitelistchecker.R
import com.whitelistchecker.domain.checkrun.NotificationDecision
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState
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
            notifyOrPermissionError(notificationId, notification)
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
            notifyOrPermissionError(TEST_CHECK_NOTIFICATION_ID, notification)
        } catch (exception: Exception) {
            LocalNotificationResult.Failure(
                reason = exception.message ?: exception.javaClass.simpleName,
            )
        }
    }

    fun sendDecisionNotification(
        decision: NotificationDecision,
        checkResult: NetworkCheckResult?,
    ): LocalNotificationResult {
        if (decision == NotificationDecision.None) {
            return LocalNotificationResult.Disabled
        }
        if (!permissionChecker.areNotificationsAllowed()) {
            return LocalNotificationResult.PermissionNotGranted
        }
        return try {
            channelManager.ensureChannelsCreated()
            val title = titleForDecision(decision)
            val text = textForDecision(decision, checkResult)
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
            notifyOrPermissionError(notificationIdForDecision(decision), notification)
        } catch (exception: Exception) {
            LocalNotificationResult.Failure(
                reason = exception.message ?: exception.javaClass.simpleName,
            )
        }
    }

    private fun titleForDecision(decision: NotificationDecision): String {
        return when (decision) {
            is NotificationDecision.AttemptResult -> context.getString(R.string.notification_check_result_title)
            is NotificationDecision.StateChanged -> context.getString(R.string.notification_state_changed_title)
            is NotificationDecision.AccessRestored -> context.getString(R.string.notification_access_restored_title)
            is NotificationDecision.AccessRestoredAndStateChanged ->
                context.getString(R.string.notification_access_restored_title)
            is NotificationDecision.AttemptUnavailable -> context.getString(R.string.notification_attempt_unavailable_title)
            is NotificationDecision.AttemptFailed -> context.getString(R.string.notification_attempt_failed_title)
            NotificationDecision.None -> ""
        }
    }

    private fun textForDecision(
        decision: NotificationDecision,
        checkResult: NetworkCheckResult?,
    ): String {
        return when (decision) {
            is NotificationDecision.AttemptResult -> checkResult?.let(formatter::textForCheckResult)
                ?: decision.currentState?.let { formatter.conclusionFor(it) }
                ?: context.getString(R.string.notification_check_result_unknown)
            is NotificationDecision.StateChanged -> buildResultText(decision.newState, checkResult)
            is NotificationDecision.AccessRestored -> context.getString(
                R.string.notification_access_restored_text,
                buildResultText(decision.currentState, checkResult),
            )
            is NotificationDecision.AccessRestoredAndStateChanged -> context.getString(
                R.string.notification_access_restored_state_changed_text,
                formatter.conclusionFor(decision.newState),
                checkResult?.let(formatter::checkSummaryLine).orEmpty(),
            ).trim()
            is NotificationDecision.AttemptUnavailable -> context.getString(
                R.string.notification_attempt_unavailable_text,
                formatter.conclusionFor(decision.state),
            )
            is NotificationDecision.AttemptFailed -> context.getString(
                R.string.notification_attempt_failed_text,
                decision.error,
            )
            NotificationDecision.None -> ""
        }
    }

    private fun buildResultText(
        state: WhitelistState?,
        checkResult: NetworkCheckResult?,
    ): String {
        if (checkResult != null) return formatter.textForCheckResult(checkResult)
        if (state != null) return formatter.conclusionFor(state)
        return context.getString(R.string.notification_check_result_unknown)
    }

    private fun notificationIdForDecision(decision: NotificationDecision): Int {
        return when (decision) {
            is NotificationDecision.AttemptResult -> CHECK_ATTEMPT_NOTIFICATION_ID
            is NotificationDecision.StateChanged -> CHECK_STATE_CHANGED_NOTIFICATION_ID
            is NotificationDecision.AccessRestored,
            is NotificationDecision.AccessRestoredAndStateChanged,
            -> CHECK_ACCESS_RESTORED_NOTIFICATION_ID
            is NotificationDecision.AttemptUnavailable -> CHECK_UNAVAILABLE_NOTIFICATION_ID
            is NotificationDecision.AttemptFailed -> CHECK_FAILED_NOTIFICATION_ID
            NotificationDecision.None -> CHECK_ATTEMPT_NOTIFICATION_ID
        }
    }

    private fun notifyOrPermissionError(
        notificationId: Int,
        notification: Notification,
    ): LocalNotificationResult {
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            LocalNotificationResult.Success
        } catch (exception: SecurityException) {
            LocalNotificationResult.PermissionNotGranted
        }
    }

    companion object {
        private const val WHITELIST_TURNED_ON_NOTIFICATION_ID = 1001
        private const val WHITELIST_TURNED_OFF_NOTIFICATION_ID = 1002
        private const val TEST_CHECK_NOTIFICATION_ID = 1003
        private const val CHECK_ATTEMPT_NOTIFICATION_ID = 1101
        private const val CHECK_STATE_CHANGED_NOTIFICATION_ID = 1102
        private const val CHECK_ACCESS_RESTORED_NOTIFICATION_ID = 1103
        private const val CHECK_UNAVAILABLE_NOTIFICATION_ID = 1104
        private const val CHECK_FAILED_NOTIFICATION_ID = 1105
    }
}
