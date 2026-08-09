package com.whitelistchecker.ui.publicservice

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.OperatorSelectionMode
import com.whitelistchecker.domain.model.PublicServiceCatalog
import com.whitelistchecker.domain.model.PublicServiceLink
import com.whitelistchecker.domain.model.PublicServiceRegistrationState
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.PublicServiceStatus
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime

private enum class PublicServiceTab {
    STATISTICS,
    TELEGRAM,
}

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
    var selectedTab by rememberSaveable { mutableStateOf(PublicServiceTab.STATISTICS) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showOperatorDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.public_service_title), onBack = onBack) {
        TabRow(selectedTabIndex = PublicServiceTab.entries.indexOf(selectedTab)) {
            PublicServiceTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(stringResource(tab.titleRes())) },
                )
            }
        }

        when (selectedTab) {
            PublicServiceTab.STATISTICS -> StatisticsTab(
                uiState = uiState,
                onShareReportsChange = onShareReportsChange,
                onDetectAutomatically = {
                    onUseAutoOperator()
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        onDetectArea()
                    } else {
                        showLocationPermissionDialog = true
                    }
                },
                onOpenRegion = { showRegionDialog = true },
                onOpenCity = { showCityDialog = true },
                onOpenOperator = { showOperatorDialog = true },
                onRetryReports = onRetryReports,
                onOpenPrivacy = { showPrivacyDialog = true },
                onSaveSettings = onSaveSettings,
                onDeleteServerData = { showDeleteDialog = true },
            )
            PublicServiceTab.TELEGRAM -> TelegramTab(
                uiState = uiState,
                onRemoteChecksChange = onRemoteChecksChange,
                onDeviceAliasChange = onDeviceAliasChange,
                onCreateLinkCode = onCreateLinkCode,
                onRefreshLinks = onRefreshLinks,
                onRevokeLink = onRevokeLink,
            )
        }

        uiState.publicServiceMessage?.let {
            MessageCard(message = it)
        }
        uiState.errorMessage?.let { ErrorCard(it) }
    }

    PublicServiceDialogs(
        uiState = uiState,
        showDeleteDialog = showDeleteDialog,
        showLocationPermissionDialog = showLocationPermissionDialog,
        showPrivacyDialog = showPrivacyDialog,
        showRegionDialog = showRegionDialog,
        showCityDialog = showCityDialog,
        showOperatorDialog = showOperatorDialog,
        onDismissDelete = { showDeleteDialog = false },
        onDismissLocationPermission = { showLocationPermissionDialog = false },
        onDismissPrivacy = { showPrivacyDialog = false },
        onDismissRegion = { showRegionDialog = false },
        onDismissCity = { showCityDialog = false },
        onDismissOperator = { showOperatorDialog = false },
        onRequestLocationPermission = onRequestLocationPermission,
        onDeleteServerData = onDeleteServerData,
        onConfirmDetectedArea = onConfirmDetectedArea,
        onDismissDetectedArea = onDismissDetectedArea,
        onRegionChange = onRegionChange,
        onCityChange = onCityChange,
        onClearCity = onClearCity,
        onOperatorChange = onOperatorChange,
    )
}

@Composable
private fun StatisticsTab(
    uiState: MainUiState,
    onShareReportsChange: (Boolean) -> Unit,
    onDetectAutomatically: () -> Unit,
    onOpenRegion: () -> Unit,
    onOpenCity: () -> Unit,
    onOpenOperator: () -> Unit,
    onRetryReports: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSaveSettings: () -> Unit,
    onDeleteServerData: () -> Unit,
) {
    val settings = uiState.publicServiceSettings

    SectionTitle(R.string.public_service_statistics_sending)
    AppCard(title = null) {
        ToggleRow(
            title = stringResource(R.string.public_service_share_reports_title),
            description = stringResource(R.string.public_service_share_reports_compact_description),
            checked = settings.shareReports,
            onCheckedChange = onShareReportsChange,
        )
    }

    SectionTitle(R.string.public_service_upload_state_title)
    PublicServiceUploadStatus(
        settings = settings,
        status = uiState.publicServiceStatus,
        onRetryReports = onRetryReports,
    )

    SectionTitle(R.string.public_service_stats_data_title)
    AppCard(title = null) {
        PublicServiceSettingRow(
            label = stringResource(R.string.public_service_region),
            value = settings.regionDisplayName(),
            meta = areaSourceText(settings.areaSource, settings.areaConfirmedByUser),
            onClick = onOpenRegion,
        )
        HorizontalDivider()
        PublicServiceSettingRow(
            label = stringResource(R.string.public_service_city),
            value = settings.cityDisplayName(),
            meta = if (settings.cityCode == null && settings.customCityName == null) {
                stringResource(R.string.public_service_optional)
            } else {
                areaSourceText(settings.areaSource, settings.areaConfirmedByUser)
            },
            onClick = onOpenCity,
            enabled = settings.regionCode != PublicServiceSettings.DEFAULT_REGION_CODE,
        )
        HorizontalDivider()
        PublicServiceSettingRow(
            label = stringResource(R.string.public_service_operator),
            value = settings.selectedOperatorLabel,
            meta = operatorSourceText(settings.operatorSource, settings.operatorSelectionMode),
            onClick = onOpenOperator,
        )
    }

    OutlinedButton(
        onClick = onDetectAutomatically,
        enabled = !uiState.isDetectingPublicServiceArea && !uiState.isDetectingPublicServiceOperator,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (uiState.isDetectingPublicServiceArea || uiState.isDetectingPublicServiceOperator) {
                stringResource(R.string.public_service_detecting)
            } else {
                stringResource(R.string.public_service_detect_automatically)
            },
        )
    }

    TextButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.public_service_privacy_details_action))
    }

    Button(
        onClick = onSaveSettings,
        enabled = !uiState.isSavingPublicServiceSettings,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.public_service_save_changes))
    }

    SectionTitle(R.string.public_service_danger_zone)
    AppCard(
        title = null,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(R.string.public_service_danger_zone_description),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = onDeleteServerData,
            enabled = !uiState.isDeletingPublicServiceData,
        ) {
            Text(stringResource(R.string.public_service_delete_server_data))
        }
    }
}

@Composable
private fun PublicServiceUploadStatus(
    settings: PublicServiceSettings,
    status: PublicServiceStatus,
    onRetryReports: () -> Unit,
) {
    val registrationNeedsAttention = settings.registrationState != PublicServiceRegistrationState.REGISTERED &&
        (settings.shareReports || settings.allowRemoteChecks || settings.registrationState != PublicServiceRegistrationState.NOT_REGISTERED)
    val hasError = !status.lastUploadError.isNullOrBlank()
    val hasQueue = status.pendingReportCount > 0

    AppCard(title = null) {
        CompactDetailRow(
            label = stringResource(R.string.public_service_last_upload),
            value = status.lastUploadAtMillis?.toDisplayDateTime() ?: stringResource(R.string.public_service_no_value),
        )
        if (hasQueue) {
            CompactDetailRow(
                label = stringResource(R.string.public_service_pending_reports_compact),
                value = status.pendingReportCount.toString(),
            )
        }
        if (registrationNeedsAttention) {
            CompactWarning(
                title = stringResource(R.string.public_service_registration_attention),
                message = registrationStateText(settings.registrationState),
            )
        }
        if (hasError) {
            CompactWarning(
                title = stringResource(R.string.public_service_upload_failed_title),
                message = status.lastUploadError.orEmpty(),
            )
        }
        if (hasQueue || hasError) {
            TextButton(onClick = onRetryReports) {
                Text(stringResource(R.string.public_service_retry_short))
            }
        }
    }
}

@Composable
private fun TelegramTab(
    uiState: MainUiState,
    onRemoteChecksChange: (Boolean) -> Unit,
    onDeviceAliasChange: (String) -> Unit,
    onCreateLinkCode: () -> Unit,
    onRefreshLinks: () -> Unit,
    onRevokeLink: (String) -> Unit,
) {
    val settings = uiState.publicServiceSettings
    val links = uiState.publicServiceLinks

    SectionTitle(R.string.public_service_remote_title)
    AppCard(title = null) {
        ToggleRow(
            title = stringResource(R.string.public_service_remote_checks_short_title),
            description = stringResource(R.string.public_service_remote_checks_description),
            checked = settings.allowRemoteChecks,
            onCheckedChange = onRemoteChecksChange,
        )
    }

    TelegramLinksSection(
        uiState = uiState,
        links = links,
        onDeviceAliasChange = onDeviceAliasChange,
        onCreateLinkCode = onCreateLinkCode,
        onRefreshLinks = onRefreshLinks,
        onRevokeLink = onRevokeLink,
    )

    SectionTitle(R.string.public_service_device_state_title)
    DeviceStateCard(
        activeState = uiState.activeMonitoringStatus.state,
        status = uiState.publicServiceStatus,
    )
}

@Composable
private fun TelegramLinksSection(
    uiState: MainUiState,
    links: List<PublicServiceLink>,
    onDeviceAliasChange: (String) -> Unit,
    onCreateLinkCode: () -> Unit,
    onRefreshLinks: () -> Unit,
    onRevokeLink: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            title = stringResource(
                if (links.isEmpty()) {
                    R.string.public_service_linking_title
                } else {
                    R.string.public_service_linked_chats_title
                },
            ),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRefreshLinks) {
            Text(stringResource(R.string.public_service_refresh_short))
        }
    }

    if (links.isEmpty()) {
        AppCard(title = null) {
            Text(
                text = stringResource(R.string.public_service_telegram_not_linked),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.public_service_telegram_not_linked_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeviceAliasField(
                value = uiState.publicServiceSettings.deviceAlias,
                onValueChange = onDeviceAliasChange,
            )
            Button(
                onClick = onCreateLinkCode,
                enabled = !uiState.isCreatingPublicServiceLinkCode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_service_create_link_code))
            }
            LinkCodeBlock(status = uiState.publicServiceStatus)
        }
    } else {
        links.forEach { link ->
            TelegramLinkCard(link = link, onRevokeLink = onRevokeLink)
        }
        AppCard(title = null) {
            Text(
                text = stringResource(R.string.public_service_add_link_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeviceAliasField(
                value = uiState.publicServiceSettings.deviceAlias,
                onValueChange = onDeviceAliasChange,
            )
            OutlinedButton(
                onClick = onCreateLinkCode,
                enabled = !uiState.isCreatingPublicServiceLinkCode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_service_create_new_link_code))
            }
            LinkCodeBlock(status = uiState.publicServiceStatus)
        }
    }

    uiState.publicServiceStatus.lastLinkError?.takeIf { it.isNotBlank() }?.let {
        CompactWarning(
            title = stringResource(R.string.public_service_link_error),
            message = it,
        )
    }
}

@Composable
private fun DeviceAliasField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.public_service_device_alias)) },
        singleLine = true,
    )
}

@Composable
private fun LinkCodeBlock(status: PublicServiceStatus) {
    val code = status.lastLinkCode?.takeIf { it.isNotBlank() } ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.public_service_link_code_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                LinkCodeCopyButton(code = code, compact = true)
            }
            status.lastLinkCodeExpiresAtMillis?.let {
                Text(
                    text = stringResource(R.string.public_service_link_valid_until_value, it.toDisplayDateTime()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.public_service_link_instruction, code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TelegramLinkCard(
    link: PublicServiceLink,
    onRevokeLink: (String) -> Unit,
) {
    AppCard(title = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = link.deviceAlias,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.public_service_telegram_chat, link.chatId.maskedChatId()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.public_service_link_connected),
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

@Composable
private fun DeviceStateCard(
    activeState: ActiveMonitoringState,
    status: PublicServiceStatus,
) {
    AppCard(title = null) {
        if (activeState.canServeRemoteChecks()) {
            CompactDetailRow(
                label = stringResource(R.string.public_service_active_monitoring),
                value = activeMonitoringRemoteText(activeState),
            )
        } else {
            CompactWarning(
                title = stringResource(R.string.public_service_remote_unavailable_title),
                message = stringResource(R.string.public_service_remote_unavailable_message),
            )
            CompactDetailRow(
                label = stringResource(R.string.public_service_active_monitoring),
                value = activeMonitoringRemoteText(activeState),
            )
        }
        status.lastServiceSyncAtMillis?.let {
            CompactDetailRow(
                label = stringResource(R.string.public_service_last_heartbeat),
                value = it.toDisplayDateTime(),
            )
        }
        status.lastServiceSyncError?.takeIf { it.isNotBlank() }?.let {
            CompactWarning(
                title = stringResource(R.string.public_service_heartbeat_error),
                message = it,
            )
        }
        status.lastRemoteCommandAtMillis?.let {
            CompactDetailRow(
                label = stringResource(R.string.public_service_last_command),
                value = it.toDisplayDateTime(),
            )
        }
        status.lastRemoteCommandResult?.takeIf { it.isNotBlank() }?.let {
            CompactDetailRow(
                label = stringResource(R.string.public_service_command_result),
                value = it,
            )
        }
    }
}

@Composable
private fun PublicServiceSettingRow(
    label: String,
    value: String,
    meta: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.public_service_row_affordance),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.padding(start = 8.dp),
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
private fun CompactWarning(
    title: String,
    message: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun SectionTitle(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    SectionTitle(title = stringResource(titleRes), modifier = modifier)
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
    )
}

@Composable
private fun PublicServiceDialogs(
    uiState: MainUiState,
    showDeleteDialog: Boolean,
    showLocationPermissionDialog: Boolean,
    showPrivacyDialog: Boolean,
    showRegionDialog: Boolean,
    showCityDialog: Boolean,
    showOperatorDialog: Boolean,
    onDismissDelete: () -> Unit,
    onDismissLocationPermission: () -> Unit,
    onDismissPrivacy: () -> Unit,
    onDismissRegion: () -> Unit,
    onDismissCity: () -> Unit,
    onDismissOperator: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDeleteServerData: () -> Unit,
    onConfirmDetectedArea: () -> Unit,
    onDismissDetectedArea: () -> Unit,
    onRegionChange: (String) -> Unit,
    onCityChange: (String?, String?) -> Unit,
    onClearCity: () -> Unit,
    onOperatorChange: (String) -> Unit,
) {
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
                TextButton(onClick = onConfirmDetectedArea) {
                    Text(stringResource(R.string.public_service_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDetectedArea) {
                    Text(stringResource(R.string.public_service_change))
                }
            },
        )
    }

    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = onDismissLocationPermission,
            title = { Text(stringResource(R.string.public_service_location_permission_title)) },
            text = { Text(stringResource(R.string.public_service_location_permission_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissLocationPermission()
                        onRequestLocationPermission()
                    },
                ) {
                    Text(stringResource(R.string.public_service_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLocationPermission) {
                    Text(stringResource(R.string.public_service_cancel))
                }
            },
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = onDismissPrivacy)
    }

    if (showRegionDialog) {
        RegionSelectorDialog(
            selectedCode = uiState.publicServiceSettings.regionCode,
            onDismiss = onDismissRegion,
            onSelect = {
                onDismissRegion()
                onRegionChange(it)
            },
        )
    }

    if (showCityDialog) {
        CitySelectorDialog(
            regionCode = uiState.publicServiceSettings.regionCode,
            selectedCode = uiState.publicServiceSettings.cityCode,
            onDismiss = onDismissCity,
            onClear = {
                onDismissCity()
                onClearCity()
            },
            onSelect = { cityCode, custom ->
                onDismissCity()
                onCityChange(cityCode, custom)
            },
        )
    }

    if (showOperatorDialog) {
        OperatorSelectorDialog(
            selectedCode = uiState.publicServiceSettings.operatorCode,
            onDismiss = onDismissOperator,
            onSelect = {
                onDismissOperator()
                onOperatorChange(it)
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.public_service_delete_confirm_title)) },
            text = { Text(stringResource(R.string.public_service_delete_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissDelete()
                        onDeleteServerData()
                    },
                ) {
                    Text(stringResource(R.string.public_service_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.public_service_cancel))
                }
            },
        )
    }
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.public_service_privacy_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.public_service_privacy_sent_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.public_service_privacy_sent_details),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.public_service_privacy_not_sent_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.public_service_privacy_not_sent_details),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.public_service_close))
            }
        },
    )
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
        SelectorItem(stringResource(R.string.public_service_not_selected), selectedCode == PublicServiceSettings.DEFAULT_REGION_CODE) {
            onSelect(PublicServiceSettings.DEFAULT_REGION_CODE)
        }
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
        PublicServiceCatalog.operators.filterNot { it.code == PublicServiceSettings.DEFAULT_OPERATOR_CODE }.filter { operator ->
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
        SelectorItem(stringResource(R.string.public_service_not_selected), selectedCode == PublicServiceSettings.DEFAULT_OPERATOR_CODE) {
            onSelect(PublicServiceSettings.DEFAULT_OPERATOR_CODE)
        }
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.public_service_close))
            }
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

@StringRes
private fun PublicServiceTab.titleRes(): Int {
    return when (this) {
        PublicServiceTab.STATISTICS -> R.string.public_service_tab_statistics
        PublicServiceTab.TELEGRAM -> R.string.public_service_tab_telegram
    }
}

@Composable
private fun PublicServiceSettings.regionDisplayName(): String {
    return regionName.ifBlank {
        PublicServiceCatalog.regionByCode(regionCode)?.label
            ?: stringResource(R.string.public_service_not_selected)
    }
}

@Composable
private fun PublicServiceSettings.cityDisplayName(): String {
    return cityName ?: customCityName ?: stringResource(R.string.public_service_not_selected)
}

@Composable
private fun areaSourceText(source: AreaSource, confirmed: Boolean): String {
    return when (source) {
        AreaSource.AUTOMATIC_LOCATION -> if (confirmed) {
            stringResource(R.string.public_service_area_auto_confirmed)
        } else {
            stringResource(R.string.public_service_area_pending_confirmation)
        }
        AreaSource.MANUAL_SELECTION -> stringResource(R.string.public_service_manual_selection)
    }
}

@Composable
private fun operatorSourceText(source: OperatorDetectionSource, mode: OperatorSelectionMode): String {
    return when {
        mode == OperatorSelectionMode.MANUAL -> stringResource(R.string.public_service_manual_selection)
        source == OperatorDetectionSource.NETWORK_OPERATOR -> stringResource(R.string.public_service_operator_network)
        source == OperatorDetectionSource.SIM_OPERATOR -> stringResource(R.string.public_service_operator_sim_fallback)
        else -> stringResource(R.string.public_service_operator_auto_mode)
    }
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
private fun registrationStateText(state: PublicServiceRegistrationState): String {
    return when (state) {
        PublicServiceRegistrationState.NOT_REGISTERED -> stringResource(R.string.public_service_registration_not_registered)
        PublicServiceRegistrationState.REGISTERING -> stringResource(R.string.public_service_registration_registering)
        PublicServiceRegistrationState.REGISTERED -> stringResource(R.string.public_service_registration_registered)
        PublicServiceRegistrationState.REVOKED -> stringResource(R.string.public_service_registration_revoked)
        PublicServiceRegistrationState.ERROR -> stringResource(R.string.public_service_registration_error)
    }
}

private fun ActiveMonitoringState.canServeRemoteChecks(): Boolean {
    return this == ActiveMonitoringState.RUNNING || this == ActiveMonitoringState.CHECKING
}

private fun String.maskedChatId(): String {
    return when {
        length <= 4 -> "****"
        else -> "...${takeLast(4)}"
    }
}
