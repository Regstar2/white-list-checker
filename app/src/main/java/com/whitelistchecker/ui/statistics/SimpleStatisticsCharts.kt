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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineBucket

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
    valueLabel: String? = null,
) {
    val safeMax = maxCount.coerceAtLeast(1f)
    val fraction = (count.toFloat() / safeMax).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    Column(modifier = modifier.fillMaxWidth()) {
        BarHeader(label = label, valueLabel = valueLabel ?: count.toString())
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

@Composable
fun DailyTrendChart(
    labels: List<String>,
    percents: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val values = labels.zip(percents)
        .filter { (_, value) -> value.isFinite() }
        .takeLast(MAX_TREND_POINTS)
    if (values.size < 2) {
        ChartInsufficientDataMessage(modifier = modifier)
        return
    }

    val chartLabels = values.map { it.first }
    val safePercents = values.map { it.second.coerceIn(0f, 100f) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val fillColor = lineColor.copy(alpha = 0.14f)
    val pointColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        ) {
            val topPadding = 10.dp.toPx()
            val bottomPadding = 18.dp.toPx()
            val horizontalPadding = 4.dp.toPx()
            val chartHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val chartWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
            val baselineY = topPadding + chartHeight

            listOf(0f, 0.5f, 1f).forEach { marker ->
                val y = baselineY - chartHeight * marker
                drawLine(
                    color = gridColor,
                    start = Offset(horizontalPadding, y),
                    end = Offset(horizontalPadding + chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val points = safePercents.mapIndexed { index, percent ->
                val x = if (safePercents.size == 1) {
                    horizontalPadding + chartWidth
                } else {
                    horizontalPadding + chartWidth * (index.toFloat() / (safePercents.lastIndex).toFloat())
                }
                val y = baselineY - chartHeight * (percent / 100f)
                Offset(x, y)
            }

            val fillPath = Path().apply {
                moveTo(points.first().x, baselineY)
                points.forEach { point -> lineTo(point.x, point.y) }
                lineTo(points.last().x, baselineY)
                close()
            }
            drawPath(path = fillPath, color = fillColor)

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point -> lineTo(point.x, point.y) }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            points.forEach { point ->
                drawCircle(color = pointColor, radius = 4.5.dp.toPx(), center = point)
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = point)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = chartLabels.first(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chartLabels.last(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BinaryTimelineChart(
    buckets: List<WhitelistTimelineBucket>,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty() || buckets.none { it.sampleCount > 0 }) {
        ChartInsufficientDataMessage(modifier = modifier)
        return
    }

    val onColor = MaterialTheme.colorScheme.tertiary
    val offColor = MaterialTheme.colorScheme.primary
    val unknownColor = MaterialTheme.colorScheme.outlineVariant
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
        ) {
            val gap = 2.dp.toPx()
            val topPadding = 8.dp.toPx()
            val bottomPadding = 10.dp.toPx()
            val chartHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val itemWidth = ((size.width - gap * (buckets.size - 1)) / buckets.size)
                .coerceAtLeast(1f)
            val offHeight = chartHeight * 0.28f
            val onHeight = chartHeight * 0.82f
            val baselineY = topPadding + chartHeight

            drawLine(
                color = gridColor,
                start = Offset(0f, baselineY - offHeight),
                end = Offset(size.width, baselineY - offHeight),
                strokeWidth = 1.dp.toPx(),
            )

            buckets.forEachIndexed { index, bucket ->
                val left = index * (itemWidth + gap)
                val (barHeight, color) = when (bucket.state) {
                    WhitelistBinaryState.ON -> onHeight to onColor
                    WhitelistBinaryState.OFF -> offHeight to offColor
                    WhitelistBinaryState.UNKNOWN -> (chartHeight * 0.08f) to unknownColor
                }
                val top = baselineY - barHeight
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(itemWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
        }
        TimelineEdgeLabels(
            first = buckets.first().label,
            last = buckets.last().label,
        )
        TimelineLegend()
    }
}

@Composable
private fun TimelineEdgeLabels(first: String, last: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = first,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = last,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimelineLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_timeline_legend_on),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stringResource(R.string.statistics_timeline_legend_off),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.statistics_timeline_legend_unknown),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

private const val MAX_TREND_POINTS = 14
