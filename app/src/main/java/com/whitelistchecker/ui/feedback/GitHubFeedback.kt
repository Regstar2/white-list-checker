package com.whitelistchecker.ui.feedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.whitelistchecker.R

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
private const val REPOSITORY_PATH = "Regstar2/white-list-checker"

fun buildFeedbackUrl(
    destination: FeedbackDestination,
    appVersion: String,
): Uri {
    val safeVersion = appVersion.trim().ifBlank { "unknown" }
    return Uri.Builder()
        .scheme("https")
        .authority(GITHUB_HOST)
        .appendPath("Regstar2")
        .appendPath("white-list-checker")
        .appendPath("issues")
        .appendPath("new")
        .appendQueryParameter("template", destination.templateName)
        .appendQueryParameter("title", "${destination.titlePrefix} [$safeVersion] ")
        .build()
}

fun openFeedbackForm(
    context: Context,
    destination: FeedbackDestination,
    appVersion: String,
): Boolean {
    val uri = buildFeedbackUrl(destination, appVersion)
    if (uri.scheme != "https" || uri.host != GITHUB_HOST || !uri.path.orEmpty().startsWith("/$REPOSITORY_PATH/issues/new")) {
        showOpenError(context)
        return false
    }

    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            },
        )
        true
    } catch (_: ActivityNotFoundException) {
        showOpenError(context)
        false
    } catch (_: SecurityException) {
        showOpenError(context)
        false
    }
}

private fun showOpenError(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.feedback_open_error),
        Toast.LENGTH_SHORT,
    ).show()
}
