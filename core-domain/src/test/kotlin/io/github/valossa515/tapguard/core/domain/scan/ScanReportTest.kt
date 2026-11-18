package io.github.valossa515.tapguard.core.domain.scan

import io.github.valossa515.tapguard.core.domain.aRiskResult
import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class ScanReportTest {
    private val first = ScannedApp(anInstalledApp(packageName = "com.example.first"), aRiskResult())
    private val second =
        ScannedApp(
            anInstalledApp(packageName = "com.example.second"),
            aRiskResult(score = 30, level = RiskLevel.YELLOW),
        )

    @Test
    fun `fields and apps in the given order are read back`() {
        val report = aReport(apps = listOf(second, first))

        assertEquals(ScanTrigger.MANUAL, report.trigger)
        assertEquals(STARTED, report.startedAt)
        assertEquals(FINISHED, report.finishedAt)
        assertEquals(listOf(second, first), report.apps)
    }

    @Test
    fun `finished before started is rejected and equal is accepted`() {
        assertThrows(IllegalArgumentException::class.java) {
            aReport(finishedAt = STARTED.minusMillis(1))
        }
        assertEquals(STARTED, aReport(finishedAt = STARTED).finishedAt)
    }

    @Test
    fun `scanned app holds app and risk`() {
        val app = anInstalledApp()
        val risk = aRiskResult(score = 65, level = RiskLevel.RED)

        val scanned = ScannedApp(app = app, risk = risk)

        assertEquals(app, scanned.app)
        assertEquals(risk, scanned.risk)
    }

    @Test
    fun `mutating source apps does not change the report`() {
        val apps = mutableListOf(first)
        val report = aReport(apps = apps)

        apps.add(second)

        assertEquals(listOf(first), report.apps)
    }

    @Test
    fun `exposed apps reject add remove and clear through mutable list cast`() {
        val report = aReport(apps = listOf(first))
        val mutable = report.apps as MutableList<ScannedApp>

        assertThrows(UnsupportedOperationException::class.java) { mutable.add(second) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.remove(first) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.clear() }
        assertEquals(listOf(first), report.apps)
    }

    private fun aReport(
        finishedAt: Instant = FINISHED,
        apps: List<ScannedApp> = emptyList(),
    ): ScanReport =
        ScanReport(
            trigger = ScanTrigger.MANUAL,
            startedAt = STARTED,
            finishedAt = finishedAt,
            apps = apps,
        )

    private companion object {
        val STARTED: Instant = Instant.parse("2026-11-18T10:00:00Z")
        val FINISHED: Instant = Instant.parse("2026-11-18T10:00:03Z")
    }
}
