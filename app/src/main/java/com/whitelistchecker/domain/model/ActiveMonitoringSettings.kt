package com.whitelistchecker.domain.model

data class ActiveMonitoringSettings(
    val intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES,
    val notificationPolicy: NotificationPolicy = NotificationPolicy.STATE_CHANGE_ONLY,
    val notifyOnAccessRestored: Boolean = false,
    val telegramCommandsEnabled: Boolean = false,
) {
    val normalizedIntervalMinutes: Long
        get() = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    val isValidInterval: Boolean
        get() = intervalMinutes in MIN_INTERVAL_MINUTES..MAX_INTERVAL_MINUTES

    companion object {
        const val MIN_INTERVAL_MINUTES = 1L
        const val MAX_INTERVAL_MINUTES = 60L
        const val DEFAULT_INTERVAL_MINUTES = 5L
    }
}
