package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.availability.WhitelistAvailabilityDashboard

object HomeStatisticsMapper {

    fun map(dashboard: WhitelistAvailabilityDashboard?): HomeStatisticsUiState {
        if (dashboard == null) return HomeStatisticsUiState.Hidden
        val summary = dashboard.summary
        if (summary.totalTargets <= 0 &&
            summary.totalBecameAvailableEvents == 0 &&
            summary.totalBecameUnavailableEvents == 0
        ) {
            return HomeStatisticsUiState.Hidden
        }
        return HomeStatisticsUiState.Content(
            availableTargets = summary.currentlyAvailableTargets,
            availabilityPercent = summary.availabilityPercent,
            periodChanges = summary.totalBecameAvailableEvents + summary.totalBecameUnavailableEvents,
            lastUpdatedAt = dashboard.lastUpdatedAt ?: summary.lastUpdatedAt,
            isStale = dashboard.isStale,
        )
    }
}
