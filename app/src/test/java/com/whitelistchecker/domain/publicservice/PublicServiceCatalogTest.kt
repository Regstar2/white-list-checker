package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.domain.model.PublicServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicServiceCatalogTest {

    @Test
    fun `normalizes russian region abbreviation`() {
        val region = PublicServiceCatalog.normalizeRegion("Рязанская обл.")

        assertEquals("RU-RYA", region?.code)
    }

    @Test
    fun `normalizes english region name`() {
        val region = PublicServiceCatalog.normalizeRegion("Ryazan Oblast")

        assertEquals("RU-RYA", region?.code)
    }

    @Test
    fun `unknown region returns null`() {
        assertNull(PublicServiceCatalog.normalizeRegion("Neverland"))
    }

    @Test
    fun `normalizes city inside selected region`() {
        val city = PublicServiceCatalog.normalizeCity("RU-RYA", "Ryazan")

        assertEquals("RU-RYA-RYAZAN", city?.code)
    }

    @Test
    fun `city can be absent`() {
        assertNull(PublicServiceCatalog.normalizeCity("RU-RYA", null))
    }

    @Test
    fun `custom city name is sanitized before report persistence`() {
        val sanitized = PublicServiceCatalog.sanitizeCustomCityName("  Test\u0000\n City  ")

        assertEquals("Test City", sanitized)
    }

    @Test
    fun `megafon mcc mnc maps to stable operator code`() {
        val operator = PublicServiceCatalog.detectOperatorByMccMnc("25002")

        assertEquals("MEGAFON", operator?.code)
    }

    @Test
    fun `mts mcc mnc maps to stable operator code`() {
        val operator = PublicServiceCatalog.detectOperatorByMccMnc("25001")

        assertEquals("MTS", operator?.code)
    }

    @Test
    fun `beeline mcc mnc maps to stable operator code`() {
        val operator = PublicServiceCatalog.detectOperatorByMccMnc("25099")

        assertEquals("BEELINE", operator?.code)
    }

    @Test
    fun `t2 mcc mnc maps to stable operator code`() {
        val operator = PublicServiceCatalog.detectOperatorByMccMnc("25020")

        assertEquals("T2", operator?.code)
    }

    @Test
    fun `unknown mcc mnc returns null`() {
        assertNull(PublicServiceCatalog.detectOperatorByMccMnc("99999"))
    }

    @Test
    fun `operator name can normalize only through catalog mapping`() {
        val operator = PublicServiceCatalog.detectOperatorByName("Yota")

        assertEquals("YOTA", operator?.code)
    }

    @Test
    fun `unknown operator name does not become stable id`() {
        assertNull(PublicServiceCatalog.detectOperatorByName("Some New Operator"))
    }
}
