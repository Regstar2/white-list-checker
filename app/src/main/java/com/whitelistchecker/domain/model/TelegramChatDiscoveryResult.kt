package com.whitelistchecker.domain.model

sealed interface TelegramChatDiscoveryResult {

    data class Success(
        val candidates: List<TelegramChatCandidate>,
        val nextOffset: Long?,
    ) : TelegramChatDiscoveryResult

    data class Empty(
        val nextOffset: Long?,
    ) : TelegramChatDiscoveryResult

    data class Failure(
        val reason: String,
    ) : TelegramChatDiscoveryResult
}
