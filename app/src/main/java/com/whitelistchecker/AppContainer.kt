package com.whitelistchecker

import android.content.Context
import android.net.ConnectivityManager
import com.whitelistchecker.data.background.BackgroundCheckSettingsRepository
import com.whitelistchecker.data.background.BackgroundCheckStatusRepository
import com.whitelistchecker.data.db.AppDatabase
import com.whitelistchecker.data.monitor.MonitorStateRepository
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.checker.CellularNetworkProvider
import com.whitelistchecker.domain.checker.MobileSiteChecker
import com.whitelistchecker.domain.checker.NetworkDiagnosticsUseCase
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
import com.whitelistchecker.domain.telegram.DetailedReportFormatter
import com.whitelistchecker.domain.telegram.TelegramBroadcastUseCase
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramEventNotifierUseCase
import com.whitelistchecker.domain.telegram.TelegramQueueProcessor
import com.whitelistchecker.domain.telegram.TelegramReportFormatter
import com.whitelistchecker.domain.telegram.TelegramWorkerClient
import com.whitelistchecker.domain.telegram.WorkerUrlBuilder
import com.whitelistchecker.worker.BackgroundCheckScheduler
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)

    val database: AppDatabase = AppDatabase.getInstance(appContext)

    val monitorStateRepository = MonitorStateRepository(appContext)
    val localNotificationSettingsRepository = LocalNotificationSettingsRepository(appContext)
    val telegramSettingsRepository = TelegramSettingsRepository(appContext)
    val pendingTelegramReportRepository = PendingTelegramReportRepository(
        dao = database.pendingTelegramReportDao(),
    )
    val checkTargetsRepository = CheckTargetsRepository(appContext)
    val backgroundCheckSettingsRepository = BackgroundCheckSettingsRepository(appContext)
    val backgroundCheckStatusRepository = BackgroundCheckStatusRepository(appContext)

    val channelManager = LocalNotificationChannelManager(appContext)
    val permissionChecker = LocalNotificationPermissionChecker(appContext)
    val appSettingsNavigator = AppSettingsNavigator(appContext)

    private val telegramWorkerClient = TelegramWorkerClient(
        httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build(),
        workerUrlBuilder = WorkerUrlBuilder(),
    )

    val telegramChatIdResolverUseCase = TelegramChatIdResolverUseCase(
        settingsRepository = telegramSettingsRepository,
        telegramWorkerClient = telegramWorkerClient,
    )

    private val whitelistCheckUseCase = WhitelistCheckUseCase(
        connectivityManager = connectivityManager,
        targetsRepository = checkTargetsRepository,
        cellularNetworkProvider = CellularNetworkProvider(connectivityManager),
        mobileSiteChecker = MobileSiteChecker(),
        classifier = WhitelistStateClassifier(),
        networkDiagnosticsUseCase = NetworkDiagnosticsUseCase(),
    )

    private val whitelistMonitorUseCase = WhitelistMonitorUseCase(
        whitelistCheckUseCase = whitelistCheckUseCase,
        monitorStateRepository = monitorStateRepository,
        stateChangeDetector = StateChangeDetector(),
    )

    private val localNotificationEventUseCase = LocalNotificationEventUseCase(
        settingsRepository = localNotificationSettingsRepository,
        notificationSender = LocalNotificationSender(
            context = appContext,
            channelManager = channelManager,
            permissionChecker = permissionChecker,
            formatter = LocalNotificationFormatter(),
        ),
    )

    private val checkAndLocalNotifyUseCase = CheckAndLocalNotifyUseCase(
        whitelistMonitorUseCase = whitelistMonitorUseCase,
        localNotificationEventUseCase = localNotificationEventUseCase,
    )

    private val telegramBroadcastUseCase = TelegramBroadcastUseCase(
        settingsRepository = telegramSettingsRepository,
        telegramWorkerClient = telegramWorkerClient,
        pendingTelegramReportRepository = pendingTelegramReportRepository,
    )

    private val telegramEventNotifierUseCase = TelegramEventNotifierUseCase(
        telegramBroadcastUseCase = telegramBroadcastUseCase,
        reportFormatter = TelegramReportFormatter(),
    )

    private val telegramQueueProcessor = TelegramQueueProcessor(
        pendingReportRepository = pendingTelegramReportRepository,
        settingsRepository = telegramSettingsRepository,
        telegramWorkerClient = telegramWorkerClient,
    )

    val checkAndNotifyUseCase = CheckAndNotifyUseCase(
        checkAndLocalNotifyUseCase = checkAndLocalNotifyUseCase,
        telegramEventNotifierUseCase = telegramEventNotifierUseCase,
        telegramQueueProcessor = telegramQueueProcessor,
        pendingTelegramReportRepository = pendingTelegramReportRepository,
    )

    val backgroundCheckScheduler = BackgroundCheckScheduler(appContext)

    val detailedReportFormatter = DetailedReportFormatter()
    val telegramWorkerClientForUi = telegramWorkerClient
    val telegramEventNotifierUseCaseForUi = telegramEventNotifierUseCase
}
