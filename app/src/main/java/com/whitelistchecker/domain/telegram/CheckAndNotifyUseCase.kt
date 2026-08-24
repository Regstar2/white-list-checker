package com.whitelistchecker.domain.telegram

import android.util.Log
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.checkrun.CheckStateRepository
import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.domain.checkrun.CheckAlreadyRunningException
import com.whitelistchecker.domain.checkrun.CheckExecutionCoordinator
import com.whitelistchecker.domain.checkrun.CheckExecutionResult
import com.whitelistchecker.domain.checkrun.CheckOutcome
import com.whitelistchecker.domain.checkrun.NotificationDecisionEngine
import com.whitelistchecker.domain.checkrun.isValidWhitelistStatus
import com.whitelistchecker.domain.history.SaveCheckHistoryUseCase
import com.whitelistchecker.domain.model.CheckAndNotifyResult
import com.whitelistchecker.domain.model.CheckPersistenceStatus
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase
import com.whitelistchecker.domain.statistics.LocalStatisticsWriter
import com.whitelistchecker.domain.statistics.WhitelistTimelineWriter

class CheckAndNotifyUseCase(
    private val checkAndLocalNotifyUseCase: CheckAndLocalNotifyUseCase,
    private val telegramEventNotifierUseCase: TelegramEventNotifierUseCase,
    private val telegramQueueProcessor: TelegramQueueProcessor,
    private val pendingTelegramReportRepository: PendingTelegramReportRepository,
    private val checkExecutionCoordinator: CheckExecutionCoordinator,
    private val checkStateRepository: CheckStateRepository,
    private val notificationDecisionEngine: NotificationDecisionEngine,
    private val lastCheckRepository: LastCheckRepository,
    private val saveCheckHistoryUseCase: SaveCheckHistoryUseCase,
    private val localStatisticsWriter: LocalStatisticsWriter,
    private val whitelistTimelineWriter: WhitelistTimelineWriter,
) {

    suspend fun execute(
        triggerType: CheckTriggerType = CheckTriggerType.MANUAL_UI,
        notificationPolicy: NotificationPolicy = NotificationPolicy.STATE_CHANGE_ONLY,
        notifyOnAccessRestored: Boolean = false,
    ): CheckAndNotifyResult {
        return when (
            val result = tryExecute(
                triggerType = triggerType,
                notificationPolicy = notificationPolicy,
                notifyOnAccessRestored = notifyOnAccessRestored,
            )
        ) {
            is CheckExecutionResult.Completed -> result.value
            is CheckExecutionResult.AlreadyRunning -> throw CheckAlreadyRunningException(result.runningTrigger)
        }
    }

    suspend fun tryExecute(
        triggerType: CheckTriggerType = CheckTriggerType.MANUAL_UI,
        notificationPolicy: NotificationPolicy = NotificationPolicy.STATE_CHANGE_ONLY,
        notifyOnAccessRestored: Boolean = false,
    ): CheckExecutionResult<CheckAndNotifyResult> {
        return checkExecutionCoordinator.runSingle(triggerType) {
            executeSingle(
                triggerType = triggerType,
                notificationPolicy = notificationPolicy,
                notifyOnAccessRestored = notifyOnAccessRestored,
            )
        }
    }

    private suspend fun executeSingle(
        triggerType: CheckTriggerType,
        notificationPolicy: NotificationPolicy,
        notifyOnAccessRestored: Boolean,
    ): CheckAndNotifyResult {
        val queueFlushResult = flushTelegramQueueSafely()
        val startedAtMillis = System.currentTimeMillis()
        val previousSnapshot = checkStateRepository.getSnapshot()
        val previousValidState = previousSnapshot.lastValidWhitelistState
            ?: loadMigratedValidState()

        val monitorResult = try {
            checkAndLocalNotifyUseCase.checkOnly()
        } catch (exception: Exception) {
            val finishedAtMillis = System.currentTimeMillis()
            val failureOutcome = CheckOutcome.fromFailure(exception)
            val decision = notificationDecisionEngine.evaluate(
                policy = notificationPolicy,
                previousAttempt = previousSnapshot.lastAttemptOutcome,
                previousValidState = previousValidState,
                currentAttempt = failureOutcome,
                trigger = triggerType,
                notifyOnAccessRestored = notifyOnAccessRestored,
            )
            checkStateRepository.saveAfterAttempt(failureOutcome, finishedAtMillis)
            checkAndLocalNotifyUseCase.notifyDecisionIfNeeded(decision, checkResult = null)
            telegramEventNotifierUseCase.notifyDecisionIfNeeded(decision, checkResult = null)
            throw exception
        }

        val finishedAtMillis = System.currentTimeMillis()
        val checkResult = monitorResult.checkResult
        val currentOutcome = CheckOutcome.fromResult(checkResult)
        val decision = notificationDecisionEngine.evaluate(
            policy = notificationPolicy,
            previousAttempt = previousSnapshot.lastAttemptOutcome,
            previousValidState = previousValidState,
            currentAttempt = currentOutcome,
            trigger = triggerType,
            notifyOnAccessRestored = notifyOnAccessRestored,
        )

        lastCheckRepository.save(checkResult)
        val persistence = persistCheckResult(
            checkResult = checkResult,
            triggerType = triggerType,
            startedAtMillis = startedAtMillis,
            finishedAtMillis = finishedAtMillis,
        )
        checkStateRepository.saveAfterAttempt(currentOutcome, finishedAtMillis)
        val localNotificationResult = checkAndLocalNotifyUseCase.notifyDecisionIfNeeded(
            decision = decision,
            checkResult = checkResult,
        )
        val telegramSendResult = telegramEventNotifierUseCase.notifyDecisionIfNeeded(
            decision = decision,
            checkResult = checkResult,
        )

        return CheckAndNotifyResult(
            monitorResult = monitorResult,
            localNotificationResult = localNotificationResult,
            telegramSendResult = telegramSendResult,
            queueFlushResult = queueFlushResult,
            pendingReportsCount = pendingTelegramReportRepository.count(),
            persistenceStatus = persistence,
        )
    }

    private suspend fun persistCheckResult(
        checkResult: NetworkCheckResult,
        triggerType: CheckTriggerType,
        startedAtMillis: Long,
        finishedAtMillis: Long,
    ): CheckPersistenceStatus {
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

        return CheckPersistenceStatus(
            historySaved = savedHistory != null,
            technicalStatisticsUpdated = technicalStatisticsUpdated,
            whitelistTimelineUpdated = whitelistTimelineUpdated,
            errorMessage = persistenceError,
        )
    }

    private suspend fun flushTelegramQueueSafely(): TelegramQueueFlushResult {
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

    private suspend fun loadMigratedValidState(): WhitelistState? {
        val state = checkAndLocalNotifyUseCase.loadMonitorState().lastConfirmedState
        return state.takeIf { it.isValidWhitelistStatus() }
    }

    suspend fun flushPendingReports(): TelegramQueueFlushResult {
        return flushTelegramQueueSafely()
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
