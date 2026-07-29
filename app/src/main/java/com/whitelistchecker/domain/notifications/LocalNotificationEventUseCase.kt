package com.whitelistchecker.domain.notifications

import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.domain.checkrun.NotificationDecision
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class LocalNotificationEventUseCase(
    private val settingsRepository: LocalNotificationSettingsRepository,
    private val notificationSender: LocalNotificationSender,
) {

    suspend fun notifyIfNeeded(
        event: WhitelistStateChangeEvent?,
        checkResult: NetworkCheckResult,
    ): LocalNotificationResult? {
        if (event == null) return null
        if (event.type != WhitelistStateChangeType.WHITELIST_TURNED_ON &&
            event.type != WhitelistStateChangeType.WHITELIST_TURNED_OFF
        ) {
            return null
        }
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) {
            return LocalNotificationResult.Disabled
        }
        return notificationSender.sendStateChangeNotification(event, checkResult)
    }

    suspend fun sendTestOnManualCheck(checkResult: NetworkCheckResult): LocalNotificationResult {
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) {
            return LocalNotificationResult.Disabled
        }
        return notificationSender.sendTestCheckNotification(checkResult)
    }

    suspend fun notifyDecisionIfNeeded(
        decision: NotificationDecision,
        checkResult: NetworkCheckResult?,
    ): LocalNotificationResult? {
        if (decision == NotificationDecision.None) return null
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) {
            return LocalNotificationResult.Disabled
        }
        return notificationSender.sendDecisionNotification(decision, checkResult)
    }
}
