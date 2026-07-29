package com.whitelistchecker.domain.model

data class TelegramCommandUpdate(
    val updateId: Long,
    val chatId: String,
    val text: String,
)

sealed interface TelegramCommandUpdatesResult {
    data class Success(
        val updates: List<TelegramCommandUpdate>,
        val nextOffset: Long?,
    ) : TelegramCommandUpdatesResult

    data class Failure(
        val reason: String,
        val retryAfterSeconds: Long? = null,
    ) : TelegramCommandUpdatesResult
}
