package com.whitelistchecker.data.monitor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.monitorPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "whitelist_monitor",
)

class MonitorStateRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.monitorPreferencesDataStore)

    suspend fun getState(): WhitelistMonitorState {
        return dataStore.data.map { preferences ->
            WhitelistMonitorState(
                lastConfirmedState = parseWhitelistState(
                    preferences[Keys.LAST_CONFIRMED_STATE],
                ),
                pendingState = parseWhitelistState(preferences[Keys.PENDING_STATE]),
                pendingStateCount = preferences[Keys.PENDING_STATE_COUNT] ?: 0,
                lastConfirmedAtMillis = preferences[Keys.LAST_CONFIRMED_AT_MILLIS],
                lastStateChangeAtMillis = preferences[Keys.LAST_STATE_CHANGE_AT_MILLIS],
            )
        }.first()
    }

    suspend fun saveState(state: WhitelistMonitorState) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CONFIRMED_STATE] = state.lastConfirmedState.name
            preferences[Keys.PENDING_STATE] = state.pendingState.name
            preferences[Keys.PENDING_STATE_COUNT] = state.pendingStateCount
            if (state.lastConfirmedAtMillis != null) {
                preferences[Keys.LAST_CONFIRMED_AT_MILLIS] = state.lastConfirmedAtMillis
            } else {
                preferences.remove(Keys.LAST_CONFIRMED_AT_MILLIS)
            }
            if (state.lastStateChangeAtMillis != null) {
                preferences[Keys.LAST_STATE_CHANGE_AT_MILLIS] = state.lastStateChangeAtMillis
            } else {
                preferences.remove(Keys.LAST_STATE_CHANGE_AT_MILLIS)
            }
        }
    }

    private fun parseWhitelistState(value: String?): WhitelistState {
        if (value.isNullOrBlank()) {
            return WhitelistState.UNKNOWN
        }
        return runCatching {
            WhitelistState.valueOf(value)
        }.getOrDefault(WhitelistState.UNKNOWN)
    }

    private object Keys {
        val LAST_CONFIRMED_STATE = stringPreferencesKey("last_confirmed_state")
        val PENDING_STATE = stringPreferencesKey("pending_state")
        val PENDING_STATE_COUNT = intPreferencesKey("pending_state_count")
        val LAST_CONFIRMED_AT_MILLIS = longPreferencesKey("last_confirmed_at_millis")
        val LAST_STATE_CHANGE_AT_MILLIS = longPreferencesKey("last_state_change_at_millis")
    }
}
