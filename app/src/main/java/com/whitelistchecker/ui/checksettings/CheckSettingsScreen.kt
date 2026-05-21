package com.whitelistchecker.ui.checksettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun CheckSettingsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onToggleTarget: (String, Boolean) -> Unit,
    onAddTarget: (EditableCheckTarget) -> Unit,
    onResetDefaults: () -> Unit,
    onRemoveTarget: (String) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("https://") }
    var newGroup by remember { mutableStateOf(TargetGroup.FOREIGN) }
    var formError by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Настройки проверки", onBack = onBack) {
        TargetGroupSection(
            title = "Внешние сайты",
            targets = uiState.checkTargets.filter { it.group == TargetGroup.FOREIGN },
            onToggleTarget = onToggleTarget,
            onRemoveTarget = onRemoveTarget,
        )
        TargetGroupSection(
            title = "Локальные сайты",
            targets = uiState.checkTargets.filter { it.group == TargetGroup.LOCAL },
            onToggleTarget = onToggleTarget,
            onRemoveTarget = onRemoveTarget,
        )

        if (showAddForm) {
            AppCard(title = "Новый сайт") {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") },
                    singleLine = true,
                )
                Text("Группа", style = MaterialTheme.typography.labelMedium)
                TargetGroup.entries.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = newGroup == group,
                            onClick = { newGroup = group },
                        )
                        Text(if (group == TargetGroup.FOREIGN) "Внешние" else "Локальные")
                    }
                }
                formError?.let { ErrorCard(it) }
                Button(
                    onClick = {
                        val validation = validateTarget(newName, newUrl)
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
                    Text("Сохранить сайт")
                }
            }
        } else {
            OutlinedButton(onClick = { showAddForm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить сайт")
            }
        }

        OutlinedButton(onClick = onResetDefaults, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить сайты по умолчанию")
        }
    }
}

@Composable
private fun TargetGroupSection(
    title: String,
    targets: List<EditableCheckTarget>,
    onToggleTarget: (String, Boolean) -> Unit,
    onRemoveTarget: (String) -> Unit,
) {
    AppCard(title = title) {
        if (targets.isEmpty()) {
            Text("Нет сайтов в этой группе.", style = MaterialTheme.typography.bodySmall)
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
                    if (!target.builtIn) {
                        TextButton(onClick = { onRemoveTarget(target.id) }) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }
}

private fun validateTarget(name: String, url: String): String? {
    if (name.isBlank()) return "Название не может быть пустым"
    if (!url.startsWith("https://") && !url.startsWith("http://")) {
        return "URL должен начинаться с https:// или http://"
    }
    return null
}
