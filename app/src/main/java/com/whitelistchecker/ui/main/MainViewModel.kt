package com.whitelistchecker.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.data.active.ActiveMonitoringRepository
import com.whitelistchecker.data.background.BackgroundCheckSettingsRepository
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.background.BackgroundCheckStatusRepository
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.active.ActiveMonitoringController
import com.whitelistchecker.domain.check.LastCheckStateResolver
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckLoadResult
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.DetectedOperator
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.OperatorSelectionMode
import com.whitelistchecker.domain.model.PublicServiceCatalog
import com.whitelistchecker.domain.model.UserArea
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.domain.notifications.LocalNotificationPermissionChecker
import com.whitelistchecker.domain.publicservice.PublicReportUploadUseCase
import com.whitelistchecker.domain.publicservice.MobileOperatorDetector
import com.whitelistchecker.domain.publicservice.PublicServiceAreaDetector
import com.whitelistchecker.domain.publicservice.PublicServiceLinkUseCase
import com.whitelistchecker.domain.publicservice.PublicServiceRegistrationUseCase
import com.whitelistchecker.domain.system.AppSettingsNavigator
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase
import com.whitelistchecker.domain.telegram.DetailedReportFormatter
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramEventNotifierUseCase
import com.whitelistchecker.domain.statistics.LoadStatisticsDashboardUseCase
import com.whitelistchecker.domain.statistics.LoadStatisticsDiagnosticsUseCase
import com.whitelistchecker.domain.statistics.LoadWhitelistTimelineDashboardUseCase
import com.whitelistchecker.domain.statistics.RebuildCheckStatisticsUseCase
import com.whitelistchecker.domain.statistics.RebuildWhitelistTimelineUseCase
import com.whitelistchecker.domain.statistics.RebuildStatisticsResult
import com.whitelistchecker.domain.statistics.StatisticsDashboard
import com.whitelistchecker.domain.statistics.StatisticsDiagnosticsMetaRepository
import com.whitelistchecker.domain.statistics.StatisticsLoadResult
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard
import com.whitelistchecker.domain.statistics.WhitelistTimelineLoadResult
import com.whitelistchecker.domain.telegram.TelegramWorkerClient
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.diagnostics.RebuildStatisticsUiState
import com.whitelistchecker.ui.diagnostics.StatisticsDiagnosticsUiState
import com.whitelistchecker.ui.statistics.HomeStatisticsMapper
import com.whitelistchecker.ui.statistics.HomeStatisticsUiState
import com.whitelistchecker.ui.statistics.StatisticsFreshnessMapper
import com.whitelistchecker.ui.statistics.StatisticsUiState
import com.whitelistchecker.ui.userMessage
import com.whitelistchecker.worker.BackgroundCheckScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
    private val lastCheckRepository: LastCheckRepository,
    private val lastCheckStateResolver: LastCheckStateResolver = LastCheckStateResolver(),
    private val localNotificationSettingsRepository: LocalNotificationSettingsRepository,
    private val telegramSettingsRepository: TelegramSettingsRepository,
    private val telegramEventNotifierUseCase: TelegramEventNotifierUseCase,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val telegramChatIdResolverUseCase: TelegramChatIdResolverUseCase,
    private val permissionChecker: LocalNotificationPermissionChecker,
    private val channelManager: LocalNotificationChannelManager,
    private val appSettingsNavigator: AppSettingsNavigator,
    private val backgroundCheckSettingsRepository: BackgroundCheckSettingsRepository,
    private val backgroundCheckStatusRepository: BackgroundCheckStatusRepository,
    private val backgroundCheckScheduler: BackgroundCheckScheduler,
    private val activeMonitoringRepository: ActiveMonitoringRepository,
    private val activeMonitoringController: ActiveMonitoringController,
    private val publicServiceSettingsRepository: PublicServiceSettingsRepository,
    private val publicServiceAreaDetector: PublicServiceAreaDetector,
    private val mobileOperatorDetector: MobileOperatorDetector,
    private val publicServiceRegistrationUseCase: PublicServiceRegistrationUseCase,
    private val publicServiceLinkUseCase: PublicServiceLinkUseCase,
    private val publicReportUploadUseCase: PublicReportUploadUseCase,
    private val checkTargetsRepository: CheckTargetsRepository,
    private val detailedReportFormatter: DetailedReportFormatter,
    private val loadStatisticsDashboardUseCase: LoadStatisticsDashboardUseCase,
    private val loadStatisticsDiagnosticsUseCase: LoadStatisticsDiagnosticsUseCase,
    private val rebuildCheckStatisticsUseCase: RebuildCheckStatisticsUseCase,
    private val rebuildWhitelistTimelineUseCase: RebuildWhitelistTimelineUseCase,
    private val loadWhitelistTimelineDashboardUseCase: LoadWhitelistTimelineDashboardUseCase,
    private val statisticsDiagnosticsMetaRepository: StatisticsDiagnosticsMetaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var lastCheckLoadFailed: Boolean = false

    init {
        channelManager.ensureChannelsCreated()
        refreshNotificationPermissionState()
        loadInitialState()
        refreshStatistics()
        observeBackgroundCheck()
        observeActiveMonitoring()
        observePublicService()
        observeCheckTargets()
    }

    fun openScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
        when (screen) {
            AppScreen.STATISTICS -> refreshStatistics(forStatisticsScreen = true)
            AppScreen.DIAGNOSTICS -> loadStatisticsDiagnostics()
            AppScreen.PUBLIC_SERVICE -> refreshPublicServiceLinks()
            else -> Unit
        }
    }

    fun loadStatisticsDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(statisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Loading) }
            try {
                val diagnostics = loadStatisticsDiagnosticsUseCase.load()
                _uiState.update {
                    it.copy(
                        statisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Content(
                            diagnostics = diagnostics,
                        ),
                    )
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Statistics diagnostics load failed", exception)
                _uiState.update {
                    it.copy(
                        statisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Error(
                            message = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun rebuildStatisticsFromHistory() {
        viewModelScope.launch {
            val current = _uiState.value.statisticsDiagnosticsUiState
            if (current !is StatisticsDiagnosticsUiState.Content) return@launch
            _uiState.update {
                it.copy(
                    statisticsDiagnosticsUiState = current.copy(
                        rebuildState = RebuildStatisticsUiState.Running,
                    ),
                )
            }
            when (val checkRebuild = rebuildCheckStatisticsUseCase.rebuildFromHistory()) {
                RebuildStatisticsResult.Success -> {
                    rebuildWhitelistTimelineUseCase.rebuildFromHistory()
                    val rebuiltAt = System.currentTimeMillis()
                    statisticsDiagnosticsMetaRepository.recordRebuildCompleted(rebuiltAt)
                    refreshStatistics()
                    try {
                        val diagnostics = loadStatisticsDiagnosticsUseCase.load(nowMillis = rebuiltAt)
                        _uiState.update {
                            it.copy(
                                statisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Content(
                                    diagnostics = diagnostics,
                                    rebuildState = RebuildStatisticsUiState.Success,
                                ),
                            )
                        }
                    } catch (exception: Exception) {
                        Log.w(TAG, "Statistics diagnostics reload after rebuild failed", exception)
                        _uiState.update {
                            it.copy(
                                statisticsDiagnosticsUiState = StatisticsDiagnosticsUiState.Error(
                                    message = exception.message ?: exception.javaClass.simpleName,
                                ),
                            )
                        }
                    }
                }
                is RebuildStatisticsResult.Failure -> {
                    Log.w(TAG, "Statistics rebuild failed", checkRebuild.cause)
                    val message = checkRebuild.cause.message ?: checkRebuild.cause.javaClass.simpleName
                    _uiState.update { state ->
                        val diagnosticsState = state.statisticsDiagnosticsUiState
                        if (diagnosticsState is StatisticsDiagnosticsUiState.Content) {
                            state.copy(
                                statisticsDiagnosticsUiState = diagnosticsState.copy(
                                    rebuildState = RebuildStatisticsUiState.Failure(message),
                                ),
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    fun retryStatisticsLoad() {
        refreshStatistics(forStatisticsScreen = true)
    }

    fun refreshStatistics(forStatisticsScreen: Boolean = false) {
        viewModelScope.launch {
            if (forStatisticsScreen) {
                _uiState.update { it.copy(statisticsUiState = StatisticsUiState.Loading) }
            }
            _uiState.update { state ->
                val homeLoading = state.homeStatisticsUiState !is HomeStatisticsUiState.Content
                if (homeLoading) {
                    state.copy(homeStatisticsUiState = HomeStatisticsUiState.Loading)
                } else {
                    state
                }
            }
            val checkResult = loadStatisticsDashboardUseCase.load()
            val timelineResult = loadWhitelistTimelineDashboardUseCase.load()
            applyStatisticsLoadResult(checkResult, timelineResult)
        }
    }

    fun goHome() {
        openScreen(AppScreen.HOME)
    }

    fun buildDetailedReport(): String {
        val state = _uiState.value
        val result = state.result ?: return ""
        val event = state.lastStateChangeEvent
        return if (event != null) {
            detailedReportFormatter.formatStateChange(event, result)
        } else {
            detailedReportFormatter.formatCheckResult(result)
        }
    }

    fun runBackgroundCheckNow() {
        backgroundCheckScheduler.runNow()
    }

    private fun observeCheckTargets() {
        viewModelScope.launch {
            checkTargetsRepository.observeTargets().collect { targets ->
                _uiState.update { it.copy(checkTargets = targets) }
            }
        }
    }

    fun setCheckTargetEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            checkTargetsRepository.setTargetEnabled(id, enabled)
        }
    }

    fun addCheckTarget(target: EditableCheckTarget) {
        viewModelScope.launch {
            checkTargetsRepository.addTarget(target)
        }
    }

    fun removeCheckTarget(id: String) {
        viewModelScope.launch {
            checkTargetsRepository.removeTarget(id)
        }
    }

    fun resetCheckTargets() {
        viewModelScope.launch {
            checkTargetsRepository.resetToDefaults()
        }
    }

    fun addTelegramRecipient(candidate: TelegramChatCandidate) {
        viewModelScope.launch {
            try {
                telegramSettingsRepository.addRecipient(candidate)
                val savedSettings = telegramSettingsRepository.getSettings()
                _uiState.update {
                    it.copy(
                        telegramSettings = savedSettings,
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            selectedCandidate = candidate,
                            statusMessage = "Получатель добавлен",
                            errorMessage = null,
                        ),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            errorMessage = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun removeTelegramRecipient(recipientId: String) {
        viewModelScope.launch {
            telegramSettingsRepository.removeRecipient(recipientId)
            _uiState.update { it.copy(telegramSettings = telegramSettingsRepository.getSettings()) }
        }
    }

    fun setTelegramRecipientEnabled(recipientId: String, enabled: Boolean) {
        viewModelScope.launch {
            telegramSettingsRepository.setRecipientEnabled(recipientId, enabled)
            _uiState.update { it.copy(telegramSettings = telegramSettingsRepository.getSettings()) }
        }
    }

    fun selectPresetInterval(minutes: Long) {
        _uiState.update {
            it.copy(
                useCustomInterval = false,
                intervalError = null,
                backgroundCheckSettings = it.backgroundCheckSettings.copy(intervalMinutes = minutes),
            )
        }
    }

    fun setUseCustomInterval(enabled: Boolean) {
        _uiState.update { it.copy(useCustomInterval = enabled, intervalError = null) }
    }

    fun updateCustomIntervalInput(value: String) {
        _uiState.update { it.copy(customIntervalInput = value.filter { ch -> ch.isDigit() }, intervalError = null) }
    }

    fun updateBackgroundCheckEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        backgroundCheckSettings = it.backgroundCheckSettings.copy(enabled = enabled),
                    )
                }
                backgroundCheckSettingsRepository.setEnabled(enabled)
                val settings = backgroundCheckSettingsRepository.getSettings()
                if (enabled) {
                    backgroundCheckScheduler.schedule(settings.normalizedIntervalMinutes)
                } else {
                    backgroundCheckScheduler.cancel()
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    fun updateBackgroundCheckInterval(minutes: Long) {
        val normalized = BackgroundCheckSettings(intervalMinutes = minutes).normalizedIntervalMinutes
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        backgroundCheckSettings = it.backgroundCheckSettings.copy(
                            intervalMinutes = normalized,
                        ),
                    )
                }
                backgroundCheckSettingsRepository.setIntervalMinutes(normalized)
                val settings = backgroundCheckSettingsRepository.getSettings()
                if (settings.enabled) {
                    backgroundCheckScheduler.schedule(normalized)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    fun updateBackgroundNotificationPolicy(policy: NotificationPolicy) {
        viewModelScope.launch {
            try {
                backgroundCheckSettingsRepository.setNotificationPolicy(policy)
                val settings = backgroundCheckSettingsRepository.getSettings()
                _uiState.update { it.copy(backgroundCheckSettings = settings) }
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName) }
            }
        }
    }

    fun saveBackgroundCheckSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingBackgroundSettings = true) }
            try {
                val intervalMinutes = resolveIntervalMinutes()
                if (intervalMinutes == null) {
                    _uiState.update { it.copy(isSavingBackgroundSettings = false) }
                    return@launch
                }
                val settings = _uiState.value.backgroundCheckSettings.copy(
                    intervalMinutes = intervalMinutes,
                )
                backgroundCheckSettingsRepository.saveSettings(settings)
                backgroundCheckScheduler.reschedule(settings)
                _uiState.update {
                    it.copy(
                        backgroundCheckSettings = settings,
                        isSavingBackgroundSettings = false,
                        intervalError = null,
                        errorMessage = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingBackgroundSettings = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private fun resolveIntervalMinutes(): Long? {
        val state = _uiState.value
        if (!state.useCustomInterval) {
            return state.backgroundCheckSettings.intervalMinutes
        }
        val parsed = state.customIntervalInput.toLongOrNull()
        if (parsed == null || parsed < BackgroundCheckSettings.MIN_INTERVAL_MINUTES) {
            _uiState.update {
                it.copy(
                    intervalError = "Минимальный интервал фоновой проверки через WorkManager — 15 минут.",
                )
            }
            return null
        }
        return parsed
    }

    fun rescheduleBackgroundCheck() {
        viewModelScope.launch {
            try {
                val settings = backgroundCheckSettingsRepository.getSettings()
                backgroundCheckScheduler.reschedule(settings)
                _uiState.update {
                    it.copy(backgroundCheckSettings = settings, errorMessage = null)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    fun stopBackgroundCheck() {
        viewModelScope.launch {
            try {
                backgroundCheckSettingsRepository.setEnabled(false)
                backgroundCheckScheduler.cancel()
                _uiState.update {
                    it.copy(
                        backgroundCheckSettings = it.backgroundCheckSettings.copy(enabled = false),
                        errorMessage = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    private fun observeBackgroundCheck() {
        viewModelScope.launch {
            backgroundCheckSettingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(backgroundCheckSettings = settings) }
            }
        }
        viewModelScope.launch {
            backgroundCheckStatusRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(backgroundCheckStatus = status) }
            }
        }
    }

    private fun observeActiveMonitoring() {
        viewModelScope.launch {
            activeMonitoringController.reconcileStateWithProcess()
            activeMonitoringRepository.observeSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        activeMonitoringSettings = settings,
                        activeMonitoringIntervalInput = settings.intervalMinutes.toString(),
                    )
                }
            }
        }
        viewModelScope.launch {
            activeMonitoringRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(activeMonitoringStatus = status) }
            }
        }
    }

    private fun observePublicService() {
        viewModelScope.launch {
            publicServiceSettingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(publicServiceSettings = settings) }
            }
        }
        viewModelScope.launch {
            publicServiceSettingsRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(publicServiceStatus = status) }
            }
        }
    }

    fun updatePublicServiceShareReports(enabled: Boolean) {
        if (enabled) {
            val validationError = validatePublicSharing(_uiState.value.publicServiceSettings)
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return
            }
        }
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(shareReports = enabled),
                errorMessage = null,
            )
        }
    }

    fun updatePublicServiceRemoteChecks(enabled: Boolean) {
        _uiState.update {
            it.copy(publicServiceSettings = it.publicServiceSettings.copy(allowRemoteChecks = enabled))
        }
    }

    fun selectPublicServiceRegion(code: String) {
        val region = PublicServiceCatalog.regionByCode(code) ?: return
        val now = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(
                    regionCode = region.code,
                    regionName = region.label,
                    cityCode = null,
                    cityName = null,
                    customCityName = null,
                    areaSource = AreaSource.MANUAL_SELECTION,
                    areaConfirmedByUser = region.code != "UNKNOWN",
                    areaUpdatedAtMillis = now,
                    shareReports = if (region.code == "UNKNOWN") false else it.publicServiceSettings.shareReports,
                ),
                pendingDetectedArea = null,
            )
        }
    }

    fun selectPublicServiceCity(cityCode: String?, customCityName: String? = null) {
        val settings = _uiState.value.publicServiceSettings
        val city = PublicServiceCatalog.cityByCode(cityCode)
        val safeCustomCity = customCityName?.let(PublicServiceCatalog::sanitizeCustomCityName).orEmpty()
        _uiState.update {
            it.copy(
                publicServiceSettings = settings.copy(
                    cityCode = city?.code,
                    cityName = city?.label,
                    customCityName = safeCustomCity.ifBlank { null },
                    areaSource = AreaSource.MANUAL_SELECTION,
                    areaConfirmedByUser = settings.regionCode != "UNKNOWN",
                    areaUpdatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun clearPublicServiceCity() {
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(
                    cityCode = null,
                    cityName = null,
                    customCityName = null,
                    areaSource = AreaSource.MANUAL_SELECTION,
                    areaUpdatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun selectPublicServiceOperator(code: String) {
        val operator = PublicServiceCatalog.operatorByCode(code) ?: return
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(
                    operatorCode = operator.code,
                    operatorSelectionMode = OperatorSelectionMode.MANUAL,
                    operatorSource = OperatorDetectionSource.MANUAL,
                    operatorDisplayName = operator.label,
                    operatorMccMnc = null,
                    operatorUpdatedAtMillis = System.currentTimeMillis(),
                    shareReports = if (operator.code == "UNKNOWN") false else it.publicServiceSettings.shareReports,
                ),
            )
        }
    }

    fun useAutoPublicServiceOperator() {
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(
                    operatorSelectionMode = OperatorSelectionMode.AUTO,
                    operatorSource = OperatorDetectionSource.UNKNOWN,
                ),
            )
        }
        detectPublicServiceOperator()
    }

    fun detectPublicServiceArea() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetectingPublicServiceArea = true,
                    pendingDetectedArea = null,
                    errorMessage = null,
                    publicServiceMessage = "Определяю регион и город",
                )
            }
            when (val result = publicServiceAreaDetector.detect()) {
                is PublicServiceAreaDetector.AreaDetectionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDetectingPublicServiceArea = false,
                            pendingDetectedArea = result.area,
                            publicServiceMessage = "Местоположение определено. Подтвердите результат.",
                        )
                    }
                }
                is PublicServiceAreaDetector.AreaDetectionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDetectingPublicServiceArea = false,
                            publicServiceMessage = null,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun markPublicServiceLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isDetectingPublicServiceArea = false,
                pendingDetectedArea = null,
                errorMessage = "Разрешение на приблизительное местоположение не выдано. Выберите регион вручную.",
            )
        }
    }

    fun confirmDetectedPublicServiceArea() {
        val area = _uiState.value.pendingDetectedArea ?: return
        val confirmed = area.copy(
            confirmedByUser = true,
            updatedAtMillis = System.currentTimeMillis(),
        )
        _uiState.update {
            it.copy(
                publicServiceSettings = it.publicServiceSettings.copy(
                    regionCode = confirmed.regionCode,
                    regionName = confirmed.regionName,
                    cityCode = confirmed.cityCode,
                    cityName = confirmed.cityName,
                    customCityName = confirmed.customCityName,
                    areaSource = confirmed.source,
                    areaConfirmedByUser = true,
                    areaUpdatedAtMillis = confirmed.updatedAtMillis,
                ),
                pendingDetectedArea = null,
                publicServiceMessage = "Местоположение подтверждено",
                errorMessage = null,
            )
        }
    }

    fun dismissDetectedPublicServiceArea() {
        _uiState.update { it.copy(pendingDetectedArea = null) }
    }

    fun detectPublicServiceOperator() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetectingPublicServiceOperator = true,
                    errorMessage = null,
                    publicServiceMessage = "Определяю оператора мобильной сети",
                )
            }
            val detected = mobileOperatorDetector.detect()
            if (detected.operatorCode.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isDetectingPublicServiceOperator = false,
                        publicServiceMessage = null,
                        errorMessage = "Не удалось определить оператора автоматически. Выберите его вручную.",
                    )
                }
                return@launch
            }
            applyDetectedOperator(detected)
        }
    }

    fun updatePublicServiceDeviceAlias(value: String) {
        _uiState.update {
            it.copy(publicServiceSettings = it.publicServiceSettings.copy(deviceAlias = value.take(64)))
        }
    }

    fun savePublicServiceSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingPublicServiceSettings = true,
                    publicServiceMessage = null,
                    errorMessage = null,
                )
            }
            try {
                val settings = _uiState.value.publicServiceSettings.resolveAutoOperatorIfNeeded()
                val validationError = if (settings.shareReports) validatePublicSharing(settings) else null
                if (validationError != null) {
                    _uiState.update {
                        it.copy(
                            isSavingPublicServiceSettings = false,
                            publicServiceSettings = settings.copy(shareReports = false),
                            errorMessage = validationError,
                        )
                    }
                    return@launch
                }
                publicServiceSettingsRepository.saveSettings(settings)
                if (settings.shareReports || settings.allowRemoteChecks) {
                    publicServiceRegistrationUseCase.saveSettingsToServer(settings)
                    refreshPublicServiceLinks()
                }
                _uiState.update {
                    it.copy(
                        isSavingPublicServiceSettings = false,
                        publicServiceMessage = "Настройки общего сервиса сохранены",
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingPublicServiceSettings = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun createPublicServiceLinkCode() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingPublicServiceLinkCode = true,
                    publicServiceMessage = null,
                    errorMessage = null,
                )
            }
            try {
                publicServiceSettingsRepository.saveSettings(_uiState.value.publicServiceSettings.resolveAutoOperatorIfNeeded())
                publicServiceLinkUseCase.createLinkCode()
                _uiState.update {
                    it.copy(
                        isCreatingPublicServiceLinkCode = false,
                        publicServiceMessage = "Код привязки создан",
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingPublicServiceLinkCode = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun refreshPublicServiceLinks() {
        viewModelScope.launch {
            try {
                val settings = publicServiceSettingsRepository.getSettings()
                if (!settings.isRegistered) {
                    _uiState.update { it.copy(publicServiceLinks = emptyList()) }
                    return@launch
                }
                val links = publicServiceLinkUseCase.refreshLinks()
                _uiState.update { it.copy(publicServiceLinks = links) }
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName) }
            }
        }
    }

    fun revokePublicServiceLink(linkId: String) {
        viewModelScope.launch {
            try {
                publicServiceLinkUseCase.revokeLink(linkId)
                refreshPublicServiceLinks()
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName) }
            }
        }
    }

    fun retryPublicReportUpload() {
        viewModelScope.launch {
            try {
                val result = publicReportUploadUseCase.flushQueue()
                _uiState.update {
                    it.copy(
                        publicServiceMessage = "Отправлено: ${result.sentCount}, ошибок: ${result.failedCount}",
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName) }
            }
        }
    }

    fun deletePublicServiceServerData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingPublicServiceData = true) }
            try {
                publicServiceRegistrationUseCase.revokeServerData()
                publicReportUploadUseCase.clearPendingReports()
                _uiState.update {
                    it.copy(
                        isDeletingPublicServiceData = false,
                        publicServiceLinks = emptyList(),
                        publicServiceMessage = "Серверные данные установки удалены или отозваны",
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingPublicServiceData = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private fun validatePublicSharing(settings: com.whitelistchecker.domain.model.PublicServiceSettings): String? {
        return when {
            settings.regionCode == "UNKNOWN" || !settings.areaConfirmedByUser ->
                "Перед отправкой данных выберите и подтвердите регион"
            settings.operatorCode == "UNKNOWN" ->
                "Не удалось определить оператора. Выберите его вручную."
            else -> null
        }
    }

    private fun com.whitelistchecker.domain.model.PublicServiceSettings.resolveAutoOperatorIfNeeded():
        com.whitelistchecker.domain.model.PublicServiceSettings {
        if (operatorSelectionMode != OperatorSelectionMode.AUTO || operatorCode != "UNKNOWN") return this
        val detected = mobileOperatorDetector.detect()
        val code = detected.operatorCode ?: return this
        return copy(
            operatorCode = code,
            operatorSource = detected.source,
            operatorDisplayName = detected.displayName,
            operatorMccMnc = detected.mccMnc,
            operatorUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun applyDetectedOperator(detected: DetectedOperator) {
        _uiState.update {
            it.copy(
                isDetectingPublicServiceOperator = false,
                publicServiceSettings = it.publicServiceSettings.copy(
                    operatorCode = detected.operatorCode ?: "UNKNOWN",
                    operatorSelectionMode = OperatorSelectionMode.AUTO,
                    operatorSource = detected.source,
                    operatorDisplayName = detected.displayName,
                    operatorMccMnc = detected.mccMnc,
                    operatorUpdatedAtMillis = System.currentTimeMillis(),
                ),
                publicServiceMessage = "Оператор определён: ${detected.displayName ?: detected.operatorCode}",
                errorMessage = null,
            )
        }
    }

    fun updateActiveMonitoringIntervalInput(value: String) {
        _uiState.update {
            it.copy(
                activeMonitoringIntervalInput = value.filter { ch -> ch.isDigit() },
                activeMonitoringIntervalError = null,
            )
        }
    }

    fun saveActiveMonitoringInterval() {
        viewModelScope.launch {
            val interval = resolveActiveMonitoringInterval() ?: return@launch
            val settings = _uiState.value.activeMonitoringSettings.copy(intervalMinutes = interval)
            activeMonitoringRepository.saveSettings(settings)
            _uiState.update {
                it.copy(
                    activeMonitoringSettings = settings,
                    activeMonitoringIntervalError = null,
                )
            }
        }
    }

    fun updateActiveMonitoringNotificationPolicy(policy: NotificationPolicy) {
        viewModelScope.launch {
            val settings = _uiState.value.activeMonitoringSettings.copy(notificationPolicy = policy)
            activeMonitoringRepository.saveSettings(settings)
            _uiState.update { it.copy(activeMonitoringSettings = settings) }
        }
    }

    fun updateNotifyOnAccessRestored(enabled: Boolean) {
        viewModelScope.launch {
            val settings = _uiState.value.activeMonitoringSettings.copy(notifyOnAccessRestored = enabled)
            activeMonitoringRepository.saveSettings(settings)
            _uiState.update { it.copy(activeMonitoringSettings = settings) }
        }
    }

    fun updateTelegramCommandsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val settings = _uiState.value.activeMonitoringSettings.copy(telegramCommandsEnabled = enabled)
            activeMonitoringRepository.saveSettings(settings)
            _uiState.update { it.copy(activeMonitoringSettings = settings) }
        }
    }

    fun startActiveMonitoring() {
        viewModelScope.launch {
            val interval = resolveActiveMonitoringInterval() ?: return@launch
            if (permissionChecker.requiresRuntimePermission() && !permissionChecker.areNotificationsAllowed()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Разрешите уведомления перед запуском активного мониторинга.",
                    )
                }
                return@launch
            }
            val settings = _uiState.value.activeMonitoringSettings.copy(intervalMinutes = interval)
            activeMonitoringRepository.saveSettings(settings)
            activeMonitoringController.start()
            _uiState.update {
                it.copy(
                    activeMonitoringSettings = settings,
                    activeMonitoringIntervalError = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun stopActiveMonitoring() {
        activeMonitoringController.stop()
    }

    fun runActiveMonitoringCheckNow() {
        activeMonitoringController.checkNow()
    }

    private fun resolveActiveMonitoringInterval(): Long? {
        val parsed = _uiState.value.activeMonitoringIntervalInput.toLongOrNull()
        if (parsed == null ||
            parsed !in ActiveMonitoringSettings.MIN_INTERVAL_MINUTES..ActiveMonitoringSettings.MAX_INTERVAL_MINUTES
        ) {
            _uiState.update {
                it.copy(
                    activeMonitoringIntervalError =
                    "Интервал активного мониторинга должен быть от 1 до 60 минут.",
                )
            }
            return null
        }
        return parsed
    }

    fun refreshLastCheckPresentation() {
        _uiState.update { state ->
            state.copy(
                lastCheckDisplayState = resolveLastCheckDisplayState(
                    isChecking = state.isChecking,
                    lastCheck = state.result,
                ),
            )
        }
    }

    fun checkMobileNetwork() {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    errorMessage = null,
                    lastPersistenceStatus = null,
                    lastCheckDisplayState = resolveLastCheckDisplayState(
                        isChecking = true,
                        lastCheck = it.result,
                    ),
                )
            }
            try {
                val result = checkAndNotifyUseCase.execute()
                val monitorResult = result.monitorResult
                _uiState.update {
                    val checkResult = monitorResult.checkResult
                    it.copy(
                        isChecking = false,
                        result = checkResult,
                        monitorState = monitorResult.monitorState,
                        lastStateChangeEvent = monitorResult.stateChangeEvent,
                        lastLocalNotificationResult = result.localNotificationResult,
                        lastTelegramSendResult = result.telegramSendResult,
                        lastQueueFlushResult = result.queueFlushResult,
                        pendingReportsCount = result.pendingReportsCount,
                        lastPersistenceStatus = result.persistenceStatus,
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                        errorMessage = if (checkResult.error == WhitelistCheckUseCase.CHANGE_NETWORK_STATE_DENIED_MESSAGE) {
                            checkResult.error
                        } else {
                            null
                        },
                        lastCheckDisplayState = resolveLastCheckDisplayState(
                            isChecking = false,
                            lastCheck = checkResult,
                        ),
                    )
                }
                refreshStatistics()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                        lastCheckDisplayState = resolveLastCheckDisplayState(
                            isChecking = false,
                            lastCheck = it.result,
                        ),
                    )
                }
            }
        }
    }

    fun updateLocalNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localNotificationSettingsRepository.setEnabled(enabled)
            _uiState.update {
                it.copy(localNotificationSettings = LocalNotificationSettings(enabled = enabled))
            }
            refreshNotificationPermissionState()
        }
    }

    fun sendLocalTestNotification() {
        val checkResult = _uiState.value.result
        if (checkResult == null) {
            _uiState.update {
                it.copy(lastLocalNotificationResult = LocalNotificationResult.Failure("Сначала выполните проверку мобильной сети"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingLocalTest = true) }
            try {
                val result = checkAndNotifyUseCase.sendLocalTestNotification(checkResult)
                _uiState.update {
                    it.copy(
                        isSendingLocalTest = false,
                        lastLocalNotificationResult = result,
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingLocalTest = false,
                        lastLocalNotificationResult = LocalNotificationResult.Failure(
                            exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun refreshNotificationPermissionState() {
        _uiState.update {
            it.copy(
                notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                notificationPermissionRequired = permissionChecker.requiresRuntimePermission(),
            )
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        refreshNotificationPermissionState()
        if (!granted) {
            _uiState.update {
                it.copy(lastLocalNotificationResult = LocalNotificationResult.PermissionNotGranted)
            }
        }
    }

    fun openBatteryOptimizationSettings() {
        appSettingsNavigator.openBatteryOptimizationSettings()
    }

    fun openAppDetailsSettings() {
        appSettingsNavigator.openAppDetailsSettings()
    }

    fun updateTelegramEnabled(enabled: Boolean) {
        viewModelScope.launch {
            telegramSettingsRepository.setEnabled(enabled)
            _uiState.update {
                it.copy(telegramSettings = it.telegramSettings.copy(enabled = enabled))
            }
        }
    }

    fun updateTelegramWorkerUrl(value: String) {
        _uiState.update { it.copy(telegramSettings = it.telegramSettings.copy(workerUrl = value)) }
    }

    fun updateTelegramRelaySecret(value: String) {
        _uiState.update { it.copy(telegramSettings = it.telegramSettings.copy(relaySecret = value)) }
    }

    fun updateTelegramChatId(value: String) {
        _uiState.update { it.copy(telegramSettings = it.telegramSettings.copy(chatId = value)) }
    }

    fun saveTelegramSettings() {
        viewModelScope.launch {
            try {
                telegramSettingsRepository.saveSettings(_uiState.value.telegramSettings)
                val flushResult = if (_uiState.value.telegramSettings.isConfigured) {
                    checkAndNotifyUseCase.flushPendingReports()
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        lastQueueFlushResult = flushResult,
                        pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    fun retryPendingTelegramReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFlushingTelegramQueue = true, errorMessage = null) }
            try {
                val flushResult = checkAndNotifyUseCase.flushPendingReports()
                _uiState.update {
                    it.copy(
                        isFlushingTelegramQueue = false,
                        lastQueueFlushResult = flushResult,
                        pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isFlushingTelegramQueue = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun clearPendingTelegramReports() {
        viewModelScope.launch {
            try {
                checkAndNotifyUseCase.clearPendingReports()
                _uiState.update {
                    it.copy(
                        pendingReportsCount = 0,
                        lastQueueFlushResult = null,
                        errorMessage = null,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    fun testTelegramWorker() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isTestingTelegram = true, errorMessage = null, lastTelegramTestMessage = null)
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramWorkerClient.getMe(state.telegramSettings)
                _uiState.update {
                    it.copy(
                        isTestingTelegram = false,
                        lastTelegramTestResult = result,
                        lastTelegramTestMessage = when (result) {
                            TelegramTestResult.Success -> "Worker работает, бот доступен"
                            is TelegramTestResult.Failure -> result.reason
                        },
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isTestingTelegram = false,
                        lastTelegramTestMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun sendTelegramTestMessage() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSendingTelegramTest = true, errorMessage = null, lastTelegramSendMessage = null)
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramEventNotifierUseCase.sendTestMessage(TEST_MESSAGE_TEXT)
                val broadcastMessage = when (result) {
                    TelegramSendResult.Success -> "Тестовое сообщение отправлено всем включённым получателям"
                    is TelegramSendResult.Failure -> result.reason
                    null -> "Telegram-уведомления выключены или нет включённых получателей"
                }
                _uiState.update {
                    it.copy(
                        isSendingTelegramTest = false,
                        lastTelegramSendResult = result,
                        lastTelegramSendMessage = broadcastMessage,
                        pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingTelegramTest = false,
                        lastTelegramSendMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun sendTelegramCheckReport() {
        val state = _uiState.value
        val checkResult = state.result
        if (checkResult == null) {
            _uiState.update {
                it.copy(lastTelegramSendMessage = "Сначала выполните проверку мобильной сети")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSendingCheckReport = true, errorMessage = null, lastTelegramSendMessage = null)
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramEventNotifierUseCase.sendOnManualCheck(checkResult)
                val message = when (result) {
                    TelegramSendResult.Success -> "Отчёт о проверке отправлен всем включённым получателям"
                    is TelegramSendResult.Failure -> result.reason
                    null -> "Telegram-уведомления выключены или нет включённых получателей"
                }
                _uiState.update {
                    it.copy(
                        isSendingCheckReport = false,
                        lastTelegramSendResult = result,
                        lastTelegramSendMessage = message,
                        pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingCheckReport = false,
                        lastTelegramSendMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun prepareTelegramChatDiscovery() {
        val state = _uiState.value
        if (!state.telegramSettings.isReadyForDiscovery) {
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        errorMessage = discoverySettingsError(state.telegramSettings),
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        isPreparing = true,
                        errorMessage = null,
                        statusMessage = null,
                    ),
                )
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramChatIdResolverUseCase.prepareChatDiscovery()
                val offset = telegramSettingsRepository.getChatDiscoveryOffset()
                handleDiscoveryResult(
                    result = result,
                    offset = offset,
                    preparing = false,
                    successStatus = buildPrepareStatusMessage(result, offset),
                )
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isPreparing = false,
                            errorMessage = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun findTelegramChatId() {
        val state = _uiState.value
        if (!state.telegramSettings.isReadyForDiscovery) {
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        errorMessage = discoverySettingsError(state.telegramSettings),
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        isLoading = true,
                        errorMessage = null,
                    ),
                )
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramChatIdResolverUseCase.findNewChats()
                val offset = telegramSettingsRepository.getChatDiscoveryOffset()
                handleDiscoveryResult(
                    result = result,
                    offset = offset,
                    loading = false,
                    successStatus = when (result) {
                        is TelegramChatDiscoveryResult.Success ->
                            "Найдено кандидатов: ${result.candidates.size}"
                        else -> null
                    },
                )
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun resetTelegramChatDiscovery() {
        viewModelScope.launch {
            telegramSettingsRepository.clearChatDiscoveryOffset()
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = TelegramChatDiscoveryUiState(
                        statusMessage = RESET_DISCOVERY_MESSAGE,
                    ),
                )
            }
        }
    }

    fun findRecentTelegramChats() {
        val state = _uiState.value
        if (!state.telegramSettings.isReadyForDiscovery) {
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        errorMessage = discoverySettingsError(state.telegramSettings),
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            val preservedOffset = telegramSettingsRepository.getChatDiscoveryOffset()
            _uiState.update {
                it.copy(
                    telegramChatDiscovery = it.telegramChatDiscovery.copy(
                        isLoadingRecent = true,
                        errorMessage = null,
                    ),
                )
            }
            try {
                telegramSettingsRepository.saveSettings(state.telegramSettings)
                val result = telegramChatIdResolverUseCase.findRecentChats()
                handleDiscoveryResult(
                    result = result,
                    offset = preservedOffset,
                    loadingRecent = false,
                    successStatus = when (result) {
                        is TelegramChatDiscoveryResult.Success ->
                            "$RECENT_CHATS_STATUS\nНайдено кандидатов: ${result.candidates.size}"
                        is TelegramChatDiscoveryResult.Empty ->
                            when {
                                result.rawUpdatesCount == 0 -> "getUpdates вернул 0 updates"
                                else -> "getUpdates вернул updates, но message.chat не найден"
                            }
                        else -> null
                    },
                )
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isLoadingRecent = false,
                            errorMessage = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun useTelegramChat(candidate: TelegramChatCandidate) {
        addTelegramRecipient(candidate)
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                val lastCheckLoad = lastCheckRepository.load()
                val persistedResult = when (lastCheckLoad) {
                    is LastCheckLoadResult.Success -> {
                        lastCheckLoadFailed = false
                        lastCheckLoad.result
                    }
                    is LastCheckLoadResult.Error -> {
                        lastCheckLoadFailed = true
                        null
                    }
                    LastCheckLoadResult.None -> {
                        lastCheckLoadFailed = false
                        null
                    }
                }
                val localSettings = localNotificationSettingsRepository.getSettings()
                val telegramSettings = telegramSettingsRepository.getSettings()
                val discoveryOffset = telegramSettingsRepository.getChatDiscoveryOffset()
                val monitorState = checkAndNotifyUseCase.loadMonitorState()
                val pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount()
                val backgroundSettings = backgroundCheckSettingsRepository.getSettings()
                val backgroundStatus = backgroundCheckStatusRepository.getStatus()
                activeMonitoringController.reconcileStateWithProcess()
                val activeSettings = activeMonitoringRepository.getSettings()
                val activeStatus = activeMonitoringRepository.getStatus()
                val checkTargets = checkTargetsRepository.getTargets()
                backgroundCheckScheduler.reschedule(backgroundSettings)
                val useCustom = !backgroundSettings.isPresetInterval
                _uiState.update { state ->
                    val restoredResult = persistedResult ?: state.result
                    state.copy(
                        localNotificationSettings = localSettings,
                        telegramSettings = telegramSettings,
                        telegramChatDiscovery = state.telegramChatDiscovery.copy(
                            discoveryOffset = discoveryOffset,
                        ),
                        result = restoredResult,
                        monitorState = monitorState,
                        pendingReportsCount = pendingReportsCount,
                        backgroundCheckSettings = backgroundSettings,
                        backgroundCheckStatus = backgroundStatus,
                        activeMonitoringSettings = activeSettings,
                        activeMonitoringStatus = activeStatus,
                        activeMonitoringIntervalInput = activeSettings.intervalMinutes.toString(),
                        checkTargets = checkTargets,
                        useCustomInterval = useCustom,
                        customIntervalInput = backgroundSettings.intervalMinutes.toString(),
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                        notificationPermissionRequired = permissionChecker.requiresRuntimePermission(),
                        lastCheckDisplayState = resolveLastCheckDisplayState(
                            isChecking = state.isChecking,
                            lastCheck = restoredResult,
                        ),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: exception.javaClass.simpleName)
                }
            }
        }
    }

    private fun handleDiscoveryResult(
        result: TelegramChatDiscoveryResult,
        offset: Long?,
        preparing: Boolean = false,
        loading: Boolean = false,
        loadingRecent: Boolean = false,
        successStatus: String? = null,
    ) {
        when (result) {
            is TelegramChatDiscoveryResult.Success -> {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isPreparing = preparing,
                            isLoading = loading,
                            isLoadingRecent = loadingRecent,
                            discoveryOffset = offset,
                            candidates = result.candidates,
                            lastResult = result,
                            statusMessage = successStatus,
                            errorMessage = null,
                        ),
                    )
                }
            }
            is TelegramChatDiscoveryResult.Empty -> {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isPreparing = preparing,
                            isLoading = loading,
                            isLoadingRecent = loadingRecent,
                            discoveryOffset = offset,
                            candidates = emptyList(),
                            lastResult = result,
                            statusMessage = successStatus,
                            errorMessage = if (successStatus != null) {
                                null
                            } else {
                                result.userMessage()
                            },
                        ),
                    )
                }
            }
            is TelegramChatDiscoveryResult.Failure -> {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isPreparing = preparing,
                            isLoading = loading,
                            isLoadingRecent = loadingRecent,
                            discoveryOffset = offset,
                            lastResult = result,
                            statusMessage = null,
                            errorMessage = result.reason,
                        ),
                    )
                }
            }
        }
    }

    private fun buildPrepareStatusMessage(
        result: TelegramChatDiscoveryResult,
        offset: Long?,
    ): String {
        return buildString {
            append(PREPARE_SUCCESS_MESSAGE)
            if (result is TelegramChatDiscoveryResult.Empty && result.rawUpdatesCount > 0 && offset != null) {
                append("\nСтарые updates пропущены. Offset: $offset")
            }
        }
    }

    private fun discoverySettingsError(settings: TelegramSettings): String {
        return when {
            settings.workerUrl.isBlank() -> "Worker URL не указан"
            settings.relaySecret.isBlank() -> "Relay Secret не указан"
            else -> "Настройки Telegram неполные"
        }
    }

    private fun resolveLastCheckDisplayState(
        isChecking: Boolean,
        lastCheck: NetworkCheckResult?,
    ): LastCheckDisplayState {
        return lastCheckStateResolver.resolve(
            isChecking = isChecking,
            loadFailed = lastCheckLoadFailed,
            lastCheck = lastCheck,
            nowMillis = System.currentTimeMillis(),
        )
    }

    private fun applyStatisticsLoadResult(
        checkResult: StatisticsLoadResult,
        timelineResult: WhitelistTimelineLoadResult,
    ) {
        if (checkResult is StatisticsLoadResult.Failure) {
            Log.w(TAG, "Statistics load failed", checkResult.cause)
            val message = checkResult.cause.message ?: checkResult.cause.javaClass.simpleName
            _uiState.update { state ->
                state.copy(
                    statisticsUiState = if (state.currentScreen == AppScreen.STATISTICS) {
                        StatisticsUiState.Error(message)
                    } else {
                        state.statisticsUiState
                    },
                    homeStatisticsUiState = HomeStatisticsUiState.Error,
                )
            }
            return
        }

        val timelineDashboard = when (timelineResult) {
            is WhitelistTimelineLoadResult.Success -> timelineResult.dashboard
            else -> null
        }
        val timelineEmpty = timelineResult is WhitelistTimelineLoadResult.Empty

        val checkDashboard = when (checkResult) {
            is StatisticsLoadResult.Success -> checkResult.dashboard
            else -> emptyCheckStatisticsDashboard()
        }
        val hasTimelineContent = timelineDashboard != null && !timelineEmpty

        when {
            !hasTimelineContent && checkResult is StatisticsLoadResult.Empty -> {
                _uiState.update {
                    it.copy(
                        statisticsUiState = StatisticsUiState.Empty,
                        homeStatisticsUiState = HomeStatisticsUiState.Hidden,
                    )
                }
            }
            else -> {
                _uiState.update { state ->
                    state.copy(
                        statisticsUiState = buildStatisticsContent(
                            state = state,
                            checkDashboard = checkDashboard,
                            timelineDashboard = timelineDashboard,
                            timelineEmpty = timelineEmpty,
                        ),
                        homeStatisticsUiState = HomeStatisticsMapper.map(timelineDashboard),
                    )
                }
            }
        }
    }

    private fun buildStatisticsContent(
        state: MainUiState,
        checkDashboard: StatisticsDashboard,
        timelineDashboard: WhitelistTimelineDashboard?,
        timelineEmpty: Boolean,
    ): StatisticsUiState.Content {
        val freshness = StatisticsFreshnessMapper.map(
            checkDashboard = checkDashboard,
            lastCheck = state.result,
            lastCheckDisplay = state.lastCheckDisplayState,
            whitelistAvailableTargets = 0,
            whitelistTotalTargets = 0,
            whitelistLowSample = (timelineDashboard?.binarySamples ?: 0) < 3,
        )
        return StatisticsUiState.Content(
            dashboard = checkDashboard,
            whitelistTimeline = timelineDashboard,
            whitelistTimelineEmpty = timelineEmpty,
            freshness = freshness,
        )
    }

    private fun emptyCheckStatisticsDashboard(): StatisticsDashboard {
        return StatisticsDashboard(
            summary = com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary(),
            targets = emptyList(),
            routeKinds = emptyList(),
            networks = emptyList(),
            daily = emptyList(),
            isStale = false,
            lastUpdatedAt = null,
        )
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val PREPARE_SUCCESS_MESSAGE =
            "Поиск chat_id начат. Теперь отправь боту новое сообщение, например /start или id test, " +
                "затем нажми «Получить chat_id»."
        private const val RESET_DISCOVERY_MESSAGE =
            "Поиск chat_id сброшен. Нажми «Начать получение chat_id», затем отправь боту новое сообщение."
        private const val RECENT_CHATS_STATUS =
            "Показаны последние чаты из getUpdates. Проверь, что это нужный чат."
        private const val TEST_MESSAGE_TEXT = "Тестовое сообщение Whitelist Monitor"
    }
}
