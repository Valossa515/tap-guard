package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SmsPermissionCollectorUnrelatedPermissionTest(
    private val permission: String,
) {
    @Test
    fun `unrelated sms permission yields nothing`() =
        runTest {
            val signals = SmsPermissionCollector().collect(anInstalledApp(requestedPermissions = setOf(permission)))

            assertEquals(emptySet<Signal>(), signals)
        }

    companion object {
        /** Permissões ligadas a SMS que não geram S8 (ADR-019). */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun permissions(): List<String> =
            listOf(
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_MMS",
                "android.permission.RECEIVE_WAP_PUSH",
                "android.permission.READ_CELL_BROADCASTS",
                "android.permission.BROADCAST_SMS",
            )
    }
}
