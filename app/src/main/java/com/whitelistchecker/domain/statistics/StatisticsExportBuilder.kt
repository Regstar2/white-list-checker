package com.whitelistchecker.domain.statistics

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StatisticsExportBuilder(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    fun build(request: StatisticsExportRequest): StatisticsExportDocument? {
        val selectedSamples = request.dashboard.samplesFor(request.scope)
        if (selectedSamples.isEmpty()) return null

        val content = when (request.format) {
            StatisticsExportFormat.CSV -> buildCsv(request, selectedSamples)
            StatisticsExportFormat.JSON -> buildJson(request, selectedSamples)
            StatisticsExportFormat.TXT -> buildTxt(request, selectedSamples)
        }
        return StatisticsExportDocument(
            fileName = buildFileName(request),
            mimeType = request.format.mimeType,
            content = content,
        )
    }

    private fun buildCsv(
        request: StatisticsExportRequest,
        samples: List<WhitelistTimelineSample>,
    ): String {
        val rows = buildList {
            add(
                listOf(
                    "checked_at_iso",
                    "checked_at_epoch_ms",
                    "whitelist_state",
                    "binary_state",
                    "period",
                ),
            )
            samples.forEach { sample ->
                add(
                    listOf(
                        iso(sample.checkedAtMillis),
                        sample.checkedAtMillis.toString(),
                        sample.whitelistState.exportName(),
                        sample.binaryState.name,
                        request.scope.periodText(request.labels),
                    ),
                )
            }
        }
        return UTF8_BOM + rows.joinToString("\n") { row ->
            row.joinToString(",") { value -> value.csvEscaped() }
        }
    }

    private fun buildJson(
        request: StatisticsExportRequest,
        samples: List<WhitelistTimelineSample>,
    ): String {
        val dashboard = request.dashboard
        val summary = request.checkStatistics.summary
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAt", iso(request.generatedAtMillis))
            .put(
                "scope",
                JSONObject()
                    .put("type", request.scope.typeName)
                    .put("title", request.scope.displayTitle(request.labels)),
            )
            .put("period", request.scope.periodJson())
            .put(
                "summary",
                JSONObject()
                    .put("currentState", dashboard.currentState.name)
                    .put("currentStateAt", nullableIso(dashboard.currentStateAtMillis))
                    .put("whitelistOnPercent", dashboard.whitelistOnPercent?.times(100.0) ?: JSONObject.NULL)
                    .put("totalSamples", dashboard.totalSamples)
                    .put("binarySamples", dashboard.binarySamples)
                    .put("whitelistOnSamples", dashboard.whitelistOnSamples)
                    .put("whitelistOffSamples", dashboard.whitelistOffSamples),
            )
            .put(
                "freshness",
                JSONObject()
                    .put("dataUpdatedAt", nullableIso(request.freshness.dataUpdatedAt))
                    .put("isStale", request.freshness.isStale)
                    .put("isLowSample", request.freshness.isLowSample)
                    .put("lastCheckAt", nullableIso(request.freshness.lastCheckAt))
                    .put("lastCheckStatus", request.freshness.lastCheckStatus ?: JSONObject.NULL)
                    .put("targetsCheckedAvailable", request.freshness.targetsCheckedAvailable)
                    .put("targetsCheckedTotal", request.freshness.targetsCheckedTotal),
            )
            .put(
                "technicalSummary",
                JSONObject()
                    .put("totalRuns", summary.totalRuns)
                    .put("successRuns", summary.successRuns)
                    .put("partialFailureRuns", summary.partialFailureRuns)
                    .put("failureRuns", summary.failureRuns)
                    .put("successRate", summary.successRate ?: JSONObject.NULL)
                    .put("averageLatencyMs", summary.averageLatencyMs ?: JSONObject.NULL)
                    .put("lastRunAt", nullableIso(summary.lastRunAt)),
            )
            .put(
                "samples",
                JSONArray().apply {
                    samples.forEach { sample ->
                        put(
                            JSONObject()
                                .put("checkedAt", iso(sample.checkedAtMillis))
                                .put("whitelistState", sample.whitelistState.name)
                                .put("binaryState", sample.binaryState.name),
                        )
                    }
                },
            )
            .put(
                "buckets",
                JSONArray().apply {
                    request.scope.bucketsForExport().forEach { bucket ->
                        put(
                            JSONObject()
                                .put("label", bucket.label)
                                .put("startAt", iso(bucket.startMillis))
                                .put("endAt", iso(bucket.endMillis))
                                .put("state", bucket.state.name)
                                .put("sampleCount", bucket.sampleCount)
                                .put("whitelistOnCount", bucket.whitelistOnCount)
                                .put("whitelistOffCount", bucket.whitelistOffCount),
                        )
                    }
                },
            )

        return root.toString(2)
    }

    private fun buildTxt(
        request: StatisticsExportRequest,
        samples: List<WhitelistTimelineSample>,
    ): String {
        val labels = request.labels
        val dashboard = request.dashboard
        return buildString {
            appendLine(labels.appTitle)
            appendLine()
            appendLine("${request.scope.displayTitle(request.labels)}:")
            appendLine(request.scope.periodText(labels))
            appendLine()
            appendLine("${labels.currentState}:")
            appendLine(dashboard.currentState.exportLabel(labels))
            appendLine()
            appendLine("${labels.currentStateAt}:")
            appendLine(dashboard.currentStateAtMillis?.let(::humanDateTime) ?: labels.noData)
            appendLine()
            appendLine("${labels.whitelistOnPercent}:")
            appendLine(formatPercent(dashboard.whitelistOnPercent) ?: labels.noData)
            appendLine()
            appendLine("${labels.binarySamples}:")
            appendLine("${dashboard.binarySamples} / ${dashboard.totalSamples}")
            appendLine()
            appendLine("${labels.lastCheck}:")
            appendLine(request.freshness.lastCheckAt?.let(::humanDateTime) ?: labels.noData)
            appendLine()
            appendLine("${labels.history}:")
            samples.forEach { sample ->
                appendLine("${humanDateTime(sample.checkedAtMillis)} - ${sample.binaryState.exportLabel(labels)}")
            }
        }
    }

    private fun buildFileName(request: StatisticsExportRequest): String {
        val date = FILE_DATE_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(request.generatedAtMillis))
        val scope = when (val exportScope = request.scope) {
            StatisticsExportScope.AllHistory -> request.labels.allHistoryFilePart
            is StatisticsExportScope.SelectedPeriod -> when (exportScope.kind) {
                StatisticsExportPeriodKind.DAY -> "${request.labels.scopeDayFilePart}-${periodFileDate(exportScope.startMillis)}"
                StatisticsExportPeriodKind.WEEK -> "${request.labels.scopeWeekFilePart}-${periodFileDate(exportScope.startMillis)}"
                StatisticsExportPeriodKind.MONTH -> "${request.labels.scopeMonthFilePart}-${MONTH_FILE_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(exportScope.startMillis))}"
                StatisticsExportPeriodKind.YEAR -> "${request.labels.scopeYearFilePart}-${YEAR_FILE_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(exportScope.startMillis))}"
            }
        }
        return "WhiteListChecker-statistics-$scope-$date.${request.format.extension}"
    }

    private fun WhitelistTimelineDashboard.samplesFor(scope: StatisticsExportScope): List<WhitelistTimelineSample> {
        return when (scope) {
            StatisticsExportScope.AllHistory -> samples
            is StatisticsExportScope.SelectedPeriod -> samples.filter { sample ->
                sample.checkedAtMillis >= scope.startMillis && sample.checkedAtMillis < scope.endMillis
            }
        }
    }

    private fun StatisticsExportScope.bucketsForExport(): List<WhitelistTimelineBucket> {
        return when (this) {
            StatisticsExportScope.AllHistory -> emptyList()
            is StatisticsExportScope.SelectedPeriod -> buckets
        }
    }

    private fun StatisticsExportScope.periodJson(): Any {
        return when (this) {
            StatisticsExportScope.AllHistory -> JSONObject.NULL
            is StatisticsExportScope.SelectedPeriod -> JSONObject()
                .put("kind", kind.name)
                .put("title", title)
                .put("startAt", iso(startMillis))
                .put("endAt", iso(endMillis))
                .put("startMillis", startMillis)
                .put("endMillis", endMillis)
        }
    }

    private fun StatisticsExportScope.periodText(labels: StatisticsExportLabels): String {
        return when (this) {
            StatisticsExportScope.AllHistory -> labels.allHistoryScope
            is StatisticsExportScope.SelectedPeriod -> title
        }
    }

    private fun StatisticsExportScope.displayTitle(labels: StatisticsExportLabels): String {
        return when (this) {
            StatisticsExportScope.AllHistory -> labels.allHistoryScope
            is StatisticsExportScope.SelectedPeriod -> labels.selectedPeriodScope
        }
    }

    private fun periodFileDate(millis: Long): String {
        return FILE_DATE_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    private fun humanDateTime(millis: Long): String {
        return HUMAN_FORMATTER.withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    private fun nullableIso(millis: Long?): Any {
        return millis?.let(::iso) ?: JSONObject.NULL
    }

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    private fun formatPercent(fraction: Double?): String? {
        if (fraction == null || fraction.isNaN() || fraction.isInfinite()) return null
        val percent = (fraction * 100.0).coerceIn(0.0, 100.0)
        return "%.1f%%".format(java.util.Locale.US, percent)
    }

    private fun String.csvEscaped(): String {
        val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private val StatisticsExportFormat.mimeType: String
        get() = when (this) {
            StatisticsExportFormat.CSV -> "text/csv"
            StatisticsExportFormat.JSON -> "application/json"
            StatisticsExportFormat.TXT -> "text/plain"
        }

    private val StatisticsExportFormat.extension: String
        get() = when (this) {
            StatisticsExportFormat.CSV -> "csv"
            StatisticsExportFormat.JSON -> "json"
            StatisticsExportFormat.TXT -> "txt"
        }

    private val StatisticsExportScope.typeName: String
        get() = when (this) {
            StatisticsExportScope.AllHistory -> "all_history"
            is StatisticsExportScope.SelectedPeriod -> "selected_period"
        }

    companion object {
        private const val UTF8_BOM = "\uFEFF"
        private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val MONTH_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
        private val YEAR_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy")
        private val HUMAN_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
