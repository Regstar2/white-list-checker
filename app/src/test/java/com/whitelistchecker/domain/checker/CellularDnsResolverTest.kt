package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.TargetGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.InetAddress

class CellularDnsResolverTest {

    private val network: Network = mock()

    @Test
    fun lookup_firstResolverARecordSuccess_doesNotUseAaaaOrSecondResolver() {
        val first = server("first", "1.1.1.1")
        val second = server("second", "8.8.8.8")
        val executor = FakeDnsQueryExecutor { server, type ->
            if (server.id == first.id && type == DnsRecordType.A) {
                success(ipv4(1, 2, 3, 4))
            } else {
                success()
            }
        }
        val resolver = CellularDnsResolver(network, listOf(first, second), executor)

        val result = resolver.lookup("example.com")

        assertEquals(listOf("1.2.3.4"), result.map { it.hostAddress })
        assertEquals(listOf(first.id to DnsRecordType.A), executor.calls)
    }

    @Test
    fun lookup_firstResolverTimeout_usesSecondResolver() {
        val first = server("first", "1.1.1.1")
        val second = server("second", "8.8.8.8")
        val executor = FakeDnsQueryExecutor { server, type ->
            when {
                server.id == first.id -> failure(DnsCheckErrorType.TIMEOUT)
                type == DnsRecordType.A -> success(ipv4(5, 6, 7, 8))
                else -> success()
            }
        }
        val resolver = CellularDnsResolver(network, listOf(first, second), executor)

        val result = resolver.lookup("example.com")

        assertEquals(listOf("5.6.7.8"), result.map { it.hostAddress })
        assertTrue(executor.calls.any { it.first == second.id })
    }

    @Test
    fun lookup_aEmpty_usesAaaaFallback() {
        val server = server("first", "1.1.1.1")
        val ipv6 = InetAddress.getByAddress(ByteArray(16).also { it[15] = 1 })
        val executor = FakeDnsQueryExecutor { _, type ->
            when (type) {
                DnsRecordType.A -> success()
                DnsRecordType.AAAA -> success(ipv6)
            }
        }
        val resolver = CellularDnsResolver(network, listOf(server), executor)

        val result = resolver.lookup("ipv6.example")

        assertEquals(listOf(ipv6.hostAddress), result.map { it.hostAddress })
        assertEquals(
            listOf(server.id to DnsRecordType.A, server.id to DnsRecordType.AAAA),
            executor.calls,
        )
    }

    @Test
    fun lookup_sameHostTwice_usesRunLocalCache() {
        val server = server("first", "1.1.1.1")
        val executor = FakeDnsQueryExecutor { _, type ->
            if (type == DnsRecordType.A) success(ipv4(9, 9, 9, 9)) else success()
        }
        val resolver = CellularDnsResolver(network, listOf(server), executor)

        resolver.lookup("Example.COM")
        resolver.lookup("example.com")

        assertEquals(1, executor.calls.size)
        assertEquals(1, resolver.cachedHostCount())
    }

    @Test
    fun lookup_disabledServer_isNeverUsed() {
        val disabled = server("disabled", "1.1.1.1").copy(enabled = false)
        val enabled = server("enabled", "8.8.8.8")
        val executor = FakeDnsQueryExecutor { server, type ->
            if (server.id == enabled.id && type == DnsRecordType.A) {
                success(ipv4(8, 8, 8, 8))
            } else {
                success()
            }
        }
        val resolver = CellularDnsResolver(network, listOf(disabled, enabled), executor)

        resolver.lookup("example.com")

        assertFalse(executor.calls.any { it.first == disabled.id })
    }

    private fun server(id: String, address: String) = EditableDnsServer.create(
        id = id,
        name = id,
        address = address,
        group = TargetGroup.FOREIGN,
    )

    private fun success(vararg addresses: InetAddress) = DnsQueryResult(addresses = addresses.toList())

    private fun failure(errorType: DnsCheckErrorType) = DnsQueryResult(
        addresses = emptyList(),
        errorType = errorType,
        error = errorType.name,
    )

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress = InetAddress.getByAddress(
        byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()),
    )

    private class FakeDnsQueryExecutor(
        private val result: (EditableDnsServer, DnsRecordType) -> DnsQueryResult,
    ) : DnsQueryExecutor {
        val calls = mutableListOf<Pair<String, DnsRecordType>>()

        override fun query(
            network: Network,
            server: EditableDnsServer,
            hostname: String,
            type: DnsRecordType,
            timeoutMs: Int,
        ): DnsQueryResult {
            calls += server.id to type
            return result(server, type)
        }
    }
}
