package com.whitelistchecker.data.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.LocalNotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.localNotificationSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_notification_settings",
)

class LocalNotificationSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.localNotificationSettingsDataStore)

    fun observeSettings(): Flow<LocalNotificationSettings> {
        return dataStore.data.map { preferences ->
            LocalNotificationSettings(
                enabled = preferences[Keys.LOCAL_NOTIFICATIONS_ENABLED] ?: true,
            )
        }
    }

    suspend fun getSettings(): LocalNotificationSettings = observeSettings().first()

    suspend fun saveSettings(settings: LocalNotificationSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.LOCAL_NOTIFICATIONS_ENABLED] = settings.enabled
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        saveSettings(LocalNotificationSettings(enabled = enabled))
    }

    private object Keys {
        val LOCAL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("local_notifications_enabled")
    }
}
