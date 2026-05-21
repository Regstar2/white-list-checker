package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.domain.model.CheckAndNotifyResult
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase

class CheckAndNotifyUseCase(
    private val checkAndLocalNotifyUseCase: CheckAndLocalNotifyUseCase,
    private val telegramEventNotifierUseCase: TelegramEventNotifierUseCase,
    private val telegramQueueProcessor: TelegramQueueProcessor,
    private val pendingTelegramReportRepository: PendingTelegramReportRepository,
) {

    suspend fun execute(): CheckAndNotifyResult {
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

        val localResult = checkAndLocalNotifyUseCase.execute()
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
}
