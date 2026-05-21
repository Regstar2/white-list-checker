package com.whitelistchecker.ui.main

import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult

data class TelegramChatDiscoveryUiState(
    val isPreparing: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingRecent: Boolean = false,
    val discoveryOffset: Long? = null,
    val candidates: List<TelegramChatCandidate> = emptyList(),
    val selectedCandidate: TelegramChatCandidate? = null,
    val lastResult: TelegramChatDiscoveryResult? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)
