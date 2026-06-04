package com.whitelistchecker.ui.diagnostics

import com.whitelistchecker.domain.statistics.StatisticsDiagnostics

sealed class StatisticsDiagnosticsUiState {
    data object Idle : StatisticsDiagnosticsUiState()

    data object Loading : StatisticsDiagnosticsUiState()

    data class Content(
        val diagnostics: StatisticsDiagnostics,
        val rebuildState: RebuildStatisticsUiState = RebuildStatisticsUiState.Idle,
    ) : StatisticsDiagnosticsUiState()

    data class Error(
        val message: String,
    ) : StatisticsDiagnosticsUiState()
}

sealed class RebuildStatisticsUiState {
    data object Idle : RebuildStatisticsUiState()

    data object Running : RebuildStatisticsUiState()

    data object Success : RebuildStatisticsUiState()

    data class Failure(
        val message: String,
    ) : RebuildStatisticsUiState()
}
