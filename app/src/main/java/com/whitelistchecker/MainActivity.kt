package com.whitelistchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitelistchecker.data.update.AppUpdateCheckPreferences
import com.whitelistchecker.domain.model.UserSettings
import com.whitelistchecker.ui.main.MainScreen
import com.whitelistchecker.ui.main.MainViewModel
import com.whitelistchecker.ui.settings.AppLocaleController
import com.whitelistchecker.ui.theme.WhiteListCheckerTheme
import com.whitelistchecker.ui.update.AppUpdateViewModel

class MainActivity : ComponentActivity() {

    private val appContainer: AppContainer
        get() = (application as WhitelistCheckerApplication).appContainer

    private val appUpdateCheckPreferences by lazy {
        AppUpdateCheckPreferences(applicationContext)
    }

    private val viewModelFactory by lazy {
        AppViewModelFactory(
            appContainer = appContainer,
            appUpdateCheckPreferences = appUpdateCheckPreferences,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSettings = appContainer.userSettingsRepository
                .observeSettings()
                .collectAsStateWithLifecycle(initialValue = UserSettings())
                .value

            LaunchedEffect(userSettings.language) {
                AppLocaleController.apply(userSettings.language)
            }

            val localizedContext = remember(userSettings.language) {
                AppLocaleController.localizedContext(this, userSettings.language)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this,
            ) {
                WhiteListCheckerTheme(themeMode = userSettings.themeMode) {
                    val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
                    val appUpdateViewModel: AppUpdateViewModel = viewModel(factory = viewModelFactory)
                    MainScreen(
                        viewModel = mainViewModel,
                        appUpdateViewModel = appUpdateViewModel,
                    )
                }
            }
        }
    }

    private class AppViewModelFactory(
        private val appContainer: AppContainer,
        private val appUpdateCheckPreferences: AppUpdateCheckPreferences,
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
                    checkTargetsRepository = appContainer.checkTargetsRepository,
                    dnsServersRepository = appContainer.dnsServersRepository,
                    detailedReportFormatter = appContainer.detailedReportFormatter,
                    loadStatisticsDashboardUseCase = appContainer.loadStatisticsDashboardUseCase,
                    loadStatisticsDiagnosticsUseCase = appContainer.loadStatisticsDiagnosticsUseCase,
                    rebuildCheckStatisticsUseCase = appContainer.rebuildCheckStatisticsUseCase,
                    rebuildWhitelistTimelineUseCase = appContainer.rebuildWhitelistTimelineUseCase,
                    loadWhitelistTimelineDashboardUseCase = appContainer.loadWhitelistTimelineDashboardUseCase,
                    statisticsDiagnosticsMetaRepository = appContainer.statisticsDiagnosticsMetaRepository,
                    userSettingsRepository = appContainer.userSettingsRepository,
                ) as T
            }
            if (modelClass.isAssignableFrom(AppUpdateViewModel::class.java)) {
                return AppUpdateViewModel(
                    checkForAppUpdateUseCase = appContainer.checkForAppUpdateUseCase,
                    tryAcquireAutomaticCheck = appUpdateCheckPreferences::tryAcquireAutomaticCheck,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
