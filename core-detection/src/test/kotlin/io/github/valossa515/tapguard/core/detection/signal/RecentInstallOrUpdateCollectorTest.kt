package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RecentInstallOrUpdateCollectorTest {
    private val collector = collectorAt(NOW)

    @Test
    fun `install one day ago yields s6`() =
        runTest {
            val app = appInstalledAt(NOW.minus(Duration.ofDays(1)))

            assertEquals(S6, collector.collect(app))
        }

    @Test
    fun `install and update thirty days ago yield nothing`() =
        runTest {
            val app = appInstalledAt(NOW.minus(Duration.ofDays(30)))

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `old install with update two days ago yields s6`() =
        runTest {
            val app =
                anInstalledApp(
                    firstInstallTime = NOW.minus(Duration.ofDays(400)),
                    lastUpdateTime = NOW.minus(Duration.ofDays(2)),
                )

            assertEquals(S6, collector.collect(app))
        }

    @Test
    fun `latest exactly seven days ago yields s6`() =
        runTest {
            val app = appInstalledAt(NOW.minus(Duration.ofHours(168)))

            assertEquals(S6, collector.collect(app))
        }

    @Test
    fun `latest seven days and one milli ago yields nothing`() =
        runTest {
            val app = appInstalledAt(NOW.minus(Duration.ofDays(7)).minusMillis(1))

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `install and update in the future yield s6`() =
        runTest {
            val app = appInstalledAt(NOW.plus(Duration.ofHours(1)))

            assertEquals(S6, collector.collect(app))
        }

    @Test
    fun `old install with update in the future yields s6`() =
        runTest {
            val app =
                anInstalledApp(
                    firstInstallTime = NOW.minus(Duration.ofDays(30)),
                    lastUpdateTime = NOW.plus(Duration.ofDays(3)),
                )

            assertEquals(S6, collector.collect(app))
        }

    @Test
    fun `same app against two clocks`() =
        runTest {
            val app = appInstalledAt(Instant.parse("2026-01-10T00:00:00Z"))

            assertEquals(S6, collectorAt(Instant.parse("2026-01-12T00:00:00Z")).collect(app))
            assertEquals(emptySet<Signal>(), collectorAt(Instant.parse("2026-02-12T00:00:00Z")).collect(app))
        }

    @Test
    fun `yields only s6 for app with all batch signals`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    firstInstallTime = NOW,
                    lastUpdateTime = NOW,
                    requestedPermissions = setOf(SYSTEM_ALERT_WINDOW, READ_SMS, NFC),
                    cardEmulationServices = setOf("com.example.app.CardService"),
                )

            assertEquals(S6, collector.collect(app))
        }

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-12T00:00:00Z")
        const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        const val READ_SMS = "android.permission.READ_SMS"
        const val NFC = "android.permission.NFC"
        val S6: Set<Signal> = setOf(Signal.RECENTLY_INSTALLED_OR_UPDATED)

        /** App instalado em [instant] e sem atualização depois: as duas datas iguais, como no aparelho. */
        fun appInstalledAt(instant: Instant) = anInstalledApp(firstInstallTime = instant, lastUpdateTime = instant)

        fun collectorAt(now: Instant) = RecentInstallOrUpdateCollector(Clock.fixed(now, ZoneOffset.UTC))
    }
}
