package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent

data class MainUiState(
    val isChecking: Boolean = false,
    val result: NetworkCheckResult? = null,
    val monitorState: WhitelistMonitorState? = null,
    val lastStateChangeEvent: WhitelistStateChangeEvent? = null,
    val localNotificationSettings: LocalNotificationSettings = LocalNotificationSettings(),
    val notificationsAllowed: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val lastLocalNotificationResult: LocalNotificationResult? = null,
    val errorMessage: String? = null,
)
