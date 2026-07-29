package com.whitelistchecker.domain.telegram

import android.util.Log
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.domain.history.SaveCheckHistoryUseCase
import com.whitelistchecker.domain.statistics.LocalStatisticsWriter
import com.whitelistchecker.domain.statistics.WhitelistTimelineWriter
import com.whitelistchecker.domain.model.CheckAndNotifyResult
import com.whitelistchecker.domain.model.CheckPersistenceStatus
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
    private val localStatisticsWriter: LocalStatisticsWriter,
    private val whitelistTimelineWriter: WhitelistTimelineWriter,
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
        val savedHistoryResult = runCatching {
            saveCheckHistoryUseCase.saveCompletedCheck(
                result = checkResult,
                triggerType = triggerType,
                startedAtMillis = startedAtMillis,
                finishedAtMillis = finishedAtMillis,
            )
        }.onFailure { exception ->
            Log.w(TAG, "Failed to save check history", exception)
        }
        val savedHistory = savedHistoryResult.getOrNull()

        var technicalStatisticsUpdated = false
        var whitelistTimelineUpdated = false
        var persistenceError = savedHistoryResult.exceptionOrNull()?.let { exception ->
            "История проверки не сохранена: ${exception.message ?: exception.javaClass.simpleName}"
        }

        if (savedHistory != null) {
            val technicalStatisticsResult = runCatching {
                localStatisticsWriter.onCheckRunSaved(
                    checkRun = savedHistory.checkRun,
                    targetResults = savedHistory.targetResults,
                )
            }.onFailure { exception ->
                Log.w(TAG, "Failed to update check statistics", exception)
            }
            technicalStatisticsUpdated = technicalStatisticsResult.getOrDefault(false)
            if (!technicalStatisticsUpdated && persistenceError == null) {
                persistenceError = "История сохранена, но техническая статистика не обновилась"
            }

            val whitelistTimelineResult = runCatching {
                whitelistTimelineWriter.onCheckRunSaved(checkRun = savedHistory.checkRun)
            }.onFailure { exception ->
                Log.w(TAG, "Failed to update whitelist timeline statistics", exception)
            }
            whitelistTimelineUpdated = whitelistTimelineResult.getOrDefault(false)
            if (!whitelistTimelineUpdated && persistenceError == null) {
                persistenceError = "История сохранена, но график состояния белых списков не обновился"
            }
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
            persistenceStatus = CheckPersistenceStatus(
                historySaved = savedHistory != null,
                technicalStatisticsUpdated = technicalStatisticsUpdated,
                whitelistTimelineUpdated = whitelistTimelineUpdated,
                errorMessage = persistenceError,
            ),
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
