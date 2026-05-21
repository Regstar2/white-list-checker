package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramBroadcastResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class TelegramEventNotifierUseCase(
    private val telegramBroadcastUseCase: TelegramBroadcastUseCase,
    private val reportFormatter: TelegramReportFormatter,
) {

    suspend fun sendOnManualCheck(checkResult: NetworkCheckResult): TelegramSendResult? {
        val text = reportFormatter.formatManualCheck(checkResult)
        return sendWithBroadcast(
            text = text,
            baseReportId = "${WhitelistStateChangeType.MANUAL_CHECK.name}_${checkResult.checkedAtMillis}",
            eventType = WhitelistStateChangeType.MANUAL_CHECK.name,
        )
    }

    suspend fun sendTestMessage(text: String): TelegramSendResult? {
        return sendWithBroadcast(
            text = text,
            baseReportId = "${WhitelistStateChangeType.TEST_MESSAGE.name}_${System.currentTimeMillis()}",
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

        val text = reportFormatter.format(event, checkResult)
        return sendWithBroadcast(
            text = text,
            baseReportId = "${event.type.name}_${event.changedAtMillis}",
            eventType = event.type.name,
            oldState = event.oldState.name,
            newState = event.newState.name,
        )
    }

    private suspend fun sendWithBroadcast(
        text: String,
        baseReportId: String,
        eventType: String,
        oldState: String = WhitelistState.UNKNOWN.name,
        newState: String = WhitelistState.UNKNOWN.name,
    ): TelegramSendResult? {
        val broadcast = telegramBroadcastUseCase.sendToEnabledRecipients(
            text = text,
            baseReportId = baseReportId,
            eventType = eventType,
            oldState = oldState,
            newState = newState,
        )
        return broadcast.toTelegramSendResult()
    }
}

fun TelegramBroadcastResult.toTelegramSendResult(): TelegramSendResult? {
    return when {
        sentCount == 0 && failedCount == 0 -> null
        failedCount == 0 -> TelegramSendResult.Success
        sentCount == 0 -> TelegramSendResult.Failure(
            failures.firstOrNull()?.reason ?: summary,
        )
        else -> TelegramSendResult.Failure(summary)
    }
}
