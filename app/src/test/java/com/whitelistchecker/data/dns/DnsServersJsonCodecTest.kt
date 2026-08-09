package com.whitelistchecker.data.dns

import com.whitelistchecker.domain.model.DnsServerProtocol
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.TargetGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsServersJsonCodecTest {

    @Test
    fun roundTrip_preservesDnsFields() {
        val original = listOf(
            EditableDnsServer(
                id = "custom-1",
                name = "Custom",
                address = "9.9.9.9",
                group = TargetGroup.LOCAL,
                enabled = false,
                builtIn = false,
                protocol = DnsServerProtocol.DNS_UDP_TCP,
                port = 5353,
            ),
        )

        val decoded = DnsServersJsonCodec.decode(DnsServersJsonCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun decode_corruptJson_returnsEmptyList() {
        assertTrue(DnsServersJsonCodec.decode("not-json").isEmpty())
    }

    @Test
    fun decode_missingOptionalFields_usesBackwardCompatibleDefaults() {
        val raw = """[{"id":"legacy","name":"Legacy","address":"1.2.3.4","group":"FOREIGN"}]"""

        val decoded = DnsServersJsonCodec.decode(raw).single()

        assertTrue(decoded.enabled)
        assertEquals(false, decoded.builtIn)
        assertEquals(DnsServerProtocol.DNS_UDP_TCP, decoded.protocol)
        assertEquals(53, decoded.port)
        assertEquals(TargetGroup.FOREIGN, decoded.group)
    }

    @Test
    fun decode_invalidOptionalValues_fallsBackSafely() {
        val raw = """[{"id":"legacy","name":"Legacy","address":"1.2.3.4","group":"UNKNOWN","protocol":"UNKNOWN","port":70000}]"""

        val decoded = DnsServersJsonCodec.decode(raw).single()

        assertEquals(TargetGroup.FOREIGN, decoded.group)
        assertEquals(DnsServerProtocol.DNS_UDP_TCP, decoded.protocol)
        assertEquals(53, decoded.port)
    }
}
