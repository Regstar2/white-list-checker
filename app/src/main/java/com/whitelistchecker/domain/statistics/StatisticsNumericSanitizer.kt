package com.whitelistchecker.domain.statistics

object StatisticsNumericSanitizer {

    fun sanitizeSuccessRate(rate: Double?): Double? {
        if (rate == null) return null
        if (rate.isNaN() || rate.isInfinite() || rate < 0.0) return null
        return rate.coerceAtMost(1.0)
    }

    fun sanitizeLatencyMs(latencyMs: Long?): Long? {
        if (latencyMs == null) return null
        if (latencyMs < 0L) return null
        return latencyMs
    }

    fun hasInvalidSuccessRate(rate: Double?): Boolean {
        return rate != null && sanitizeSuccessRate(rate) == null
    }

    fun hasInvalidAverageLatency(latencyMs: Long?): Boolean {
        return latencyMs != null && sanitizeLatencyMs(latencyMs) == null
    }

    fun hasNegativeCount(count: Int): Boolean = count < 0
}
