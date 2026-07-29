package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.history.CheckHistoryRepository

class RebuildWhitelistTimelineUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val whitelistTimelineRepository: WhitelistTimelineRepository,
) {

    suspend fun rebuildFromHistory(): RebuildStatisticsResult {
        return try {
            val runs = checkHistoryRepository.getRecentCheckRuns(Int.MAX_VALUE)
                .map { it.run }
                .sortedBy { it.finishedAtMillis }
            whitelistTimelineRepository.replaceAll(runs.map { it.toTimelineSample() })
            RebuildStatisticsResult.Success
        } catch (exception: Exception) {
            RebuildStatisticsResult.Failure(exception)
        }
    }
}
