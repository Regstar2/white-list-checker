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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R

@Composable
fun ChartInsufficientDataMessage(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.statistics_chart_insufficient_data),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun PercentBarChart(
    label: String,
    percent: Float,
    maxPercent: Float = 100f,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val safeMax = maxPercent.coerceAtLeast(1f)
    val fraction = (percent / safeMax).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val valueLabel = StatisticsValueFormatter.formatPercentValue(percent.toDouble())
    Column(modifier = modifier.fillMaxWidth()) {
        BarHeader(label = label, valueLabel = valueLabel)
        BarCanvas(fraction = fraction, barColor = barColor, trackColor = trackColor)
    }
}

@Composable
fun CountBarChart(
    label: String,
    count: Int,
    maxCount: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val safeMax = maxCount.coerceAtLeast(1f)
    val fraction = (count.toFloat() / safeMax).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    Column(modifier = modifier.fillMaxWidth()) {
        BarHeader(label = label, valueLabel = count.toString())
        BarCanvas(fraction = fraction, barColor = barColor, trackColor = trackColor)
    }
}

@Composable
fun DailyPercentBars(
    labels: List<String>,
    percents: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty() || !hasMeaningfulPercentChart(percents)) {
        ChartInsufficientDataMessage(modifier = modifier)
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.zip(percents).forEach { (label, percent) ->
            PercentBarChart(label = label, percent = percent, barColor = barColor)
        }
    }
}

fun hasMeaningfulPercentChart(values: List<Float>): Boolean {
    return values.size >= 2
}

fun hasMeaningfulCountChart(values: List<Int>): Boolean {
    if (values.isEmpty()) return false
    if (values.size < 2) return false
    if (values.distinct().size <= 1 && values.all { it <= 1 }) return false
    return true
}

fun allCountValuesEqual(values: List<Int>): Boolean {
    return values.isNotEmpty() && values.distinct().size <= 1
}

fun allPercentValuesAtLeast(values: List<Float>, threshold: Float): Boolean {
    return values.isNotEmpty() && values.all { it >= threshold }
}

@Composable
private fun BarHeader(label: String, valueLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarCanvas(fraction: Float, barColor: Color, trackColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(top = 4.dp),
    ) {
        drawRoundRect(
            color = trackColor,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(4f, 4f),
        )
        drawRoundRect(
            color = barColor,
            size = Size(size.width * fraction, size.height),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }
}
