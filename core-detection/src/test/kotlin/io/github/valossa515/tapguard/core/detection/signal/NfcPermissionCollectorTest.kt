package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NfcPermissionCollectorTest {
    private val collector = NfcPermissionCollector()

    @Test
    fun `nfc permission yields s2`() =
        runTest {
            val app = anInstalledApp(requestedPermissions = setOf(NFC, RECEIVE_SMS))

            assertEquals(S2, collector.collect(app))
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
    fun `nfc permission in other case yields nothing`() =
        runTest {
            for (permission in listOf("ANDROID.PERMISSION.NFC", "android.permission.nfc")) {
                val signals = collector.collect(anInstalledApp(requestedPermissions = setOf(permission)))

                assertEquals(permission, emptySet<Signal>(), signals)
            }
        }

    @Test
    fun `yields only s2 for app with hce and null installer`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = null,
                    requestedPermissions = setOf(NFC),
                    cardEmulationServices = setOf("com.example.app.CardService"),
                )

            assertEquals(S2, collector.collect(app))
        }

    private companion object {
        const val NFC = "android.permission.NFC"
        const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
        val S2: Set<Signal> = setOf(Signal.NFC_PERMISSION)
    }
}
