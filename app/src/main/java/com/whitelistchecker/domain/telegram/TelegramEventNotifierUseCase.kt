package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class TelegramEventNotifierUseCase(
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val reportFormatter: TelegramReportFormatter,
    private val pendingTelegramReportRepository: PendingTelegramReportRepository,
) {

    suspend fun sendOnManualCheck(checkResult: NetworkCheckResult): TelegramSendResult? {
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) return null

        val text = reportFormatter.formatManualCheck(checkResult)
        return sendMessageWithQueue(
            text = text,
            reportId = "${WhitelistStateChangeType.MANUAL_CHECK.name}_${checkResult.checkedAtMillis}",
            eventType = WhitelistStateChangeType.MANUAL_CHECK.name,
        )
    }

    suspend fun sendTestMessage(text: String): TelegramSendResult? {
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) return null

        return sendMessageWithQueue(
            text = text,
            reportId = "${WhitelistStateChangeType.TEST_MESSAGE.name}_${System.currentTimeMillis()}",
            eventType = WhitelistStateChangeType.TEST_MESSAGE.name,
        )
    }

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

        val text = reportFormatter.format(event, checkResult)
        return sendMessageWithQueue(
            text = text,
            reportId = PendingTelegramReportRepository.buildReportId(event),
            eventType = event.type.name,
            oldState = event.oldState.name,
            newState = event.newState.name,
        )
    }

    private suspend fun sendMessageWithQueue(
        text: String,
        reportId: String,
        eventType: String,
        oldState: String = WhitelistState.UNKNOWN.name,
        newState: String = WhitelistState.UNKNOWN.name,
    ): TelegramSendResult {
        val settings = settingsRepository.getSettings()
        if (!settings.isConfigured) {
            pendingTelegramReportRepository.saveMessage(
                text = text,
                id = reportId,
                eventType = eventType,
                oldState = oldState,
                newState = newState,
                error = "Telegram не настроен",
                attemptedSend = false,
            )
            return TelegramSendResult.Failure("Telegram не настроен")
        }

        return when (val result = telegramWorkerClient.sendMessage(settings, text)) {
            TelegramSendResult.Success -> result
            is TelegramSendResult.Failure -> {
                pendingTelegramReportRepository.saveMessage(
                    text = text,
                    id = reportId,
                    eventType = eventType,
                    oldState = oldState,
                    newState = newState,
                    error = result.reason,
                    attemptedSend = true,
                )
                result
            }
        }
    }
}
