package com.whitelistchecker.data.update

import com.whitelistchecker.domain.update.ReleaseSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseJsonParserTest {

    @Test
    fun `parses releases and ignores drafts`() {
        val json = """
            [
              {
                "tag_name": "v1.1.0",
                "name": "WhiteListChecker 1.1.0",
                "body": "Release notes",
                "draft": false,
                "prerelease": false
              },
              {
                "tag_name": "v1.2.0-beta.1",
                "name": "Beta",
                "body": "Beta notes",
                "draft": true,
                "prerelease": true
              }
            ]
        """.trimIndent()

        val result = GitHubReleaseJsonParser.parse(json)
        assertTrue(result is ReleaseSourceResult.Success)
        val releases = (result as ReleaseSourceResult.Success).releases
        assertEquals(1, releases.size)
        assertEquals("v1.1.0", releases.single().tagName)
        assertEquals(
            "https://github.com/Regstar2/white-list-checker/releases/tag/v1.1.0",
            releases.single().pageUrl,
        )
    }

    @Test
    fun `invalid json returns typed failure`() {
        val result = GitHubReleaseJsonParser.parse("not-json")

        assertTrue(result is ReleaseSourceResult.InvalidResponse)
    }

    @Test
    fun `release page encodes tag and stays on official repository`() {
        assertEquals(
            "https://github.com/Regstar2/white-list-checker/releases/tag/v1.2.0-rc.1",
            GitHubReleaseSource.officialReleasePage("v1.2.0-rc.1"),
        )
    }

    @Test
    fun `latest stable web redirect becomes official release`() {
        val release = GitHubReleaseSource.latestStableReleaseFromFinalUrl(
            "https://github.com/Regstar2/white-list-checker/releases/tag/v1.1.0",
        )

        requireNotNull(release)
        assertEquals("v1.1.0", release.tagName)
        assertEquals(false, release.isPreRelease)
        assertEquals(
            "https://github.com/Regstar2/white-list-checker/releases/tag/v1.1.0",
            release.pageUrl,
        )
    }

    @Test
    fun `latest stable web parser rejects foreign repository`() {
        assertNull(
            GitHubReleaseSource.latestStableReleaseFromFinalUrl(
                "https://github.com/example/other/releases/tag/v9.9.9",
            ),
        )
    }
}
