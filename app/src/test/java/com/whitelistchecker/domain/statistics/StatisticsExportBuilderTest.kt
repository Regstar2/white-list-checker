package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StatisticsExportBuilderTest {

    private val zoneId = ZoneId.of("UTC")
    private val builder = StatisticsExportBuilder(zoneId)

    @Test
    fun `csv selected period has bom escaping cyrillic and boundary filtering`() {
        val start = millis(2026, 8, 9, 0, 0)
        val end = millis(2026, 8, 10, 0, 0)
        val dashboard = dashboard(
            samples = listOf(
                sample("before", start - 1, WhitelistState.WHITELIST_ON),
                sample("inside", start, WhitelistState.WHITELIST_OFF),
                sample("comma,\"quote\"\nкириллица", millis(2026, 8, 9, 12, 0), WhitelistState.WHITELIST_ON),
                sample("end", end, WhitelistState.WHITELIST_ON),
            ),
        )

        val document = builder.build(
            request(
                format = StatisticsExportFormat.CSV,
                scope = selectedScope(StatisticsExportPeriodKind.DAY, start, end, "09.08, \"тест\"\nстрока"),
                dashboard = dashboard,
            ),
        )

        assertNotNull(document)
        val content = document!!.content
        assertTrue(content.startsWith("\uFEFF"))
        assertTrue(content.contains("checked_at_iso,checked_at_epoch_ms,whitelist_state,binary_state,period"))
        assertTrue(content.contains("WHITELIST_OFF,OFF"))
        assertTrue(content.contains("WHITELIST_ON,ON"))
        assertTrue(content.contains("\"09.08, \"\"тест\"\"\nстрока\""))
        assertFalse(content.contains((start - 1).toString()))
        assertFalse(content.contains(end.toString()))
    }

    @Test
    fun `csv all history exports all available samples`() {
        val first = millis(2026, 1, 1, 0, 0)
        val second = millis(2026, 2, 1, 0, 0)
        val document = builder.build(
            request(
                format = StatisticsExportFormat.CSV,
                scope = StatisticsExportScope.AllHistory,
                dashboard = dashboard(
                    samples = listOf(
                        sample("first", first, WhitelistState.WHITELIST_ON),
                        sample("second", second, WhitelistState.UNKNOWN),
                    ),
                ),
            ),
        )

        assertNotNull(document)
        assertTrue(document!!.fileName.contains("all"))
        assertTrue(document.content.contains(first.toString()))
        assertTrue(document.content.contains(second.toString()))
        assertTrue(document.content.contains("UNKNOWN"))
    }

    @Test
    fun `json escapes strings nulls and binary states`() {
        val start = millis(2026, 8, 1, 0, 0)
        val end = millis(2026, 9, 1, 0, 0)
        val document = builder.build(
            request(
                format = StatisticsExportFormat.JSON,
                scope = selectedScope(
                    kind = StatisticsExportPeriodKind.MONTH,
                    start = start,
                    end = end,
                    title = "Август \"test\" \\ \n",
                ),
                dashboard = dashboard(
                    samples = listOf(
                        sample("on", millis(2026, 8, 2, 0, 0), WhitelistState.WHITELIST_ON),
                        sample("off", millis(2026, 8, 3, 0, 0), WhitelistState.WHITELIST_OFF),
                        sample("unknown", millis(2026, 8, 4, 0, 0), WhitelistState.PARTIAL_PROBLEM),
                    ),
                    currentState = WhitelistBinaryState.UNKNOWN,
                    currentStateAtMillis = null,
                ),
                freshness = exportFreshness(lastCheckAt = null),
            ),
        )

        assertNotNull(document)
        val json = JSONObject(document!!.content)
        assertEquals(1, json.getInt("schemaVersion"))
        assertEquals("Август \"test\" \\ \n", json.getJSONObject("period").getString("title"))
        assertTrue(json.getJSONObject("summary").isNull("currentStateAt"))
        assertTrue(json.getJSONObject("freshness").isNull("lastCheckAt"))
        val samples = json.getJSONArray("samples")
        assertEquals("ON", samples.getJSONObject(0).getString("binaryState"))
        assertEquals("OFF", samples.getJSONObject(1).getString("binaryState"))
        assertEquals("UNKNOWN", samples.getJSONObject(2).getString("binaryState"))
    }

    @Test
    fun `txt is human readable and selected period uses graph period`() {
        val start = millis(2026, 8, 9, 0, 0)
        val end = millis(2026, 8, 10, 0, 0)
        val document = builder.build(
            request(
                format = StatisticsExportFormat.TXT,
                scope = selectedScope(StatisticsExportPeriodKind.DAY, start, end, "09.08.2026"),
                dashboard = dashboard(
                    samples = listOf(
                        sample("inside", millis(2026, 8, 9, 11, 0), WhitelistState.WHITELIST_ON),
                    ),
                ),
            ),
        )

        assertNotNull(document)
        assertTrue(document!!.content.contains("WhiteListChecker"))
        assertTrue(document.content.contains("09.08.2026"))
        assertTrue(document.content.contains("Похоже на включённые белые списки"))
    }

    @Test
    fun `selected scopes produce day week month and year file names`() {
        val start = millis(2026, 8, 9, 0, 0)
        val end = millis(2026, 8, 10, 0, 0)
        listOf(
            StatisticsExportPeriodKind.DAY to "day",
            StatisticsExportPeriodKind.WEEK to "week",
            StatisticsExportPeriodKind.MONTH to "month",
            StatisticsExportPeriodKind.YEAR to "year",
        ).forEach { (kind, filePart) ->
            val document = builder.build(
                request(
                    format = StatisticsExportFormat.JSON,
                    scope = selectedScope(kind, start, end),
                    dashboard = dashboard(
                        samples = listOf(sample("inside", start, WhitelistState.WHITELIST_OFF)),
                    ),
                ),
            )

            assertNotNull(document)
            assertTrue(document!!.fileName.contains(filePart))
        }
    }

    @Test
    fun `empty selected period returns null instead of empty file`() {
        val document = builder.build(
            request(
                format = StatisticsExportFormat.JSON,
                scope = selectedScope(
                    kind = StatisticsExportPeriodKind.WEEK,
                    start = millis(2026, 8, 1, 0, 0),
                    end = millis(2026, 8, 8, 0, 0),
                ),
                dashboard = dashboard(
                    samples = listOf(sample("outside", millis(2026, 8, 9, 0, 0), WhitelistState.WHITELIST_ON)),
                ),
            ),
        )

        assertEquals(null, document)
    }

    private fun request(
        format: StatisticsExportFormat,
        scope: StatisticsExportScope,
        dashboard: WhitelistTimelineDashboard,
        freshness: StatisticsExportFreshness = exportFreshness(),
    ): StatisticsExportRequest {
        return StatisticsExportRequest(
            format = format,
            scope = scope,
            dashboard = dashboard,
            checkStatistics = StatisticsDashboard(
                summary = CheckStatisticsSummary(
                    totalRuns = 3,
                    successRuns = 1,
                    partialFailureRuns = 1,
                    failureRuns = 1,
                    successRate = 0.33,
                    averageLatencyMs = 123,
                    lastRunAt = millis(2026, 8, 9, 12, 0),
                ),
                targets = emptyList(),
                routeKinds = emptyList(),
                networks = emptyList(),
                daily = emptyList(),
                isStale = false,
                lastUpdatedAt = millis(2026, 8, 9, 12, 0),
            ),
            freshness = freshness,
            generatedAtMillis = millis(2026, 8, 9, 13, 0),
            labels = labels(),
        )
    }

    private fun selectedScope(
        kind: StatisticsExportPeriodKind,
        start: Long,
        end: Long,
        title: String = "period",
    ): StatisticsExportScope.SelectedPeriod {
        return StatisticsExportScope.SelectedPeriod(
            kind = kind,
            title = title,
            startMillis = start,
            endMillis = end,
            buckets = listOf(
                WhitelistTimelineBucket(
                    label = "bucket,\"quoted\"\nкириллица",
                    startMillis = start,
                    endMillis = end,
                    scale = WhitelistTimelineBucketScale.DAY,
                    state = WhitelistBinaryState.ON,
                    sampleCount = 1,
                    whitelistOnCount = 1,
                    whitelistOffCount = 0,
                ),
            ),
        )
    }

    private fun dashboard(
        samples: List<WhitelistTimelineSample>,
        currentState: WhitelistBinaryState = WhitelistBinaryState.ON,
        currentStateAtMillis: Long? = samples.lastOrNull()?.checkedAtMillis,
    ): WhitelistTimelineDashboard {
        val binarySamples = samples.filter { it.binaryState != WhitelistBinaryState.UNKNOWN }
        return WhitelistTimelineDashboard(
            currentState = currentState,
            currentStateAtMillis = currentStateAtMillis,
            totalSamples = samples.size,
            binarySamples = binarySamples.size,
            whitelistOnSamples = binarySamples.count { it.binaryState == WhitelistBinaryState.ON },
            whitelistOffSamples = binarySamples.count { it.binaryState == WhitelistBinaryState.OFF },
            whitelistOnPercent = if (binarySamples.isEmpty()) null else {
                binarySamples.count { it.binaryState == WhitelistBinaryState.ON }.toDouble() / binarySamples.size
            },
            generatedAtMillis = millis(2026, 8, 9, 13, 0),
            samples = samples,
            dayHourly = emptyList(),
            weekDaily = emptyList(),
            monthDaily = emptyList(),
            yearMonthly = emptyList(),
            lastUpdatedAt = samples.lastOrNull()?.checkedAtMillis,
            isStale = false,
        )
    }

    private fun sample(id: String, checkedAtMillis: Long, state: WhitelistState): WhitelistTimelineSample {
        return WhitelistTimelineSample(
            checkRunId = id,
            checkedAtMillis = checkedAtMillis,
            whitelistState = state,
            binaryState = state.toBinaryWhitelistState(),
            createdAtMillis = checkedAtMillis,
        )
    }

    private fun exportFreshness(lastCheckAt: Long? = millis(2026, 8, 9, 12, 0)): StatisticsExportFreshness {
        return StatisticsExportFreshness(
            dataUpdatedAt = millis(2026, 8, 9, 12, 30),
            isStale = false,
            isLowSample = false,
            lastCheckAt = lastCheckAt,
            lastCheckStatus = null,
            targetsCheckedAvailable = 1,
            targetsCheckedTotal = 2,
        )
    }

    private fun labels(): StatisticsExportLabels {
        return StatisticsExportLabels(
            appTitle = "WhiteListChecker — статистика",
            selectedPeriodScope = "Текущий выбранный период",
            allHistoryScope = "Вся сохранённая история",
            allHistoryFilePart = "all",
            currentState = "Текущее состояние",
            currentStateAt = "Последний бинарный статус",
            whitelistOnPercent = "БС были за период",
            binarySamples = "Бинарных сэмплов",
            lastCheck = "Последняя проверка",
            history = "История",
            noData = "Нет данных",
            statusOn = "Похоже на включённые белые списки",
            statusOff = "Белые списки не обнаружены",
            statusUnknown = "Нет бинарного статуса",
            scopeDayFilePart = "day",
            scopeWeekFilePart = "week",
            scopeMonthFilePart = "month",
            scopeYearFilePart = "year",
        )
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
