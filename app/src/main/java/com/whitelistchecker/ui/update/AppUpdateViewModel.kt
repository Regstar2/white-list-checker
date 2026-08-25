package com.whitelistchecker.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.domain.update.AppRelease
import com.whitelistchecker.domain.update.AppUpdateCheckResult
import com.whitelistchecker.domain.update.AppUpdateError
import com.whitelistchecker.domain.update.CheckForAppUpdateUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState

    data class UpToDate(
        val installedVersion: String,
    ) : AppUpdateUiState

    data class Available(
        val installedVersion: String,
        val release: AppRelease,
        val promptDismissed: Boolean = false,
    ) : AppUpdateUiState

    data class Error(
        val error: AppUpdateError,
    ) : AppUpdateUiState
}

class AppUpdateViewModel(
    private val checkForAppUpdateUseCase: CheckForAppUpdateUseCase,
    private val tryAcquireAutomaticCheck: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()
    private var checkJob: Job? = null

    init {
        if (tryAcquireAutomaticCheck()) {
            checkForUpdates(manual = false)
        }
    }

    fun checkForUpdates(manual: Boolean = true) {
        if (checkJob?.isActive == true) return

        checkJob = viewModelScope.launch {
            _uiState.value = AppUpdateUiState.Checking

            when (val result = checkForAppUpdateUseCase.check()) {
                is AppUpdateCheckResult.UpdateAvailable -> {
                    _uiState.value = AppUpdateUiState.Available(
                        installedVersion = result.installedVersion,
                        release = result.release,
                        promptDismissed = false,
                    )
                }
                is AppUpdateCheckResult.UpToDate -> {
                    _uiState.value = AppUpdateUiState.UpToDate(result.installedVersion)
                }
                is AppUpdateCheckResult.Failure -> {
                    _uiState.value = if (manual) {
                        AppUpdateUiState.Error(result.error)
                    } else {
                        AppUpdateUiState.Idle
                    }
                }
            }
        }
    }

    fun dismissAvailablePrompt() {
        _uiState.update { state ->
            if (state is AppUpdateUiState.Available) {
                state.copy(promptDismissed = true)
            } else {
                state
            }
        }
    }
}
