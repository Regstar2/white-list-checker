package com.whitelistchecker.domain.statistics

import java.util.concurrent.TimeUnit

object WhitelistTimelineConfig {
    val MAX_SAMPLE_AGE_MS: Long = TimeUnit.DAYS.toMillis(370)
    const val MAX_SAMPLES: Int = 50_000
    val LOAD_RANGE_MS: Long = TimeUnit.DAYS.toMillis(370)
}
