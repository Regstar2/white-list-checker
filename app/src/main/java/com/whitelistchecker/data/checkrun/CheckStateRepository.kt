package com.whitelistchecker.data.checkrun

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.checkrun.CheckOutcome
import com.whitelistchecker.domain.checkrun.CheckStateSnapshot
import com.whitelistchecker.domain.checkrun.isValidWhitelistStatus
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.checkStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "check_state",
)

class CheckStateRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.checkStateDataStore)

    suspend fun getSnapshot(): CheckStateSnapshot {
        return dataStore.data.map { preferences -> preferences.toSnapshot() }.first()
    }

    suspend fun saveAfterAttempt(
        outcome: CheckOutcome,
        attemptedAtMillis: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_ATTEMPT_AVAILABILITY] = outcome.availability.name
            preferences[Keys.LAST_ATTEMPT_AT_MILLIS] = attemptedAtMillis
            when (outcome) {
                is CheckOutcome.Success -> {
                    preferences[Keys.LAST_ATTEMPT_STATE] = outcome.state.name
                    preferences.remove(Keys.LAST_ATTEMPT_ERROR)
                    if (outcome.state.isValidWhitelistStatus()) {
                        preferences[Keys.LAST_VALID_WHITELIST_STATE] = outcome.state.name
                        preferences[Keys.LAST_VALID_WHITELIST_AT_MILLIS] = attemptedAtMillis
                    }
                }
                is CheckOutcome.Unavailable -> {
                    preferences[Keys.LAST_ATTEMPT_STATE] = outcome.state.name
                    if (outcome.reason.isNullOrBlank()) {
                        preferences.remove(Keys.LAST_ATTEMPT_ERROR)
                    } else {
                        preferences[Keys.LAST_ATTEMPT_ERROR] = outcome.reason
                    }
                }
                is CheckOutcome.Failure -> {
                    preferences.remove(Keys.LAST_ATTEMPT_STATE)
                    preferences[Keys.LAST_ATTEMPT_ERROR] = outcome.error
                }
                CheckOutcome.Unknown -> {
                    preferences.remove(Keys.LAST_ATTEMPT_STATE)
                    preferences.remove(Keys.LAST_ATTEMPT_ERROR)
                }
            }
        }
    }

    private fun Preferences.toSnapshot(): CheckStateSnapshot {
        return CheckStateSnapshot(
            lastAttemptOutcome = parseOutcome(
                availability = this[Keys.LAST_ATTEMPT_AVAILABILITY],
                state = this[Keys.LAST_ATTEMPT_STATE],
                error = this[Keys.LAST_ATTEMPT_ERROR],
            ),
            lastAttemptAtMillis = this[Keys.LAST_ATTEMPT_AT_MILLIS],
            lastValidWhitelistState = parseValidWhitelistState(this[Keys.LAST_VALID_WHITELIST_STATE]),
            lastValidWhitelistAtMillis = this[Keys.LAST_VALID_WHITELIST_AT_MILLIS],
        )
    }

    private fun parseOutcome(
        availability: String?,
        state: String?,
        error: String?,
    ): CheckOutcome {
        return when (availability) {
            "AVAILABLE" -> CheckOutcome.Success(parseWhitelistState(state))
            "UNAVAILABLE" -> CheckOutcome.Unavailable(parseWhitelistState(state), error)
            "FAILED" -> CheckOutcome.Failure(error ?: "Unknown error")
            else -> CheckOutcome.Unknown
        }
    }

    private fun parseWhitelistState(value: String?): WhitelistState {
        if (value.isNullOrBlank()) return WhitelistState.UNKNOWN
        return runCatching { WhitelistState.valueOf(value) }.getOrDefault(WhitelistState.UNKNOWN)
    }

    private fun parseValidWhitelistState(value: String?): WhitelistState? {
        val state = parseWhitelistState(value)
        return state.takeIf { it.isValidWhitelistStatus() }
    }

    private object Keys {
        val LAST_ATTEMPT_AVAILABILITY = stringPreferencesKey("last_attempt_availability")
        val LAST_ATTEMPT_STATE = stringPreferencesKey("last_attempt_state")
        val LAST_ATTEMPT_ERROR = stringPreferencesKey("last_attempt_error")
        val LAST_ATTEMPT_AT_MILLIS = longPreferencesKey("last_attempt_at_millis")
        val LAST_VALID_WHITELIST_STATE = stringPreferencesKey("last_valid_whitelist_state")
        val LAST_VALID_WHITELIST_AT_MILLIS = longPreferencesKey("last_valid_whitelist_at_millis")
    }
}
