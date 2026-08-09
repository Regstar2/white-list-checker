package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.WhitelistState

enum class StatisticsExportFormat {
    CSV,
    JSON,
    TXT,
}

enum class StatisticsExportPeriodKind {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

sealed class StatisticsExportScope {
    data class SelectedPeriod(
        val kind: StatisticsExportPeriodKind,
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val buckets: List<WhitelistTimelineBucket>,
    ) : StatisticsExportScope()

    data object AllHistory : StatisticsExportScope()
}

data class StatisticsExportRequest(
    val format: StatisticsExportFormat,
    val scope: StatisticsExportScope,
    val dashboard: WhitelistTimelineDashboard,
    val checkStatistics: StatisticsDashboard,
    val freshness: StatisticsExportFreshness,
    val generatedAtMillis: Long,
    val labels: StatisticsExportLabels,
)

data class StatisticsExportFreshness(
    val dataUpdatedAt: Long?,
    val isStale: Boolean,
    val isLowSample: Boolean,
    val lastCheckAt: Long?,
    val lastCheckStatus: String?,
    val targetsCheckedAvailable: Int,
    val targetsCheckedTotal: Int,
)

data class StatisticsExportDocument(
    val fileName: String,
    val mimeType: String,
    val content: String,
)

data class StatisticsExportLabels(
    val appTitle: String,
    val selectedPeriodScope: String,
    val allHistoryScope: String,
    val allHistoryFilePart: String,
    val currentState: String,
    val currentStateAt: String,
    val whitelistOnPercent: String,
    val binarySamples: String,
    val lastCheck: String,
    val history: String,
    val noData: String,
    val statusOn: String,
    val statusOff: String,
    val statusUnknown: String,
    val scopeDayFilePart: String,
    val scopeWeekFilePart: String,
    val scopeMonthFilePart: String,
    val scopeYearFilePart: String,
)

fun WhitelistBinaryState.exportLabel(labels: StatisticsExportLabels): String {
    return when (this) {
        WhitelistBinaryState.ON -> labels.statusOn
        WhitelistBinaryState.OFF -> labels.statusOff
        WhitelistBinaryState.UNKNOWN -> labels.statusUnknown
    }
}

fun WhitelistState.exportName(): String = name
