package com.whitelistchecker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.AppLanguage
import com.whitelistchecker.domain.model.AppThemeMode
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    var themeDialogVisible by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        AppCard(title = stringResource(R.string.settings_appearance_title)) {
            SettingsRow(
                title = stringResource(R.string.settings_theme),
                value = uiState.userSettings.themeMode.toDisplayText(),
                onClick = { themeDialogVisible = true },
            )
            SettingsRow(
                title = stringResource(R.string.settings_language),
                value = uiState.userSettings.language.toDisplayText(),
                onClick = { languageDialogVisible = true },
            )
        }
    }

    if (themeDialogVisible) {
        SettingsChoiceDialog(
            title = stringResource(R.string.settings_theme),
            values = AppThemeMode.entries,
            selected = uiState.userSettings.themeMode,
            label = { it.toDisplayText() },
            onSelect = {
                onThemeModeChange(it)
                themeDialogVisible = false
            },
            onDismiss = { themeDialogVisible = false },
        )
    }

    if (languageDialogVisible) {
        SettingsChoiceDialog(
            title = stringResource(R.string.settings_language),
            values = AppLanguage.entries,
            selected = uiState.userSettings.language,
            label = { it.toDisplayText() },
            onSelect = {
                onLanguageChange(it)
                languageDialogVisible = false
            },
            onDismiss = { languageDialogVisible = false },
        )
    }
}

@Composable
private fun SettingsRow(
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

@Composable
private fun <T> SettingsChoiceDialog(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                values.forEach { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                        )
                        Text(
                            text = label(value),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun AppThemeMode.toDisplayText(): String {
    return when (this) {
        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
}

@Composable
private fun AppLanguage.toDisplayText(): String {
    return when (this) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.RUSSIAN -> stringResource(R.string.settings_language_russian)
        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    }
}
