package com.whitelistchecker.domain.notifications

import com.whitelistchecker.domain.model.CheckAndLocalNotifyResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistMonitorResult
import com.whitelistchecker.domain.monitor.WhitelistMonitorUseCase

class CheckAndLocalNotifyUseCase(
    private val whitelistMonitorUseCase: WhitelistMonitorUseCase,
    private val localNotificationEventUseCase: LocalNotificationEventUseCase,
) {

    suspend fun execute(): CheckAndLocalNotifyResult {
        val monitorResult = checkOnly()
        val eventNotificationResult = localNotificationEventUseCase.notifyIfNeeded(
            event = monitorResult.stateChangeEvent,
            checkResult = monitorResult.checkResult,
        )
        return CheckAndLocalNotifyResult(
            monitorResult = monitorResult,
            localNotificationResult = eventNotificationResult,
        )
    }

    suspend fun checkOnly(): WhitelistMonitorResult {
        return whitelistMonitorUseCase.checkAndUpdateState()
    }

    suspend fun notifyDecisionIfNeeded(
        decision: com.whitelistchecker.domain.checkrun.NotificationDecision,
        checkResult: NetworkCheckResult?,
    ) = localNotificationEventUseCase.notifyDecisionIfNeeded(decision, checkResult)

    suspend fun sendLocalTestNotification(checkResult: NetworkCheckResult) =
        localNotificationEventUseCase.sendTestOnManualCheck(checkResult)

    suspend fun loadMonitorState() = whitelistMonitorUseCase.loadMonitorState()
}
