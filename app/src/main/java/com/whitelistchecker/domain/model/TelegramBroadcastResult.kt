package com.whitelistchecker.domain.model

data class TelegramBroadcastResult(
    val sentCount: Int,
    val failedCount: Int,
    val failures: List<TelegramRecipientSendFailure>,
) {
    val isFullSuccess: Boolean
        get() = failedCount == 0 && sentCount > 0

    val summary: String
        get() = when {
            sentCount == 0 && failedCount == 0 -> "Нет включённых Telegram-получателей"
            failedCount == 0 -> "Отправлено: $sentCount"
            sentCount == 0 -> "Ошибок: $failedCount"
            else -> "Отправлено: $sentCount, ошибок: $failedCount"
        }
}

data class TelegramRecipientSendFailure(
    val recipient: TelegramRecipient,
    val reason: String,
)
