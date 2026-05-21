package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
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
    val telegramSettings: TelegramSettings = TelegramSettings(),
    val isTestingTelegram: Boolean = false,
    val isSendingTelegramTest: Boolean = false,
    val lastTelegramTestResult: TelegramTestResult? = null,
    val lastTelegramTestMessage: String? = null,
    val lastTelegramSendResult: TelegramSendResult? = null,
    val lastTelegramSendMessage: String? = null,
    val telegramChatDiscovery: TelegramChatDiscoveryUiState = TelegramChatDiscoveryUiState(),
    val errorMessage: String? = null,
)
