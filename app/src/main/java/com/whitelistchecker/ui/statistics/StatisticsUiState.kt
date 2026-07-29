package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.statistics.StatisticsDashboard
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard

sealed class StatisticsUiState {
    data object Loading : StatisticsUiState()

    data object Empty : StatisticsUiState()

    data class Content(
        val dashboard: StatisticsDashboard,
        val whitelistTimeline: WhitelistTimelineDashboard? = null,
        val whitelistTimelineEmpty: Boolean = false,
        val freshness: StatisticsFreshnessUi = StatisticsFreshnessUi(
            dataUpdatedAt = null,
            isStale = false,
            isLowSample = true,
            lastCheckAt = null,
            lastCheckStatus = LastCheckTechnicalStatus.NONE,
            targetsCheckedAvailable = 0,
            targetsCheckedTotal = 0,
        ),
    ) : StatisticsUiState()

    data class Error(
        val message: String,
    ) : StatisticsUiState()
}

sealed class HomeStatisticsUiState {
    data object Hidden : HomeStatisticsUiState()

    data object Loading : HomeStatisticsUiState()

    data class Content(
        val currentState: WhitelistBinaryState,
        val currentStateAtMillis: Long?,
        val whitelistOnPercent: Double?,
        val binarySamples: Int,
        val lastUpdatedAt: Long?,
        val isStale: Boolean,
    ) : HomeStatisticsUiState()

    data object Error : HomeStatisticsUiState()
}
