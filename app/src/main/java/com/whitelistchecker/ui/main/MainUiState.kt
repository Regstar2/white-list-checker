package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.BackgroundCheckStatus
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringStatus
import com.whitelistchecker.domain.model.CheckPersistenceStatus
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.PublicServiceLink
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.PublicServiceStatus
import com.whitelistchecker.domain.model.UserArea
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.diagnostics.StatisticsDiagnosticsUiState
import com.whitelistchecker.ui.statistics.HomeStatisticsUiState
import com.whitelistchecker.ui.statistics.StatisticsUiState

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val isChecking: Boolean = false,
    val result: NetworkCheckResult? = null,
    val lastCheckDisplayState: LastCheckDisplayState = LastCheckDisplayState.NoCheck,
    val monitorState: WhitelistMonitorState? = null,
    val lastStateChangeEvent: WhitelistStateChangeEvent? = null,
    val localNotificationSettings: LocalNotificationSettings = LocalNotificationSettings(),
    val notificationsAllowed: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val lastLocalNotificationResult: LocalNotificationResult? = null,
    val telegramSettings: TelegramSettings = TelegramSettings(),
    val isTestingTelegram: Boolean = false,
    val isSendingTelegramTest: Boolean = false,
    val isSendingLocalTest: Boolean = false,
    val isSendingCheckReport: Boolean = false,
    val isFlushingTelegramQueue: Boolean = false,
    val lastTelegramTestResult: TelegramTestResult? = null,
    val lastTelegramTestMessage: String? = null,
    val lastTelegramSendResult: TelegramSendResult? = null,
    val lastTelegramSendMessage: String? = null,
    val lastQueueFlushResult: TelegramQueueFlushResult? = null,
    val pendingReportsCount: Int = 0,
    val lastPersistenceStatus: CheckPersistenceStatus? = null,
    val backgroundCheckSettings: BackgroundCheckSettings = BackgroundCheckSettings(),
    val backgroundCheckStatus: BackgroundCheckStatus = BackgroundCheckStatus(),
    val activeMonitoringSettings: ActiveMonitoringSettings = ActiveMonitoringSettings(),
    val activeMonitoringStatus: ActiveMonitoringStatus = ActiveMonitoringStatus(),
    val publicServiceSettings: PublicServiceSettings = PublicServiceSettings(),
    val publicServiceStatus: PublicServiceStatus = PublicServiceStatus(),
    val publicServiceLinks: List<PublicServiceLink> = emptyList(),
    val pendingDetectedArea: UserArea? = null,
    val isDetectingPublicServiceArea: Boolean = false,
    val isDetectingPublicServiceOperator: Boolean = false,
    val isSavingPublicServiceSettings: Boolean = false,
    val isCreatingPublicServiceLinkCode: Boolean = false,
    val isDeletingPublicServiceData: Boolean = false,
    val publicServiceMessage: String? = null,
    val activeMonitoringIntervalInput: String = ActiveMonitoringSettings.DEFAULT_INTERVAL_MINUTES.toString(),
    val activeMonitoringIntervalError: String? = null,
    val isSavingBackgroundSettings: Boolean = false,
    val useCustomInterval: Boolean = false,
    val customIntervalInput: String = "15",
    val intervalError: String? = null,
    val checkTargets: List<EditableCheckTarget> = emptyList(),
    val dnsServers: List<EditableDnsServer> = emptyList(),
    val telegramChatDiscovery: TelegramChatDiscoveryUiState = TelegramChatDiscoveryUiState(),
    val errorMessage: String? = null,
    val statisticsUiState: StatisticsUiState = StatisticsUiState.Loading,
    val homeStatisticsUiState: HomeStatisticsUiState = HomeStatisticsUiState.Loading,
    val statisticsDiagnosticsUiState: StatisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Idle,
)
