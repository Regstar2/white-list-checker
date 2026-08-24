package com.whitelistchecker.domain.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckForAppUpdateUseCaseTest {

    @Test
    fun `stable build ignores newer prerelease and selects newest stable`() = runTest {
        val useCase = useCase(
            installed = "1.0.0",
            releases = listOf(
                release("v1.2.0-beta.1", prerelease = true),
                release("v1.1.0"),
                release("v1.0.1"),
            ),
        )

        val result = useCase.check()
        assertTrue(result is AppUpdateCheckResult.UpdateAvailable)
        assertEquals("v1.1.0", (result as AppUpdateCheckResult.UpdateAvailable).release.tagName)
    }

    @Test
    fun `stable build also ignores prerelease suffix when GitHub flag is false`() = runTest {
        val useCase = useCase(
            installed = "1.0.0",
            releases = listOf(
                release("v1.1.0-rc.1", prerelease = false),
            ),
        )

        assertTrue(useCase.check() is AppUpdateCheckResult.UpToDate)
    }

    @Test
    fun `prerelease build can receive stable release`() = runTest {
        val useCase = useCase(
            installed = "1.1.0-beta.1",
            releases = listOf(
                release("v1.1.0"),
            ),
        )

        val result = useCase.check()
        assertTrue(result is AppUpdateCheckResult.UpdateAvailable)
        assertEquals("v1.1.0", (result as AppUpdateCheckResult.UpdateAvailable).release.tagName)
    }

    @Test
    fun `returns up to date when no newer eligible release exists`() = runTest {
        val useCase = useCase(
            installed = "1.1.0",
            releases = listOf(
                release("v1.1.0"),
                release("v1.0.9"),
            ),
        )

        assertTrue(useCase.check() is AppUpdateCheckResult.UpToDate)
    }

    @Test
    fun `maps network failure without throwing`() = runTest {
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { ReleaseSourceResult.NetworkFailure },
            installedVersionProvider = { "1.0.0" },
        )

        val result = useCase.check()
        assertTrue(result is AppUpdateCheckResult.Failure)
        assertEquals(AppUpdateError.NETWORK, (result as AppUpdateCheckResult.Failure).error)
    }

    @Test
    fun `maps rate limit separately`() = runTest {
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource {
                ReleaseSourceResult.HttpFailure(statusCode = 403, rateLimited = true)
            },
            installedVersionProvider = { "1.0.0" },
        )

        val result = useCase.check()
        assertTrue(result is AppUpdateCheckResult.Failure)
        assertEquals(AppUpdateError.RATE_LIMITED, (result as AppUpdateCheckResult.Failure).error)
    }

    @Test
    fun `invalid installed version is reported before network access`() = runTest {
        var sourceCalled = false
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource {
                sourceCalled = true
                ReleaseSourceResult.Success(emptyList())
            },
            installedVersionProvider = { "dev" },
        )

        val result = useCase.check()
        assertTrue(result is AppUpdateCheckResult.Failure)
        assertEquals(
            AppUpdateError.INVALID_INSTALLED_VERSION,
            (result as AppUpdateCheckResult.Failure).error,
        )
        assertFalse(sourceCalled)
    }

    private fun useCase(
        installed: String,
        releases: List<AppRelease>,
    ): CheckForAppUpdateUseCase {
        return CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { ReleaseSourceResult.Success(releases) },
            installedVersionProvider = { installed },
        )
    }

    private fun release(
        tag: String,
        prerelease: Boolean = false,
    ): AppRelease {
        return AppRelease(
            tagName = tag,
            title = tag,
            notes = "notes",
            pageUrl = "https://github.com/Regstar2/white-list-checker/releases/tag/$tag",
            isPreRelease = prerelease,
        )
    }
}
