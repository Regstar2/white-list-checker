package com.whitelistchecker.domain.model

sealed interface LocalNotificationResult {

    data object Success : LocalNotificationResult

    data object Disabled : LocalNotificationResult

    data object PermissionNotGranted : LocalNotificationResult

    data class Failure(
        val reason: String,
    ) : LocalNotificationResult
}
