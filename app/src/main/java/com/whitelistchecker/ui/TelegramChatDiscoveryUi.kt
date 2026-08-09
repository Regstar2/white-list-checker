package com.whitelistchecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult
import com.whitelistchecker.domain.model.TelegramChatType

@Composable
fun TelegramChatType.displayLabel(): String = when (this) {
    TelegramChatType.PRIVATE -> stringResource(R.string.notifications_chat_type_private)
    TelegramChatType.GROUP -> stringResource(R.string.notifications_chat_type_group)
    TelegramChatType.SUPERGROUP -> stringResource(R.string.notifications_chat_type_supergroup)
    TelegramChatType.CHANNEL -> stringResource(R.string.notifications_chat_type_channel)
    TelegramChatType.UNKNOWN -> stringResource(R.string.notifications_chat_type_unknown)
}

@Composable
fun TelegramChatCandidate.displayName(): String {
    if (!title.isNullOrBlank()) return title
    val fullName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .trim()
    if (fullName.isNotBlank()) return fullName
    if (!username.isNullOrBlank()) return "@$username"
    return stringResource(R.string.notifications_chat_no_name)
}

fun TelegramChatDiscoveryResult.userMessage(): String? = when (this) {
    is TelegramChatDiscoveryResult.Success -> null
    is TelegramChatDiscoveryResult.Empty ->
        "Новых сообщений не найдено. Отправь боту новое сообщение после нажатия «Начать получение chat_id» и попробуй снова. " +
            "Если ты уже писал /start раньше, нажми «Показать последние чаты»."
    is TelegramChatDiscoveryResult.Failure -> reason
}
