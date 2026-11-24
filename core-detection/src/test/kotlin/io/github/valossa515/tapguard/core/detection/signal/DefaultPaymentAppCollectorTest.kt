package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Coletor de S7 com uma lista pequena escrita à mão, não [KnownWallets.packageNames]: mexer na lista
 * de produção não muda estes testes (o conteúdo dela é testado no `KnownWalletsTest`).
 */
class DefaultPaymentAppCollectorTest {
    private val collector = DefaultPaymentAppCollector(WALLETS)

    @Test
    fun `default outside list yields s7`() =
        runTest {
            assertEquals(S7, collector.collect(defaultPaymentApp(SCAM_APP)))
        }

    @Test
    fun `default in list yields nothing`() =
        runTest {
            assertEquals(emptySet<Signal>(), collector.collect(defaultPaymentApp(WALLET_ONE)))
        }

    @Test
    fun `app that is not default yields nothing`() =
        runTest {
            val app = anInstalledApp(packageName = SCAM_APP, cardEmulationServices = setOf(SERVICE))

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `package differing by case yields s7`() =
        runTest {
            for (packageName in listOf("COM.WALLET.ONE", "com.Wallet.one")) {
                assertEquals(packageName, S7, collector.collect(defaultPaymentApp(packageName)))
            }
        }

    @Test
    fun `later change to given set does not affect collector`() =
        runTest {
            val given = mutableSetOf(WALLET_ONE)
            val copied = DefaultPaymentAppCollector(given)

            given.add(SCAM_APP)

            assertEquals(S7, copied.collect(defaultPaymentApp(SCAM_APP)))
        }

    @Test
    fun `empty list yields s7 for every default app`() =
        runTest {
            val withoutWallets = DefaultPaymentAppCollector(emptySet())

            assertEquals(S7, withoutWallets.collect(defaultPaymentApp(WALLET_ONE)))
        }

    @Test
    fun `yields only s7 for app with other risk traits`() =
        runTest {
            val app =
                anInstalledApp(
                    packageName = SCAM_APP,
                    installerPackageName = null,
                    requestedPermissions = setOf(NFC, OVERLAY, READ_SMS),
                    services = setOf(SERVICE, A11Y_SERVICE),
                    cardEmulationServices = setOf(SERVICE),
                    enabledAccessibilityServices = setOf(A11Y_SERVICE),
                    defaultPaymentServices = setOf(SERVICE),
                )

            assertEquals(S7, collector.collect(app))
        }

    private fun defaultPaymentApp(packageName: String): InstalledApp =
        anInstalledApp(
            packageName = packageName,
            cardEmulationServices = setOf(SERVICE),
            defaultPaymentServices = setOf(SERVICE),
        )

    private companion object {
        val S7: Set<Signal> = setOf(Signal.UNKNOWN_DEFAULT_PAYMENT_APP)
        const val WALLET_ONE = "com.wallet.one"
        const val WALLET_TWO = "com.wallet.two"
        val WALLETS: Set<String> = setOf(WALLET_ONE, WALLET_TWO)
        const val SCAM_APP = "com.scam.app"
        const val SERVICE = "com.example.PaymentService"
        const val A11Y_SERVICE = "com.example.A11yService"
        const val NFC = "android.permission.NFC"
        const val OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"
        const val READ_SMS = "android.permission.READ_SMS"
    }
}
