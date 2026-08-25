package com.whitelistchecker.domain.update

class CheckForAppUpdateUseCase(
    private val releaseSource: AppReleaseSource,
    private val installedVersionProvider: () -> String,
) {
    suspend fun check(): AppUpdateCheckResult {
        val installedVersionName = installedVersionProvider().trim()
        val installedVersion = SemanticVersion.parse(installedVersionName)
            ?: return AppUpdateCheckResult.Failure(AppUpdateError.INVALID_INSTALLED_VERSION)

        return when (
            val sourceResult = releaseSource.fetchReleases(
                includePreReleases = installedVersion.isPreRelease,
            )
        ) {
            is ReleaseSourceResult.Success -> selectRelease(
                installedVersionName = installedVersionName,
                installedVersion = installedVersion,
                releases = sourceResult.releases,
            )
            is ReleaseSourceResult.HttpFailure -> AppUpdateCheckResult.Failure(
                if (sourceResult.rateLimited) AppUpdateError.RATE_LIMITED else AppUpdateError.HTTP,
            )
            ReleaseSourceResult.NetworkFailure -> AppUpdateCheckResult.Failure(AppUpdateError.NETWORK)
            ReleaseSourceResult.InvalidResponse -> AppUpdateCheckResult.Failure(AppUpdateError.INVALID_RESPONSE)
        }
    }

    private fun selectRelease(
        installedVersionName: String,
        installedVersion: SemanticVersion,
        releases: List<AppRelease>,
    ): AppUpdateCheckResult {
        val allowPreRelease = installedVersion.isPreRelease
        val candidate = releases
            .mapNotNull { release ->
                val version = SemanticVersion.parse(release.tagName) ?: return@mapNotNull null
                val isPreRelease = release.isPreRelease || version.isPreRelease
                if (!allowPreRelease && isPreRelease) return@mapNotNull null
                if (version <= installedVersion) return@mapNotNull null
                release to version
            }
            .maxWithOrNull(compareBy<Pair<AppRelease, SemanticVersion>> { it.second })
            ?.first

        return if (candidate == null) {
            AppUpdateCheckResult.UpToDate(installedVersion = installedVersionName)
        } else {
            AppUpdateCheckResult.UpdateAvailable(
                installedVersion = installedVersionName,
                release = candidate,
            )
        }
    }
}
