package com.whitelistchecker.ui.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.whitelistchecker.R
import com.whitelistchecker.domain.update.AppUpdateError

@Composable
fun AppUpdateAvailableDialog(
    state: AppUpdateUiState.Available,
    onDismiss: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    if (state.promptDismissed) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.update_available_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.update_available_message,
                    state.release.tagName,
                    state.installedVersion,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenRelease) {
                Text(stringResource(R.string.update_open_release))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

fun openOfficialRelease(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        showOpenReleaseError(context)
        false
    } catch (_: SecurityException) {
        showOpenReleaseError(context)
        false
    }
}

private fun showOpenReleaseError(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.update_open_release_error),
        Toast.LENGTH_SHORT,
    ).show()
}

@StringRes
fun AppUpdateError.messageRes(): Int = when (this) {
    AppUpdateError.NETWORK -> R.string.update_error_network
    AppUpdateError.HTTP -> R.string.update_error_http
    AppUpdateError.RATE_LIMITED -> R.string.update_error_rate_limited
    AppUpdateError.INVALID_RESPONSE -> R.string.update_error_invalid_response
    AppUpdateError.INVALID_INSTALLED_VERSION -> R.string.update_error_invalid_version
}
