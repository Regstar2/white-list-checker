package com.whitelistchecker.domain.model

import java.util.UUID

data class TelegramRecipient(
    val id: String,
    val chatId: String,
    val type: TelegramChatType,
    val displayName: String,
    val username: String? = null,
    val enabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    companion object {
        fun fromCandidate(candidate: TelegramChatCandidate): TelegramRecipient {
            val name = buildDisplayName(candidate)
            return TelegramRecipient(
                id = UUID.randomUUID().toString(),
                chatId = candidate.chatId,
                type = candidate.type,
                displayName = name,
                username = candidate.username,
            )
        }

        fun fromLegacyChatId(chatId: String): TelegramRecipient {
            return TelegramRecipient(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                type = TelegramChatType.PRIVATE,
                displayName = "Получатель $chatId",
            )
        }

        private fun buildDisplayName(candidate: TelegramChatCandidate): String {
            val title = candidate.title?.trim().orEmpty()
            if (title.isNotBlank()) return title
            val fullName = listOfNotNull(candidate.firstName, candidate.lastName)
                .joinToString(" ")
                .trim()
            if (fullName.isNotBlank()) return fullName
            candidate.username?.let { return "@$it" }
            return "Chat ${candidate.chatId}"
        }
    }
}
