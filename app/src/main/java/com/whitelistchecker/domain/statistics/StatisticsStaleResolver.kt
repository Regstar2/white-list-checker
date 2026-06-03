package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.check.LastCheckConfig

class StatisticsStaleResolver {

    fun isStale(
        lastRunAtMillis: Long?,
        nowMillis: Long,
        staleThresholdMs: Long = LastCheckConfig.DEFAULT_LAST_CHECK_STALE_THRESHOLD_MS,
    ): Boolean {
        if (lastRunAtMillis == null) return false
        return nowMillis - lastRunAtMillis > staleThresholdMs
    }
}
