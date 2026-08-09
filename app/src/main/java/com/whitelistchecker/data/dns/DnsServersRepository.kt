package com.whitelistchecker.data.dns

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.EditableDnsServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dnsServersDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dns_servers",
)

class DnsServersRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.dnsServersDataStore)

    fun observeServers(): Flow<List<EditableDnsServer>> {
        return dataStore.data.map { preferences ->
            val stored = DnsServersJsonCodec.decode(preferences[Keys.SERVERS_JSON])
            DefaultDnsServers.mergeNewBuiltIns(stored)
        }
    }

    suspend fun getServers(): List<EditableDnsServer> = observeServers().first()

    suspend fun getEnabledServers(): List<EditableDnsServer> = getServers().filter { it.enabled }

    suspend fun addServer(server: EditableDnsServer): Boolean {
        val current = getServers()
        val duplicate = current.any { existing ->
            existing.address.equals(server.address, ignoreCase = true) &&
                existing.port == server.port &&
                existing.protocol == server.protocol
        }
        if (duplicate) return false
        saveServers(current + server)
        return true
    }

    suspend fun removeServer(id: String): Boolean {
        val current = getServers()
        val target = current.firstOrNull { it.id == id } ?: return false
        if (target.enabled && current.count { it.enabled } <= 1) return false
        saveServers(current.filterNot { it.id == id })
        return true
    }

    suspend fun setServerEnabled(id: String, enabled: Boolean): Boolean {
        val current = getServers()
        val target = current.firstOrNull { it.id == id } ?: return false
        if (!enabled && target.enabled && current.count { it.enabled } <= 1) return false
        saveServers(
            current.map { server ->
                if (server.id == id) server.copy(enabled = enabled) else server
            },
        )
        return true
    }

    suspend fun resetToDefaults() {
        saveServers(DefaultDnsServers.defaults())
    }

    private suspend fun saveServers(servers: List<EditableDnsServer>) {
        dataStore.edit { preferences ->
            preferences[Keys.SERVERS_JSON] = DnsServersJsonCodec.encode(servers)
        }
    }

    private object Keys {
        val SERVERS_JSON = stringPreferencesKey("dns_servers_json")
    }
}
