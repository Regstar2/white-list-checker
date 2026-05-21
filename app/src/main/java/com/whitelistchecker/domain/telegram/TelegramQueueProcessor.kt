package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.PendingTelegramReportRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.TelegramSendResult

class TelegramQueueProcessor(
    private val pendingReportRepository: PendingTelegramReportRepository,
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
) {

    suspend fun flushQueue(): TelegramQueueFlushResult {
        val nowMillis = System.currentTimeMillis()
        pendingReportRepository.deleteOldReports(nowMillis)
        pendingReportRepository.enforceQueueLimit()

        val settings = settingsRepository.getSettings()
        val pendingReports = pendingReportRepository.getAll()
        if (!settings.isConfigured) {
            return TelegramQueueFlushResult(
                attemptedCount = 0,
                sentCount = 0,
                failedCount = 0,
                skippedCount = pendingReports.size,
                lastError = if (pendingReports.isNotEmpty()) "Telegram не настроен" else null,
            )
        }

        val sendableReports = pendingReports.filter {
            it.attemptCount < PendingTelegramReportRepository.MAX_ATTEMPT_COUNT
        }
        val skippedByAttempts = pendingReports.size - sendableReports.size

        var attemptedCount = 0
        var sentCount = 0
        var failedCount = 0
        var lastError: String? = null

        for (report in sendableReports) {
            val chatId = report.chatId.ifBlank {
                settings.enabledRecipients.firstOrNull()?.chatId.orEmpty()
            }
            if (chatId.isBlank()) {
                failedCount++
                lastError = "Chat ID не указан"
                pendingReportRepository.markAttempt(
                    entity = report,
                    error = lastError,
                    nowMillis = System.currentTimeMillis(),
                )
                break
            }

            attemptedCount++
            when (val result = telegramWorkerClient.sendMessageToChat(settings, chatId, report.text)) {
                TelegramSendResult.Success -> {
                    sentCount++
                    pendingReportRepository.delete(report.id)
                }
                is TelegramSendResult.Failure -> {
                    failedCount++
                    lastError = result.reason
                    pendingReportRepository.markAttempt(
                        entity = report,
                        error = result.reason,
                        nowMillis = System.currentTimeMillis(),
                    )
                    break
                }
            }
        }

        return TelegramQueueFlushResult(
            attemptedCount = attemptedCount,
            sentCount = sentCount,
            failedCount = failedCount,
            skippedCount = skippedByAttempts,
            lastError = lastError,
        )
    }
}
