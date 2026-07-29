package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.WhitelistState

data class CheckStateSnapshot(
    val lastAttemptOutcome: CheckOutcome = CheckOutcome.Unknown,
    val lastAttemptAtMillis: Long? = null,
    val lastValidWhitelistState: WhitelistState? = null,
    val lastValidWhitelistAtMillis: Long? = null,
)
