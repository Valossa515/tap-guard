package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EnabledAccessibilityServiceCollectorTest {
    private val collector = EnabledAccessibilityServiceCollector()

    @Test
    fun `enabled service yields s4`() =
        runTest {
            val app = anInstalledApp(enabledAccessibilityServices = setOf(A11Y_SERVICE))

            assertEquals(S4, collector.collect(app))
        }

    @Test
    fun `no enabled service yields nothing`() =
        runTest {
            val app = anInstalledApp(enabledAccessibilityServices = emptySet())

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `declared but not enabled service yields nothing`() =
        runTest {
            val app = anInstalledApp(services = setOf(A11Y_SERVICE), enabledAccessibilityServices = emptySet())

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `yields only s4 for app with all spec signals`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    requestedPermissions = setOf(SYSTEM_ALERT_WINDOW, READ_SMS, NFC),
                    cardEmulationServices = setOf("com.example.app.CardService"),
                    enabledAccessibilityServices = setOf(A11Y_SERVICE),
                )

            assertEquals(S4, collector.collect(app))
        }

    private companion object {
        const val A11Y_SERVICE = "com.example.app.A11yService"
        const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        const val READ_SMS = "android.permission.READ_SMS"
        const val NFC = "android.permission.NFC"
        val S4: Set<Signal> = setOf(Signal.ACCESSIBILITY_SERVICE_ENABLED)
    }
}
