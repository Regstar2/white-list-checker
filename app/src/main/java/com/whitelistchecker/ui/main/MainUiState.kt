package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.BackgroundCheckStatus
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.ui.navigation.AppScreen

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
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
    val isSendingCheckReport: Boolean = false,
    val isFlushingTelegramQueue: Boolean = false,
    val lastTelegramTestResult: TelegramTestResult? = null,
    val lastTelegramTestMessage: String? = null,
    val lastTelegramSendResult: TelegramSendResult? = null,
    val lastTelegramSendMessage: String? = null,
    val lastQueueFlushResult: TelegramQueueFlushResult? = null,
    val pendingReportsCount: Int = 0,
    val backgroundCheckSettings: BackgroundCheckSettings = BackgroundCheckSettings(),
    val backgroundCheckStatus: BackgroundCheckStatus = BackgroundCheckStatus(),
    val isSavingBackgroundSettings: Boolean = false,
    val useCustomInterval: Boolean = false,
    val customIntervalInput: String = "15",
    val intervalError: String? = null,
    val checkTargets: List<EditableCheckTarget> = emptyList(),
    val telegramChatDiscovery: TelegramChatDiscoveryUiState = TelegramChatDiscoveryUiState(),
    val errorMessage: String? = null,
)
