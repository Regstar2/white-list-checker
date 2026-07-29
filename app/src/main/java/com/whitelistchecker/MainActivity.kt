package com.whitelistchecker

import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitelistchecker.ui.main.MainScreen
import com.whitelistchecker.ui.main.MainViewModel
import com.whitelistchecker.ui.theme.WhiteListCheckerTheme

class MainActivity : ComponentActivity() {

    private val appContainer: AppContainer
        get() = (application as WhitelistCheckerApplication).appContainer

    private val viewModelFactory by lazy {
        MainViewModelFactory(appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhiteListCheckerTheme {
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private class MainViewModelFactory(
        private val appContainer: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    checkAndNotifyUseCase = appContainer.checkAndNotifyUseCase,
                    lastCheckRepository = appContainer.lastCheckRepository,
                    localNotificationSettingsRepository = appContainer.localNotificationSettingsRepository,
                    telegramSettingsRepository = appContainer.telegramSettingsRepository,
                    telegramEventNotifierUseCase = appContainer.telegramEventNotifierUseCaseForUi,
                    telegramWorkerClient = appContainer.telegramWorkerClientForUi,
                    telegramChatIdResolverUseCase = appContainer.telegramChatIdResolverUseCase,
                    permissionChecker = appContainer.permissionChecker,
                    channelManager = appContainer.channelManager,
                    appSettingsNavigator = appContainer.appSettingsNavigator,
                    backgroundCheckSettingsRepository = appContainer.backgroundCheckSettingsRepository,
                    backgroundCheckStatusRepository = appContainer.backgroundCheckStatusRepository,
                    backgroundCheckScheduler = appContainer.backgroundCheckScheduler,
                    activeMonitoringRepository = appContainer.activeMonitoringRepository,
                    activeMonitoringController = appContainer.activeMonitoringController,
                    publicServiceSettingsRepository = appContainer.publicServiceSettingsRepository,
                    publicServiceAreaDetector = appContainer.publicServiceAreaDetector,
                    mobileOperatorDetector = appContainer.mobileOperatorDetector,
                    publicServiceRegistrationUseCase = appContainer.publicServiceRegistrationUseCase,
                    publicServiceLinkUseCase = appContainer.publicServiceLinkUseCase,
                    publicReportUploadUseCase = appContainer.publicReportUploadUseCase,
                    checkTargetsRepository = appContainer.checkTargetsRepository,
                    detailedReportFormatter = appContainer.detailedReportFormatter,
                    loadStatisticsDashboardUseCase = appContainer.loadStatisticsDashboardUseCase,
                    loadStatisticsDiagnosticsUseCase = appContainer.loadStatisticsDiagnosticsUseCase,
                    rebuildCheckStatisticsUseCase = appContainer.rebuildCheckStatisticsUseCase,
                    rebuildWhitelistTimelineUseCase = appContainer.rebuildWhitelistTimelineUseCase,
                    loadWhitelistTimelineDashboardUseCase = appContainer.loadWhitelistTimelineDashboardUseCase,
                    statisticsDiagnosticsMetaRepository = appContainer.statisticsDiagnosticsMetaRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
