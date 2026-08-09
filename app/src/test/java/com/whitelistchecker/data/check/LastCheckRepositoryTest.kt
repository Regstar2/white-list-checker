package com.whitelistchecker.data.check

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.DnsCheckResult
import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.LastCheckLoadResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastCheckRepositoryTest {

    @Test
    fun saveAndLoad_preservesStructuredDnsDiagnostics() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = LastCheckRepository(dataStore)
        val dnsServer = EditableDnsServer.create(
            id = "dns-1",
            name = "DNS",
            address = "1.1.1.1",
            group = TargetGroup.FOREIGN,
        )
        val siteTarget = CheckTarget(
            name = "Site",
            url = "https://site.example",
            group = TargetGroup.FOREIGN,
        )
        val original = NetworkCheckResult(
            siteResults = listOf(
                SiteCheckResult(
                    target = siteTarget,
                    available = false,
                    httpCode = null,
                    error = "timeout",
                    errorType = SiteCheckErrorType.TIMEOUT,
                    durationMs = 120,
                ),
            ),
            foreignSummary = TargetGroupSummary(TargetGroup.FOREIGN, 0, 1),
            localSummary = TargetGroupSummary(TargetGroup.LOCAL, 1, 1),
            state = WhitelistState.WHITELIST_ON,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 1234L,
            dnsResults = listOf(
                DnsCheckResult(
                    server = dnsServer,
                    available = false,
                    responseTimeMs = 2500,
                    errorType = DnsCheckErrorType.TIMEOUT,
                    error = "timeout",
                    resolvedAddressesCount = 0,
                ),
            ),
            foreignDnsSummary = TargetGroupSummary(TargetGroup.FOREIGN, 0, 1),
            localDnsSummary = TargetGroupSummary(TargetGroup.LOCAL, 1, 1),
            dnsSignal = DnsWhitelistSignal.WHITELIST_LIKE,
            siteState = WhitelistState.WHITELIST_ON,
            privateDnsActive = true,
            privateDnsServerName = "dns.example",
            customDnsUsed = true,
        )

        repository.save(original)
        val loaded = (repository.load() as LastCheckLoadResult.Success).result

        assertEquals(original, loaded)
    }

    @Test
    fun load_legacyJsonWithoutDnsFields_usesBackwardCompatibleDefaults() = runTest {
        val legacy = """
            {
              "siteResults": [],
              "foreignSummary": {"group":"FOREIGN","availableCount":2,"totalCount":2},
              "localSummary": {"group":"LOCAL","availableCount":2,"totalCount":2},
              "state":"WHITELIST_OFF",
              "activeNetworkLabel":"Wi-Fi",
              "checkedNetworkLabel":"Mobile",
              "checkedAtMillis":1234
            }
        """.trimIndent()
        val key = stringPreferencesKey("last_check_json")
        val dataStore = FakePreferencesDataStore(mutablePreferencesOf(key to legacy))
        val repository = LastCheckRepository(dataStore)

        val loaded = (repository.load() as LastCheckLoadResult.Success).result

        assertTrue(loaded.dnsResults.isEmpty())
        assertEquals(null, loaded.foreignDnsSummary)
        assertEquals(null, loaded.localDnsSummary)
        assertEquals(DnsWhitelistSignal.UNKNOWN, loaded.dnsSignal)
        assertEquals(WhitelistState.WHITELIST_OFF, loaded.siteState)
        assertFalse(loaded.privateDnsActive)
        assertEquals(null, loaded.privateDnsServerName)
        assertFalse(loaded.customDnsUsed)
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
