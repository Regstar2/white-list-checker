package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.history.CheckHistoryConfig
import com.whitelistchecker.domain.history.CheckHistoryRepository

class RebuildCheckStatisticsUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val checkStatisticsRepository: CheckStatisticsRepository,
    private val calculator: CheckStatisticsCalculator,
) {

    suspend fun rebuildFromHistory(): RebuildStatisticsResult {
        return try {
            val runs = checkHistoryRepository.getRecentCheckRuns(CheckHistoryConfig.MAX_CHECK_RUNS)
                .asReversed()
            val snapshot = calculator.rebuildFromHistory(runs)
            checkStatisticsRepository.replaceAll(snapshot)
            RebuildStatisticsResult.Success
        } catch (exception: Exception) {
            RebuildStatisticsResult.Failure(exception)
        }
    }
}
