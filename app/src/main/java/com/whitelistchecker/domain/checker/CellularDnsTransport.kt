package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.DnsServerAddress
import com.whitelistchecker.domain.model.EditableDnsServer
import java.io.EOFException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

internal interface DnsTransport {
    fun queryUdp(
        network: Network,
        server: EditableDnsServer,
        payload: ByteArray,
        timeoutMs: Int,
    ): ByteArray

    fun queryTcp(
        network: Network,
        server: EditableDnsServer,
        payload: ByteArray,
        timeoutMs: Int,
    ): ByteArray
}

internal class CellularDnsTransport : DnsTransport {

    override fun queryUdp(
        network: Network,
        server: EditableDnsServer,
        payload: ByteArray,
        timeoutMs: Int,
    ): ByteArray {
        val endpoint = endpoint(server)
        DatagramSocket(null).use { socket ->
            network.bindSocket(socket)
            socket.soTimeout = timeoutMs
            socket.connect(endpoint)
            socket.send(DatagramPacket(payload, payload.size))
            val buffer = ByteArray(MAX_UDP_RESPONSE_BYTES)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            return response.data.copyOfRange(response.offset, response.offset + response.length)
        }
    }

    override fun queryTcp(
        network: Network,
        server: EditableDnsServer,
        payload: ByteArray,
        timeoutMs: Int,
    ): ByteArray {
        val endpoint = endpoint(server)
        network.socketFactory.createSocket().use { socket ->
            socket.soTimeout = timeoutMs
            socket.connect(endpoint, timeoutMs)
            val output = socket.getOutputStream()
            output.write((payload.size ushr 8) and 0xff)
            output.write(payload.size and 0xff)
            output.write(payload)
            output.flush()

            val input = socket.getInputStream()
            val high = input.read()
            val low = input.read()
            if (high < 0 || low < 0) throw EOFException("DNS TCP response has no length prefix")
            val responseLength = (high shl 8) or low
            if (responseLength <= 0 || responseLength > MAX_TCP_RESPONSE_BYTES) {
                throw DnsPacketException("Invalid DNS TCP response length")
            }
            val response = ByteArray(responseLength)
            var offset = 0
            while (offset < responseLength) {
                val read = input.read(response, offset, responseLength - offset)
                if (read < 0) throw EOFException("DNS TCP response ended early")
                offset += read
            }
            return response
        }
    }

    private fun endpoint(server: EditableDnsServer): InetSocketAddress {
        return InetSocketAddress(parseIpv4Literal(server.address), server.port)
    }

    private fun parseIpv4Literal(raw: String): InetAddress {
        val bytes = requireNotNull(DnsServerAddress.parseIpv4Bytes(raw)) {
            "DNS server address must be a literal IPv4 address"
        }
        return InetAddress.getByAddress(bytes)
    }

    companion object {
        private const val MAX_UDP_RESPONSE_BYTES = 4_096
        private const val MAX_TCP_RESPONSE_BYTES = 65_535
    }
}
