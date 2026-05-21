package com.whitelistchecker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val whitelistCheckUseCase: WhitelistCheckUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun checkMobileNetwork() {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            try {
                val result = whitelistCheckUseCase.execute()
                _uiState.update { it.copy(isChecking = false, result = result) }
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
}
