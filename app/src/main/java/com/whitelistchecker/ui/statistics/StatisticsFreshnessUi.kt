package com.whitelistchecker.ui.statistics

enum class LastCheckTechnicalStatus {
    NONE,
    COMPLETED,
    PARTIAL,
    FAILED,
}

data class StatisticsFreshnessUi(
    val dataUpdatedAt: Long?,
    val isStale: Boolean,
    val isLowSample: Boolean,
    val lastCheckAt: Long?,
    val lastCheckStatus: LastCheckTechnicalStatus,
    val targetsCheckedAvailable: Int,
    val targetsCheckedTotal: Int,
)
