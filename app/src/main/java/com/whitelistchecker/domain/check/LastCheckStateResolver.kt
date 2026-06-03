package com.whitelistchecker.domain.check

import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.NetworkCheckResult

class LastCheckStateResolver {

    fun resolve(
        isChecking: Boolean,
        loadFailed: Boolean,
        lastCheck: NetworkCheckResult?,
        nowMillis: Long,
        staleThresholdMs: Long = LastCheckConfig.DEFAULT_LAST_CHECK_STALE_THRESHOLD_MS,
    ): LastCheckDisplayState {
        if (isChecking) {
            return LastCheckDisplayState.Running
        }
        if (loadFailed) {
            return LastCheckDisplayState.LoadError
        }
        if (lastCheck == null) {
            return LastCheckDisplayState.NoCheck
        }
        val freshness = if (nowMillis - lastCheck.checkedAtMillis > staleThresholdMs) {
            LastCheckFreshness.STALE
        } else {
            LastCheckFreshness.FRESH
        }
        val outcome = if (lastCheck.error.isNullOrBlank()) {
            LastCheckOutcome.SUCCESS
        } else {
            LastCheckOutcome.FAILURE
        }
        return LastCheckDisplayState.Available(
            result = lastCheck,
            freshness = freshness,
            outcome = outcome,
        )
    }
}
