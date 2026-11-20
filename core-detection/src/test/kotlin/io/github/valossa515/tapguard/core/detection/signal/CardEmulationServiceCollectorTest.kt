package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CardEmulationServiceCollectorTest {
    private val collector = CardEmulationServiceCollector()

    @Test
    fun `hce service yields s3`() =
        runTest {
            val app = anInstalledApp(services = setOf(HCE_SERVICE), cardEmulationServices = setOf(HCE_SERVICE))

            assertEquals(S3, collector.collect(app))
        }

    @Test
    fun `no services yields nothing`() =
        runTest {
            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp()))
        }

    @Test
    fun `ordinary service without hce yields nothing`() =
        runTest {
            val app = anInstalledApp(services = setOf("com.example.PaymentService"), cardEmulationServices = emptySet())

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `hce without nfc permission yields s3`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = emptySet(), cardEmulationServices = setOf(HCE_SERVICE))

            assertEquals(S3, collector.collect(app))
        }

    @Test
    fun `yields only s3 for app with nfc and null installer`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    requestedPermissions = setOf("android.permission.NFC"),
                    cardEmulationServices = setOf(HCE_SERVICE),
                )

            assertEquals(S3, collector.collect(app))
        }

    private companion object {
        const val HCE_SERVICE = "com.example.app.CardService"
        val S3: Set<Signal> = setOf(Signal.CARD_EMULATION_SERVICE)
    }
}
