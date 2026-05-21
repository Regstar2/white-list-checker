package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.TelegramBroadcastResult
import com.whitelistchecker.domain.model.TelegramRecipient
import com.whitelistchecker.domain.model.TelegramRecipientSendFailure
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.WhitelistState

class TelegramBroadcastUseCase(
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val pendingTelegramReportRepository: PendingTelegramReportRepository,
) {

    suspend fun sendToEnabledRecipients(
        text: String,
        baseReportId: String,
        eventType: String,
        oldState: String = WhitelistState.UNKNOWN.name,
        newState: String = WhitelistState.UNKNOWN.name,
        queueOnFailure: Boolean = true,
    ): TelegramBroadcastResult {
        val settings = settingsRepository.getSettings()
        if (!settings.enabled) {
            return TelegramBroadcastResult(sentCount = 0, failedCount = 0, failures = emptyList())
        }

        val recipients = settings.enabledRecipients
        if (recipients.isEmpty()) {
            if (queueOnFailure) {
                queueMessage(
                    settings = settings,
                    text = text,
                    reportId = baseReportId,
                    eventType = eventType,
                    oldState = oldState,
                    newState = newState,
                    recipient = null,
                    error = "Нет включённых Telegram-получателей",
                    attemptedSend = false,
                )
            }
            return TelegramBroadcastResult(
                sentCount = 0,
                failedCount = 0,
                failures = emptyList(),
            )
        }

        if (!settings.canTestWorker) {
            if (queueOnFailure) {
                recipients.forEach { recipient ->
                    queueMessage(
                        settings = settings,
                        text = text,
                        reportId = PendingTelegramReportRepository.buildReportId(baseReportId, recipient),
                        eventType = eventType,
                        oldState = oldState,
                        newState = newState,
                        recipient = recipient,
                        error = "Telegram не настроен",
                        attemptedSend = false,
                    )
                }
            }
            return TelegramBroadcastResult(
                sentCount = 0,
                failedCount = recipients.size,
                failures = recipients.map {
                    TelegramRecipientSendFailure(it, "Telegram не настроен")
                },
            )
        }

        var sentCount = 0
        val failures = mutableListOf<TelegramRecipientSendFailure>()

        for (recipient in recipients) {
            when (val result = telegramWorkerClient.sendMessageToChat(settings, recipient.chatId, text)) {
                TelegramSendResult.Success -> sentCount++
                is TelegramSendResult.Failure -> {
                    failures += TelegramRecipientSendFailure(recipient, result.reason)
                    if (queueOnFailure) {
                        queueMessage(
                            settings = settings,
                            text = text,
                            reportId = PendingTelegramReportRepository.buildReportId(baseReportId, recipient),
                            eventType = eventType,
                            oldState = oldState,
                            newState = newState,
                            recipient = recipient,
                            error = result.reason,
                            attemptedSend = true,
                        )
                    }
                }
            }
        }

        return TelegramBroadcastResult(
            sentCount = sentCount,
            failedCount = failures.size,
            failures = failures,
        )
    }

    private suspend fun queueMessage(
        settings: TelegramSettings,
        text: String,
        reportId: String,
        eventType: String,
        oldState: String,
        newState: String,
        recipient: TelegramRecipient?,
        error: String,
        attemptedSend: Boolean,
    ) {
        pendingTelegramReportRepository.saveMessage(
            text = text,
            id = reportId,
            eventType = eventType,
            oldState = oldState,
            newState = newState,
            error = error,
            attemptedSend = attemptedSend,
            recipient = recipient,
        )
    }
}
