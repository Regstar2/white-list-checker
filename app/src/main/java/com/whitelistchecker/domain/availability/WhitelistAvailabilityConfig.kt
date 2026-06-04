package com.whitelistchecker.domain.availability

import java.util.concurrent.TimeUnit

object WhitelistAvailabilityConfig {
    const val SCHEMA_VERSION: Int = 1
    const val SUMMARY_ROW_ID: Int = 1
    const val MAX_EVENTS: Int = 500
    const val MAX_DAILY_DAYS: Int = 30
    const val CHART_DAYS_LIMIT: Int = 14
    const val TOP_TARGETS_LIMIT: Int = 5
    const val RECENT_EVENTS_LIMIT: Int = 10
    const val TARGET_STATES_LIMIT: Int = 32
    val MAX_EVENT_AGE_MS: Long = TimeUnit.DAYS.toMillis(MAX_DAILY_DAYS.toLong())
}
