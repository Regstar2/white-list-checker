package com.whitelistchecker.domain.model

data class TargetGroupSummary(
    val group: TargetGroup,
    val availableCount: Int,
    val totalCount: Int,
) {
    val availabilityRate: Double
        get() {
            if (totalCount == 0) {
                return 0.0
            }
            return availableCount.toDouble() / totalCount.toDouble()
        }
}
