package com.whitelistchecker.ui.navigation

enum class AppScreen {
    HOME,
    NOTIFICATIONS,
    LOCAL_NOTIFICATIONS,
    TELEGRAM_NOTIFICATIONS,
    TELEGRAM_WORKER_SETUP,
    TELEGRAM_RECIPIENT_DISCOVERY,
    TELEGRAM_QUEUE,
    CHECK_SETTINGS,
    AUTO_CHECK,
    DIAGNOSTICS,
    STATISTICS,
    SETTINGS,
    ABOUT,
}

fun AppScreen.parentScreen(): AppScreen? = when (this) {
    AppScreen.HOME -> null
    AppScreen.NOTIFICATIONS,
    AppScreen.STATISTICS,
    AppScreen.CHECK_SETTINGS,
    AppScreen.AUTO_CHECK,
    AppScreen.DIAGNOSTICS,
    AppScreen.SETTINGS,
    AppScreen.ABOUT,
    -> AppScreen.HOME
    AppScreen.LOCAL_NOTIFICATIONS,
    AppScreen.TELEGRAM_NOTIFICATIONS,
    -> AppScreen.NOTIFICATIONS
    AppScreen.TELEGRAM_WORKER_SETUP,
    AppScreen.TELEGRAM_RECIPIENT_DISCOVERY,
    AppScreen.TELEGRAM_QUEUE,
    -> AppScreen.TELEGRAM_NOTIFICATIONS
}
