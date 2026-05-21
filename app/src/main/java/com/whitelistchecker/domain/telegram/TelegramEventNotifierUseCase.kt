package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class TelegramEventNotifierUseCase(
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val reportFormatter: TelegramReportFormatter,
) {

    suspend fun notifyIfNeeded(
        event: WhitelistStateChangeEvent?,
        checkResult: NetworkCheckResult,
    ): TelegramSendResult? {
        if (event == null) return null
        if (event.type != WhitelistStateChangeType.WHITELIST_TURNED_ON &&
            event.type != WhitelistStateChangeType.WHITELIST_TURNED_OFF
        ) {
            return null
        }
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) return null
        if (!settings.isConfigured) {
            return TelegramSendResult.Failure("Telegram не настроен")
        }
        val text = reportFormatter.format(event, checkResult)
        return telegramWorkerClient.sendMessage(settings, text)
    }
}
