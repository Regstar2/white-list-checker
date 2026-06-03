package com.whitelistchecker.domain.model

sealed class LastCheckLoadResult {
    data object None : LastCheckLoadResult()

    data class Success(val result: NetworkCheckResult) : LastCheckLoadResult()

    data class Error(val cause: Throwable) : LastCheckLoadResult()
}
