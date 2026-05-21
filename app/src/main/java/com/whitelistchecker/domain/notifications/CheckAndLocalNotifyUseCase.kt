package com.whitelistchecker.domain.notifications

import com.whitelistchecker.domain.model.CheckAndLocalNotifyResult
import com.whitelistchecker.domain.monitor.WhitelistMonitorUseCase

class CheckAndLocalNotifyUseCase(
    private val whitelistMonitorUseCase: WhitelistMonitorUseCase,
    private val localNotificationEventUseCase: LocalNotificationEventUseCase,
) {

    suspend fun execute(): CheckAndLocalNotifyResult {
        val monitorResult = whitelistMonitorUseCase.checkAndUpdateState()
        val testNotificationResult = localNotificationEventUseCase.sendTestOnManualCheck(
            checkResult = monitorResult.checkResult,
        )
        val eventNotificationResult = localNotificationEventUseCase.notifyIfNeeded(
            event = monitorResult.stateChangeEvent,
            checkResult = monitorResult.checkResult,
        )
        val localNotificationResult = eventNotificationResult ?: testNotificationResult
        return CheckAndLocalNotifyResult(
            monitorResult = monitorResult,
            localNotificationResult = localNotificationResult,
        )
    }

    suspend fun loadMonitorState() = whitelistMonitorUseCase.loadMonitorState()
}
