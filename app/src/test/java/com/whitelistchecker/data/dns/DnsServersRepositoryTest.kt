package com.whitelistchecker.data.dns

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.whitelistchecker.domain.model.TargetGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsServersRepositoryTest {

    @Test
    fun getServers_firstUse_persistsAndReturnsDefaults() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = DnsServersRepository(dataStore)

        val servers = repository.getServers()
        val secondRead = DnsServersRepository(dataStore).getServers()

        assertEquals(DefaultDnsServers.defaults(), servers)
        assertEquals(servers, secondRead)
    }

    @Test
    fun removeBuiltIn_restartDoesNotRestoreIt_resetRestoresIt() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = DnsServersRepository(dataStore)
        val removedId = "builtin_dns_foreign_cloudflare"

        assertTrue(repository.removeServer(removedId))
        val afterRestart = DnsServersRepository(dataStore).getServers()
        assertFalse(afterRestart.any { it.id == removedId })

        DnsServersRepository(dataStore).resetToDefaults()
        val afterReset = DnsServersRepository(dataStore).getServers()
        assertTrue(afterReset.any { it.id == removedId })
    }

    @Test
    fun setServerEnabled_doesNotAllowDisablingLastEnabledResolver() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = DnsServersRepository(dataStore)
        val defaults = repository.getServers()

        defaults.dropLast(1).forEach { server ->
            assertTrue(repository.setServerEnabled(server.id, false))
        }
        val last = defaults.last()

        assertFalse(repository.setServerEnabled(last.id, false))
        assertEquals(1, repository.getEnabledServers().size)
    }

    @Test
    fun addServer_rejectsDuplicateEndpointProtocolAndPort() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = DnsServersRepository(dataStore)
        repository.getServers()
        val duplicate = DefaultDnsServers.defaults()
            .first { it.group == TargetGroup.FOREIGN }
            .copy(id = "custom-duplicate", name = "Duplicate", builtIn = false)

        assertFalse(repository.addServer(duplicate))
    }

    private class FakePreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
