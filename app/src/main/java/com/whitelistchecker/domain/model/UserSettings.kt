package com.whitelistchecker.domain.model

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
)
