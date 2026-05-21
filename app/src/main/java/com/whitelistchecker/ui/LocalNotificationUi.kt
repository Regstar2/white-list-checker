package com.whitelistchecker.ui

import com.whitelistchecker.domain.model.LocalNotificationResult

fun LocalNotificationResult.toResultLabel(): String = when (this) {
    LocalNotificationResult.Success -> "Локальное уведомление отправлено"
    LocalNotificationResult.Disabled -> "Локальные уведомления выключены"
    LocalNotificationResult.PermissionNotGranted -> "Нет разрешения на уведомления"
    is LocalNotificationResult.Failure -> "Ошибка локального уведомления: ${reason}"
}

fun permissionStatusLabel(
    permissionRequired: Boolean,
    notificationsAllowed: Boolean,
): String = when {
    !permissionRequired -> "не требуется"
    notificationsAllowed -> "выдано"
    else -> "не выдано"
}
