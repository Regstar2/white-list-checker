package com.whitelistchecker.domain.update

data class AppRelease(
    val tagName: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val isPreRelease: Boolean,
)

sealed interface ReleaseSourceResult {
    data class Success(val releases: List<AppRelease>) : ReleaseSourceResult
    data class HttpFailure(
        val statusCode: Int,
        val rateLimited: Boolean = false,
    ) : ReleaseSourceResult
    data object NetworkFailure : ReleaseSourceResult
    data object InvalidResponse : ReleaseSourceResult
}

fun interface AppReleaseSource {
    suspend fun fetchReleases(): ReleaseSourceResult
}

enum class AppUpdateError {
    NETWORK,
    HTTP,
    RATE_LIMITED,
    INVALID_RESPONSE,
    INVALID_INSTALLED_VERSION,
}

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(
        val installedVersion: String,
        val release: AppRelease,
    ) : AppUpdateCheckResult

    data class UpToDate(
        val installedVersion: String,
    ) : AppUpdateCheckResult

    data class Failure(
        val error: AppUpdateError,
    ) : AppUpdateCheckResult
}
