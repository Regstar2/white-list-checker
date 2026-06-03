package com.whitelistchecker.domain.telegram

import android.util.Log
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.domain.history.SaveCheckHistoryUseCase
import com.whitelistchecker.domain.model.CheckAndNotifyResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase

class CheckAndNotifyUseCase(
    private val checkAndLocalNotifyUseCase: CheckAndLocalNotifyUseCase,
    private val telegramEventNotifierUseCase: TelegramEventNotifierUseCase,
    private val telegramQueueProcessor: TelegramQueueProcessor,
    private val pendingTelegramReportRepository: PendingTelegramReportRepository,
    private val lastCheckRepository: LastCheckRepository,
    private val saveCheckHistoryUseCase: SaveCheckHistoryUseCase,
) {

    suspend fun execute(
        triggerType: CheckTriggerType = CheckTriggerType.MANUAL,
    ): CheckAndNotifyResult {
        val queueFlushResult = runCatching {
            telegramQueueProcessor.flushQueue()
        }.getOrElse { exception ->
            TelegramQueueFlushResult(
                attemptedCount = 0,
                sentCount = 0,
                failedCount = 1,
                skippedCount = 0,
                lastError = exception.message ?: exception.javaClass.simpleName,
            )
        }

        val startedAtMillis = System.currentTimeMillis()
        val localResult = checkAndLocalNotifyUseCase.execute()
        val finishedAtMillis = System.currentTimeMillis()
        val checkResult = localResult.monitorResult.checkResult
        lastCheckRepository.save(checkResult)
        runCatching {
            saveCheckHistoryUseCase.saveCompletedCheck(
                result = checkResult,
                triggerType = triggerType,
                startedAtMillis = startedAtMillis,
                finishedAtMillis = finishedAtMillis,
            )
        }.onFailure { exception ->
            Log.w(TAG, "Failed to save check history", exception)
        }
        val eventResult = telegramEventNotifierUseCase.notifyIfNeeded(
            event = localResult.monitorResult.stateChangeEvent,
            checkResult = localResult.monitorResult.checkResult,
        )

        return CheckAndNotifyResult(
            monitorResult = localResult.monitorResult,
            localNotificationResult = localResult.localNotificationResult,
            telegramSendResult = eventResult,
            queueFlushResult = queueFlushResult,
            pendingReportsCount = pendingTelegramReportRepository.count(),
        )
    }

    suspend fun flushPendingReports(): TelegramQueueFlushResult {
        return runCatching {
            telegramQueueProcessor.flushQueue()
        }.getOrElse { exception ->
            TelegramQueueFlushResult(
                attemptedCount = 0,
                sentCount = 0,
                failedCount = 1,
                skippedCount = 0,
                lastError = exception.message ?: exception.javaClass.simpleName,
            )
        }
    }

    suspend fun getPendingReportsCount(): Int = pendingTelegramReportRepository.count()

    suspend fun clearPendingReports() {
        pendingTelegramReportRepository.clear()
    }

    suspend fun loadMonitorState() = checkAndLocalNotifyUseCase.loadMonitorState()

    suspend fun sendLocalTestNotification(checkResult: NetworkCheckResult) =
        checkAndLocalNotifyUseCase.sendLocalTestNotification(checkResult)

    companion object {
        private const val TAG = "CheckAndNotifyUseCase"
    }
}
