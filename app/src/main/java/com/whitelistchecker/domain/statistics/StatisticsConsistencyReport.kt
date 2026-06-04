package com.whitelistchecker.domain.statistics

data class StatisticsConsistencyReport(
    val warnings: List<StatisticsConsistencyWarningCode> = emptyList(),
) {
    val hasWarnings: Boolean = warnings.isNotEmpty()
}
