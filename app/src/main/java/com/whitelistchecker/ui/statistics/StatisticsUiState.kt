package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.availability.WhitelistAvailabilityDashboard
import com.whitelistchecker.domain.statistics.StatisticsDashboard

sealed class StatisticsUiState {
    data object Loading : StatisticsUiState()

    data object Empty : StatisticsUiState()

    data class Content(
        val dashboard: StatisticsDashboard,
        val whitelistAvailability: WhitelistAvailabilityDashboard? = null,
        val whitelistAvailabilityEmpty: Boolean = false,
    ) : StatisticsUiState()

    data class Error(
        val message: String,
    ) : StatisticsUiState()
}

sealed class HomeStatisticsUiState {
    data object Hidden : HomeStatisticsUiState()

    data object Loading : HomeStatisticsUiState()

    data class Content(
        val totalRuns: Int,
        val fullySuccessfulRate: Double?,
        val partialFailureRuns: Int,
        val failureRuns: Int,
        val lastRunAt: Long?,
        val consecutiveFullFailureCount: Int,
        val isStale: Boolean,
    ) : HomeStatisticsUiState()

    data object Error : HomeStatisticsUiState()
}
