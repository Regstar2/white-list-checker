package com.whitelistchecker.ui.statistics

import android.content.res.Resources
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityTransitionType
import com.whitelistchecker.domain.statistics.StatisticsNumericSanitizer
import com.whitelistchecker.ui.home.LastCheckAgeFormatter
import java.net.URI
import java.util.Locale
import kotlin.math.roundToInt

object StatisticsValueFormatter {

    fun formatSuccessRate(rate: Double?): String {
        val sanitized = StatisticsNumericSanitizer.sanitizeSuccessRate(rate) ?: return ""
        return formatPercentFraction(sanitized)
    }

    fun formatPercentFraction(rate: Double?): String {
        val sanitized = StatisticsNumericSanitizer.sanitizeSuccessRate(rate) ?: return ""
        val percent = (sanitized * 100.0).coerceIn(0.0, 100.0)
        return formatPercentValue(percent)
    }

    fun formatPercentValue(percent: Double?): String {
        if (percent == null || percent.isNaN() || percent.isInfinite()) return ""
        val clamped = percent.coerceIn(0.0, 100.0)
        return if (clamped == clamped.roundToInt().toDouble()) {
            "${clamped.roundToInt()}%"
        } else {
            String.format(Locale.US, "%.1f%%", clamped)
        }
    }

    fun formatLatency(resources: Resources, latencyMs: Long?): String {
        val sanitized = StatisticsNumericSanitizer.sanitizeLatencyMs(latencyMs)
            ?: return resources.getString(R.string.statistics_value_not_available)
        if (sanitized >= 1000L) {
            val seconds = sanitized / 1000.0
            return if (seconds == seconds.roundToInt().toDouble()) {
                resources.getString(R.string.statistics_duration_seconds, seconds.roundToInt())
            } else {
                resources.getString(R.string.statistics_duration_seconds_float, seconds)
            }
        }
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

    fun formatEndpointLabel(resources: Resources, displayLabel: String, hostOrUrl: String?): String {
        val host = sanitizeHost(hostOrUrl)
        if (host.isNotBlank()) return host
        return formatTextLabel(resources, displayLabel)
    }

    fun formatRelativeTime(resources: Resources, epochMillis: Long?, nowMillis: Long): String {
        if (epochMillis == null) {
            return resources.getString(R.string.statistics_value_not_available)
        }
        return LastCheckAgeFormatter.formatAge(resources, epochMillis, nowMillis)
    }

    fun formatCount(resources: Resources, count: Int, singularRes: Int, pluralRes: Int): String {
        return if (count == 1) {
            resources.getString(singularRes, count)
        } else {
            resources.getString(pluralRes, count)
        }
    }

    fun formatChangeCount(resources: Resources, count: Int): String {
        return formatCount(
            resources,
            count,
            R.string.statistics_change_count_one,
            R.string.statistics_change_count_many,
        )
    }

    fun formatUnstableScore(resources: Resources, score: Int): String {
        return formatCount(
            resources,
            score,
            R.string.statistics_unstable_score_one,
            R.string.statistics_unstable_score_many,
        )
    }

    fun formatBecameAvailableCount(resources: Resources, count: Int): String {
        return formatCount(
            resources,
            count,
            R.string.statistics_became_available_one,
            R.string.statistics_became_available_many,
        )
    }

    fun formatBecameUnavailableCount(resources: Resources, count: Int): String {
        return formatCount(
            resources,
            count,
            R.string.statistics_became_unavailable_one,
            R.string.statistics_became_unavailable_many,
        )
    }

    fun formatWhitelistState(resources: Resources, state: WhitelistAvailabilityState): String {
        return when (state) {
            WhitelistAvailabilityState.AVAILABLE ->
                resources.getString(R.string.whitelist_target_state_available)
            WhitelistAvailabilityState.UNAVAILABLE ->
                resources.getString(R.string.whitelist_target_state_unavailable)
            WhitelistAvailabilityState.UNKNOWN ->
                resources.getString(R.string.whitelist_target_state_unknown)
            WhitelistAvailabilityState.ERROR ->
                resources.getString(R.string.whitelist_target_state_error)
        }
    }

    fun formatTechnicalCheckStatus(resources: Resources, status: LastCheckTechnicalStatus): String {
        return when (status) {
            LastCheckTechnicalStatus.NONE ->
                resources.getString(R.string.statistics_value_not_available)
            LastCheckTechnicalStatus.COMPLETED ->
                resources.getString(R.string.statistics_last_check_completed)
            LastCheckTechnicalStatus.PARTIAL ->
                resources.getString(R.string.statistics_last_check_partial)
            LastCheckTechnicalStatus.FAILED ->
                resources.getString(R.string.statistics_last_check_failed)
        }
    }

    fun formatRecentEventLine(
        resources: Resources,
        event: WhitelistAvailabilityEvent,
        nowMillis: Long,
    ): String {
        val label = formatEndpointLabel(resources, event.targetLabel, event.targetId)
        val time = formatRelativeTime(resources, event.detectedAt, nowMillis)
        val action = when (event.transitionType) {
            WhitelistAvailabilityTransitionType.BECAME_AVAILABLE,
            WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE,
            -> resources.getString(R.string.whitelist_event_became_available)
            WhitelistAvailabilityTransitionType.BECAME_UNAVAILABLE,
            WhitelistAvailabilityTransitionType.UNKNOWN_TO_UNAVAILABLE,
            -> resources.getString(R.string.whitelist_event_became_unavailable)
            WhitelistAvailabilityTransitionType.ERROR_STATE ->
                resources.getString(R.string.whitelist_event_result_unknown)
            else -> resources.getString(R.string.whitelist_event_result_unknown)
        }
        return resources.getString(R.string.whitelist_recent_event_line, label, action, time)
    }

    fun sanitizeHost(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val trimmed = value.trim()
        return try {
            val uri = URI(trimmed)
            val host = uri.host?.trim().orEmpty()
            if (host.isNotBlank()) {
                host.removePrefix("www.")
            } else {
                trimmed.substringBefore('/').substringBefore('?').removePrefix("www.")
            }
        } catch (_: Exception) {
            trimmed.substringBefore('/').substringBefore('?').removePrefix("www.")
        }
    }
}
