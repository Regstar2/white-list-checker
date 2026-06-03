package com.whitelistchecker.domain.model

sealed class LastCheckDisplayState {
    data object NoCheck : LastCheckDisplayState()

    data object Running : LastCheckDisplayState()

    data object LoadError : LastCheckDisplayState()

    data class Available(
        val result: NetworkCheckResult,
        val freshness: LastCheckFreshness,
        val outcome: LastCheckOutcome,
    ) : LastCheckDisplayState()
}

enum class LastCheckFreshness {
    FRESH,
    STALE,
}

enum class LastCheckOutcome {
    SUCCESS,
    FAILURE,
}
