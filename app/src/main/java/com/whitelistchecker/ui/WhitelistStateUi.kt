package com.whitelistchecker.ui

import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeType

fun WhitelistState.toDisplayLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> "⚪ Неизвестное состояние"
    WhitelistState.WHITELIST_OFF -> "🟢 Белые списки не обнаружены"
    WhitelistState.WHITELIST_ON -> "🟠 Похоже на включённые белые списки"
    WhitelistState.NO_MOBILE_INTERNET -> "🔴 Мобильного интернета нет"
    WhitelistState.MOBILE_DNS_FAILURE -> "🟡 Проблема DNS в мобильной сети"
    WhitelistState.PARTIAL_PROBLEM -> "🟡 Частичная проблема сети"
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "⚪ Мобильная сеть недоступна"
}

fun WhitelistState.toPlainLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> "Неизвестное состояние"
    WhitelistState.WHITELIST_OFF -> "Белые списки не обнаружены"
    WhitelistState.WHITELIST_ON -> "Похоже на включённые белые списки"
    WhitelistState.NO_MOBILE_INTERNET -> "Мобильного интернета нет"
    WhitelistState.MOBILE_DNS_FAILURE -> "Проблема DNS в мобильной сети"
    WhitelistState.PARTIAL_PROBLEM -> "Частичная проблема сети"
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "Мобильная сеть недоступна"
}

fun WhitelistState.toDescription(): String? = when (this) {
    WhitelistState.MOBILE_DNS_FAILURE ->
        "Встроенная DNS-проверка не получила ответа от настроенных DNS-серверов " +
            "через мобильную сеть. Проверь доступность DNS-серверов и ограничения мобильной сети."
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE ->
        "Android не дал cellular Network. Мобильные данные, SIM или сигнал могут быть недоступны."
    else -> null
}

fun WhitelistStateChangeType.toEventTitle(): String = when (this) {
    WhitelistStateChangeType.WHITELIST_TURNED_ON -> "🟠 Белые списки включились"
    WhitelistStateChangeType.WHITELIST_TURNED_OFF -> "🟢 Белые списки выключились"
    WhitelistStateChangeType.MANUAL_CHECK -> "📊 Результат проверки"
    WhitelistStateChangeType.TEST_MESSAGE -> "✉️ Тестовое сообщение"
    WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> "⚪ Подтверждённое изменение состояния"
    WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
}
