package com.whitelistchecker.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `parses stable version with optional v prefix`() {
        assertEquals(
            SemanticVersion(1, 2, 3),
            SemanticVersion.parse("v1.2.3"),
        )
    }

    @Test
    fun `stable version is newer than prerelease with same core`() {
        val stable = requireNotNull(SemanticVersion.parse("1.2.0"))
        val prerelease = requireNotNull(SemanticVersion.parse("1.2.0-rc.1"))

        assertTrue(stable > prerelease)
    }

    @Test
    fun `numeric prerelease identifiers use numeric ordering`() {
        val beta2 = requireNotNull(SemanticVersion.parse("1.2.0-beta.2"))
        val beta10 = requireNotNull(SemanticVersion.parse("1.2.0-beta.10"))

        assertTrue(beta10 > beta2)
    }

    @Test
    fun `build metadata does not affect precedence`() {
        val first = requireNotNull(SemanticVersion.parse("1.2.3+build.1"))
        val second = requireNotNull(SemanticVersion.parse("1.2.3+build.9"))

        assertEquals(0, first.compareTo(second))
    }

    @Test
    fun `rejects non semantic version`() {
        assertNull(SemanticVersion.parse("release-1"))
    }
}
