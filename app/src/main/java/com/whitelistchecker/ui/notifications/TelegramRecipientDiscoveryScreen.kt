package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.ui.displayName
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun TelegramRecipientDiscoveryScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onPrepareChatDiscovery: () -> Unit,
    onFindChatId: () -> Unit,
    onFindRecentChats: () -> Unit,
    onResetChatDiscovery: () -> Unit,
    onAddRecipient: (TelegramChatCandidate) -> Unit,
) {
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    val discovery = uiState.telegramChatDiscovery
    val isBusy = discovery.isPreparing || discovery.isLoading || discovery.isLoadingRecent
    val selectedCandidate = discovery.candidates.firstOrNull { it.chatId == selectedChatId }

    LaunchedEffect(discovery.candidates) {
        if (selectedChatId == null && discovery.candidates.size == 1) {
            selectedChatId = discovery.candidates.first().chatId
        }
    }

    ScreenScaffold(title = stringResource(R.string.notifications_add_recipient_title), onBack = onBack) {
        AppCard(title = stringResource(R.string.notifications_add_recipient_steps_title)) {
            Text(
                text = stringResource(R.string.notifications_add_recipient_step_1),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.notifications_add_recipient_step_2),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    if (discovery.discoveryOffset == null) {
                        onPrepareChatDiscovery()
                    } else {
                        onFindChatId()
                    }
                },
                enabled = !isBusy && uiState.telegramSettings.isReadyForDiscovery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (discovery.discoveryOffset == null) {
                        stringResource(R.string.notifications_start_search)
                    } else {
                        stringResource(R.string.notifications_find_chats)
                    },
                )
            }
            TextButton(
                onClick = onFindRecentChats,
                enabled = !isBusy && uiState.telegramSettings.isReadyForDiscovery,
            ) {
                Text(stringResource(R.string.notifications_show_recent_chats))
            }
            if (discovery.discoveryOffset != null || discovery.candidates.isNotEmpty()) {
                TextButton(
                    onClick = {
                        selectedChatId = null
                        onResetChatDiscovery()
                    },
                    enabled = !isBusy,
                ) {
                    Text(stringResource(R.string.notifications_reset_search))
                }
            }
        }

        if (isBusy) {
            StatusLine(
                text = stringResource(R.string.notifications_searching_chats),
                tone = StatusTone.Neutral,
            )
            CircularProgressIndicator()
        }

        discovery.statusMessage?.let {
            StatusLine(text = it, tone = StatusTone.Success)
        }
        discovery.errorMessage?.let {
            StatusLine(
                text = stringResource(R.string.notifications_chat_search_error),
                tone = StatusTone.Error,
                detail = it,
            )
        }

        AppCard(title = stringResource(R.string.notifications_found_chats)) {
            if (discovery.candidates.isEmpty() && !isBusy) {
                Text(
                    text = stringResource(R.string.notifications_no_new_chats),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.notifications_no_new_chats_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                discovery.candidates.forEachIndexed { index, candidate ->
                    CandidateRow(
                        candidate = candidate,
                        selected = candidate.chatId == selectedChatId,
                        onSelect = { selectedChatId = candidate.chatId },
                    )
                    if (index != discovery.candidates.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
            Button(
                onClick = {
                    selectedCandidate?.let(onAddRecipient)
                },
                enabled = selectedCandidate != null && !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.notifications_add_selected))
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: TelegramChatCandidate,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = candidate.displayName(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            candidate.username?.let {
                Text(
                    text = "@$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = candidate.type.displayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            candidate.sourceMessageText?.let {
                Text(
                    text = stringResource(R.string.notifications_last_message, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
