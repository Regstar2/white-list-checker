package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.Network
import com.whitelistchecker.data.dns.DnsServersRepository
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.domain.classifier.DnsWhitelistSignalClassifier
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.DnsCheckResult
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WhitelistCheckUseCaseTest {

    private val connectivityManager: ConnectivityManager = mock()
    private val targetsRepository: CheckTargetsRepository = mock()
    private val dnsServersRepository: DnsServersRepository = mock()
    private val cellularNetworkProvider: CellularNetworkProvider = mock()
    private val dnsProbe: CellularDnsProbe = mock()
    private val dnsResolverFactory: CellularDnsResolverFactory = mock()
    private val mobileSiteChecker: MobileSiteChecker = mock()
    private val networkDiagnosticsUseCase: NetworkDiagnosticsUseCase = mock()
    private val privateDnsDiagnosticsProvider: PrivateDnsDiagnosticsProvider = mock()

    @Test
    fun execute_usesOneCellularNetworkForDnsAndSiteChecksAndReportsPrivateDns() = runTest {
        val network: Network = mock()
        val foreignTarget = target("foreign", TargetGroup.FOREIGN)
        val localTarget = target("local", TargetGroup.LOCAL)
        val foreignDns = dns("foreign-dns", "1.1.1.1", TargetGroup.FOREIGN)
        val localDns = dns("local-dns", "77.88.8.8", TargetGroup.LOCAL)
        val dnsResults = listOf(
            dnsResult(foreignDns, available = true, latency = 10),
            dnsResult(localDns, available = true, latency = 20),
        )
        val resolver: CellularDnsResolver = mock()
        val session: MobileSiteChecker.Session = mock()

        whenever(targetsRepository.getEnabledTargets()).thenReturn(listOf(foreignTarget, localTarget))
        whenever(dnsServersRepository.getEnabledServers()).thenReturn(listOf(foreignDns, localDns))
        whenever(cellularNetworkProvider.requestCellularNetwork()).thenReturn(CellularNetworkRequestResult(network))
        whenever(privateDnsDiagnosticsProvider.read(network)).thenReturn(
            PrivateDnsDiagnostics(active = true, serverName = "dns.example"),
        )
        whenever(dnsProbe.probe(network, listOf(foreignDns, localDns))).thenReturn(dnsResults)
        whenever(dnsResolverFactory.create(network, listOf(foreignDns, localDns))).thenReturn(resolver)
        whenever(mobileSiteChecker.createSession(network, resolver)).thenReturn(session)
        whenever(session.checkTarget(foreignTarget)).thenReturn(siteResult(foreignTarget, available = true))
        whenever(session.checkTarget(localTarget)).thenReturn(siteResult(localTarget, available = true))

        val result = useCase().execute()

        assertEquals(WhitelistState.WHITELIST_OFF, result.state)
        assertTrue(result.customDnsUsed)
        assertTrue(result.privateDnsActive)
        assertEquals("dns.example", result.privateDnsServerName)
        verify(dnsProbe).probe(network, listOf(foreignDns, localDns))
        verify(dnsResolverFactory).create(network, listOf(foreignDns, localDns))
        verify(mobileSiteChecker).createSession(network, resolver)
        verify(cellularNetworkProvider).release()
    }

    @Test
    fun execute_noDnsAccess_butSiteChecksSucceed_keepsClearSiteResult() = runTest {
        val network: Network = mock()
        val foreignTarget = target("foreign", TargetGroup.FOREIGN)
        val localTarget = target("local", TargetGroup.LOCAL)
        val foreignDns = dns("foreign-dns", "1.1.1.1", TargetGroup.FOREIGN)
        val localDns = dns("local-dns", "77.88.8.8", TargetGroup.LOCAL)
        val dnsResults = listOf(
            dnsResult(foreignDns, available = false, latency = 100),
            dnsResult(localDns, available = false, latency = 100),
        )
        val resolver: CellularDnsResolver = mock()
        val session: MobileSiteChecker.Session = mock()

        whenever(targetsRepository.getEnabledTargets()).thenReturn(listOf(foreignTarget, localTarget))
        whenever(dnsServersRepository.getEnabledServers()).thenReturn(listOf(foreignDns, localDns))
        whenever(cellularNetworkProvider.requestCellularNetwork()).thenReturn(CellularNetworkRequestResult(network))
        whenever(privateDnsDiagnosticsProvider.read(network)).thenReturn(PrivateDnsDiagnostics(false, null))
        whenever(dnsProbe.probe(network, listOf(foreignDns, localDns))).thenReturn(dnsResults)
        whenever(dnsResolverFactory.create(network, emptyList())).thenReturn(resolver)
        whenever(mobileSiteChecker.createSession(network, resolver)).thenReturn(session)
        whenever(session.checkTarget(foreignTarget)).thenReturn(siteResult(foreignTarget, available = true))
        whenever(session.checkTarget(localTarget)).thenReturn(siteResult(localTarget, available = true))

        val result = useCase().execute()

        assertFalse(result.customDnsUsed)
        assertEquals(WhitelistState.WHITELIST_OFF, result.state)
        assertNull(result.diagnosticsMessage)
        verify(dnsResolverFactory).create(network, emptyList())
        verify(mobileSiteChecker).createSession(network, resolver)
        verify(networkDiagnosticsUseCase, never()).diagnoseDnsConnectivity(network)
        verify(cellularNetworkProvider).release()
    }

    @Test
    fun execute_noDnsAccess_andAllSiteChecksFailDns_returnsConfirmedDnsFailure() = runTest {
        val network: Network = mock()
        val foreignTarget = target("foreign", TargetGroup.FOREIGN)
        val localTarget = target("local", TargetGroup.LOCAL)
        val foreignDns = dns("foreign-dns", "1.1.1.1", TargetGroup.FOREIGN)
        val localDns = dns("local-dns", "77.88.8.8", TargetGroup.LOCAL)
        val dnsResults = listOf(
            dnsResult(foreignDns, available = false, latency = 100),
            dnsResult(localDns, available = false, latency = 100),
        )
        val resolver: CellularDnsResolver = mock()
        val session: MobileSiteChecker.Session = mock()

        whenever(targetsRepository.getEnabledTargets()).thenReturn(listOf(foreignTarget, localTarget))
        whenever(dnsServersRepository.getEnabledServers()).thenReturn(listOf(foreignDns, localDns))
        whenever(cellularNetworkProvider.requestCellularNetwork()).thenReturn(CellularNetworkRequestResult(network))
        whenever(privateDnsDiagnosticsProvider.read(network)).thenReturn(PrivateDnsDiagnostics(false, null))
        whenever(dnsProbe.probe(network, listOf(foreignDns, localDns))).thenReturn(dnsResults)
        whenever(dnsResolverFactory.create(network, emptyList())).thenReturn(resolver)
        whenever(mobileSiteChecker.createSession(network, resolver)).thenReturn(session)
        whenever(session.checkTarget(foreignTarget)).thenReturn(
            siteResult(foreignTarget, available = false, errorType = SiteCheckErrorType.DNS),
        )
        whenever(session.checkTarget(localTarget)).thenReturn(
            siteResult(localTarget, available = false, errorType = SiteCheckErrorType.DNS),
        )

        val result = useCase().execute()

        assertFalse(result.customDnsUsed)
        assertEquals(WhitelistState.MOBILE_DNS_FAILURE, result.state)
        assertNull(result.diagnosticsMessage)
        verify(dnsResolverFactory).create(network, emptyList())
        verify(mobileSiteChecker).createSession(network, resolver)
        verify(networkDiagnosticsUseCase, never()).diagnoseDnsConnectivity(network)
        verify(cellularNetworkProvider).release()
    }

    @Test
    fun execute_cellularUnavailable_returnsTypedStateAndReleasesCallback() = runTest {
        whenever(targetsRepository.getEnabledTargets()).thenReturn(emptyList())
        whenever(dnsServersRepository.getEnabledServers()).thenReturn(emptyList())
        whenever(cellularNetworkProvider.requestCellularNetwork()).thenReturn(CellularNetworkRequestResult())

        val result = useCase().execute()

        assertEquals(WhitelistState.CELLULAR_NETWORK_UNAVAILABLE, result.state)
        verify(cellularNetworkProvider).release()
    }

    private fun useCase() = WhitelistCheckUseCase(
        connectivityManager = connectivityManager,
        targetsRepository = targetsRepository,
        dnsServersRepository = dnsServersRepository,
        cellularNetworkProvider = cellularNetworkProvider,
        dnsProbe = dnsProbe,
        dnsResolverFactory = dnsResolverFactory,
        mobileSiteChecker = mobileSiteChecker,
        dnsSignalClassifier = DnsWhitelistSignalClassifier(),
        classifier = WhitelistStateClassifier(),
        networkDiagnosticsUseCase = networkDiagnosticsUseCase,
        privateDnsDiagnosticsProvider = privateDnsDiagnosticsProvider,
    )

    private fun target(id: String, group: TargetGroup) = CheckTarget(
        name = id,
        url = "https://$id.example",
        group = group,
    )

    private fun dns(id: String, address: String, group: TargetGroup) = EditableDnsServer.create(
        id = id,
        name = id,
        address = address,
        group = group,
    )

    private fun dnsResult(
        server: EditableDnsServer,
        available: Boolean,
        latency: Long,
    ) = DnsCheckResult(
        server = server,
        available = available,
        responseTimeMs = latency,
        errorType = if (available) DnsCheckErrorType.NONE else DnsCheckErrorType.TIMEOUT,
        error = if (available) null else "timeout",
        resolvedAddressesCount = if (available) 1 else 0,
    )

    private fun siteResult(
        target: CheckTarget,
        available: Boolean,
        errorType: SiteCheckErrorType = if (available) SiteCheckErrorType.NONE else SiteCheckErrorType.CONNECTION,
    ) = SiteCheckResult(
        target = target,
        available = available,
        httpCode = if (available) 200 else null,
        error = if (available) null else "failed",
        errorType = errorType,
        durationMs = 50,
    )
}
