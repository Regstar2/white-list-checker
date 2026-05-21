package com.whitelistchecker.domain.model

sealed interface TelegramSendResult {

    data object Success : TelegramSendResult

    data class Failure(
        val reason: String,
    ) : TelegramSendResult
}
