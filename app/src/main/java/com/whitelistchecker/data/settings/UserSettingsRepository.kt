package com.whitelistchecker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.AppLanguage
import com.whitelistchecker.domain.model.AppThemeMode
import com.whitelistchecker.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings",
)

class UserSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.userSettingsDataStore)

    fun observeSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
            UserSettings(
                themeMode = preferences[Keys.THEME_MODE].toEnumOrDefault(AppThemeMode.SYSTEM),
                language = preferences[Keys.LANGUAGE].toEnumOrDefault(AppLanguage.SYSTEM),
            )
        }
    }

    suspend fun getSettings(): UserSettings = observeSettings().first()

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language.name
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
        if (this == null) return default
        return enumValues<T>().firstOrNull { it.name == this } ?: default
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
    }
}
