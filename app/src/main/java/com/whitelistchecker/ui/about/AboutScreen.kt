package com.whitelistchecker.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.BuildConfig
import com.whitelistchecker.R
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.update.AppUpdateUiState
import com.whitelistchecker.ui.update.messageRes
import com.whitelistchecker.ui.update.openOfficialRelease

private const val GITHUB_URL = "https://github.com/Regstar2/white-list-checker"

@Composable
fun AboutScreen(
    updateUiState: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    ScreenScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        AppCard(title = null) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppCard(title = null) {
            CompactDetailRow(
                label = stringResource(R.string.about_version),
                value = BuildConfig.VERSION_NAME,
            )
            AboutLinkRow(
                title = stringResource(R.string.about_source_code),
                value = stringResource(R.string.about_github),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Unit
                    }
                },
            )
            CompactDetailRow(
                label = stringResource(R.string.about_license),
                value = stringResource(R.string.about_license_mit),
            )
        }

        AppCard(title = stringResource(R.string.update_section_title)) {
            when (updateUiState) {
                AppUpdateUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.update_not_checked),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                AppUpdateUiState.Checking -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.update_checking),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                is AppUpdateUiState.UpToDate -> {
                    Text(
                        text = stringResource(
                            R.string.update_up_to_date,
                            updateUiState.installedVersion,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is AppUpdateUiState.Available -> {
                    Text(
                        text = stringResource(
                            R.string.update_available_inline,
                            updateUiState.release.tagName,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = updateUiState.release.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.update_release_notes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = updateUiState.release.notes.ifBlank {
                            stringResource(R.string.update_release_notes_empty)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OutlinedButton(
                        onClick = {
                            openOfficialRelease(context, updateUiState.release.pageUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_open_release))
                    }
                }
                is AppUpdateUiState.Error -> {
                    Text(
                        text = stringResource(updateUiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Button(
                onClick = onCheckForUpdates,
                enabled = updateUiState != AppUpdateUiState.Checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.update_check_button))
            }

            Text(
                text = stringResource(R.string.update_source_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
