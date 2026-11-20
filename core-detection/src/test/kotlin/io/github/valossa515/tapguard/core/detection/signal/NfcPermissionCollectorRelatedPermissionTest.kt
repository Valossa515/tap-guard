package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NfcPermissionCollectorRelatedPermissionTest(
    private val permission: String,
) {
    @Test
    fun `related permission yields nothing`() =
        runTest {
            val signals = NfcPermissionCollector().collect(anInstalledApp(requestedPermissions = setOf(permission)))

            assertEquals(emptySet<Signal>(), signals)
        }

    companion object {
        /** Permissões ligadas a NFC que não geram S2 (ADR-018). */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun permissions(): List<String> =
            listOf(
                "android.permission.NFC_TRANSACTION_EVENT",
                "android.permission.NFC_PREFERRED_PAYMENT_INFO",
                "android.permission.BIND_NFC_SERVICE",
            )
    }
}
