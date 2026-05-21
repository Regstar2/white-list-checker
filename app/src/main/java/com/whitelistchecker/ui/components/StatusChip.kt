package com.whitelistchecker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class StatusTone {
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL,
}

@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (tone) {
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        StatusTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when (tone) {
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
