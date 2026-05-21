package com.whitelistchecker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.data.notifications.LocalNotificationSettingsRepository
import com.whitelistchecker.domain.notifications.CheckAndLocalNotifyUseCase
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.domain.notifications.LocalNotificationPermissionChecker
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.LocalNotificationSettings
import com.whitelistchecker.domain.system.AppSettingsNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val checkAndLocalNotifyUseCase: CheckAndLocalNotifyUseCase,
    private val localNotificationSettingsRepository: LocalNotificationSettingsRepository,
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
                val result = checkAndLocalNotifyUseCase.execute()
                val monitorResult = result.monitorResult
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        result = monitorResult.checkResult,
                        monitorState = monitorResult.monitorState,
                        lastStateChangeEvent = monitorResult.stateChangeEvent,
                        lastLocalNotificationResult = result.localNotificationResult,
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
                it.copy(
                    localNotificationSettings = LocalNotificationSettings(enabled = enabled),
                )
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

    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                val settings = localNotificationSettingsRepository.getSettings()
                val monitorState = checkAndLocalNotifyUseCase.loadMonitorState()
                _uiState.update {
                    it.copy(
                        localNotificationSettings = settings,
                        monitorState = monitorState,
                        notificationsAllowed = permissionChecker.areNotificationsAllowed(),
                        notificationPermissionRequired = permissionChecker.requiresRuntimePermission(),
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }
        }
    }
}
