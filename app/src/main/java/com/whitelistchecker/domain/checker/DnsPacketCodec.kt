package com.whitelistchecker.domain.checker

import java.net.InetAddress
import kotlin.random.Random

internal enum class DnsRecordType(val code: Int) {
    A(1),
    AAAA(28),
}

internal data class DnsQueryPacket(
    val transactionId: Int,
    val bytes: ByteArray,
)

internal data class DnsParsedResponse(
    val addresses: List<InetAddress>,
    val truncated: Boolean,
    val responseCode: Int,
)

internal class DnsPacketException(message: String) : Exception(message)

internal object DnsPacketCodec {

    fun buildQuery(hostname: String, type: DnsRecordType): DnsQueryPacket {
        val normalized = hostname.trim().trimEnd('.')
        require(normalized.isNotBlank()) { "DNS hostname is empty" }
        val transactionId = Random.nextInt(0, 65_536)
        val labels = normalized.split('.')
        val nameSize = labels.sumOf { it.toByteArray(Charsets.UTF_8).size + 1 } + 1
        val bytes = ByteArray(DNS_HEADER_SIZE + nameSize + DNS_QUESTION_TAIL_SIZE)

        writeUnsignedShort(bytes, 0, transactionId)
        writeUnsignedShort(bytes, 2, FLAG_RECURSION_DESIRED)
        writeUnsignedShort(bytes, 4, 1)

        var offset = DNS_HEADER_SIZE
        labels.forEach { label ->
            val encoded = label.toByteArray(Charsets.UTF_8)
            require(encoded.isNotEmpty() && encoded.size <= MAX_LABEL_LENGTH) { "Invalid DNS label" }
            bytes[offset++] = encoded.size.toByte()
            encoded.copyInto(bytes, offset)
            offset += encoded.size
        }
        bytes[offset++] = 0
        writeUnsignedShort(bytes, offset, type.code)
        offset += 2
        writeUnsignedShort(bytes, offset, DNS_CLASS_IN)
        return DnsQueryPacket(transactionId = transactionId, bytes = bytes)
    }

    fun parseResponse(
        query: DnsQueryPacket,
        response: ByteArray,
    ): DnsParsedResponse {
        if (response.size < DNS_HEADER_SIZE) throw DnsPacketException("DNS response is shorter than header")
        if (readUnsignedShort(response, 0) != query.transactionId) {
            throw DnsPacketException("DNS transaction ID mismatch")
        }
        val flags = readUnsignedShort(response, 2)
        if (flags and FLAG_RESPONSE == 0) throw DnsPacketException("DNS packet is not a response")
        val responseCode = flags and RESPONSE_CODE_MASK
        val truncated = flags and FLAG_TRUNCATED != 0
        val questionCount = readUnsignedShort(response, 4)
        val answerCount = readUnsignedShort(response, 6)

        var offset = DNS_HEADER_SIZE
        repeat(questionCount) {
            offset = skipName(response, offset)
            ensureAvailable(response, offset, DNS_QUESTION_TAIL_SIZE)
            offset += DNS_QUESTION_TAIL_SIZE
        }

        val addresses = mutableListOf<InetAddress>()
        repeat(answerCount) {
            offset = skipName(response, offset)
            ensureAvailable(response, offset, DNS_RECORD_HEADER_SIZE)
            val type = readUnsignedShort(response, offset)
            val dnsClass = readUnsignedShort(response, offset + 2)
            val dataLength = readUnsignedShort(response, offset + 8)
            offset += DNS_RECORD_HEADER_SIZE
            ensureAvailable(response, offset, dataLength)
            if (dnsClass == DNS_CLASS_IN) {
                when {
                    type == DnsRecordType.A.code && dataLength == IPV4_BYTES -> {
                        addresses += InetAddress.getByAddress(response.copyOfRange(offset, offset + dataLength))
                    }
                    type == DnsRecordType.AAAA.code && dataLength == IPV6_BYTES -> {
                        addresses += InetAddress.getByAddress(response.copyOfRange(offset, offset + dataLength))
                    }
                }
            }
            offset += dataLength
        }
        return DnsParsedResponse(
            addresses = addresses.distinctBy { it.hostAddress },
            truncated = truncated,
            responseCode = responseCode,
        )
    }

    private fun skipName(bytes: ByteArray, start: Int): Int {
        var offset = start
        var labels = 0
        while (true) {
            ensureAvailable(bytes, offset, 1)
            val length = bytes[offset].toInt() and 0xff
            if (length == 0) return offset + 1
            if (length and POINTER_MASK == POINTER_MASK) {
                ensureAvailable(bytes, offset, 2)
                return offset + 2
            }
            if (length > MAX_LABEL_LENGTH) throw DnsPacketException("Invalid DNS label length")
            offset += 1
            ensureAvailable(bytes, offset, length)
            offset += length
            labels += 1
            if (labels > MAX_LABELS) throw DnsPacketException("Too many DNS labels")
        }
    }

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int {
        ensureAvailable(bytes, offset, 2)
        return ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
    }

    private fun writeUnsignedShort(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 8) and 0xff).toByte()
        bytes[offset + 1] = (value and 0xff).toByte()
    }

    private fun ensureAvailable(bytes: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset + length > bytes.size) {
            throw DnsPacketException("Malformed DNS packet")
        }
    }

    private const val DNS_HEADER_SIZE = 12
    private const val DNS_QUESTION_TAIL_SIZE = 4
    private const val DNS_RECORD_HEADER_SIZE = 10
    private const val DNS_CLASS_IN = 1
    private const val FLAG_RECURSION_DESIRED = 0x0100
    private const val FLAG_RESPONSE = 0x8000
    private const val FLAG_TRUNCATED = 0x0200
    private const val RESPONSE_CODE_MASK = 0x000f
    private const val POINTER_MASK = 0xC0
    private const val MAX_LABEL_LENGTH = 63
    private const val MAX_LABELS = 128
    private const val IPV4_BYTES = 4
    private const val IPV6_BYTES = 16
}
