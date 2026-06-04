package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.statistics.StatisticsDiagnosticsMetaRepository

class SaveCheckHistoryUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val mapper: CheckHistoryFromNetworkResultMapper,
    private val appVersionProvider: () -> String,
    private val diagnosticsMetaRepository: StatisticsDiagnosticsMetaRepository? = null,
) {

    suspend fun saveCompletedCheck(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
        startedAtMillis: Long,
        finishedAtMillis: Long,
    ): SavedCheckHistory {
        val (checkRun, targetResults) = mapper.toCheckRun(
            result = result,
            triggerType = triggerType,
            startedAtMillis = startedAtMillis,
            finishedAtMillis = finishedAtMillis,
            appVersion = appVersionProvider(),
        )
        checkHistoryRepository.saveCheckRun(checkRun, targetResults)
        val deletedCount = checkHistoryRepository.applyRetentionPolicy(finishedAtMillis)
        if (deletedCount > 0) {
            diagnosticsMetaRepository?.recordCleanupCompleted(finishedAtMillis)
        }
        return SavedCheckHistory(checkRun = checkRun, targetResults = targetResults)
    }
}
