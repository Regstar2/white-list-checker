package com.whitelistchecker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.CompactPairRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDisplayLabel
import kotlinx.coroutines.delay

@Composable
fun LastCheckResultCard(
    displayState: LastCheckDisplayState,
    onRefreshPresentation: () -> Unit,
) {
    LaunchedEffect(displayState) {
        while (true) {
            delay(PRESENTATION_REFRESH_INTERVAL_MS)
            onRefreshPresentation()
        }
    }

    AppCard(title = "Последний результат") {
        when (displayState) {
            LastCheckDisplayState.NoCheck -> {
                Text(
                    text = stringResource(R.string.last_check_never_run),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LastCheckDisplayState.Running -> {
                Text(
                    text = stringResource(R.string.last_check_running),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LastCheckDisplayState.LoadError -> {
                Text(
                    text = stringResource(R.string.last_check_load_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is LastCheckDisplayState.Available -> {
                val result = displayState.result
                val resources = LocalContext.current.resources
                var nowMillis by remember(result.checkedAtMillis) {
                    mutableLongStateOf(System.currentTimeMillis())
                }
                LaunchedEffect(result.checkedAtMillis) {
                    while (true) {
                        delay(PRESENTATION_REFRESH_INTERVAL_MS)
                        nowMillis = System.currentTimeMillis()
                    }
                }
                val ageLabel = LastCheckAgeFormatter.formatAge(
                    resources = resources,
                    checkedAtMillis = result.checkedAtMillis,
                    nowMillis = nowMillis,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.last_check_title_with_age, ageLabel),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = when (displayState.outcome) {
                            LastCheckOutcome.SUCCESS -> stringResource(R.string.last_check_status_success)
                            LastCheckOutcome.FAILURE -> stringResource(R.string.last_check_status_failure)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (displayState.outcome) {
                            LastCheckOutcome.SUCCESS -> MaterialTheme.colorScheme.onSurfaceVariant
                            LastCheckOutcome.FAILURE -> MaterialTheme.colorScheme.error
                        },
                    )
                    if (displayState.freshness == LastCheckFreshness.STALE) {
                        StatusChip(
                            text = stringResource(R.string.last_check_data_stale),
                            tone = StatusTone.WARNING,
                        )
                    }
                    result.error?.let { errorText ->
                        Text(
                            text = stringResource(R.string.last_check_error_detail, errorText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(result.state.toDisplayLabel(), style = MaterialTheme.typography.titleMedium)
                    CompactPairRow(
                        leftLabel = "Внешние",
                        leftValue = "${result.foreignSummary.availableCount}/${result.foreignSummary.totalCount}",
                        rightLabel = "Локальные",
                        rightValue = "${result.localSummary.availableCount}/${result.localSummary.totalCount}",
                    )
                    CompactPairRow(
                        leftLabel = "Сеть",
                        leftValue = result.checkedNetworkLabel,
                        rightLabel = "Активная",
                        rightValue = result.activeNetworkLabel,
                    )
                    CompactDetailRow("Время", result.checkedAtMillis.toDisplayDateTime())
                }
            }
        }
    }
}

private const val PRESENTATION_REFRESH_INTERVAL_MS = 60_000L
