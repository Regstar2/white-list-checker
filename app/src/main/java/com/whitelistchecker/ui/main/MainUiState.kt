package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.NetworkCheckResult

data class MainUiState(
    val isChecking: Boolean = false,
    val result: NetworkCheckResult? = null,
    val errorMessage: String? = null,
)
