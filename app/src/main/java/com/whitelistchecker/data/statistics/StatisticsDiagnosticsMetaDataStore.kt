package com.whitelistchecker.data.statistics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.statistics.StatisticsDiagnosticsMeta
import com.whitelistchecker.domain.statistics.StatisticsDiagnosticsMetaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.statisticsDiagnosticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "statistics_diagnostics_meta",
)

class StatisticsDiagnosticsMetaDataStore(
    private val dataStore: DataStore<Preferences>,
) : StatisticsDiagnosticsMetaRepository {

    constructor(context: Context) : this(context.applicationContext.statisticsDiagnosticsDataStore)

    override suspend fun getMeta(): StatisticsDiagnosticsMeta {
        val preferences = dataStore.data.first()
        return StatisticsDiagnosticsMeta(
            lastRebuildAtMillis = preferences[Keys.LAST_REBUILD_AT],
            lastCleanupAtMillis = preferences[Keys.LAST_CLEANUP_AT],
        )
    }

    override suspend fun recordRebuildCompleted(atMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_REBUILD_AT] = atMillis
        }
    }

    override suspend fun recordCleanupCompleted(atMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CLEANUP_AT] = atMillis
        }
    }

    private object Keys {
        val LAST_REBUILD_AT = longPreferencesKey("last_rebuild_at")
        val LAST_CLEANUP_AT = longPreferencesKey("last_cleanup_at")
    }
}
