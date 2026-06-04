package com.whitelistchecker.domain.statistics

sealed class RebuildStatisticsResult {
    data object Success : RebuildStatisticsResult()

    data class Failure(val cause: Throwable) : RebuildStatisticsResult()
}
