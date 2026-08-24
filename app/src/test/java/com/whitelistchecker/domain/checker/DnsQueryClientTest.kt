package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.TargetGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.SocketException
import java.net.SocketTimeoutException

class DnsQueryClientTest {

    private val network: Network = mock()
    private val server = EditableDnsServer.create(
        id = "dns",
        name = "DNS",
        address = "1.1.1.1",
        group = TargetGroup.FOREIGN,
    )

    @Test
    fun query_malformedResponse_returnsInvalidResponse() {
        val client = DnsQueryClient(
            transport = FakeTransport(
                udpResponse = { byteArrayOf(1, 2, 3) },
            ),
        )

        val result = client.query(network, server, "example.com", DnsRecordType.A)

        assertEquals(DnsCheckErrorType.INVALID_RESPONSE, result.errorType)
    }

    @Test
    fun query_nxdomain_returnsTypedError() {
        val client = DnsQueryClient(
            transport = FakeTransport(
                udpResponse = { query -> response(query, rcode = 3) },
            ),
        )

        val result = client.query(network, server, "missing.example", DnsRecordType.A)

        assertEquals(DnsCheckErrorType.NXDOMAIN, result.errorType)
    }

    @Test
    fun query_truncatedUdp_usesTcpFallback() {
        val transport = FakeTransport(
            udpResponse = { query -> response(query, truncated = true) },
            tcpResponse = { query -> response(query, address = byteArrayOf(1, 2, 3, 4)) },
        )
        val client = DnsQueryClient(transport)

        val result = client.query(network, server, "example.com", DnsRecordType.A)

        assertTrue(result.successful)
        assertEquals(listOf("1.2.3.4"), result.addresses.map { it.hostAddress })
        assertEquals(1, transport.tcpCalls)
    }

    @Test
    fun query_udpTimeout_usesTcpFallback() {
        val transport = FakeTransport(
            udpResponse = { throw SocketTimeoutException("udp timeout") },
            tcpResponse = { query -> response(query, address = byteArrayOf(4, 3, 2, 1)) },
        )
        val client = DnsQueryClient(transport)

        val result = client.query(network, server, "example.com", DnsRecordType.A)

        assertTrue(result.successful)
        assertEquals(listOf("4.3.2.1"), result.addresses.map { it.hostAddress })
        assertEquals(1, transport.tcpCalls)
    }

    @Test
    fun query_udpNetworkFailure_usesTcpFallback() {
        val transport = FakeTransport(
            udpResponse = { throw SocketException("udp blocked") },
            tcpResponse = { query -> response(query, address = byteArrayOf(9, 9, 9, 9)) },
        )
        val client = DnsQueryClient(transport)

        val result = client.query(network, server, "example.com", DnsRecordType.A)

        assertTrue(result.successful)
        assertEquals(listOf("9.9.9.9"), result.addresses.map { it.hostAddress })
        assertEquals(1, transport.tcpCalls)
    }

    private fun response(
        query: ByteArray,
        rcode: Int = 0,
        truncated: Boolean = false,
        address: ByteArray? = null,
    ): ByteArray {
        val question = query.copyOfRange(12, query.size)
        val answer = if (address == null) {
            byteArrayOf()
        } else {
            byteArrayOf(
                0xC0.toByte(), 0x0C,
                0x00, 0x01,
                0x00, 0x01,
                0x00, 0x00, 0x00, 0x3C,
                0x00, 0x04,
            ) + address
        }
        val header = ByteArray(12)
        header[0] = query[0]
        header[1] = query[1]
        val flags = 0x8180 or rcode or if (truncated) 0x0200 else 0
        writeShort(header, 2, flags)
        writeShort(header, 4, 1)
        writeShort(header, 6, if (address == null) 0 else 1)
        return header + question + answer
    }

    private fun writeShort(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 8) and 0xff).toByte()
        bytes[offset + 1] = (value and 0xff).toByte()
    }

    private class FakeTransport(
        private val udpResponse: (ByteArray) -> ByteArray,
        private val tcpResponse: (ByteArray) -> ByteArray = { error("Unexpected TCP DNS query") },
    ) : DnsTransport {
        var tcpCalls: Int = 0

        override fun queryUdp(
            network: Network,
            server: EditableDnsServer,
            payload: ByteArray,
            timeoutMs: Int,
        ): ByteArray = udpResponse(payload)

        override fun queryTcp(
            network: Network,
            server: EditableDnsServer,
            payload: ByteArray,
            timeoutMs: Int,
        ): ByteArray {
            tcpCalls += 1
            return tcpResponse(payload)
        }
    }
}
