package com.whitelistchecker.ui.navigation

enum class AppScreen(val title: String) {
    HOME("Whitelist Checker"),
    NOTIFICATIONS("Уведомления"),
    LOCAL_NOTIFICATIONS("Локальные уведомления"),
    TELEGRAM_NOTIFICATIONS("Telegram"),
    TELEGRAM_WORKER_SETUP("Настройка Worker"),
    TELEGRAM_RECIPIENT_DISCOVERY("Добавить получателя"),
    TELEGRAM_QUEUE("Очередь сообщений"),
    CHECK_SETTINGS("Настройки проверки"),
    AUTO_CHECK("Автопроверка"),
    DIAGNOSTICS("Диагностика"),
    STATISTICS("Статистика"),
    SETTINGS("Настройки"),
    ABOUT("О приложении"),
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
