package com.whitelistchecker

import android.content.Context
import android.net.ConnectivityManager
import com.whitelistchecker.data.active.ActiveMonitoringRepository
import com.whitelistchecker.data.background.BackgroundCheckSettingsRepository
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.background.BackgroundCheckStatusRepository
import com.whitelistchecker.data.checkrun.CheckStateRepository
import com.whitelistchecker.data.db.AppDatabase
import com.whitelistchecker.data.dns.DnsServersRepository
import com.whitelistchecker.data.monitor.MonitorStateRepository
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.publicservice.PendingPublicReportRepository
import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.data.publicservice.SecureDeviceTokenStore
import com.whitelistchecker.data.resources.AndroidDetailedReportTextProvider
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.data.timeline.RoomWhitelistTimelineRepository
import com.whitelistchecker.data.history.RoomCheckHistoryRepository
import com.whitelistchecker.data.statistics.RoomCheckStatisticsRepository
import com.whitelistchecker.data.statistics.StatisticsDiagnosticsMetaDataStore
import com.whitelistchecker.data.system.PackageAppVersionProvider
import com.whitelistchecker.domain.active.ActiveMonitoringController
import com.whitelistchecker.domain.checker.CellularDnsProbe
import com.whitelistchecker.domain.checker.CellularDnsResolverFactory
import com.whitelistchecker.domain.checker.CellularNetworkProvider
import com.whitelistchecker.domain.history.CheckHistoryFromNetworkResultMapper
import com.whitelistchecker.domain.history.SaveCheckHistoryUseCase
import com.whitelistchecker.domain.statistics.CheckStatisticsCalculator
import com.whitelistchecker.domain.statistics.LocalStatisticsWriter
import com.whitelistchecker.domain.statistics.LoadStatisticsDashboardUseCase
import com.whitelistchecker.domain.statistics.LoadStatisticsDiagnosticsUseCase
import com.whitelistchecker.domain.statistics.LoadWhitelistTimelineDashboardUseCase
import com.whitelistchecker.domain.statistics.RebuildCheckStatisticsUseCase
import com.whitelistchecker.domain.statistics.RebuildWhitelistTimelineUseCase
import com.whitelistchecker.domain.statistics.WhitelistTimelineWriter
import com.whitelistchecker.domain.checker.MobileSiteChecker
import com.whitelistchecker.domain.checker.NetworkDiagnosticsUseCase
import com.whitelistchecker.domain.checker.PrivateDnsDiagnosticsProvider
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import com.whitelistchecker.domain.classifier.DnsWhitelistSignalClassifier
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.checkrun.CheckExecutionCoordinator
import com.whitelistchecker.domain.checkrun.NotificationDecisionEngine
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.domain.monitor.WhitelistMonitorUseCase
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.domain.notifications.LocalNotificationEventUseCase
import com.whitelistchecker.domain.notifications.LocalNotificationFormatter
import com.whitelistchecker.domain.notifications.LocalNotificationPermissionChecker
import com.whitelistchecker.domain.notifications.LocalNotificationSender
import com.whitelistchecker.domain.publicservice.PublicReportPayloadBuilder
import com.whitelistchecker.domain.publicservice.PublicReportUploadUseCase
import com.whitelistchecker.domain.publicservice.MobileOperatorDetector
import com.whitelistchecker.domain.publicservice.PublicServiceClient
import com.whitelistchecker.domain.publicservice.PublicServiceAreaDetector
import com.whitelistchecker.domain.publicservice.PublicServiceLinkUseCase
import com.whitelistchecker.domain.publicservice.PublicServiceRegistrationUseCase
import com.whitelistchecker.domain.publicservice.PublicServiceRemoteCommandLoop
import com.whitelistchecker.domain.publicservice.PublicServiceUrlBuilder
import com.whitelistchecker.domain.system.AppSettingsNavigator
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase
import com.whitelistchecker.domain.telegram.DetailedReportFormatter
import com.whitelistchecker.domain.telegram.TelegramBroadcastUseCase
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramEventNotifierUseCase
import com.whitelistchecker.domain.telegram.TelegramCommandHandler
import com.whitelistchecker.domain.telegram.TelegramCommandListener
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
    private val appVersionProvider = PackageAppVersionProvider(appContext)
    val publicServiceBaseUrl: String = BuildConfig.PUBLIC_SERVICE_BASE_URL

    val database: AppDatabase = AppDatabase.getInstance(appContext)

    val monitorStateRepository = MonitorStateRepository(appContext)
    val lastCheckRepository = LastCheckRepository(appContext)
    private val checkHistoryRepository = RoomCheckHistoryRepository(
        dao = database.checkHistoryDao(),
    )
    val statisticsDiagnosticsMetaRepository = StatisticsDiagnosticsMetaDataStore(appContext)
    val saveCheckHistoryUseCase = SaveCheckHistoryUseCase(
        checkHistoryRepository = checkHistoryRepository,
        mapper = CheckHistoryFromNetworkResultMapper(),
        appVersionProvider = appVersionProvider::versionName,
        diagnosticsMetaRepository = statisticsDiagnosticsMetaRepository,
    )
    private val checkStatisticsRepository = RoomCheckStatisticsRepository(
        database = database,
        dao = database.checkStatisticsDao(),
        calculator = CheckStatisticsCalculator(),
    )
    val localStatisticsWriter = LocalStatisticsWriter(
        checkStatisticsRepository = checkStatisticsRepository,
    )
    val rebuildCheckStatisticsUseCase = RebuildCheckStatisticsUseCase(
        checkHistoryRepository = checkHistoryRepository,
        checkStatisticsRepository = checkStatisticsRepository,
        calculator = CheckStatisticsCalculator(),
    )
    val loadStatisticsDashboardUseCase = LoadStatisticsDashboardUseCase(
        checkStatisticsRepository = checkStatisticsRepository,
    )
    val loadStatisticsDiagnosticsUseCase = LoadStatisticsDiagnosticsUseCase(
        checkHistoryRepository = checkHistoryRepository,
        checkStatisticsRepository = checkStatisticsRepository,
        diagnosticsMetaRepository = statisticsDiagnosticsMetaRepository,
    )
    private val whitelistTimelineRepository = RoomWhitelistTimelineRepository(
        database = database,
        dao = database.whitelistTimelineDao(),
    )
    val whitelistTimelineWriter = WhitelistTimelineWriter(
        repository = whitelistTimelineRepository,
    )
    val loadWhitelistTimelineDashboardUseCase = LoadWhitelistTimelineDashboardUseCase(
        repository = whitelistTimelineRepository,
    )
    val rebuildWhitelistTimelineUseCase = RebuildWhitelistTimelineUseCase(
        checkHistoryRepository = checkHistoryRepository,
        whitelistTimelineRepository = whitelistTimelineRepository,
    )
    val localNotificationSettingsRepository = LocalNotificationSettingsRepository(appContext)
    val publicServiceSettingsRepository = PublicServiceSettingsRepository(appContext)
    private val secureDeviceTokenStore = SecureDeviceTokenStore(appContext)
    val publicServiceAreaDetector = PublicServiceAreaDetector(appContext)
    val mobileOperatorDetector = MobileOperatorDetector(appContext)
    val telegramSettingsRepository = TelegramSettingsRepository(appContext)
    val pendingTelegramReportRepository = PendingTelegramReportRepository(
        dao = database.pendingTelegramReportDao(),
    )
    val pendingPublicReportRepository = PendingPublicReportRepository(
        dao = database.pendingPublicReportDao(),
    )
    val checkTargetsRepository = CheckTargetsRepository(appContext)
    val dnsServersRepository = DnsServersRepository(appContext)
    val backgroundCheckSettingsRepository = BackgroundCheckSettingsRepository(appContext)
    val backgroundCheckStatusRepository = BackgroundCheckStatusRepository(appContext)
    val checkStateRepository = CheckStateRepository(appContext)
    val checkExecutionCoordinator = CheckExecutionCoordinator()
    val notificationDecisionEngine = NotificationDecisionEngine()
    val activeMonitoringRepository = ActiveMonitoringRepository(appContext)
    val activeMonitoringController = ActiveMonitoringController(
        context = appContext,
        repository = activeMonitoringRepository,
    )

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

    private val publicServiceClient = PublicServiceClient(
        httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build(),
        urlBuilder = PublicServiceUrlBuilder(publicServiceBaseUrl),
    )

    val publicServiceRegistrationUseCase = PublicServiceRegistrationUseCase(
        settingsRepository = publicServiceSettingsRepository,
        tokenStore = secureDeviceTokenStore,
        client = publicServiceClient,
        appVersionProvider = appVersionProvider::versionName,
    )

    val publicReportUploadUseCase = PublicReportUploadUseCase(
        settingsRepository = publicServiceSettingsRepository,
        pendingReportRepository = pendingPublicReportRepository,
        registrationUseCase = publicServiceRegistrationUseCase,
        client = publicServiceClient,
        payloadBuilder = PublicReportPayloadBuilder(appVersionProvider::versionName),
    )

    val publicServiceLinkUseCase = PublicServiceLinkUseCase(
        settingsRepository = publicServiceSettingsRepository,
        registrationUseCase = publicServiceRegistrationUseCase,
        client = publicServiceClient,
    )

    val telegramChatIdResolverUseCase = TelegramChatIdResolverUseCase(
        settingsRepository = telegramSettingsRepository,
        telegramWorkerClient = telegramWorkerClient,
    )

    private val whitelistCheckUseCase = WhitelistCheckUseCase(
        connectivityManager = connectivityManager,
        targetsRepository = checkTargetsRepository,
        dnsServersRepository = dnsServersRepository,
        cellularNetworkProvider = CellularNetworkProvider(connectivityManager),
        dnsProbe = CellularDnsProbe(),
        dnsResolverFactory = CellularDnsResolverFactory(),
        mobileSiteChecker = MobileSiteChecker(),
        dnsSignalClassifier = DnsWhitelistSignalClassifier(),
        classifier = WhitelistStateClassifier(),
        networkDiagnosticsUseCase = NetworkDiagnosticsUseCase(),
        privateDnsDiagnosticsProvider = PrivateDnsDiagnosticsProvider(connectivityManager),
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

    private val telegramReportFormatter = TelegramReportFormatter()

    private val telegramEventNotifierUseCase = TelegramEventNotifierUseCase(
        telegramBroadcastUseCase = telegramBroadcastUseCase,
        reportFormatter = telegramReportFormatter,
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
        checkExecutionCoordinator = checkExecutionCoordinator,
        checkStateRepository = checkStateRepository,
        notificationDecisionEngine = notificationDecisionEngine,
        lastCheckRepository = lastCheckRepository,
        saveCheckHistoryUseCase = saveCheckHistoryUseCase,
        localStatisticsWriter = localStatisticsWriter,
        whitelistTimelineWriter = whitelistTimelineWriter,
        publicReportUploadUseCase = publicReportUploadUseCase,
    )

    val publicServiceRemoteCommandLoop = PublicServiceRemoteCommandLoop(
        settingsRepository = publicServiceSettingsRepository,
        registrationUseCase = publicServiceRegistrationUseCase,
        publicServiceClient = publicServiceClient,
        checkAndNotifyUseCase = checkAndNotifyUseCase,
        appVersionProvider = appVersionProvider,
    )

    private val telegramCommandHandler = TelegramCommandHandler(
        settingsRepository = telegramSettingsRepository,
        activeMonitoringRepository = activeMonitoringRepository,
        lastCheckRepository = lastCheckRepository,
        checkStateRepository = checkStateRepository,
        telegramWorkerClient = telegramWorkerClient,
        checkAndNotifyUseCase = checkAndNotifyUseCase,
        reportFormatter = telegramReportFormatter,
    )

    val telegramCommandListener = TelegramCommandListener(
        activeMonitoringRepository = activeMonitoringRepository,
        settingsRepository = telegramSettingsRepository,
        telegramWorkerClient = telegramWorkerClient,
        commandHandler = telegramCommandHandler,
    )

    val backgroundCheckScheduler = BackgroundCheckScheduler(appContext)

    val detailedReportFormatter = DetailedReportFormatter(
        textProvider = AndroidDetailedReportTextProvider(appContext),
    )
    val telegramWorkerClientForUi = telegramWorkerClient
    val telegramEventNotifierUseCaseForUi = telegramEventNotifierUseCase
}
