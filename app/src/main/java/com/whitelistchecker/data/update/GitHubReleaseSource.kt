package com.whitelistchecker.data.update

import com.whitelistchecker.domain.update.AppRelease
import com.whitelistchecker.domain.update.AppReleaseSource
import com.whitelistchecker.domain.update.ReleaseSourceResult
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException

class GitHubReleaseSource(
    private val httpClient: OkHttpClient,
) : AppReleaseSource {

    override suspend fun fetchReleases(includePreReleases: Boolean): ReleaseSourceResult =
        withContext(Dispatchers.IO) {
            if (includePreReleases) {
                fetchReleaseListFromApi()
            } else {
                fetchLatestStableFromWeb()
            }
        }

    private fun fetchLatestStableFromWeb(): ReleaseSourceResult {
        val request = Request.Builder()
            .url(LATEST_STABLE_WEB_URL)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ReleaseSourceResult.HttpFailure(
                        statusCode = response.code,
                        rateLimited = response.isRateLimited(),
                    )
                }

                val release = latestStableReleaseFromFinalUrl(response.request.url.toString())
                    ?: return ReleaseSourceResult.InvalidResponse
                ReleaseSourceResult.Success(listOf(release))
            }
        } catch (_: IOException) {
            ReleaseSourceResult.NetworkFailure
        } catch (_: RuntimeException) {
            ReleaseSourceResult.InvalidResponse
        }
    }

    private fun fetchReleaseListFromApi(): ReleaseSourceResult {
        val request = Request.Builder()
            .url(RELEASES_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ReleaseSourceResult.HttpFailure(
                        statusCode = response.code,
                        rateLimited = response.isRateLimited(),
                    )
                }

                val body = response.body?.string()
                    ?: return ReleaseSourceResult.InvalidResponse
                GitHubReleaseJsonParser.parse(body)
            }
        } catch (_: IOException) {
            ReleaseSourceResult.NetworkFailure
        } catch (_: RuntimeException) {
            ReleaseSourceResult.InvalidResponse
        }
    }

    private fun okhttp3.Response.isRateLimited(): Boolean {
        return code == 429 ||
            (code == 403 && (
                header("X-RateLimit-Remaining") == "0" ||
                    header("Retry-After") != null
                ))
    }

    companion object {
        private const val LATEST_STABLE_WEB_URL =
            "https://github.com/Regstar2/white-list-checker/releases/latest"
        private const val RELEASES_API_URL =
            "https://api.github.com/repos/Regstar2/white-list-checker/releases?per_page=20"
        private const val RELEASE_PAGE_PREFIX =
            "https://github.com/Regstar2/white-list-checker/releases/tag/"
        private const val RELEASE_TAG_PATH = "/Regstar2/white-list-checker/releases/tag/"
        private const val USER_AGENT = "WhiteListChecker-Android"

        internal fun latestStableReleaseFromFinalUrl(url: String): AppRelease? {
            if (!url.startsWith("https://github.com$RELEASE_TAG_PATH")) return null
            val encodedTag = url.substringAfter(RELEASE_TAG_PATH).substringBefore('?').substringBefore('#')
            if (encodedTag.isBlank() || encodedTag.contains('/')) return null
            val tagName = java.net.URLDecoder.decode(
                encodedTag,
                StandardCharsets.UTF_8.toString(),
            ).trim()
            if (tagName.isBlank()) return null

            return AppRelease(
                tagName = tagName,
                title = tagName,
                notes = "",
                pageUrl = officialReleasePage(tagName),
                isPreRelease = false,
            )
        }

        internal fun officialReleasePage(tagName: String): String {
            val encodedTag = URLEncoder.encode(
                tagName,
                StandardCharsets.UTF_8.toString(),
            ).replace("+", "%20")
            return RELEASE_PAGE_PREFIX + encodedTag
        }
    }
}

internal object GitHubReleaseJsonParser {
    fun parse(body: String): ReleaseSourceResult {
        return try {
            val array = JSONArray(body)
            val releases = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    if (item.optBoolean("draft", false)) continue

                    val tagName = item.optString("tag_name").trim()
                    if (tagName.isBlank()) continue

                    add(
                        AppRelease(
                            tagName = tagName,
                            title = item.optString("name").trim().ifBlank { tagName },
                            notes = item.optString("body").trim(),
                            pageUrl = GitHubReleaseSource.officialReleasePage(tagName),
                            isPreRelease = item.optBoolean("prerelease", false),
                        ),
                    )
                }
            }
            ReleaseSourceResult.Success(releases)
        } catch (_: JSONException) {
            ReleaseSourceResult.InvalidResponse
        }
    }
}
