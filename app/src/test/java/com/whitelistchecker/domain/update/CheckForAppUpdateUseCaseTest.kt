package com.whitelistchecker.domain.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        val result = assertType<AppUpdateCheckResult.UpdateAvailable>(useCase.check())
        assertEquals("v1.1.0", result.release.tagName)
    }

    @Test
    fun `stable build also ignores prerelease suffix when GitHub flag is false`() = runTest {
        val useCase = useCase(
            installed = "1.0.0",
            releases = listOf(
                release("v1.1.0-rc.1", prerelease = false),
            ),
        )

        assertType<AppUpdateCheckResult.UpToDate>(useCase.check())
    }

    @Test
    fun `prerelease build can receive stable release`() = runTest {
        val useCase = useCase(
            installed = "1.1.0-beta.1",
            releases = listOf(
                release("v1.1.0"),
            ),
        )

        val result = assertType<AppUpdateCheckResult.UpdateAvailable>(useCase.check())
        assertEquals("v1.1.0", result.release.tagName)
    }

    @Test
    fun `stable build requests stable-only source`() = runTest {
        var includePreReleases: Boolean? = null
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { include ->
                includePreReleases = include
                ReleaseSourceResult.Success(emptyList())
            },
            installedVersionProvider = { "1.0.0" },
        )

        useCase.check()

        assertEquals(false, includePreReleases)
    }

    @Test
    fun `prerelease build requests prerelease-capable source`() = runTest {
        var includePreReleases: Boolean? = null
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { include ->
                includePreReleases = include
                ReleaseSourceResult.Success(emptyList())
            },
            installedVersionProvider = { "1.1.0-beta.1" },
        )

        useCase.check()

        assertEquals(true, includePreReleases)
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

        assertType<AppUpdateCheckResult.UpToDate>(useCase.check())
    }

    @Test
    fun `maps network failure without throwing`() = runTest {
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { _ -> ReleaseSourceResult.NetworkFailure },
            installedVersionProvider = { "1.0.0" },
        )

        val result = assertType<AppUpdateCheckResult.Failure>(useCase.check())
        assertEquals(AppUpdateError.NETWORK, result.error)
    }

    @Test
    fun `maps rate limit separately`() = runTest {
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { _ ->
                ReleaseSourceResult.HttpFailure(statusCode = 403, rateLimited = true)
            },
            installedVersionProvider = { "1.0.0" },
        )

        val result = assertType<AppUpdateCheckResult.Failure>(useCase.check())
        assertEquals(AppUpdateError.RATE_LIMITED, result.error)
    }

    @Test
    fun `invalid installed version is reported before network access`() = runTest {
        var sourceCalled = false
        val useCase = CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { _ ->
                sourceCalled = true
                ReleaseSourceResult.Success(emptyList())
            },
            installedVersionProvider = { "dev" },
        )

        val result = assertType<AppUpdateCheckResult.Failure>(useCase.check())
        assertEquals(AppUpdateError.INVALID_INSTALLED_VERSION, result.error)
        assertEquals(false, sourceCalled)
    }

    private fun useCase(
        installed: String,
        releases: List<AppRelease>,
    ): CheckForAppUpdateUseCase {
        return CheckForAppUpdateUseCase(
            releaseSource = AppReleaseSource { _ -> ReleaseSourceResult.Success(releases) },
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

    private inline fun <reified T> assertType(value: Any): T {
        assertTrue("Expected ${T::class.java.simpleName}, got ${value::class.java.simpleName}", value is T)
        return value as T
    }
}
