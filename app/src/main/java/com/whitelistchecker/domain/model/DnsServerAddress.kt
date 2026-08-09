package com.whitelistchecker.domain.model

object DnsServerAddress {

    fun isValidIpv4Literal(value: String): Boolean = parseIpv4Bytes(value) != null

    fun parseIpv4Bytes(value: String): ByteArray? {
        val parts = value.trim().split('.')
        if (parts.size != IPV4_PART_COUNT) return null
        val bytes = ByteArray(IPV4_PART_COUNT)
        parts.forEachIndexed { index, part ->
            if (part.isEmpty() || !part.all(Char::isDigit)) return null
            val number = part.toIntOrNull() ?: return null
            if (number !in IPV4_MIN..IPV4_MAX) return null
            if (part != "0" && part.startsWith('0')) return null
            bytes[index] = number.toByte()
        }
        return bytes
    }

    private const val IPV4_PART_COUNT = 4
    private const val IPV4_MIN = 0
    private const val IPV4_MAX = 255
}
