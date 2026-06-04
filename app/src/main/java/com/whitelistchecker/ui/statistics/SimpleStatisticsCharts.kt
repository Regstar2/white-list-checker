package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalBarChart(
    label: String,
    value: Float,
    maxValue: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val safeMax = if (maxValue > 0f) maxValue else 1f
    val fraction = (value / safeMax).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Text(
                text = formatChartValue(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 4.dp),
        ) {
            val barWidth = size.width * fraction
            drawRoundRect(
                color = trackColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
                color = barColor,
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
fun DailyPercentBars(
    labels: List<String>,
    values: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.zip(values).forEach { (label, value) ->
            HorizontalBarChart(
                label = label,
                value = value,
                maxValue = max,
                barColor = barColor,
            )
        }
    }
}

private fun formatChartValue(value: Float): String {
    return if (value == value.toInt().toFloat()) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
