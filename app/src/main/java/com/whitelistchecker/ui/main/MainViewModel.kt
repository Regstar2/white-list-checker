package com.whitelistchecker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.data.background.BackgroundCheckSettingsRepository
import com.whitelistchecker.data.background.BackgroundCheckStatusRepository
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.domain.notifications.LocalNotificationPermissionChecker
import com.whitelistchecker.domain.system.AppSettingsNavigator
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase
import com.whitelistchecker.domain.telegram.DetailedReportFormatter
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramEventNotifierUseCase
import com.whitelistchecker.domain.telegram.TelegramWorkerClient
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.userMessage
import com.whitelistchecker.worker.BackgroundCheckScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
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
    private val checkTargetsRepository: CheckTargetsRepository,
    private val detailedReportFormatter: DetailedReportFormatter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        channelManager.ensureChannelsCreated()
        refreshNotificationPermissionState()
        loadInitialState()
        observeBackgroundCheck()
        observeCheckTargets()
    }

    fun openScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
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

    fun checkMobileNetwork() {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            try {
                val result = checkAndNotifyUseCase.execute()
                val monitorResult = result.monitorResult
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        result = monitorResult.checkResult,
                        monitorState = monitorResult.monitorState,
                        lastStateChangeEvent = monitorResult.stateChangeEvent,
                        lastLocalNotificationResult = result.localNotificationResult,
                        lastTelegramSendResult = result.telegramSendResult,
                        lastQueueFlushResult = result.queueFlushResult,
                        pendingReportsCount = result.pendingReportsCount,
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
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
                val localSettings = localNotificationSettingsRepository.getSettings()
                val telegramSettings = telegramSettingsRepository.getSettings()
                val discoveryOffset = telegramSettingsRepository.getChatDiscoveryOffset()
                val monitorState = checkAndNotifyUseCase.loadMonitorState()
                val pendingReportsCount = checkAndNotifyUseCase.getPendingReportsCount()
                val backgroundSettings = backgroundCheckSettingsRepository.getSettings()
                val backgroundStatus = backgroundCheckStatusRepository.getStatus()
                val checkTargets = checkTargetsRepository.getTargets()
                backgroundCheckScheduler.reschedule(backgroundSettings)
                val useCustom = !backgroundSettings.isPresetInterval
                _uiState.update {
                    it.copy(
                        localNotificationSettings = localSettings,
                        telegramSettings = telegramSettings,
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            discoveryOffset = discoveryOffset,
                        ),
                        monitorState = monitorState,
                        pendingReportsCount = pendingReportsCount,
                        backgroundCheckSettings = backgroundSettings,
                        backgroundCheckStatus = backgroundStatus,
                        checkTargets = checkTargets,
                        useCustomInterval = useCustom,
                        customIntervalInput = backgroundSettings.intervalMinutes.toString(),
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                        notificationPermissionRequired = permissionChecker.requiresRuntimePermission(),
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

    companion object {
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
