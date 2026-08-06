package com.whitelistchecker.data.system

import com.whitelistchecker.domain.model.PublicServiceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceAliasResolverTest {

    @Test
    fun `uses configured Android device name first`() {
        val alias = DeviceAliasResolver.resolve(
            configuredDeviceName = "  Sergey Phone  ",
            modelName = "Pixel 9",
            deviceName = "tokay",
        )

        assertEquals("Sergey Phone", alias)
    }

    @Test
    fun `falls back to model when configured name is blank`() {
        val alias = DeviceAliasResolver.resolve(
            configuredDeviceName = " ",
            modelName = "Pixel 9",
            deviceName = "tokay",
        )

        assertEquals("Pixel 9", alias)
    }

    @Test
    fun `uses generic fallback when Android names are unavailable`() {
        val alias = DeviceAliasResolver.resolve(
            configuredDeviceName = null,
            modelName = null,
            deviceName = "",
        )

        assertEquals(PublicServiceSettings.DEFAULT_DEVICE_ALIAS, alias)
    }

    @Test
    fun `limits alias to server field length`() {
        val alias = DeviceAliasResolver.resolve(
            configuredDeviceName = "a".repeat(80),
            modelName = null,
            deviceName = null,
        )

        assertEquals(64, alias.length)
    }
}
