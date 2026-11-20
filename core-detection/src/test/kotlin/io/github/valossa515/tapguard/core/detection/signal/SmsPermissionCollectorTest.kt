package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsPermissionCollectorTest {
    private val collector = SmsPermissionCollector()

    @Test
    fun `read sms yields s8`() =
        runTest {
            assertEquals(S8, collector.collect(anInstalledApp(requestedPermissions = setOf(READ_SMS))))
        }

    @Test
    fun `receive sms yields s8`() =
        runTest {
            assertEquals(S8, collector.collect(anInstalledApp(requestedPermissions = setOf(RECEIVE_SMS))))
        }

    @Test
    fun `both sms permissions yield one s8`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = setOf(READ_SMS, RECEIVE_SMS))

            assertEquals(S8, collector.collect(app))
        }

    @Test
    fun `no permissions yields nothing`() =
        runTest {
            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp(requestedPermissions = emptySet())))
        }

    @Test
    fun `unrelated permission yields nothing`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = setOf(NFC))

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `sms permission in other case yields nothing`() =
        runTest {
            for (permission in listOf("ANDROID.PERMISSION.READ_SMS", "android.permission.receive_sms")) {
                val signals = collector.collect(anInstalledApp(requestedPermissions = setOf(permission)))

                assertEquals(permission, emptySet<Signal>(), signals)
            }
        }

    @Test
    fun `yields only s8 for app with all batch signals`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    requestedPermissions = setOf(SYSTEM_ALERT_WINDOW, READ_SMS, NFC),
                    cardEmulationServices = setOf("com.example.app.CardService"),
                )

            assertEquals(S8, collector.collect(app))
        }

    private companion object {
        const val READ_SMS = "android.permission.READ_SMS"
        const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
        const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        const val NFC = "android.permission.NFC"
        val S8: Set<Signal> = setOf(Signal.SMS_PERMISSION)
    }
}
