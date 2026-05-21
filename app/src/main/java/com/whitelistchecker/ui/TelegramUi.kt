package com.whitelistchecker.ui

import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult

fun TelegramSettings.configurationStatusLabel(): String {
    return when {
        !enabled -> "выкл"
        isConfigured -> "настроен"
        canTestWorker && enabledRecipients.isEmpty() -> "нет получателей"
        else -> "не настроен"
    }
}

fun TelegramTestResult.toDisplayLabel(): String = when (this) {
    TelegramTestResult.Success -> "Worker работает, бот доступен"
    is TelegramTestResult.Failure -> "ошибка: $reason"
}

fun TelegramSendResult.toDisplayLabel(): String = when (this) {
    TelegramSendResult.Success -> "Тестовое сообщение отправлено"
    is TelegramSendResult.Failure -> "ошибка: $reason"
}

fun TelegramSendResult?.toLastSendStatusLabel(): String = when (this) {
    null -> "ещё не отправлялось"
    TelegramSendResult.Success -> "успешно"
    is TelegramSendResult.Failure -> "ошибка: $reason"
}

fun TelegramTestResult?.toLastTestStatusLabel(): String = when (this) {
    null -> "ещё не проверялось"
    TelegramTestResult.Success -> "Worker работает, бот доступен"
    is TelegramTestResult.Failure -> "ошибка: $reason"
}
