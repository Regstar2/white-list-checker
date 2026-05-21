package com.whitelistchecker.domain.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkerUrlBuilderTest {

    private val builder = WorkerUrlBuilder()

    @Test
    fun buildEndpoint_usesCorrectMethodCase() {
        assertEquals(
            "https://example.workers.dev/tg/sendMessage",
            builder.buildEndpoint("https://example.workers.dev", WorkerUrlBuilder.METHOD_SEND_MESSAGE),
        )
    }

    @Test
    fun buildEndpoint_trimsTrailingSlashFromBaseUrl() {
        assertEquals(
            "https://example.workers.dev/tg/sendMessage",
            builder.buildEndpoint("https://example.workers.dev/", WorkerUrlBuilder.METHOD_SEND_MESSAGE),
        )
    }

    @Test
    fun buildEndpoint_stripsAccidentalTgMethodSuffixFromBaseUrl() {
        assertEquals(
            "https://example.workers.dev/tg/sendMessage",
            builder.buildEndpoint(
                "https://example.workers.dev/tg/getMe",
                WorkerUrlBuilder.METHOD_SEND_MESSAGE,
            ),
        )
    }

    @Test
    fun buildEndpoint_rejectsUnsupportedMethod() {
        assertThrows(IllegalArgumentException::class.java) {
            builder.buildEndpoint("https://example.workers.dev", "sendmessage")
        }
    }
}
