package com.whitelistchecker.ui.publicservice

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.OperatorSelectionMode
import com.whitelistchecker.domain.model.PublicServiceCatalog
import com.whitelistchecker.domain.model.PublicServiceRegistrationState
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime

@Composable
fun PublicServiceScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onShareReportsChange: (Boolean) -> Unit,
    onRemoteChecksChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDetectArea: () -> Unit,
    onConfirmDetectedArea: () -> Unit,
    onDismissDetectedArea: () -> Unit,
    onRegionChange: (String) -> Unit,
    onCityChange: (String?, String?) -> Unit,
    onClearCity: () -> Unit,
    onDetectOperator: () -> Unit,
    onUseAutoOperator: () -> Unit,
    onOperatorChange: (String) -> Unit,
    onDeviceAliasChange: (String) -> Unit,
    onSaveSettings: () -> Unit,
    onCreateLinkCode: () -> Unit,
    onRefreshLinks: () -> Unit,
    onRevokeLink: (String) -> Unit,
    onRetryReports: () -> Unit,
    onDeleteServerData: () -> Unit,
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showOperatorDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.public_service_title), onBack = onBack) {
        val settings = uiState.publicServiceSettings
        val status = uiState.publicServiceStatus

        AppCard(title = stringResource(R.string.public_service_stats_title)) {
            Text(
                text = stringResource(R.string.public_service_stats_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleRow(
                title = stringResource(R.string.public_service_share_reports_title),
                description = stringResource(R.string.public_service_share_reports_description),
                checked = settings.shareReports,
                onCheckedChange = onShareReportsChange,
            )
            CompactDetailRow(stringResource(R.string.public_service_registration), registrationStateText(settings.registrationState))
            CompactDetailRow(stringResource(R.string.public_service_last_upload), status.lastUploadAtMillis?.toDisplayDateTime() ?: "—")
            CompactDetailRow(stringResource(R.string.public_service_pending_reports), status.pendingReportCount.toString())
            status.lastUploadError?.let { CompactDetailRow(stringResource(R.string.public_service_upload_error), it) }
            OutlinedButton(onClick = onRetryReports, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.public_service_retry_reports))
            }
        }

        AppCard(title = stringResource(R.string.public_service_location_title)) {
            SettingsActionRow(
                title = stringResource(R.string.public_service_region),
                value = settings.regionName.ifBlank {
                    PublicServiceCatalog.regionByCode(settings.regionCode)?.label
                        ?: stringResource(R.string.public_service_not_selected)
                },
                meta = areaSourceText(settings.areaSource, settings.areaConfirmedByUser),
                onClick = { showRegionDialog = true },
            )
            SettingsActionRow(
                title = stringResource(R.string.public_service_city),
                value = settings.cityName ?: settings.customCityName ?: stringResource(R.string.public_service_not_selected),
                meta = if (settings.cityCode == null && settings.customCityName == null) {
                    stringResource(R.string.public_service_optional)
                } else {
                    areaSourceText(settings.areaSource, settings.areaConfirmedByUser)
                },
                onClick = { showCityDialog = true },
                enabled = settings.regionCode != "UNKNOWN",
            )
            Button(
                onClick = {
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        onDetectArea()
                    } else {
                        showLocationPermissionDialog = true
                    }
                },
                enabled = !uiState.isDetectingPublicServiceArea,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.isDetectingPublicServiceArea) {
                        stringResource(R.string.public_service_detecting)
                    } else {
                        stringResource(R.string.public_service_detect_location)
                    },
                )
            }
        }

        AppCard(title = stringResource(R.string.public_service_operator_title)) {
            SettingsActionRow(
                title = stringResource(R.string.public_service_operator),
                value = settings.selectedOperatorLabel,
                meta = operatorSourceText(settings.operatorSource, settings.operatorSelectionMode),
                onClick = { showOperatorDialog = true },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onUseAutoOperator,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.public_service_auto))
                }
                Button(
                    onClick = onDetectOperator,
                    enabled = !uiState.isDetectingPublicServiceOperator,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (uiState.isDetectingPublicServiceOperator) {
                            stringResource(R.string.public_service_detecting)
                        } else {
                            stringResource(R.string.public_service_detect)
                        },
                    )
                }
            }
        }

        AppCard(title = stringResource(R.string.public_service_remote_title)) {
            ToggleRow(
                title = stringResource(R.string.public_service_remote_checks_title),
                description = stringResource(R.string.public_service_remote_checks_description),
                checked = settings.allowRemoteChecks,
                onCheckedChange = onRemoteChecksChange,
            )
            Text(
                text = stringResource(R.string.public_service_remote_fgs_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = settings.deviceAlias,
                onValueChange = onDeviceAliasChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.public_service_device_alias)) },
                singleLine = true,
            )
            Button(
                onClick = onCreateLinkCode,
                enabled = !uiState.isCreatingPublicServiceLinkCode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_service_create_link_code))
            }
            status.lastLinkCode?.let { code ->
                CompactDetailRow(stringResource(R.string.public_service_link_code), code)
                LinkCodeCopyButton(code)
                CompactDetailRow(
                    stringResource(R.string.public_service_link_valid_until),
                    status.lastLinkCodeExpiresAtMillis?.toDisplayDateTime() ?: "—",
                )
                Text(
                    text = stringResource(R.string.public_service_link_instruction, code),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            status.lastLinkError?.let { CompactDetailRow(stringResource(R.string.public_service_link_error), it) }
            OutlinedButton(onClick = onRefreshLinks, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.public_service_refresh_links))
            }
            if (uiState.publicServiceLinks.isEmpty()) {
                Text(
                    text = stringResource(R.string.public_service_no_links),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.publicServiceLinks.forEach { link ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(link.deviceAlias, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = stringResource(R.string.public_service_telegram_chat, link.chatId.maskedChatId()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onRevokeLink(link.linkId) }) {
                            Text(stringResource(R.string.public_service_unlink))
                        }
                    }
                }
            }
            CompactDetailRow(
                stringResource(R.string.public_service_active_monitoring),
                activeMonitoringRemoteText(uiState.activeMonitoringStatus.state),
            )
            CompactDetailRow(stringResource(R.string.public_service_last_heartbeat), status.lastServiceSyncAtMillis?.toDisplayDateTime() ?: "—")
            status.lastServiceSyncError?.let { CompactDetailRow(stringResource(R.string.public_service_heartbeat_error), it) }
            status.lastRemoteCommandAtMillis?.let { CompactDetailRow(stringResource(R.string.public_service_last_command), it.toDisplayDateTime()) }
            status.lastRemoteCommandResult?.let { CompactDetailRow(stringResource(R.string.public_service_command_result), it) }
        }

        AppCard(title = stringResource(R.string.public_service_privacy_title)) {
            Text(
                text = stringResource(R.string.public_service_privacy_sent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.public_service_privacy_not_sent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSaveSettings,
                enabled = !uiState.isSavingPublicServiceSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_service_save_settings))
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !uiState.isDeletingPublicServiceData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_service_delete_server_data))
            }
        }

        uiState.publicServiceMessage?.let {
            AppCard(title = stringResource(R.string.public_service_state_title)) {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
    }

    uiState.pendingDetectedArea?.let { area ->
        AlertDialog(
            onDismissRequest = onDismissDetectedArea,
            title = { Text(stringResource(R.string.public_service_area_detected_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.public_service_detected_region, area.regionName))
                    Text(
                        stringResource(
                            R.string.public_service_detected_city,
                            area.cityName ?: stringResource(R.string.public_service_city_not_found),
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmDetectedArea) { Text(stringResource(R.string.public_service_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissDetectedArea) { Text(stringResource(R.string.public_service_change)) }
            },
        )
    }

    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionDialog = false },
            title = { Text(stringResource(R.string.public_service_location_permission_title)) },
            text = { Text(stringResource(R.string.public_service_location_permission_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationPermissionDialog = false
                        onRequestLocationPermission()
                    },
                ) { Text(stringResource(R.string.public_service_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPermissionDialog = false }) { Text(stringResource(R.string.public_service_cancel)) }
            },
        )
    }

    if (showRegionDialog) {
        RegionSelectorDialog(
            selectedCode = uiState.publicServiceSettings.regionCode,
            onDismiss = { showRegionDialog = false },
            onSelect = {
                showRegionDialog = false
                onRegionChange(it)
            },
        )
    }

    if (showCityDialog) {
        CitySelectorDialog(
            regionCode = uiState.publicServiceSettings.regionCode,
            selectedCode = uiState.publicServiceSettings.cityCode,
            onDismiss = { showCityDialog = false },
            onClear = {
                showCityDialog = false
                onClearCity()
            },
            onSelect = { cityCode, custom ->
                showCityDialog = false
                onCityChange(cityCode, custom)
            },
        )
    }

    if (showOperatorDialog) {
        OperatorSelectorDialog(
            selectedCode = uiState.publicServiceSettings.operatorCode,
            onDismiss = { showOperatorDialog = false },
            onSelect = {
                showOperatorDialog = false
                onOperatorChange(it)
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.public_service_delete_confirm_title)) },
            text = { Text(stringResource(R.string.public_service_delete_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteServerData()
                    },
                ) {
                    Text(stringResource(R.string.public_service_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.public_service_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    value: String,
    meta: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick, enabled = enabled) { Text(stringResource(R.string.public_service_change)) }
    }
}

@Composable
private fun RegionSelectorDialog(
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val items = remember(query) {
        PublicServiceCatalog.sortedRegions().filter { region ->
            val needle = PublicServiceCatalog.normalizeText(query)
            needle.isBlank() ||
                PublicServiceCatalog.normalizeText(region.label).contains(needle) ||
                region.aliases.any { PublicServiceCatalog.normalizeText(it).contains(needle) }
        }
    }
    SearchDialog(
        title = stringResource(R.string.public_service_select_region),
        query = query,
        onQueryChange = { query = it },
        onDismiss = onDismiss,
    ) {
        SelectorItem(stringResource(R.string.public_service_not_selected), selectedCode == "UNKNOWN") { onSelect("UNKNOWN") }
        items.forEach { region ->
            SelectorItem(region.label, selectedCode == region.code) { onSelect(region.code) }
        }
    }
}

@Composable
private fun CitySelectorDialog(
    regionCode: String,
    selectedCode: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSelect: (String?, String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var customCity by remember { mutableStateOf("") }
    val items = remember(regionCode, query) {
        PublicServiceCatalog.citiesForRegion(regionCode).filter { city ->
            val needle = PublicServiceCatalog.normalizeText(query)
            needle.isBlank() ||
                PublicServiceCatalog.normalizeText(city.label).contains(needle) ||
                city.aliases.any { PublicServiceCatalog.normalizeText(it).contains(needle) }
        }
    }
    SearchDialog(
        title = stringResource(R.string.public_service_select_city),
        query = query,
        onQueryChange = { query = it },
        onDismiss = onDismiss,
    ) {
        SelectorItem(stringResource(R.string.public_service_not_selected), selectedCode == null) { onClear() }
        items.forEach { city ->
            SelectorItem(city.label, selectedCode == city.code) { onSelect(city.code, null) }
        }
        Text(stringResource(R.string.public_service_custom_city_title), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = customCity,
            onValueChange = { customCity = it.take(64) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.public_service_custom_city_label)) },
        )
        Button(
            onClick = { onSelect(null, PublicServiceCatalog.sanitizeCustomCityName(customCity)) },
            enabled = customCity.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.public_service_save_custom_city))
        }
    }
}

@Composable
private fun OperatorSelectorDialog(
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val items = remember(query) {
        PublicServiceCatalog.operators.filterNot { it.code == "UNKNOWN" }.filter { operator ->
            val needle = PublicServiceCatalog.normalizeText(query)
            needle.isBlank() ||
                PublicServiceCatalog.normalizeText(operator.label).contains(needle) ||
                operator.aliases.any { PublicServiceCatalog.normalizeText(it).contains(needle) }
        }
    }
    SearchDialog(
        title = stringResource(R.string.public_service_select_operator),
        query = query,
        onQueryChange = { query = it },
        onDismiss = onDismiss,
    ) {
        SelectorItem(stringResource(R.string.public_service_not_selected), selectedCode == "UNKNOWN") { onSelect("UNKNOWN") }
        items.forEach { operator ->
            SelectorItem(operator.label, selectedCode == operator.code) { onSelect(operator.code) }
        }
    }
}

@Composable
private fun SearchDialog(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.public_service_search)) },
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.public_service_close)) }
        },
    )
}

@Composable
private fun SelectorItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) stringResource(R.string.public_service_selected_item, text) else text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun areaSourceText(source: AreaSource, confirmed: Boolean): String =
    when (source) {
        AreaSource.AUTOMATIC_LOCATION -> if (confirmed) {
            stringResource(R.string.public_service_area_auto_confirmed)
        } else {
            stringResource(R.string.public_service_area_pending_confirmation)
        }
        AreaSource.MANUAL_SELECTION -> stringResource(R.string.public_service_manual_selection)
    }

@Composable
private fun operatorSourceText(source: OperatorDetectionSource, mode: OperatorSelectionMode): String =
    when {
        mode == OperatorSelectionMode.MANUAL -> stringResource(R.string.public_service_manual_selection)
        source == OperatorDetectionSource.NETWORK_OPERATOR -> stringResource(R.string.public_service_operator_network)
        source == OperatorDetectionSource.SIM_OPERATOR -> stringResource(R.string.public_service_operator_sim_fallback)
        else -> stringResource(R.string.public_service_operator_auto_mode)
    }

@Composable
private fun activeMonitoringRemoteText(state: ActiveMonitoringState): String {
    return when (state) {
        ActiveMonitoringState.RUNNING,
        ActiveMonitoringState.CHECKING,
        -> stringResource(R.string.public_service_monitoring_running)
        ActiveMonitoringState.STARTING -> stringResource(R.string.public_service_monitoring_starting)
        ActiveMonitoringState.STOPPING -> stringResource(R.string.public_service_monitoring_stopping)
        ActiveMonitoringState.STOPPED_BY_SYSTEM -> stringResource(R.string.public_service_monitoring_stopped_by_system)
        ActiveMonitoringState.ERROR -> stringResource(R.string.public_service_monitoring_error)
        ActiveMonitoringState.STOPPED -> stringResource(R.string.public_service_monitoring_stopped)
    }
}

@Composable
private fun registrationStateText(state: PublicServiceRegistrationState): String =
    when (state) {
        PublicServiceRegistrationState.NOT_REGISTERED -> stringResource(R.string.public_service_registration_not_registered)
        PublicServiceRegistrationState.REGISTERING -> stringResource(R.string.public_service_registration_registering)
        PublicServiceRegistrationState.REGISTERED -> stringResource(R.string.public_service_registration_registered)
        PublicServiceRegistrationState.REVOKED -> stringResource(R.string.public_service_registration_revoked)
        PublicServiceRegistrationState.ERROR -> stringResource(R.string.public_service_registration_error)
    }

private fun String.maskedChatId(): String =
    when {
        length <= 4 -> "****"
        else -> "...${takeLast(4)}"
    }
