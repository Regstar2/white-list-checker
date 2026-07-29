package com.whitelistchecker.domain.telegram

enum class TelegramBotCommand {
    STATUS,
    CHECK,
    HELP,
    UNKNOWN,
}

object TelegramCommandParser {
    fun parse(text: String): TelegramBotCommand {
        return when (text.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            .substringBefore('@')
            .lowercase()) {
            "/status" -> TelegramBotCommand.STATUS
            "/check" -> TelegramBotCommand.CHECK
            "/help" -> TelegramBotCommand.HELP
            else -> TelegramBotCommand.UNKNOWN
        }
    }
}

object TelegramCommandAuthorizer {
    fun isAuthorized(
        allowedChatIds: Set<String>,
        chatId: String,
    ): Boolean {
        return chatId.isNotBlank() && chatId in allowedChatIds
    }
}
