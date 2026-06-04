package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySnapshot
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import com.whitelistchecker.domain.model.availability.WhitelistDailyAvailability
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult

interface WhitelistAvailabilityRepository {

    suspend fun updateFromCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    )

    suspend fun getSummary(): WhitelistAvailabilitySummary

    suspend fun summaryHasData(): Boolean

    suspend fun getDailyStatistics(limit: Int): List<WhitelistDailyAvailability>

    suspend fun getTargetStatistics(limit: Int): List<WhitelistTargetAvailabilityStats>

    suspend fun getRecentEvents(limit: Int): List<WhitelistAvailabilityEvent>

    suspend fun replaceAll(snapshot: WhitelistAvailabilitySnapshot, events: List<WhitelistAvailabilityEvent>)

    suspend fun clearAll()

    suspend fun applyRetention(nowMillis: Long)
}
