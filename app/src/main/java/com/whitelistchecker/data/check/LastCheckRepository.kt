package com.whitelistchecker.data.check

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.LastCheckLoadResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lastCheckDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "last_check",
)

class LastCheckRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.lastCheckDataStore)

    suspend fun save(result: NetworkCheckResult) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CHECK_JSON] = LastCheckJsonMapper.encode(result)
        }
    }

    suspend fun load(): LastCheckLoadResult {
        return try {
            val json = dataStore.data.map { it[Keys.LAST_CHECK_JSON] }.first()
            if (json.isNullOrBlank()) {
                LastCheckLoadResult.None
            } else {
                LastCheckLoadResult.Success(LastCheckJsonMapper.decode(json))
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to load last check result", exception)
            LastCheckLoadResult.Error(exception)
        }
    }

    private object Keys {
        val LAST_CHECK_JSON = stringPreferencesKey("last_check_json")
    }

    companion object {
        private const val TAG = "LastCheckRepository"
    }
}
