package com.whitelistchecker.domain.check

import java.util.concurrent.TimeUnit

object LastCheckConfig {
    val DEFAULT_LAST_CHECK_STALE_THRESHOLD_MS: Long =
        TimeUnit.HOURS.toMillis(24)
}
