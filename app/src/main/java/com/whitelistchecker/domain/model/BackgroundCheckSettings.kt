package com.whitelistchecker.domain.model

data class BackgroundCheckSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 15,
) {
    val normalizedIntervalMinutes: Long
        get() = when (intervalMinutes) {
            15L, 30L, 60L -> intervalMinutes
            else -> 15L
        }
}
