package com.whitelistchecker.domain.model

data class TelegramChatCandidate(
    val chatId: String,
    val type: TelegramChatType,
    val title: String?,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val sourceMessageText: String?,
)
