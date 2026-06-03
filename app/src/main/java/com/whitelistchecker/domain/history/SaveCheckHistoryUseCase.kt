package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.history.CheckTriggerType

class SaveCheckHistoryUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val mapper: CheckHistoryFromNetworkResultMapper,
    private val appVersionProvider: () -> String,
) {

    suspend fun saveCompletedCheck(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
        startedAtMillis: Long,
        finishedAtMillis: Long,
    ) {
        val (checkRun, targetResults) = mapper.toCheckRun(
            result = result,
            triggerType = triggerType,
            startedAtMillis = startedAtMillis,
            finishedAtMillis = finishedAtMillis,
            appVersion = appVersionProvider(),
        )
        checkHistoryRepository.saveCheckRun(checkRun, targetResults)
        checkHistoryRepository.applyRetentionPolicy(finishedAtMillis)
    }
}
