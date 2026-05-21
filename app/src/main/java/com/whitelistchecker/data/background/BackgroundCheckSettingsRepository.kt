package com.whitelistchecker.data.background

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backgroundCheckSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "background_check_settings",
)

class BackgroundCheckSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.backgroundCheckSettingsDataStore)

    fun observeSettings(): Flow<BackgroundCheckSettings> {
        return dataStore.data.map { preferences -> preferences.toSettings() }
    }

    suspend fun getSettings(): BackgroundCheckSettings = observeSettings().first()

    suspend fun saveSettings(settings: BackgroundCheckSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = settings.enabled
            preferences[Keys.INTERVAL_MINUTES] = settings.normalizedIntervalMinutes
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        val current = getSettings()
        saveSettings(current.copy(enabled = enabled))
    }

    suspend fun setIntervalMinutes(intervalMinutes: Long) {
        val current = getSettings()
        saveSettings(
            current.copy(
                intervalMinutes = BackgroundCheckSettings(intervalMinutes = intervalMinutes).normalizedIntervalMinutes,
            ),
        )
    }

    private fun Preferences.toSettings(): BackgroundCheckSettings {
        return BackgroundCheckSettings(
            enabled = this[Keys.ENABLED] ?: false,
            intervalMinutes = this[Keys.INTERVAL_MINUTES] ?: 15L,
        ).let { settings ->
            settings.copy(intervalMinutes = settings.normalizedIntervalMinutes)
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("background_check_enabled")
        val INTERVAL_MINUTES = longPreferencesKey("background_check_interval_minutes")
    }
}
