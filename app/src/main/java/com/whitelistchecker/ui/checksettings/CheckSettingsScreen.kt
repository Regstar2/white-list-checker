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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.DnsServerProtocol
import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.EditableDnsServer
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
    onResetTargets: () -> Unit,
    onRemoveTarget: (String) -> Unit,
    onToggleDns: (String, Boolean) -> Unit,
    onAddDns: (EditableDnsServer) -> Unit,
    onResetDns: () -> Unit,
    onRemoveDns: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(TAB_SITES) }

    ScreenScaffold(title = stringResource(R.string.check_settings_title), onBack = onBack) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == TAB_SITES,
                onClick = { selectedTab = TAB_SITES },
                text = { Text(stringResource(R.string.check_settings_tab_sites)) },
            )
            Tab(
                selected = selectedTab == TAB_DNS,
                onClick = { selectedTab = TAB_DNS },
                text = { Text(stringResource(R.string.check_settings_tab_dns)) },
            )
        }

        if (selectedTab == TAB_SITES) {
            SitesSettings(
                targets = uiState.checkTargets,
                onToggleTarget = onToggleTarget,
                onAddTarget = onAddTarget,
                onResetTargets = onResetTargets,
                onRemoveTarget = onRemoveTarget,
            )
        } else {
            DnsSettings(
                servers = uiState.dnsServers,
                onToggleDns = onToggleDns,
                onAddDns = onAddDns,
                onResetDns = onResetDns,
                onRemoveDns = onRemoveDns,
            )
        }
    }
}

@Composable
private fun SitesSettings(
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

    TargetGroupSection(
        title = stringResource(R.string.check_settings_foreign_sites),
        targets = targets.filter { it.group == TargetGroup.FOREIGN },
        onToggleTarget = onToggleTarget,
        onRemoveTarget = onRemoveTarget,
    )
    TargetGroupSection(
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
            GroupSelector(selected = newGroup, onSelected = { newGroup = it })
            formError?.let { ErrorCard(stringResource(it)) }
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
private fun DnsSettings(
    servers: List<EditableDnsServer>,
    onToggleDns: (String, Boolean) -> Unit,
    onAddDns: (EditableDnsServer) -> Unit,
    onResetDns: () -> Unit,
    onRemoveDns: (String) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newPort by remember { mutableStateOf(EditableDnsServer.DEFAULT_DNS_PORT.toString()) }
    var newGroup by remember { mutableStateOf(TargetGroup.FOREIGN) }
    var formError by remember { mutableStateOf<Int?>(null) }
    val enabledCount = servers.count { it.enabled }

    DnsGroupSection(
        title = stringResource(R.string.check_settings_foreign_dns),
        servers = servers.filter { it.group == TargetGroup.FOREIGN },
        enabledCount = enabledCount,
        onToggleDns = onToggleDns,
        onRemoveDns = onRemoveDns,
    )
    DnsGroupSection(
        title = stringResource(R.string.check_settings_local_dns),
        servers = servers.filter { it.group == TargetGroup.LOCAL },
        enabledCount = enabledCount,
        onToggleDns = onToggleDns,
        onRemoveDns = onRemoveDns,
    )

    Text(
        text = stringResource(R.string.check_settings_last_dns_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (showAddForm) {
        AppCard(title = stringResource(R.string.check_settings_new_dns)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_name)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = newAddress,
                onValueChange = { newAddress = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_address)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = newPort,
                onValueChange = { value -> newPort = value.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_port)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = stringResource(R.string.check_settings_dns_protocol_raw),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.check_settings_protocol)) },
                enabled = false,
                singleLine = true,
            )
            GroupSelector(selected = newGroup, onSelected = { newGroup = it })
            formError?.let { ErrorCard(stringResource(it)) }
            Button(
                onClick = {
                    val port = newPort.toIntOrNull()
                    val validation = validateDns(
                        name = newName,
                        address = newAddress,
                        port = port,
                        servers = servers,
                    )
                    if (validation != null) {
                        formError = validation
                    } else {
                        onAddDns(
                            EditableDnsServer.create(
                                name = newName.trim(),
                                address = newAddress.trim(),
                                group = newGroup,
                                protocol = DnsServerProtocol.DNS_UDP_TCP,
                                port = requireNotNull(port),
                            ),
                        )
                        newName = ""
                        newAddress = ""
                        newPort = EditableDnsServer.DEFAULT_DNS_PORT.toString()
                        newGroup = TargetGroup.FOREIGN
                        formError = null
                        showAddForm = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.check_settings_save_dns))
            }
        }
    } else {
        OutlinedButton(onClick = { showAddForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.check_settings_add_dns))
        }
    }

    OutlinedButton(onClick = onResetDns, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.check_settings_reset_dns))
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
private fun DnsGroupSection(
    title: String,
    servers: List<EditableDnsServer>,
    enabledCount: Int,
    onToggleDns: (String, Boolean) -> Unit,
    onRemoveDns: (String) -> Unit,
) {
    AppCard(title = title) {
        if (servers.isEmpty()) {
            Text(stringResource(R.string.check_settings_no_dns), style = MaterialTheme.typography.bodySmall)
        } else {
            servers.forEach { server ->
                val canDisableOrRemove = !server.enabled || enabledCount > 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = server.enabled,
                        onCheckedChange = { onToggleDns(server.id, it) },
                        enabled = canDisableOrRemove,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(server.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${server.address}:${server.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.check_settings_dns_protocol_raw),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { onRemoveDns(server.id) },
                        enabled = canDisableOrRemove,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.check_settings_delete_dns),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSelector(
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
private fun validateTarget(name: String, url: String): Int? {
    if (name.isBlank()) return R.string.check_settings_error_name_empty
    if (!url.startsWith("https://") && !url.startsWith("http://")) {
        return R.string.check_settings_error_url_scheme
    }
    return null
}

@StringRes
private fun validateDns(
    name: String,
    address: String,
    port: Int?,
    servers: List<EditableDnsServer>,
): Int? {
    if (name.isBlank()) return R.string.check_settings_error_name_empty
    if (!isValidIpv4Literal(address)) return R.string.check_settings_error_dns_ipv4
    if (port == null || port !in 1..65535) return R.string.check_settings_error_port
    if (
        servers.any { server ->
            server.address == address.trim() &&
                server.port == port &&
                server.protocol == DnsServerProtocol.DNS_UDP_TCP
        }
    ) {
        return R.string.check_settings_error_dns_duplicate
    }
    return null
}

private fun isValidIpv4Literal(value: String): Boolean {
    val parts = value.trim().split('.')
    if (parts.size != 4) return false
    return parts.all { part ->
        val number = part.toIntOrNull()
        part.isNotEmpty() &&
            part.all(Char::isDigit) &&
            number != null && number in 0..255 &&
            (part == "0" || !part.startsWith('0'))
    }
}

private const val TAB_SITES = 0
private const val TAB_DNS = 1
