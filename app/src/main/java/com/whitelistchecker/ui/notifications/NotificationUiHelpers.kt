package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.TelegramChatType
import java.net.URI

@Composable
internal fun SettingsNavigationRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.notifications_open_details),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
internal fun StatusLine(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val color = when (tone) {
        StatusTone.Success -> MaterialTheme.colorScheme.primary
        StatusTone.Warning -> MaterialTheme.colorScheme.tertiary
        StatusTone.Error -> MaterialTheme.colorScheme.error
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = when (tone) {
                StatusTone.Success -> "✓"
                StatusTone.Warning -> "!"
                StatusTone.Error -> "!"
                StatusTone.Neutral -> "○"
            },
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (detail.isNullOrBlank()) text else "$text\n$detail",
            color = if (tone == StatusTone.Error) color else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

internal enum class StatusTone {
    Success,
    Warning,
    Error,
    Neutral,
}

internal fun extractHost(url: String): String? {
    if (url.isBlank()) return null
    return try {
        URI(url.trim()).host
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun TelegramChatType.displayLabel(): String = when (this) {
    TelegramChatType.PRIVATE -> stringResource(R.string.notifications_chat_type_private)
    TelegramChatType.GROUP -> stringResource(R.string.notifications_chat_type_group)
    TelegramChatType.SUPERGROUP -> stringResource(R.string.notifications_chat_type_supergroup)
    TelegramChatType.CHANNEL -> stringResource(R.string.notifications_chat_type_channel)
    TelegramChatType.UNKNOWN -> stringResource(R.string.notifications_chat_type_unknown)
}
