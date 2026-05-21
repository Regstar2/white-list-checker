package com.whitelistchecker.ui

import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeType

fun WhitelistState.toDisplayLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> "⚪ Неизвестное состояние"
    WhitelistState.WHITELIST_OFF -> "🟢 Белые списки не обнаружены"
    WhitelistState.WHITELIST_ON -> "🟠 Похоже на включённые белые списки"
    WhitelistState.NO_MOBILE_INTERNET -> "🔴 Мобильного интернета нет"
    WhitelistState.PARTIAL_PROBLEM -> "🟡 Частичная проблема сети"
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "⚪ Мобильная сеть недоступна"
}

fun WhitelistState.toPlainLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> "Неизвестное состояние"
    WhitelistState.WHITELIST_OFF -> "Белые списки не обнаружены"
    WhitelistState.WHITELIST_ON -> "Похоже на включённые белые списки"
    WhitelistState.NO_MOBILE_INTERNET -> "Мобильного интернета нет"
    WhitelistState.PARTIAL_PROBLEM -> "Частичная проблема сети"
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "Мобильная сеть недоступна"
}

fun WhitelistStateChangeType.toEventTitle(): String = when (this) {
    WhitelistStateChangeType.WHITELIST_TURNED_ON -> "🟠 Белые списки включились"
    WhitelistStateChangeType.WHITELIST_TURNED_OFF -> "🟢 Белые списки выключились"
    WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> "⚪ Подтверждённое изменение состояния"
    WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
}
