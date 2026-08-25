package com.whitelistchecker.ui.feedback

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubFeedbackTest {

    @Test
    fun `bug report opens the structured bug form with app version in title`() {
        val url = buildFeedbackUrl(
            destination = FeedbackDestination.BUG_REPORT,
            appVersion = "1.0.0",
        )
        val uri = URI(url)

        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertEquals("/Regstar2/white-list-checker/issues/new", uri.path)
        assertEquals("bug_report.yml", queryValue(uri, "template"))
        assertEquals("[Bug] [1.0.0] ", queryValue(uri, "title"))
        assertTrue(isOfficialFeedbackUrl(url))
    }

    @Test
    fun `feature request opens the structured feature form`() {
        val url = buildFeedbackUrl(
            destination = FeedbackDestination.FEATURE_REQUEST,
            appVersion = "1.0.0",
        )
        val uri = URI(url)

        assertEquals("feature_request.yml", queryValue(uri, "template"))
        assertEquals("[Feature] [1.0.0] ", queryValue(uri, "title"))
        assertTrue(isOfficialFeedbackUrl(url))
    }

    @Test
    fun `version text cannot escape the official GitHub feedback URL`() {
        val url = buildFeedbackUrl(
            destination = FeedbackDestination.BUG_REPORT,
            appVersion = "1.0.0&template=evil.yml#fragment",
        )
        val uri = URI(url)

        assertEquals("github.com", uri.host)
        assertEquals("bug_report.yml", queryValue(uri, "template"))
        assertEquals(
            "[Bug] [1.0.0&template=evil.yml#fragment] ",
            queryValue(uri, "title"),
        )
        assertTrue(isOfficialFeedbackUrl(url))
        assertFalse(uri.rawFragment != null)
    }

    private fun queryValue(uri: URI, name: String): String? {
        return uri.rawQuery
            .split('&')
            .mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.toString())
                val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.toString())
                key to value
            }
            .firstOrNull { it.first == name }
            ?.second
    }
}
