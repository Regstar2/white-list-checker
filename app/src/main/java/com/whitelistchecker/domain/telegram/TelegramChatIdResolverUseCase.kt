package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult

class TelegramChatIdResolverUseCase(
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
) {

    suspend fun prepareChatDiscovery(): TelegramChatDiscoveryResult {
        val settings = settingsRepository.getSettings()
        if (!settings.isReadyForDiscovery) {
            return TelegramChatDiscoveryResult.Failure(discoverySettingsError(settings))
        }
        val result = telegramWorkerClient.getUpdates(settings, offset = null)
        if (result is TelegramChatDiscoveryResult.Failure) {
            return result
        }
        val nextOffset = when (result) {
            is TelegramChatDiscoveryResult.Success -> result.nextOffset ?: 0L
            is TelegramChatDiscoveryResult.Empty -> result.nextOffset ?: 0L
            is TelegramChatDiscoveryResult.Failure -> return result
        }
        val skippedUpdatesCount = when (result) {
            is TelegramChatDiscoveryResult.Success -> result.rawUpdatesCount
            is TelegramChatDiscoveryResult.Empty -> result.rawUpdatesCount
            is TelegramChatDiscoveryResult.Failure -> 0
        }
        settingsRepository.saveChatDiscoveryOffset(nextOffset)
        return TelegramChatDiscoveryResult.Empty(
            nextOffset = nextOffset,
            rawUpdatesCount = skippedUpdatesCount,
        )
    }

    suspend fun findRecentChats(): TelegramChatDiscoveryResult {
        val settings = settingsRepository.getSettings()
        if (!settings.isReadyForDiscovery) {
            return TelegramChatDiscoveryResult.Failure(discoverySettingsError(settings))
        }
        return telegramWorkerClient.getUpdates(settings, offset = null)
    }

    suspend fun findNewChats(): TelegramChatDiscoveryResult {
        val settings = settingsRepository.getSettings()
        if (!settings.isReadyForDiscovery) {
            return TelegramChatDiscoveryResult.Failure(discoverySettingsError(settings))
        }
        val offset = settingsRepository.getChatDiscoveryOffset()
            ?: return TelegramChatDiscoveryResult.Failure(
                "Сначала нажми «Начать получение chat_id»",
            )
        val result = telegramWorkerClient.getUpdates(settings, offset = offset)
        when (result) {
            is TelegramChatDiscoveryResult.Success -> {
                if (result.nextOffset != null) {
                    settingsRepository.saveChatDiscoveryOffset(result.nextOffset)
                }
            }
            is TelegramChatDiscoveryResult.Empty -> {
                if (result.nextOffset != null) {
                    settingsRepository.saveChatDiscoveryOffset(result.nextOffset)
                }
            }
            is TelegramChatDiscoveryResult.Failure -> Unit
        }
        return result
    }

    suspend fun useChat(candidate: TelegramChatCandidate) {
        settingsRepository.saveChatId(candidate.chatId)
    }

    private fun discoverySettingsError(settings: com.whitelistchecker.domain.model.TelegramSettings): String {
        return when {
            settings.workerUrl.isBlank() -> "Worker URL не указан"
            settings.relaySecret.isBlank() -> "Relay Secret не указан"
            else -> "Настройки Telegram неполные"
        }
    }
}
