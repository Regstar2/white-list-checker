package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import com.whitelistchecker.domain.model.availability.WhitelistDailyAvailability
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats

data class WhitelistAvailabilityDashboard(
    val summary: WhitelistAvailabilitySummary,
    val daily: List<WhitelistDailyAvailability>,
    val targetStates: List<WhitelistTargetAvailabilityStats>,
    val recentEvents: List<WhitelistAvailabilityEvent>,
    val topAvailableTargets: List<WhitelistTargetAvailabilityStats>,
    val topStableTargets: List<WhitelistTargetAvailabilityStats>,
    val topUnstableTargets: List<WhitelistTargetAvailabilityStats>,
    val lastUpdatedAt: Long?,
    val isStale: Boolean,
)

sealed class WhitelistAvailabilityLoadResult {
    data object Empty : WhitelistAvailabilityLoadResult()

    data class Success(val dashboard: WhitelistAvailabilityDashboard) : WhitelistAvailabilityLoadResult()

    data class Failure(val cause: Throwable) : WhitelistAvailabilityLoadResult()
}
