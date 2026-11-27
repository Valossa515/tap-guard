package io.github.valossa515.tapguard.core.detection.scan

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.inventory.AppInventory
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import io.github.valossa515.tapguard.core.domain.risk.TrustedAppRepository
import io.github.valossa515.tapguard.core.domain.scan.ScanReport
import io.github.valossa515.tapguard.core.domain.scan.ScanRepository
import io.github.valossa515.tapguard.core.domain.scan.ScanTrigger
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.coroutines.CoroutineContext

class DefaultScanDeviceTest {
    private val events = mutableListOf<String>()
    private val saved = mutableListOf<ScanReport>()
    private var clock: Clock = SteppingClock(T0, T1)

    @Test
    fun `steps run in order with collectors in sequence and one save at the end`() =
        runTest {
            val device =
                device(
                    apps = listOf(app(A), app(B)),
                    collectors = listOf(RecordingCollector("c1", suspendOn = setOf(A, B)), RecordingCollector("c2")),
                )

            device(ScanTrigger.MANUAL)

            assertEquals(
                listOf(
                    "inventory",
                    "trusted",
                    "c1 start $A",
                    "c1 end $A",
                    "c2 start $A",
                    "c2 end $A",
                    "c1 start $B",
                    "c1 end $B",
                    "c2 start $B",
                    "c2 end $B",
                    "save",
                ),
                events,
            )
        }

    @Test
    fun `returned report is the one saved once`() =
        runTest {
            val report = device(apps = listOf(app(A))).invoke(ScanTrigger.PERIODIC)

            assertEquals(1, saved.size)
            assertSame(saved.single(), report)
            assertEquals(ScanTrigger.PERIODIC, report.trigger)
        }

    @Test
    fun `signals are the union of the collectors and scored by the weighted scorer`() =
        runTest {
            val device =
                device(
                    apps = listOf(app(A)),
                    collectors =
                        listOf(
                            RecordingCollector("c1", signals = setOf(S1, S2)),
                            RecordingCollector("c2", signals = setOf(S2, S3)),
                        ),
                )

            val risk = device(ScanTrigger.MANUAL).apps.single().risk

            assertEquals(setOf(S1, S2, S3), risk.signals)
            assertEquals(65, risk.score)
            assertEquals(RiskLevel.RED, risk.level)
        }

    @Test
    fun `collectors run on the injected dispatcher`() =
        runTest {
            val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

            device(apps = listOf(app(A)), dispatcher = dispatcher).invoke(ScanTrigger.MANUAL)

            assertTrue("o laço não passou pelo dispatcher recebido", dispatcher.dispatches > 0)
        }

    @Test
    fun `cancelling on the second app skips the third and saves nothing`() =
        runTest {
            lateinit var scan: Job
            val cancelling = RecordingCollector("c1", onCollect = { if (it == B) scan.cancel() })
            val device = device(apps = listOf(app(A), app(B), app(C)), collectors = listOf(cancelling))

            scan = launch { device(ScanTrigger.MANUAL) }
            scan.join()

            assertTrue(scan.isCancelled)
            assertTrue("coletor chamado depois do cancelamento: $events", events.none { it.endsWith(C) })
            assertTrue(saved.isEmpty())
            assertTrue("save" !in events)
        }

    @Test
    fun `inventory error propagates and nothing is saved`() =
        runTest {
            val error = ScanTestException()

            val device = device(failure = Step.INVENTORY to error)

            val thrown = runCatching { device(ScanTrigger.MANUAL) }.exceptionOrNull()

            assertSame(error, thrown)
            assertTrue("save" !in events)
        }

    @Test
    fun `collector error on the second app propagates and nothing is saved`() =
        runTest {
            val error = ScanTestException()
            val failing = RecordingCollector("c1", onCollect = { if (it == B) throw error })
            val device = device(apps = listOf(app(A), app(B)), collectors = listOf(failing))

            val thrown = runCatching { device(ScanTrigger.MANUAL) }.exceptionOrNull()

            assertSame(error, thrown)
            assertTrue("save" !in events)
        }

    @Test
    fun `trusted apps error propagates and nothing is saved`() =
        runTest {
            val error = ScanTestException()

            val device = device(apps = listOf(app(A)), failure = Step.TRUSTED to error)

            val thrown = runCatching { device(ScanTrigger.MANUAL) }.exceptionOrNull()

            assertSame(error, thrown)
            assertTrue("save" !in events)
        }

    @Test
    fun `save error propagates`() =
        runTest {
            val error = ScanTestException()

            val device = device(apps = listOf(app(A)), failure = Step.SAVE to error)

            val thrown = runCatching { device(ScanTrigger.MANUAL) }.exceptionOrNull()

            assertSame(error, thrown)
        }

    @Test
    fun `start and finish come from the clock`() =
        runTest {
            clock = SteppingClock(T0, T1)

            val report = device(apps = listOf(app(A))).invoke(ScanTrigger.MANUAL)

            assertEquals(T0, report.startedAt)
            assertEquals(T1, report.finishedAt)
        }

    @Test
    fun `clock going back during the scan finishes at the start`() =
        runTest {
            clock = SteppingClock(T1, T0)

            val report = device(apps = listOf(app(A))).invoke(ScanTrigger.MANUAL)

            assertEquals(T1, report.startedAt)
            assertEquals(T1, report.finishedAt)
            assertSame(report, saved.single())
        }

    @Test
    fun `empty inventory saves an empty report`() =
        runTest {
            val report = device(apps = emptyList()).invoke(ScanTrigger.APP_OPEN)

            assertTrue(report.apps.isEmpty())
            assertSame(report, saved.single())
        }

    @Test
    fun `trusted app is green with its score and the other stays red`() =
        runTest {
            val device =
                device(
                    apps = listOf(app(A), app(B)),
                    collectors = listOf(RecordingCollector("c1", signals = setOf(S1, S2, S3))),
                    trusted = setOf(A),
                )

            val risks = device(ScanTrigger.MANUAL).apps.associate { it.app.packageName to it.risk }

            assertEquals(RiskLevel.GREEN, risks.getValue(A).level)
            assertEquals(65, risks.getValue(A).score)
            assertEquals(RiskLevel.RED, risks.getValue(B).level)
            assertEquals(65, risks.getValue(B).score)
        }

    @Test
    fun `report keeps the default payment services from the inventory`() =
        runTest {
            val wallet = anInstalledApp(packageName = A, defaultPaymentServices = setOf("$A.PayService"))

            val scanned = device(apps = listOf(wallet)).invoke(ScanTrigger.MANUAL).apps.single()

            assertSame(wallet, scanned.app)
            assertEquals(setOf("$A.PayService"), scanned.app.defaultPaymentServices)
        }

    /** Monta o [DefaultScanDevice] com falsos que gravam em [events]; [failure] faz uma etapa lançar. */
    private fun TestScope.device(
        apps: List<InstalledApp> = emptyList(),
        collectors: List<SignalCollector> = listOf(RecordingCollector("c1")),
        trusted: Set<String> = emptySet(),
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
        failure: Pair<Step, Throwable>? = null,
    ): DefaultScanDevice {
        fun step(step: Step) {
            events += step.event
            if (failure?.first == step) throw failure.second
        }
        return DefaultScanDevice(
            inventory =
                object : AppInventory {
                    override suspend fun installedApps(): List<InstalledApp> = apps.also { step(Step.INVENTORY) }
                },
            collectors = collectors,
            trustedApps =
                object : TrustedAppRepository {
                    override suspend fun trustedPackages(): Set<String> = trusted.also { step(Step.TRUSTED) }
                },
            scans =
                object : ScanRepository {
                    override suspend fun save(report: ScanReport) {
                        step(Step.SAVE)
                        saved += report
                    }
                },
            clock = clock,
            dispatcher = dispatcher,
        )
    }

    private fun app(packageName: String) = anInstalledApp(packageName = packageName)

    /** Coletor falso: grava início e fim em [events], pode suspender, e devolve [signals] para todo app. */
    private inner class RecordingCollector(
        private val name: String,
        private val signals: Set<Signal> = emptySet(),
        private val suspendOn: Set<String> = emptySet(),
        private val onCollect: (String) -> Unit = {},
    ) : SignalCollector {
        override suspend fun collect(app: InstalledApp): Set<Signal> {
            events += "$name start ${app.packageName}"
            onCollect(app.packageName)
            if (app.packageName in suspendOn) yield()
            events += "$name end ${app.packageName}"
            return signals
        }
    }

    /** Relógio que devolve [instants] em ordem, um por leitura. */
    private class SteppingClock(
        vararg instants: Instant,
    ) : Clock() {
        private val remaining = ArrayDeque(instants.toList())

        override fun instant(): Instant = remaining.removeFirst()

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }

    /** Repassa ao dispatcher do runTest e conta os despachos. Zero quer dizer que o código nunca trocou para ele. */
    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatches = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches++
            delegate.dispatch(context, block)
        }
    }

    private class ScanTestException : RuntimeException("falha de teste")

    private enum class Step(
        val event: String,
    ) {
        INVENTORY("inventory"),
        TRUSTED("trusted"),
        SAVE("save"),
    }

    private companion object {
        const val A = "com.example.first"
        const val B = "com.example.second"
        const val C = "com.example.third"
        val S1 = Signal.UNTRUSTED_INSTALL_SOURCE
        val S2 = Signal.NFC_PERMISSION
        val S3 = Signal.CARD_EMULATION_SERVICE
        val T0: Instant = Instant.parse("2026-11-27T10:00:00Z")
        val T1: Instant = Instant.parse("2026-11-27T10:00:01Z")
    }
}
