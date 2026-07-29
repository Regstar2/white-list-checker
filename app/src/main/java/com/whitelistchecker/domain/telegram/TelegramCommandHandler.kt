package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.active.ActiveMonitoringRepository
import com.whitelistchecker.data.check.LastCheckRepository
import com.whitelistchecker.data.checkrun.CheckStateRepository
import com.whitelistchecker.data.telegram.TelegramSettingsRepository
import com.whitelistchecker.domain.checkrun.CheckExecutionResult
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.LastCheckLoadResult
import com.whitelistchecker.domain.model.TelegramCommandUpdate
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.ui.toDisplayDateTime

class TelegramCommandHandler(
    private val settingsRepository: TelegramSettingsRepository,
    private val activeMonitoringRepository: ActiveMonitoringRepository,
    private val lastCheckRepository: LastCheckRepository,
    private val checkStateRepository: CheckStateRepository,
    private val telegramWorkerClient: TelegramWorkerClient,
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
    private val reportFormatter: TelegramReportFormatter,
) {

    suspend fun handle(update: TelegramCommandUpdate) {
        val settings = settingsRepository.getSettings()
        if (!isAuthorized(settings, update.chatId)) return
        val activeSettings = activeMonitoringRepository.getSettings()
        val command = TelegramCommandParser.parse(update.text)
        val reply = when (command) {
            TelegramBotCommand.STATUS -> buildStatusReply()
            TelegramBotCommand.CHECK -> runCommandCheck(activeSettings)
            TelegramBotCommand.HELP -> helpText()
            TelegramBotCommand.UNKNOWN -> helpText()
        }
        telegramWorkerClient.sendMessageToChat(settings, update.chatId, reply)
    }

    private fun isAuthorized(settings: TelegramSettings, chatId: String): Boolean {
        return TelegramCommandAuthorizer.isAuthorized(
            allowedChatIds = settings.enabledRecipients.map { it.chatId }.toSet(),
            chatId = chatId,
        )
    }

    private suspend fun buildStatusReply(): String {
        val lastCheck = lastCheckRepository.load()
        val checkState = checkStateRepository.getSnapshot()
        val header = "<b>Сохранённый статус Whitelist Checker</b>"
        val lastResult = when (lastCheck) {
            is LastCheckLoadResult.Success -> reportFormatter.formatManualCheck(lastCheck.result)
            is LastCheckLoadResult.Error -> "Последний результат не удалось загрузить."
            LastCheckLoadResult.None -> "Проверки ещё не выполнялись."
        }
        val attempt = buildString {
            appendLine()
            appendLine("<b>Последняя попытка:</b> ${checkState.lastAttemptOutcome.availability}")
            checkState.lastAttemptAtMillis?.let {
                appendLine("<b>Время попытки:</b> ${it.toDisplayDateTime()}")
            }
            checkState.lastValidWhitelistState?.let {
                appendLine("<b>Последний валидный статус:</b> $it")
            }
        }.trimEnd()
        return "$header\n\n$statusNote\n\n$lastResult\n$attempt".trim()
    }

    private suspend fun runCommandCheck(activeSettings: ActiveMonitoringSettings): String {
        return when (
            val result = checkAndNotifyUseCase.tryExecute(
                triggerType = CheckTriggerType.TELEGRAM_COMMAND,
                notificationPolicy = activeSettings.notificationPolicy,
                notifyOnAccessRestored = activeSettings.notifyOnAccessRestored,
            )
        ) {
            is CheckExecutionResult.Completed -> reportFormatter.formatManualCheck(
                result.value.monitorResult.checkResult,
            )
            is CheckExecutionResult.AlreadyRunning -> "Проверка уже выполняется. Дождитесь результата."
        }
    }

    private fun helpText(): String {
        return """
            <b>Команды Whitelist Checker</b>

            /status — показать последний сохранённый результат.
            /check — запустить новую проверку через мобильную сеть.
            /help — показать эту справку.

            Команды работают только пока активный мониторинг запущен и разрешены в настройках.
        """.trimIndent()
    }

    private companion object {
        const val statusNote = "/status показывает сохранённый результат, а не запускает новую проверку."
    }
}
