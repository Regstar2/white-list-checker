package com.whitelistchecker.domain.history

import java.util.concurrent.TimeUnit

object CheckHistoryConfig {
    const val SCHEMA_VERSION: Int = 1
    const val MAX_CHECK_RUNS: Int = 200
    val MAX_CHECK_RUN_AGE_MS: Long = TimeUnit.DAYS.toMillis(14)
    const val ROUTE_MODE_CELLULAR: String = "CELLULAR"
}
