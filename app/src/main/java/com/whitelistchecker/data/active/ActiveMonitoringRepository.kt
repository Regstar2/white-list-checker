package com.whitelistchecker.data.active

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.ActiveMonitoringStatus
import com.whitelistchecker.domain.model.NotificationPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.activeMonitoringDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_monitoring",
)

class ActiveMonitoringRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.activeMonitoringDataStore)

    fun observeSettings(): Flow<ActiveMonitoringSettings> {
        return dataStore.data.map { preferences -> preferences.toSettings() }
    }

    fun observeStatus(): Flow<ActiveMonitoringStatus> {
        return dataStore.data.map { preferences -> preferences.toStatus() }
    }

    suspend fun getSettings(): ActiveMonitoringSettings = observeSettings().first()

    suspend fun getStatus(): ActiveMonitoringStatus = observeStatus().first()

    suspend fun saveSettings(settings: ActiveMonitoringSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.INTERVAL_MINUTES] = settings.intervalMinutes
            preferences[Keys.NOTIFICATION_POLICY] = settings.notificationPolicy.name
            preferences[Keys.NOTIFY_ON_ACCESS_RESTORED] = settings.notifyOnAccessRestored
            preferences[Keys.TELEGRAM_COMMANDS_ENABLED] = settings.telegramCommandsEnabled
        }
    }

    suspend fun saveState(
        state: ActiveMonitoringState,
        stopReason: String? = null,
        error: String? = null,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.STATE] = state.name
            if (stopReason.isNullOrBlank()) {
                preferences.remove(Keys.LAST_STOP_REASON)
            } else {
                preferences[Keys.LAST_STOP_REASON] = stopReason
            }
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.LAST_ERROR)
            } else {
                preferences[Keys.LAST_ERROR] = error
            }
        }
    }

    suspend fun saveLastCheckAt(nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CHECK_AT_MILLIS] = nowMillis
        }
    }

    suspend fun saveBackgroundWasEnabledBeforeStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKGROUND_WAS_ENABLED_BEFORE_START] = enabled
        }
    }

    suspend fun saveTelegramCommandOffset(offset: Long?) {
        dataStore.edit { preferences ->
            if (offset == null) {
                preferences.remove(Keys.TELEGRAM_COMMAND_OFFSET)
            } else {
                preferences[Keys.TELEGRAM_COMMAND_OFFSET] = offset
            }
        }
    }

    suspend fun saveTelegramLastError(error: String?) {
        dataStore.edit { preferences ->
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.TELEGRAM_LAST_ERROR)
            } else {
                preferences[Keys.TELEGRAM_LAST_ERROR] = error
            }
        }
    }

    private fun Preferences.toSettings(): ActiveMonitoringSettings {
        return ActiveMonitoringSettings(
            intervalMinutes = this[Keys.INTERVAL_MINUTES] ?: ActiveMonitoringSettings.DEFAULT_INTERVAL_MINUTES,
            notificationPolicy = parseNotificationPolicy(this[Keys.NOTIFICATION_POLICY]),
            notifyOnAccessRestored = this[Keys.NOTIFY_ON_ACCESS_RESTORED] ?: false,
            telegramCommandsEnabled = this[Keys.TELEGRAM_COMMANDS_ENABLED] ?: false,
        )
    }

    private fun Preferences.toStatus(): ActiveMonitoringStatus {
        return ActiveMonitoringStatus(
            state = parseState(this[Keys.STATE]),
            lastCheckAtMillis = this[Keys.LAST_CHECK_AT_MILLIS],
            lastStopReason = this[Keys.LAST_STOP_REASON],
            lastError = this[Keys.LAST_ERROR],
            telegramCommandOffset = this[Keys.TELEGRAM_COMMAND_OFFSET],
            telegramLastError = this[Keys.TELEGRAM_LAST_ERROR],
            backgroundWasEnabledBeforeStart = this[Keys.BACKGROUND_WAS_ENABLED_BEFORE_START] ?: false,
        )
    }

    private fun parseNotificationPolicy(value: String?): NotificationPolicy {
        if (value.isNullOrBlank()) return NotificationPolicy.STATE_CHANGE_ONLY
        return runCatching { NotificationPolicy.valueOf(value) }
            .getOrDefault(NotificationPolicy.STATE_CHANGE_ONLY)
    }

    private fun parseState(value: String?): ActiveMonitoringState {
        if (value.isNullOrBlank()) return ActiveMonitoringState.STOPPED
        return runCatching { ActiveMonitoringState.valueOf(value) }
            .getOrDefault(ActiveMonitoringState.STOPPED)
    }

    private object Keys {
        val INTERVAL_MINUTES = longPreferencesKey("active_interval_minutes")
        val NOTIFICATION_POLICY = stringPreferencesKey("active_notification_policy")
        val NOTIFY_ON_ACCESS_RESTORED = booleanPreferencesKey("active_notify_on_access_restored")
        val TELEGRAM_COMMANDS_ENABLED = booleanPreferencesKey("active_telegram_commands_enabled")
        val STATE = stringPreferencesKey("active_state")
        val LAST_CHECK_AT_MILLIS = longPreferencesKey("active_last_check_at_millis")
        val LAST_STOP_REASON = stringPreferencesKey("active_last_stop_reason")
        val LAST_ERROR = stringPreferencesKey("active_last_error")
        val TELEGRAM_COMMAND_OFFSET = longPreferencesKey("active_telegram_command_offset")
        val TELEGRAM_LAST_ERROR = stringPreferencesKey("active_telegram_last_error")
        val BACKGROUND_WAS_ENABLED_BEFORE_START = booleanPreferencesKey("active_background_was_enabled_before_start")
    }
}
