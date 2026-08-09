package com.whitelistchecker.ui.diagnostics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.home.LastCheckAgeFormatter
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDescription
import com.whitelistchecker.ui.toDisplayDateTime

@Composable
fun DiagnosticsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    detailedReport: String,
    onLoadStatisticsDiagnostics: () -> Unit,
    onRebuildStatistics: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    ScreenScaffold(title = stringResource(R.string.diagnostics_title), onBack = onBack) {
        uiState.result?.let { result ->
            DiagnosticsResultCard(result)
            DiagnosticsSummaryCard(result)
            SiteResultsCard(result)
        } ?: EmptyCheckCard()

        MonitoringStatusCard(uiState.monitorState)

        uiState.lastStateChangeEvent?.let { event ->
            if (event.type != WhitelistStateChangeType.NO_CONFIRMED_CHANGE) {
                StateChangeEventCard(event)
            }
        }

        StatisticsDiagnosticsSection(
            uiState = uiState.statisticsDiagnosticsUiState,
            onLoad = onLoadStatisticsDiagnostics,
            onRebuildConfirmed = onRebuildStatistics,
        )

        OutlinedButton(
            onClick = { clipboardManager.setText(AnnotatedString(detailedReport)) },
            enabled = detailedReport.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
            )
            Text(stringResource(R.string.diagnostics_copy_report))
        }
    }
}

@Composable
private fun EmptyCheckCard() {
    AppCard(title = stringResource(R.string.diagnostics_current_result)) {
        Text(
            text = stringResource(R.string.diagnostics_no_checks_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.diagnostics_no_checks_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiagnosticsResultCard(result: NetworkCheckResult) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(result.checkedAtMillis) { System.currentTimeMillis() }
    val tone = result.state.toStatusTone()
    val accentColor = tone.accentColor()
    AppCard(
        title = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StatusIndicator(tone = tone, modifier = Modifier.padding(top = 4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = result.state.toDiagnosticsLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
                Text(
                    text = result.state.toDiagnosticsShortStatus(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.diagnostics_checked_ago,
                        LastCheckAgeFormatter.formatAge(resources, result.checkedAtMillis, nowMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.diagnostics_checked_via, result.checkedNetworkLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result.state.toDescription()?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                result.diagnosticsMessage?.let { diagnostics ->
                    DetailBlock(
                        label = stringResource(R.string.diagnostics_tcp_diagnostics),
                        value = diagnostics,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsSummaryCard(result: NetworkCheckResult) {
    AppCard(title = stringResource(R.string.diagnostics_summary)) {
        if (result.siteResults.isEmpty()) {
            Text(
                text = stringResource(R.string.diagnostics_sites_not_checked),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = result.error ?: stringResource(R.string.diagnostics_sites_not_checked_mobile_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SummaryMetricRow(
                left = SummaryMetric(result.checkedNetworkLabel, stringResource(R.string.diagnostics_checked_network)),
                right = SummaryMetric(result.activeNetworkLabel, stringResource(R.string.diagnostics_active_network)),
            )
        } else {
            SummaryMetricRow(
                left = SummaryMetric(
                    value = result.foreignSummary.countText(),
                    label = stringResource(R.string.diagnostics_foreign_available),
                ),
                right = SummaryMetric(
                    value = result.localSummary.countText(),
                    label = stringResource(R.string.diagnostics_local_available),
                ),
            )
            SummaryMetricRow(
                left = SummaryMetric(result.checkedNetworkLabel, stringResource(R.string.diagnostics_checked_network)),
                right = SummaryMetric(result.activeNetworkLabel, stringResource(R.string.diagnostics_active_network)),
            )
        }
    }
}

@Composable
private fun SiteResultsCard(result: NetworkCheckResult) {
    if (result.siteResults.isEmpty()) return
    val foreignResults = result.siteResults.filter { it.target.group == TargetGroup.FOREIGN }
    val localResults = result.siteResults.filter { it.target.group == TargetGroup.LOCAL }
    AppCard(title = stringResource(R.string.diagnostics_site_results)) {
        if (foreignResults.isNotEmpty()) {
            SiteGroupSection(
                title = stringResource(R.string.diagnostics_foreign_sites),
                summary = result.foreignSummary,
                sites = foreignResults,
                initiallyExpanded = foreignResults.any { !it.available },
            )
        }
        if (foreignResults.isNotEmpty() && localResults.isNotEmpty()) {
            HorizontalDivider()
        }
        if (localResults.isNotEmpty()) {
            SiteGroupSection(
                title = stringResource(R.string.diagnostics_local_sites),
                summary = result.localSummary,
                sites = localResults,
                initiallyExpanded = localResults.any { !it.available },
            )
        }
    }
}

@Composable
private fun SiteGroupSection(
    title: String,
    summary: TargetGroupSummary,
    sites: List<SiteCheckResult>,
    initiallyExpanded: Boolean,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusIndicator(
                tone = if (summary.availableCount == summary.totalCount) {
                    StatusTone.SUCCESS
                } else {
                    StatusTone.WARNING
                },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.diagnostics_available_of_total,
                    summary.availableCount,
                    summary.totalCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) {
                    stringResource(R.string.diagnostics_collapse_group)
                } else {
                    stringResource(R.string.diagnostics_expand_group)
                },
                modifier = Modifier.rotate(if (expanded) 90f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            sites.forEachIndexed { index, site ->
                if (index > 0) HorizontalDivider()
                SiteResultRow(site = site)
            }
        }
    }
}

@Composable
private fun SiteResultRow(site: SiteCheckResult) {
    var expanded by rememberSaveable(site.target.name, site.target.url) { mutableStateOf(false) }
    val tone = if (site.available) StatusTone.SUCCESS else site.errorType.toStatusTone()
    val resultText = site.toPrimaryResultText()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StatusIndicator(tone = tone, modifier = Modifier.padding(top = 6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.target.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.diagnostics_duration_ms, site.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            DetailBlock(stringResource(R.string.diagnostics_site_url), site.target.url)
            DetailBlock(stringResource(R.string.diagnostics_site_result), resultText)
            DetailBlock(
                stringResource(R.string.diagnostics_site_http),
                site.httpCode?.let { stringResource(R.string.diagnostics_http_code, it) }
                    ?: stringResource(R.string.diagnostics_not_available),
            )
            DetailBlock(
                stringResource(R.string.diagnostics_site_duration),
                stringResource(R.string.diagnostics_duration_ms, site.durationMs),
            )
            if (site.errorType != SiteCheckErrorType.NONE) {
                DetailBlock(
                    stringResource(R.string.diagnostics_site_technical_error_type),
                    site.errorType.toDisplayText(),
                )
            }
            site.error?.let { error ->
                DetailBlock(
                    label = stringResource(R.string.diagnostics_site_technical_error),
                    value = error,
                    muted = true,
                )
            }
        }
    }
}

@Composable
private fun MonitoringStatusCard(monitorState: WhitelistMonitorState?) {
    AppCard(title = stringResource(R.string.diagnostics_monitoring)) {
        if (monitorState == null ||
            (monitorState.lastConfirmedState == WhitelistState.UNKNOWN &&
                monitorState.pendingState == WhitelistState.UNKNOWN)
        ) {
            Text(
                text = stringResource(R.string.diagnostics_monitor_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AppCard
        }

        Text(
            text = stringResource(R.string.diagnostics_monitor_confirmed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StateLine(
            state = monitorState.lastConfirmedState,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = monitorState.lastConfirmedAtMillis?.toDisplayDateTime()
                ?: stringResource(R.string.diagnostics_not_available),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (monitorState.pendingState == WhitelistState.UNKNOWN) {
            Text(
                text = stringResource(R.string.diagnostics_monitor_no_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(R.string.diagnostics_monitor_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StateLine(
                state = monitorState.pendingState,
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            val required = StateChangeDetector.REQUIRED_CONFIRMATION_COUNT
            val count = monitorState.pendingStateCount.coerceAtLeast(0)
            Text(
                text = stringResource(R.string.diagnostics_monitor_confirmations, count, required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { (count.toFloat() / required.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = monitorState.pendingState.toStatusTone().accentColor(),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun StateChangeEventCard(event: WhitelistStateChangeEvent) {
    AppCard(title = stringResource(R.string.diagnostics_last_state_change)) {
        Text(
            text = event.type.toDiagnosticsEventTitle(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        DetailBlock(stringResource(R.string.diagnostics_event_old_state), event.oldState.toDiagnosticsLabel())
        DetailBlock(stringResource(R.string.diagnostics_event_new_state), event.newState.toDiagnosticsLabel())
        DetailBlock(stringResource(R.string.diagnostics_event_time), event.changedAtMillis.toDisplayDateTime())
    }
}

@Composable
private fun SummaryMetricRow(left: SummaryMetric, right: SummaryMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryMetricItem(metric = left, modifier = Modifier.weight(1f))
        SummaryMetricItem(metric = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryMetricItem(metric: SummaryMetric, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metric.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StateLine(
    state: WhitelistState,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIndicator(tone = state.toStatusTone())
        Text(
            text = state.toDiagnosticsLabel(),
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailBlock(
    label: String,
    value: String,
    muted: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (muted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusIndicator(
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val color = tone.accentColor()
    Surface(
        modifier = modifier.size(12.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
        contentColor = color,
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .background(color = color, shape = CircleShape),
        )
    }
}

@Composable
private fun StatusTone.accentColor(): Color {
    return when (this) {
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.primary
        StatusTone.WARNING -> MaterialTheme.colorScheme.tertiary
        StatusTone.ERROR -> MaterialTheme.colorScheme.error
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun WhitelistState.toDiagnosticsLabel(): String {
    return when (this) {
        WhitelistState.UNKNOWN -> stringResource(R.string.diagnostics_state_unknown)
        WhitelistState.WHITELIST_OFF -> stringResource(R.string.diagnostics_state_whitelist_off)
        WhitelistState.WHITELIST_ON -> stringResource(R.string.diagnostics_state_whitelist_on)
        WhitelistState.NO_MOBILE_INTERNET -> stringResource(R.string.diagnostics_state_no_mobile_internet)
        WhitelistState.MOBILE_DNS_FAILURE -> stringResource(R.string.diagnostics_state_mobile_dns_failure)
        WhitelistState.PARTIAL_PROBLEM -> stringResource(R.string.diagnostics_state_partial_problem)
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> stringResource(R.string.diagnostics_state_cellular_unavailable)
    }
}

@Composable
private fun WhitelistState.toDiagnosticsShortStatus(): String {
    return when (this) {
        WhitelistState.WHITELIST_OFF -> stringResource(R.string.diagnostics_short_status_ok)
        WhitelistState.WHITELIST_ON -> stringResource(R.string.diagnostics_short_status_possible_restriction)
        WhitelistState.NO_MOBILE_INTERNET -> stringResource(R.string.diagnostics_short_status_no_mobile_internet)
        WhitelistState.MOBILE_DNS_FAILURE -> stringResource(R.string.diagnostics_short_status_dns_problem)
        WhitelistState.PARTIAL_PROBLEM -> stringResource(R.string.diagnostics_short_status_partial_problem)
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> stringResource(R.string.diagnostics_short_status_cellular_unavailable)
        WhitelistState.UNKNOWN -> stringResource(R.string.diagnostics_short_status_unknown)
    }
}

private fun WhitelistState.toStatusTone(): StatusTone {
    return when (this) {
        WhitelistState.WHITELIST_OFF -> StatusTone.SUCCESS
        WhitelistState.WHITELIST_ON,
        WhitelistState.MOBILE_DNS_FAILURE,
        WhitelistState.PARTIAL_PROBLEM,
        -> StatusTone.WARNING
        WhitelistState.NO_MOBILE_INTERNET,
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
        -> StatusTone.ERROR
        WhitelistState.UNKNOWN -> StatusTone.NEUTRAL
    }
}

@Composable
private fun SiteCheckResult.toPrimaryResultText(): String {
    return when {
        available -> httpCode?.let { stringResource(R.string.diagnostics_http_code, it) }
            ?: stringResource(R.string.diagnostics_site_available)
        errorType != SiteCheckErrorType.NONE -> errorType.toDisplayText()
        else -> stringResource(R.string.diagnostics_site_unavailable)
    }
}

@Composable
private fun SiteCheckErrorType.toDisplayText(): String {
    return when (this) {
        SiteCheckErrorType.NONE -> stringResource(R.string.diagnostics_not_available)
        SiteCheckErrorType.DNS -> stringResource(R.string.diagnostics_site_error_dns)
        SiteCheckErrorType.TIMEOUT -> stringResource(R.string.diagnostics_site_error_timeout)
        SiteCheckErrorType.CONNECTION -> stringResource(R.string.diagnostics_site_error_connection)
        SiteCheckErrorType.TLS -> stringResource(R.string.diagnostics_site_error_tls)
        SiteCheckErrorType.HTTP -> stringResource(R.string.diagnostics_site_error_http)
        SiteCheckErrorType.UNKNOWN -> stringResource(R.string.diagnostics_site_error_unknown)
    }
}

private fun SiteCheckErrorType.toStatusTone(): StatusTone {
    return when (this) {
        SiteCheckErrorType.DNS,
        SiteCheckErrorType.TIMEOUT,
        SiteCheckErrorType.CONNECTION,
        -> StatusTone.WARNING
        SiteCheckErrorType.TLS,
        SiteCheckErrorType.HTTP,
        SiteCheckErrorType.UNKNOWN,
        -> StatusTone.ERROR
        SiteCheckErrorType.NONE -> StatusTone.NEUTRAL
    }
}

@Composable
private fun WhitelistStateChangeType.toDiagnosticsEventTitle(): String {
    return when (this) {
        WhitelistStateChangeType.WHITELIST_TURNED_ON -> stringResource(R.string.diagnostics_event_whitelist_turned_on)
        WhitelistStateChangeType.WHITELIST_TURNED_OFF -> stringResource(R.string.diagnostics_event_whitelist_turned_off)
        WhitelistStateChangeType.MANUAL_CHECK -> stringResource(R.string.diagnostics_event_manual_check)
        WhitelistStateChangeType.TEST_MESSAGE -> stringResource(R.string.diagnostics_event_test_message)
        WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> stringResource(R.string.diagnostics_event_other_confirmed_change)
        WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
    }
}

@Composable
private fun TargetGroupSummary.countText(): String {
    return stringResource(R.string.diagnostics_count_fraction, availableCount, totalCount)
}

private data class SummaryMetric(
    val value: String,
    val label: String,
)
