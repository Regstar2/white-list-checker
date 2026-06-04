package com.whitelistchecker.ui.statistics

import android.content.res.Resources
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.StatisticsNumericSanitizer
import com.whitelistchecker.ui.home.LastCheckAgeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object StatisticsValueFormatter {

    fun formatSuccessRate(rate: Double?): String {
        val sanitized = StatisticsNumericSanitizer.sanitizeSuccessRate(rate) ?: return ""
        val percent = sanitized * 100.0
        return if (percent == percent.roundToInt().toDouble()) {
            "${percent.roundToInt()}%"
        } else {
            String.format(Locale.US, "%.1f%%", percent)
        }
    }

    fun formatLatency(resources: Resources, latencyMs: Long?): String {
        val sanitized = StatisticsNumericSanitizer.sanitizeLatencyMs(latencyMs)
            ?: return resources.getString(R.string.statistics_value_not_available)
        return resources.getString(R.string.statistics_latency_ms, sanitized)
    }

    fun formatTextLabel(resources: Resources, value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> resources.getString(R.string.statistics_value_unknown)
            trimmed.equals("null", ignoreCase = true) ->
                resources.getString(R.string.statistics_value_unknown)
            else -> trimmed
        }
    }

    fun formatRelativeTime(resources: Resources, epochMillis: Long?, nowMillis: Long): String {
        if (epochMillis == null) {
            return resources.getString(R.string.statistics_value_not_available)
        }
        return LastCheckAgeFormatter.formatAge(resources, epochMillis, nowMillis)
    }

    fun formatCount(resources: Resources, value: Int): String {
        return value.toString()
    }
}
