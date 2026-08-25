package com.whitelistchecker.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ActionGridItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    val fullWidth: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun ActionGrid(
    items: List<ActionGridItem>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val singleColumn = maxWidth < 360.dp
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEach { item ->
                    ActionGridCell(item = item, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            TwoColumnActionGrid(items = items)
        }
    }
}

@Composable
private fun TwoColumnActionGrid(items: List<ActionGridItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        var index = 0
        while (index < items.size) {
            val item = items[index]
            if (item.fullWidth) {
                ActionGridCell(item = item, modifier = Modifier.fillMaxWidth())
                index += 1
            } else {
                val next = items.getOrNull(index + 1)?.takeUnless { it.fullWidth }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionGridCell(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    if (next != null) {
                        ActionGridCell(
                            item = next,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
                index += if (next != null) 2 else 1
            }
        }
    }
}

@Composable
private fun ActionGridCell(
    item: ActionGridItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 120.dp)
            .clickable(
                role = Role.Button,
                onClick = item.onClick,
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(item.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
