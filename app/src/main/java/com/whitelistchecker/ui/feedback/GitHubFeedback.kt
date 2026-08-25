package com.whitelistchecker.ui.feedback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.whitelistchecker.R
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class FeedbackDestination(
    val templateName: String,
    val titlePrefix: String,
) {
    BUG_REPORT(
        templateName = "bug_report.yml",
        titlePrefix = "[Bug]",
    ),
    FEATURE_REQUEST(
        templateName = "feature_request.yml",
        titlePrefix = "[Feature]",
    ),
}

private const val GITHUB_HOST = "github.com"
private const val FEEDBACK_PATH = "/Regstar2/white-list-checker/issues/new"

fun buildFeedbackUrl(
    destination: FeedbackDestination,
    appVersion: String,
): String {
    val safeVersion = appVersion.trim().ifBlank { "unknown" }
    val encodedTemplate = encodeQueryValue(destination.templateName)
    val encodedTitle = encodeQueryValue("${destination.titlePrefix} [$safeVersion] ")
    return "https://$GITHUB_HOST$FEEDBACK_PATH?template=$encodedTemplate&title=$encodedTitle"
}

internal fun isOfficialFeedbackUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        uri.scheme == "https" &&
            uri.host == GITHUB_HOST &&
            uri.path == FEEDBACK_PATH
    } catch (_: IllegalArgumentException) {
        false
    }
}

fun openFeedbackForm(
    context: Context,
    destination: FeedbackDestination,
    appVersion: String,
): Boolean {
    val url = buildFeedbackUrl(destination, appVersion)
    if (!isOfficialFeedbackUrl(url)) {
        showOpenError(context)
        return false
    }

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        showOpenError(context)
        false
    } catch (_: SecurityException) {
        showOpenError(context)
        false
    }
}

private fun encodeQueryValue(value: String): String {
    return URLEncoder.encode(
        value,
        StandardCharsets.UTF_8.toString(),
    ).replace("+", "%20")
}

private fun showOpenError(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.feedback_open_error),
        Toast.LENGTH_SHORT,
    ).show()
}
