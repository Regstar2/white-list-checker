package com.whitelistchecker.domain.statistics

import java.util.concurrent.TimeUnit

object CheckStatisticsConfig {
    const val SCHEMA_VERSION: Int = 1
    const val SUMMARY_ROW_ID: Int = 1
    const val MAX_DAILY_STATISTICS_DAYS: Int = 30
    val MAX_DAILY_STATISTICS_AGE_MS: Long =
        TimeUnit.DAYS.toMillis(MAX_DAILY_STATISTICS_DAYS.toLong())
}
