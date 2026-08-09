package com.whitelistchecker.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.whitelistchecker.ui.autocheck.AutoCheckScreen
import com.whitelistchecker.ui.checksettings.CheckSettingsScreen
import com.whitelistchecker.ui.diagnostics.DiagnosticsScreen
import com.whitelistchecker.ui.home.HomeScreen
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.notifications.LocalNotificationsScreen
import com.whitelistchecker.ui.notifications.NotificationsScreen
import com.whitelistchecker.ui.notifications.TelegramNotificationsScreen
import com.whitelistchecker.ui.notifications.TelegramQueueScreen
import com.whitelistchecker.ui.notifications.TelegramRecipientDiscoveryScreen
import com.whitelistchecker.ui.notifications.TelegramWorkerSetupScreen
import com.whitelistchecker.ui.publicservice.PublicServiceScreen
import com.whitelistchecker.ui.statistics.StatisticsScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.detectPublicServiceArea()
        } else {
            viewModel.markPublicServiceLocationPermissionDenied()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshNotificationPermissionState()
        }
    }

    BackHandler(enabled = uiState.currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    when (uiState.currentScreen) {
        AppScreen.HOME -> HomeScreen(
            uiState = uiState,
            onCheckMobileNetwork = viewModel::checkMobileNetwork,
            onOpenScreen = viewModel::openScreen,
            onRefreshLastCheckPresentation = viewModel::refreshLastCheckPresentation,
        )
        AppScreen.STATISTICS -> StatisticsScreen(
            uiState = uiState.statisticsUiState,
            onBack = viewModel::goHome,
            onRetry = viewModel::retryStatisticsLoad,
            onOpenDiagnostics = { viewModel.openScreen(AppScreen.DIAGNOSTICS) },
        )
        AppScreen.NOTIFICATIONS -> NotificationsScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onLocalEnabledChange = viewModel::updateLocalNotificationsEnabled,
            onTelegramEnabledChange = viewModel::updateTelegramEnabled,
            onOpenLocalNotifications = { viewModel.openScreen(AppScreen.LOCAL_NOTIFICATIONS) },
            onOpenTelegramNotifications = { viewModel.openScreen(AppScreen.TELEGRAM_NOTIFICATIONS) },
        )
        AppScreen.LOCAL_NOTIFICATIONS -> LocalNotificationsScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onLocalEnabledChange = viewModel::updateLocalNotificationsEnabled,
            onSendLocalTest = viewModel::sendLocalTestNotification,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
            onOpenAppSettings = viewModel::openAppDetailsSettings,
        )
        AppScreen.TELEGRAM_NOTIFICATIONS -> TelegramNotificationsScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onTelegramEnabledChange = viewModel::updateTelegramEnabled,
            onOpenWorkerSetup = { viewModel.openScreen(AppScreen.TELEGRAM_WORKER_SETUP) },
            onOpenRecipientDiscovery = { viewModel.openScreen(AppScreen.TELEGRAM_RECIPIENT_DISCOVERY) },
            onOpenQueue = { viewModel.openScreen(AppScreen.TELEGRAM_QUEUE) },
            onTestWorker = viewModel::testTelegramWorker,
            onSendTestMessage = viewModel::sendTelegramTestMessage,
            onSendCheckReport = viewModel::sendTelegramCheckReport,
            onRemoveRecipient = viewModel::removeTelegramRecipient,
            onToggleRecipient = viewModel::setTelegramRecipientEnabled,
        )
        AppScreen.TELEGRAM_WORKER_SETUP -> TelegramWorkerSetupScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onWorkerUrlChange = viewModel::updateTelegramWorkerUrl,
            onRelaySecretChange = viewModel::updateTelegramRelaySecret,
            onSaveTelegramSettings = viewModel::saveTelegramSettings,
            onTestWorker = viewModel::testTelegramWorker,
        )
        AppScreen.TELEGRAM_RECIPIENT_DISCOVERY -> TelegramRecipientDiscoveryScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onPrepareChatDiscovery = viewModel::prepareTelegramChatDiscovery,
            onFindChatId = viewModel::findTelegramChatId,
            onFindRecentChats = viewModel::findRecentTelegramChats,
            onResetChatDiscovery = viewModel::resetTelegramChatDiscovery,
            onAddRecipient = viewModel::addTelegramRecipient,
        )
        AppScreen.TELEGRAM_QUEUE -> TelegramQueueScreen(
            uiState = uiState,
            onBack = viewModel::navigateBack,
            onRetryQueue = viewModel::retryPendingTelegramReports,
            onClearQueue = viewModel::clearPendingTelegramReports,
        )
        AppScreen.CHECK_SETTINGS -> CheckSettingsScreen(
            uiState = uiState,
            onBack = viewModel::goHome,
            onToggleTarget = viewModel::setCheckTargetEnabled,
            onAddTarget = viewModel::addCheckTarget,
            onResetTargets = viewModel::resetCheckTargets,
            onRemoveTarget = viewModel::removeCheckTarget,
            onToggleDns = viewModel::setDnsServerEnabled,
            onAddDns = viewModel::addDnsServer,
            onResetDns = viewModel::resetDnsServers,
            onRemoveDns = viewModel::removeDnsServer,
        )
        AppScreen.AUTO_CHECK -> AutoCheckScreen(
            uiState = uiState,
            onBack = viewModel::goHome,
            onEnabledChange = viewModel::updateBackgroundCheckEnabled,
            onPresetIntervalChange = viewModel::selectPresetInterval,
            onUseCustomIntervalChange = viewModel::setUseCustomInterval,
            onCustomIntervalInputChange = viewModel::updateCustomIntervalInput,
            onBackgroundNotificationPolicyChange = viewModel::updateBackgroundNotificationPolicy,
            onSaveAndReschedule = viewModel::saveBackgroundCheckSettings,
            onRunNow = viewModel::runBackgroundCheckNow,
            onStop = viewModel::stopBackgroundCheck,
            onActiveIntervalInputChange = viewModel::updateActiveMonitoringIntervalInput,
            onSaveActiveInterval = viewModel::saveActiveMonitoringInterval,
            onActiveNotificationPolicyChange = viewModel::updateActiveMonitoringNotificationPolicy,
            onNotifyOnAccessRestoredChange = viewModel::updateNotifyOnAccessRestored,
            onTelegramCommandsEnabledChange = viewModel::updateTelegramCommandsEnabled,
            onStartActiveMonitoring = viewModel::startActiveMonitoring,
            onStopActiveMonitoring = viewModel::stopActiveMonitoring,
            onRunActiveCheckNow = viewModel::runActiveMonitoringCheckNow,
        )
        AppScreen.PUBLIC_SERVICE -> PublicServiceScreen(
            uiState = uiState,
            onBack = viewModel::goHome,
            onShareReportsChange = viewModel::updatePublicServiceShareReports,
            onRemoteChecksChange = viewModel::updatePublicServiceRemoteChecks,
            onRequestLocationPermission = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onDetectArea = viewModel::detectPublicServiceArea,
            onConfirmDetectedArea = viewModel::confirmDetectedPublicServiceArea,
            onDismissDetectedArea = viewModel::dismissDetectedPublicServiceArea,
            onRegionChange = viewModel::selectPublicServiceRegion,
            onCityChange = viewModel::selectPublicServiceCity,
            onClearCity = viewModel::clearPublicServiceCity,
            onDetectOperator = viewModel::detectPublicServiceOperator,
            onUseAutoOperator = viewModel::useAutoPublicServiceOperator,
            onOperatorChange = viewModel::selectPublicServiceOperator,
            onDeviceAliasChange = viewModel::updatePublicServiceDeviceAlias,
            onSaveSettings = viewModel::savePublicServiceSettings,
            onCreateLinkCode = viewModel::createPublicServiceLinkCode,
            onRefreshLinks = viewModel::refreshPublicServiceLinks,
            onRevokeLink = viewModel::revokePublicServiceLink,
            onRetryReports = viewModel::retryPublicReportUpload,
            onDeleteServerData = viewModel::deletePublicServiceServerData,
        )
        AppScreen.DIAGNOSTICS -> DiagnosticsScreen(
            uiState = uiState,
            onBack = viewModel::goHome,
            detailedReport = viewModel.buildDetailedReport(),
            onLoadStatisticsDiagnostics = viewModel::loadStatisticsDiagnostics,
            onRebuildStatistics = viewModel::rebuildStatisticsFromHistory,
        )
    }
}
