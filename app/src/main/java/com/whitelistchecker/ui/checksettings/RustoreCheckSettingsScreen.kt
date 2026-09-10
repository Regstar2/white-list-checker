package com.whitelistchecker.ui.checksettings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun RustoreCheckSettingsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onToggleTarget: (String, Boolean) -> Unit,
    onAddTarget: (EditableCheckTarget) -> Unit,
    onResetTargets: () -> Unit,
    onRemoveTarget: (String) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.check_settings_title), onBack = onBack) {
        RustoreSitesSettings(
            targets = uiState.checkTargets,
            onToggleTarget = onToggleTarget,
            onAddTarget = onAddTarget,
            onResetTargets = onResetTargets,
            onRemoveTarget = onRemoveTarget,
        )
    }
}

@Composable
private fun RustoreSitesSettings(
    targets: List<EditableCheckTarget>,
    onToggleTarget: (String, Boolean) -> Unit,
    onAddTarget: (EditableCheckTarget) -> Unit,
    onResetTargets: () -> Unit,
    onRemoveTarget: (String) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("https://") }
    var newGroup by remember { mutableStateOf(TargetGroup.FOREIGN) }
    var formError by remember { mutableStateOf<Int?>(null) }

    RustoreTargetGroupSection(
        title = stringResource(R.string.check_settings_foreign_sites),
        targets = targets.filter { it.group == TargetGroup.FOREIGN },
        onToggleTarget = onToggleTarget,
        onRemoveTarget = onRemoveTarget,
    )
    RustoreTargetGroupSection(
        title = stringResource(R.string.check_settings_local_sites),
        targets = targets.filter { it.group == TargetGroup.LOCAL },
        onToggleTarget = onToggleTarget,
        onRemoveTarget = onRemoveTarget,
    )

    if (showAddForm) {
        AppCard(title = stringResource(R.string.check_settings_new_site)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_name)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_url)) },
                singleLine = true,
            )
            RustoreGroupSelector(selected = newGroup, onSelected = { newGroup = it })
            formError?.let { ErrorCard(stringResource(it)) }
            Button(
                onClick = {
                    val validation = validateRustoreTarget(newName, newUrl)
                    if (validation != null) {
                        formError = validation
                    } else {
                        onAddTarget(
                            EditableCheckTarget.create(
                                name = newName.trim(),
                                url = newUrl.trim(),
                                group = newGroup,
                            ),
                        )
                        newName = ""
                        newUrl = "https://"
                        newGroup = TargetGroup.FOREIGN
                        formError = null
                        showAddForm = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.check_settings_save_site))
            }
        }
    } else {
        OutlinedButton(onClick = { showAddForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.check_settings_add_site))
        }
    }

    OutlinedButton(onClick = onResetTargets, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.check_settings_reset_sites))
    }
}

@Composable
private fun RustoreTargetGroupSection(
    title: String,
    targets: List<EditableCheckTarget>,
    onToggleTarget: (String, Boolean) -> Unit,
    onRemoveTarget: (String) -> Unit,
) {
    AppCard(title = title) {
        if (targets.isEmpty()) {
            Text(stringResource(R.string.check_settings_no_sites), style = MaterialTheme.typography.bodySmall)
        } else {
            targets.forEach { target ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = target.enabled,
                        onCheckedChange = { onToggleTarget(target.id, it) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = target.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onRemoveTarget(target.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.check_settings_delete_target),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RustoreGroupSelector(
    selected: TargetGroup,
    onSelected: (TargetGroup) -> Unit,
) {
    Text(stringResource(R.string.check_settings_group), style = MaterialTheme.typography.labelMedium)
    TargetGroup.entries.forEach { group ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == group,
                onClick = { onSelected(group) },
            )
            Text(
                stringResource(
                    if (group == TargetGroup.FOREIGN) {
                        R.string.check_settings_group_foreign
                    } else {
                        R.string.check_settings_group_local
                    },
                ),
            )
        }
    }
}

@StringRes
private fun validateRustoreTarget(name: String, url: String): Int? {
    if (name.isBlank()) return R.string.check_settings_error_name_empty
    if (!url.startsWith("https://") && !url.startsWith("http://")) {
        return R.string.check_settings_error_url_scheme
    }
    return null
}
