package com.whitelistchecker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
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
import com.whitelistchecker.domain.telegram.TelegramChatIdResolverUseCase
import com.whitelistchecker.domain.telegram.TelegramWorkerClient
import com.whitelistchecker.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
    private val localNotificationSettingsRepository: LocalNotificationSettingsRepository,
    private val telegramSettingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val telegramChatIdResolverUseCase: TelegramChatIdResolverUseCase,
    private val permissionChecker: LocalNotificationPermissionChecker,
    private val channelManager: LocalNotificationChannelManager,
    private val appSettingsNavigator: AppSettingsNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        channelManager.ensureChannelsCreated()
        refreshNotificationPermissionState()
        loadInitialState()
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
                _uiState.update { it.copy(errorMessage = null) }
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
                val result = telegramWorkerClient.sendMessage(
                    settings = state.telegramSettings,
                    text = TEST_MESSAGE_TEXT,
                )
                _uiState.update {
                    it.copy(
                        isSendingTelegramTest = false,
                        lastTelegramSendResult = result,
                        lastTelegramSendMessage = when (result) {
                            TelegramSendResult.Success -> "Тестовое сообщение отправлено"
                            is TelegramSendResult.Failure -> result.reason
                        },
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
                    successStatus = PREPARE_SUCCESS_MESSAGE,
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
                    successStatus = if (result is TelegramChatDiscoveryResult.Success) {
                        "Chat ID найден"
                    } else {
                        null
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

    fun useTelegramChat(candidate: TelegramChatCandidate) {
        viewModelScope.launch {
            try {
                telegramChatIdResolverUseCase.useChat(candidate)
                val savedSettings = telegramSettingsRepository.getSettings()
                _uiState.update {
                    it.copy(
                        telegramSettings = savedSettings,
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            selectedCandidate = candidate,
                            statusMessage = "Chat ID сохранён",
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

    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                val localSettings = localNotificationSettingsRepository.getSettings()
                val telegramSettings = telegramSettingsRepository.getSettings()
                val discoveryOffset = telegramSettingsRepository.getChatDiscoveryOffset()
                val monitorState = checkAndNotifyUseCase.loadMonitorState()
                _uiState.update {
                    it.copy(
                        localNotificationSettings = localSettings,
                        telegramSettings = telegramSettings,
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            discoveryOffset = discoveryOffset,
                        ),
                        monitorState = monitorState,
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
        successStatus: String? = null,
    ) {
        when (result) {
            is TelegramChatDiscoveryResult.Success -> {
                _uiState.update {
                    it.copy(
                        telegramChatDiscovery = it.telegramChatDiscovery.copy(
                            isPreparing = preparing,
                            isLoading = loading,
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

    private fun discoverySettingsError(settings: TelegramSettings): String {
        return when {
            settings.workerUrl.isBlank() -> "Worker URL не указан"
            settings.relaySecret.isBlank() -> "Relay Secret не указан"
            else -> "Настройки Telegram неполные"
        }
    }

    companion object {
        private const val PREPARE_SUCCESS_MESSAGE =
            "Теперь напиши /start боту в личном чате или в группе, затем нажми «Получить chat_id»."
        private const val TEST_MESSAGE_TEXT = "Тестовое сообщение Whitelist Monitor"
    }
}
