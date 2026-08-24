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

    override suspend fun fetchReleases(): ReleaseSourceResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ReleaseSourceResult.HttpFailure(
                        statusCode = response.code,
                        rateLimited = response.code == 403 || response.code == 429,
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext ReleaseSourceResult.InvalidResponse
                GitHubReleaseJsonParser.parse(body)
            }
        } catch (_: IOException) {
            ReleaseSourceResult.NetworkFailure
        } catch (_: RuntimeException) {
            ReleaseSourceResult.InvalidResponse
        }
    }

    companion object {
        private const val RELEASES_API_URL =
            "https://api.github.com/repos/Regstar2/white-list-checker/releases?per_page=20"
        private const val RELEASE_PAGE_PREFIX =
            "https://github.com/Regstar2/white-list-checker/releases/tag/"
        private const val USER_AGENT = "WhiteListChecker-Android"

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
