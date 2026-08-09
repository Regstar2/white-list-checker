package com.whitelistchecker.data.dns

import com.whitelistchecker.domain.model.TargetGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDnsServersTest {

    @Test
    fun defaults_haveTwoEnabledBuiltInsInEachGroupAndUniqueIdentity() {
        val defaults = DefaultDnsServers.defaults()
        val foreign = defaults.filter { it.group == TargetGroup.FOREIGN }
        val local = defaults.filter { it.group == TargetGroup.LOCAL }

        assertTrue(foreign.count { it.enabled } >= 2)
        assertTrue(local.count { it.enabled } >= 2)
        assertTrue(defaults.all { it.builtIn })
        assertEquals(defaults.size, defaults.map { it.id }.toSet().size)
        assertEquals(
            defaults.size,
            defaults.map { Triple(it.address, it.port, it.protocol) }.toSet().size,
        )
    }
}
