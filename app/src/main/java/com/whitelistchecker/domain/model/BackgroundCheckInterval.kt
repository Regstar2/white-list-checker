package com.whitelistchecker.domain.model

enum class BackgroundCheckInterval(
    val minutes: Long,
    val label: String,
) {
    FIFTEEN_MINUTES(15, "15 минут"),
    THIRTY_MINUTES(30, "30 минут"),
    SIXTY_MINUTES(60, "60 минут"),
    ;

    companion object {
        fun fromMinutes(minutes: Long): BackgroundCheckInterval {
            return entries.firstOrNull { it.minutes == minutes } ?: FIFTEEN_MINUTES
        }
    }
}
