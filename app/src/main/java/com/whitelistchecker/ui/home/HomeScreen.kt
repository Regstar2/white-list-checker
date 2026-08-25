package com.whitelistchecker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.ui.components.ActionGrid
import com.whitelistchecker.ui.components.ActionGridItem
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.navigation.AppScreen

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onCheckMobileNetwork: () -> Unit,
    onOpenScreen: (AppScreen) -> Unit,
    onRefreshLastCheckPresentation: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeHeader(onOpenAbout = { onOpenScreen(AppScreen.ABOUT) })

            LastCheckResultCard(
                displayState = uiState.lastCheckDisplayState,
                onRefreshPresentation = onRefreshLastCheckPresentation,
                onOpenDetails = { onOpenScreen(AppScreen.DIAGNOSTICS) },
            )

            QuickActionsSection(onOpenScreen = onOpenScreen)

            uiState.errorMessage?.let { ErrorCard(it) }

            CheckMobileNetworkButton(
                isChecking = uiState.isChecking,
                onClick = onCheckMobileNetwork,
            )
        }
    }
}

@Composable
private fun HomeHeader(onOpenAbout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onOpenAbout,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_action_about),
                contentDescription = stringResource(R.string.home_action_about_title),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsSection(onOpenScreen: (AppScreen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ActionGrid(
            items = listOf(
                ActionGridItem(
                    titleRes = R.string.home_action_statistics_title,
                    subtitleRes = R.string.home_action_statistics_subtitle,
                    iconRes = R.drawable.ic_home_action_statistics,
                ) { onOpenScreen(AppScreen.STATISTICS) },
                ActionGridItem(
                    titleRes = R.string.home_action_checks_title,
                    subtitleRes = R.string.home_action_checks_subtitle,
                    iconRes = R.drawable.ic_home_action_checks,
                ) { onOpenScreen(AppScreen.CHECK_SETTINGS) },
                ActionGridItem(
                    titleRes = R.string.home_action_notifications_title,
                    subtitleRes = R.string.home_action_notifications_subtitle,
                    iconRes = R.drawable.ic_home_action_notifications,
                ) { onOpenScreen(AppScreen.NOTIFICATIONS) },
                ActionGridItem(
                    titleRes = R.string.home_action_auto_check_title,
                    subtitleRes = R.string.home_action_auto_check_subtitle,
                    iconRes = R.drawable.ic_home_action_schedule,
                ) { onOpenScreen(AppScreen.AUTO_CHECK) },
                ActionGridItem(
                    titleRes = R.string.home_action_diagnostics_title,
                    subtitleRes = R.string.home_action_diagnostics_subtitle,
                    iconRes = R.drawable.ic_home_action_diagnostics,
                ) { onOpenScreen(AppScreen.DIAGNOSTICS) },
                ActionGridItem(
                    titleRes = R.string.home_action_settings_title,
                    subtitleRes = R.string.home_action_settings_subtitle,
                    iconRes = R.drawable.ic_home_action_settings,
                ) { onOpenScreen(AppScreen.SETTINGS) },
            ),
        )
    }
}

@Composable
private fun CheckMobileNetworkButton(
    isChecking: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isChecking,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(stringResource(R.string.home_check_button_running))
        } else {
            Text(stringResource(R.string.home_check_button))
        }
    }
}
