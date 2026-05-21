package com.whitelistchecker.domain.model

sealed interface TelegramTestResult {

    data object Success : TelegramTestResult

    data class Failure(
        val reason: String,
    ) : TelegramTestResult
}
