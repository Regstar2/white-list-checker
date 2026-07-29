package com.whitelistchecker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import kotlinx.coroutines.delay

@Composable
fun LastCheckResultCard(
    displayState: LastCheckDisplayState,
    onRefreshPresentation: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    LaunchedEffect(displayState) {
        while (true) {
            delay(PRESENTATION_REFRESH_INTERVAL_MS)
            onRefreshPresentation()
        }
    }

    val model = remember(displayState) {
        HomeLastCheckPresentationMapper.map(displayState)
    }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = model.tone.accentColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResultHeader(
                model = model,
                contentColor = contentColor,
                secondaryContentColor = secondaryContentColor,
                accentColor = accentColor,
            )

            if (model.localCount != null && model.foreignCount != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupCountRow(
                        label = stringResource(R.string.home_result_local_checks),
                        count = model.localCount,
                    )
                    GroupCountRow(
                        label = stringResource(R.string.home_result_foreign_checks),
                        count = model.foreignCount,
                    )
                }
            }

            ResultMeta(model = model, secondaryContentColor = secondaryContentColor)

            if (model.showDetails) {
                TextButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(stringResource(R.string.home_result_details))
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(
    model: HomeLastCheckUiModel,
    contentColor: Color,
    secondaryContentColor: Color,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.14f),
            contentColor = accentColor,
        ) {
            Icon(
                painter = painterResource(model.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(model.headlineRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            model.bodyRes?.let { bodyRes ->
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryContentColor,
                )
            }
        }
    }
}

@Composable
private fun GroupCountRow(
    label: String,
    count: HomeGroupCountUiModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.home_result_group_count,
                count.availableCount,
                count.totalCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResultMeta(
    model: HomeLastCheckUiModel,
    secondaryContentColor: Color,
) {
    val checkedAtMillis = model.checkedAtMillis
    val route = model.route
    val resources = LocalContext.current.resources
    var nowMillis by remember(checkedAtMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(checkedAtMillis) {
        if (checkedAtMillis == null) return@LaunchedEffect
        while (true) {
            delay(PRESENTATION_REFRESH_INTERVAL_MS)
            nowMillis = System.currentTimeMillis()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (checkedAtMillis != null) {
            val ageLabel = LastCheckAgeFormatter.formatAge(
                resources = resources,
                checkedAtMillis = checkedAtMillis,
                nowMillis = nowMillis,
            )
            Text(
                text = stringResource(R.string.home_result_checked_ago, ageLabel),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
            )
        }
        if (route != null) {
            Text(
                text = route.toDisplayText(),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
            )
        }
        if (model.stale) {
            StatusChip(
                text = stringResource(R.string.home_result_stale),
                tone = StatusTone.WARNING,
            )
        }
        model.error?.let { errorText ->
            Text(
                text = stringResource(R.string.home_result_error_detail, errorText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HomeNetworkRouteUiModel.toDisplayText(): String {
    return when {
        checkedNetwork != null && activeNetwork != null -> stringResource(
            textRes,
            checkedNetwork.toDisplayText(),
            activeNetwork.toDisplayText(),
        )
        else -> stringResource(textRes)
    }
}

@Composable
private fun HomeNetworkLabelUiModel.toDisplayText(): String {
    return when (this) {
        HomeNetworkLabelUiModel.MOBILE -> stringResource(R.string.home_network_mobile)
        HomeNetworkLabelUiModel.WIFI -> stringResource(R.string.home_network_wifi)
        HomeNetworkLabelUiModel.ETHERNET -> stringResource(R.string.home_network_ethernet)
        HomeNetworkLabelUiModel.UNKNOWN -> stringResource(R.string.home_network_unknown)
        is HomeNetworkLabelUiModel.Raw -> value
    }
}

@Composable
private fun HomeResultTone.accentColor(): Color {
    return when (this) {
        HomeResultTone.SUCCESS -> MaterialTheme.colorScheme.primary
        HomeResultTone.WARNING -> MaterialTheme.colorScheme.tertiary
        HomeResultTone.ERROR -> MaterialTheme.colorScheme.error
        HomeResultTone.NEUTRAL -> MaterialTheme.colorScheme.primary
    }
}

private const val PRESENTATION_REFRESH_INTERVAL_MS = 60_000L
