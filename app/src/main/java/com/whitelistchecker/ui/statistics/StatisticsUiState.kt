package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.statistics.StatisticsDashboard

sealed class StatisticsUiState {
    data object Loading : StatisticsUiState()

    data object Empty : StatisticsUiState()

    data class Content(
        val dashboard: StatisticsDashboard,
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
        val successRate: Double?,
        val lastSuccessAt: Long?,
        val consecutiveFailureCount: Int,
        val isStale: Boolean,
    ) : HomeStatisticsUiState()

    data object Error : HomeStatisticsUiState()
}
