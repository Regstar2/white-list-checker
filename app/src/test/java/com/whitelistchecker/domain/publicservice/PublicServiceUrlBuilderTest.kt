package com.whitelistchecker.domain.publicservice

import org.junit.Assert.assertEquals
import org.junit.Test

class PublicServiceUrlBuilderTest {

    @Test
    fun `normalizes fixed public service url`() {
        val builder = PublicServiceUrlBuilder("https://example.workers.dev/")

        assertEquals("https://example.workers.dev/api/v1/reports", builder.buildEndpoint("/api/v1/reports"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non https fixed url`() {
        PublicServiceUrlBuilder("http://example.workers.dev")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects query in fixed url`() {
        PublicServiceUrlBuilder("https://example.workers.dev?token=nope")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects endpoint path without leading slash`() {
        PublicServiceUrlBuilder("https://example.workers.dev").buildEndpoint("api/v1/reports")
    }
}
