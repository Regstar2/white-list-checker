package com.whitelistchecker.domain.model

enum class BackgroundCheckInterval(
    val minutes: Long,
) {
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    SIXTY_MINUTES(60),
    ;

    companion object {
        fun fromMinutes(minutes: Long): BackgroundCheckInterval {
            return entries.firstOrNull { it.minutes == minutes } ?: FIFTEEN_MINUTES
        }
    }
}
