package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.active.ActiveMonitoringRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.model.TelegramCommandUpdatesResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.random.Random

class TelegramCommandListener(
    private val activeMonitoringRepository: ActiveMonitoringRepository,
    private val settingsRepository: TelegramSettingsRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val commandHandler: TelegramCommandHandler,
) {

    suspend fun runUntilCancelled() {
        var failures = 0
        while (currentCoroutineContext().isActive) {
            val activeSettings = activeMonitoringRepository.getSettings()
            if (!activeSettings.telegramCommandsEnabled) {
                activeMonitoringRepository.saveTelegramLastError(null)
                delay(IDLE_DELAY_MS)
                continue
            }

            val telegramSettings = settingsRepository.getSettings()
            if (!telegramSettings.isConfigured) {
                activeMonitoringRepository.saveTelegramLastError(
                    "Telegram-команды включены, но Worker или получатели не настроены",
                )
                delay(CONFIG_RETRY_DELAY_MS)
                continue
            }

            val offset = activeMonitoringRepository.getStatus().telegramCommandOffset
            when (
                val result = telegramWorkerClient.getCommandUpdates(
                    settings = telegramSettings,
                    offset = offset,
                    timeoutSeconds = LONG_POLL_TIMEOUT_SECONDS,
                )
            ) {
                is TelegramCommandUpdatesResult.Success -> {
                    failures = 0
                    activeMonitoringRepository.saveTelegramLastError(null)
                    for (update in result.updates.sortedBy { it.updateId }) {
                        runCatching { commandHandler.handle(update) }
                            .onFailure { exception ->
                                activeMonitoringRepository.saveTelegramLastError(
                                    exception.message ?: exception.javaClass.simpleName,
                                )
                            }
                        activeMonitoringRepository.saveTelegramCommandOffset(update.updateId + 1)
                    }
                    if (result.updates.isEmpty() && result.nextOffset != null) {
                        activeMonitoringRepository.saveTelegramCommandOffset(result.nextOffset)
                    }
                }
                is TelegramCommandUpdatesResult.Failure -> {
                    failures += 1
                    if (failures >= VISIBLE_ERROR_FAILURES) {
                        activeMonitoringRepository.saveTelegramLastError(result.reason)
                    }
                    delay(result.retryAfterSeconds?.times(1000L) ?: backoffDelayMs(failures))
                }
            }
        }
    }

    private fun backoffDelayMs(failures: Int): Long {
        val base = min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS * (1L shl failures.coerceAtMost(5)))
        return base + Random.nextLong(0, JITTER_MS)
    }

    private companion object {
        const val LONG_POLL_TIMEOUT_SECONDS = 25L
        const val IDLE_DELAY_MS = 5_000L
        const val CONFIG_RETRY_DELAY_MS = 30_000L
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
        const val JITTER_MS = 1_000L
        const val VISIBLE_ERROR_FAILURES = 3
    }
}
