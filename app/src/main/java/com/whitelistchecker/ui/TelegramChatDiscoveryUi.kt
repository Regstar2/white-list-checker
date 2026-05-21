package com.whitelistchecker.ui

import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult
import com.whitelistchecker.domain.model.TelegramChatType

fun TelegramChatType.toDisplayLabel(): String = when (this) {
    TelegramChatType.PRIVATE -> "Личный чат"
    TelegramChatType.GROUP -> "Группа"
    TelegramChatType.SUPERGROUP -> "Супергруппа"
    TelegramChatType.CHANNEL -> "Канал"
    TelegramChatType.UNKNOWN -> "Неизвестно"
}

fun TelegramChatCandidate.displayName(): String {
    if (!title.isNullOrBlank()) return title
    val fullName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .trim()
    if (fullName.isNotBlank()) return fullName
    if (!username.isNullOrBlank()) return "@$username"
    return "Чат без имени"
}

fun TelegramChatDiscoveryResult.userMessage(): String? = when (this) {
    is TelegramChatDiscoveryResult.Success -> null
    is TelegramChatDiscoveryResult.Empty ->
        "Новых сообщений не найдено. Отправь боту новое сообщение после нажатия «Начать получение chat_id» и попробуй снова. " +
            "Если ты уже писал /start раньше, нажми «Показать последние чаты»."
    is TelegramChatDiscoveryResult.Failure -> reason
}
