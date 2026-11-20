package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPermissionCollectorTest {
    private val collector = OverlayPermissionCollector()

    @Test
    fun `overlay permission yields s5`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = setOf(SYSTEM_ALERT_WINDOW, RECEIVE_SMS))

            assertEquals(S5, collector.collect(app))
        }

    @Test
    fun `no permissions yields nothing`() =
        runTest {
            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp(requestedPermissions = emptySet())))
        }

    @Test
    fun `unrelated permission yields nothing`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = setOf(RECEIVE_SMS))

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `overlay permission in other case yields nothing`() =
        runTest {
            val otherCases = listOf("ANDROID.PERMISSION.SYSTEM_ALERT_WINDOW", "android.permission.system_alert_window")
            for (permission in otherCases) {
                val signals = collector.collect(anInstalledApp(requestedPermissions = setOf(permission)))

                assertEquals(permission, emptySet<Signal>(), signals)
            }
        }

    @Test
    fun `yields only s5 for app with all batch signals`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    requestedPermissions = setOf(SYSTEM_ALERT_WINDOW, READ_SMS, NFC),
                    cardEmulationServices = setOf("com.example.app.CardService"),
                )

            assertEquals(S5, collector.collect(app))
        }

    private companion object {
        const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
        const val READ_SMS = "android.permission.READ_SMS"
        const val NFC = "android.permission.NFC"
        val S5: Set<Signal> = setOf(Signal.OVERLAY_PERMISSION)
    }
}
