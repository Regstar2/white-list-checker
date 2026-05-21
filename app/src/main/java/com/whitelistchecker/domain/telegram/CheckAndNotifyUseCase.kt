package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.CheckAndNotifyResult
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase

class CheckAndNotifyUseCase(
    private val checkAndLocalNotifyUseCase: CheckAndLocalNotifyUseCase,
    private val telegramEventNotifierUseCase: TelegramEventNotifierUseCase,
) {

    suspend fun execute(): CheckAndNotifyResult {
        val localResult = checkAndLocalNotifyUseCase.execute()
        val manualCheckResult = telegramEventNotifierUseCase.sendOnManualCheck(
            checkResult = localResult.monitorResult.checkResult,
        )
        val eventResult = telegramEventNotifierUseCase.notifyIfNeeded(
            event = localResult.monitorResult.stateChangeEvent,
            checkResult = localResult.monitorResult.checkResult,
        )
        return CheckAndNotifyResult(
            monitorResult = localResult.monitorResult,
            localNotificationResult = localResult.localNotificationResult,
            telegramSendResult = eventResult ?: manualCheckResult,
        )
    }

    suspend fun loadMonitorState() = checkAndLocalNotifyUseCase.loadMonitorState()
}
