package com.whitelistchecker.domain.model

data class BackgroundCheckSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 15,
) {
    val normalizedIntervalMinutes: Long
        get() = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)

    val isValidInterval: Boolean
        get() = intervalMinutes >= MIN_INTERVAL_MINUTES

    val isPresetInterval: Boolean
        get() = intervalMinutes in PRESET_INTERVALS

    companion object {
        const val MIN_INTERVAL_MINUTES = 15L
        val PRESET_INTERVALS = listOf(15L, 30L, 60L)
    }
}
