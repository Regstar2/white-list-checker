package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard

object HomeStatisticsMapper {

    fun map(dashboard: WhitelistTimelineDashboard?): HomeStatisticsUiState {
        if (dashboard == null) return HomeStatisticsUiState.Hidden
        if (dashboard.totalSamples <= 0) {
            return HomeStatisticsUiState.Hidden
        }
        return HomeStatisticsUiState.Content(
            currentState = dashboard.currentState,
            currentStateAtMillis = dashboard.currentStateAtMillis,
            whitelistOnPercent = dashboard.whitelistOnPercent,
            binarySamples = dashboard.binarySamples,
            lastUpdatedAt = dashboard.lastUpdatedAt,
            isStale = dashboard.isStale,
        )
    }
}
