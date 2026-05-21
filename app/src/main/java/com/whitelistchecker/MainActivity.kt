package com.whitelistchecker

import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitelistchecker.data.monitor.MonitorStateRepository
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.targets.DefaultTargetsRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.checker.CellularNetworkProvider
import com.whitelistchecker.domain.checker.MobileSiteChecker
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.domain.monitor.WhitelistMonitorUseCase
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.domain.notifications.LocalNotificationEventUseCase
import com.whitelistchecker.domain.notifications.LocalNotificationFormatter
import com.whitelistchecker.domain.notifications.LocalNotificationPermissionChecker
import com.whitelistchecker.domain.notifications.LocalNotificationSender
import com.whitelistchecker.domain.system.AppSettingsNavigator
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramEventNotifierUseCase
import com.whitelistchecker.domain.telegram.TelegramReportFormatter
import com.whitelistchecker.domain.telegram.TelegramWorkerClient
import com.whitelistchecker.domain.telegram.WorkerUrlBuilder
import com.whitelistchecker.ui.main.MainScreen
import com.whitelistchecker.ui.main.MainViewModel
import com.whitelistchecker.ui.theme.WhiteListCheckerTheme
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        val appContext = applicationContext
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val targetsRepository = DefaultTargetsRepository()
        val cellularNetworkProvider = CellularNetworkProvider(connectivityManager)
        val mobileSiteChecker = MobileSiteChecker()
        val classifier = WhitelistStateClassifier()
        val whitelistCheckUseCase = WhitelistCheckUseCase(
            connectivityManager = connectivityManager,
            targetsRepository = targetsRepository,
            cellularNetworkProvider = cellularNetworkProvider,
            mobileSiteChecker = mobileSiteChecker,
            classifier = classifier,
        )
        val monitorStateRepository = MonitorStateRepository(appContext)
        val whitelistMonitorUseCase = WhitelistMonitorUseCase(
            whitelistCheckUseCase = whitelistCheckUseCase,
            monitorStateRepository = monitorStateRepository,
            stateChangeDetector = StateChangeDetector(),
        )
        val localNotificationSettingsRepository = LocalNotificationSettingsRepository(appContext)
        val channelManager = LocalNotificationChannelManager(appContext)
        val permissionChecker = LocalNotificationPermissionChecker(appContext)
        val notificationSender = LocalNotificationSender(
            context = appContext,
            channelManager = channelManager,
            permissionChecker = permissionChecker,
            formatter = LocalNotificationFormatter(),
        )
        val localNotificationEventUseCase = LocalNotificationEventUseCase(
            settingsRepository = localNotificationSettingsRepository,
            notificationSender = notificationSender,
        )
        val checkAndLocalNotifyUseCase = CheckAndLocalNotifyUseCase(
            whitelistMonitorUseCase = whitelistMonitorUseCase,
            localNotificationEventUseCase = localNotificationEventUseCase,
        )
        val telegramSettingsRepository = TelegramSettingsRepository(appContext)
        val telegramWorkerClient = TelegramWorkerClient(
            httpClient = OkHttpClient.Builder().build(),
            workerUrlBuilder = WorkerUrlBuilder(),
        )
        val telegramChatIdResolverUseCase = TelegramChatIdResolverUseCase(
            settingsRepository = telegramSettingsRepository,
            telegramWorkerClient = telegramWorkerClient,
        )
        val telegramEventNotifierUseCase = TelegramEventNotifierUseCase(
            settingsRepository = telegramSettingsRepository,
            telegramWorkerClient = telegramWorkerClient,
            reportFormatter = TelegramReportFormatter(),
        )
        val checkAndNotifyUseCase = CheckAndNotifyUseCase(
            checkAndLocalNotifyUseCase = checkAndLocalNotifyUseCase,
            telegramEventNotifierUseCase = telegramEventNotifierUseCase,
        )
        MainViewModelFactory(
            checkAndNotifyUseCase = checkAndNotifyUseCase,
            localNotificationSettingsRepository = localNotificationSettingsRepository,
            telegramSettingsRepository = telegramSettingsRepository,
            telegramWorkerClient = telegramWorkerClient,
            telegramChatIdResolverUseCase = telegramChatIdResolverUseCase,
            permissionChecker = permissionChecker,
            channelManager = channelManager,
            appSettingsNavigator = AppSettingsNavigator(appContext),
        )
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
        private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
        private val localNotificationSettingsRepository: LocalNotificationSettingsRepository,
        private val telegramSettingsRepository: TelegramSettingsRepository,
        private val telegramWorkerClient: TelegramWorkerClient,
        private val telegramChatIdResolverUseCase: TelegramChatIdResolverUseCase,
        private val permissionChecker: LocalNotificationPermissionChecker,
        private val channelManager: LocalNotificationChannelManager,
        private val appSettingsNavigator: AppSettingsNavigator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    checkAndNotifyUseCase = checkAndNotifyUseCase,
                    localNotificationSettingsRepository = localNotificationSettingsRepository,
                    telegramSettingsRepository = telegramSettingsRepository,
                    telegramWorkerClient = telegramWorkerClient,
                    telegramChatIdResolverUseCase = telegramChatIdResolverUseCase,
                    permissionChecker = permissionChecker,
                    channelManager = channelManager,
                    appSettingsNavigator = appSettingsNavigator,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
