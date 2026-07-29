package com.whitelistchecker.domain.model.history

enum class CheckTriggerType {
    MANUAL,
    BACKGROUND,
    MANUAL_UI,
    WORK_MANAGER,
    FOREGROUND_INTERVAL,
    FOREGROUND_NOTIFICATION_ACTION,
    TELEGRAM_COMMAND,
    REMOTE_TELEGRAM,
}
