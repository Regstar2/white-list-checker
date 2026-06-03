package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult

class LocalStatisticsWriter(
    private val checkStatisticsRepository: CheckStatisticsRepository,
) {

    suspend fun onCheckRunSaved(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ) {
        checkStatisticsRepository.updateFromCheckRun(checkRun, targetResults)
    }
}
