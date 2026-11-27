package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallSourceCollectorTest {
    private val defaultCollector = InstallSourceCollector(TrustedStores.packageNames, emptySet(), emptySet())

    @Test
    fun `null installer yields s1`() =
        runTest {
            val signals = defaultCollector.collect(anInstalledApp(installerPackageName = null))

            assertEquals(S1, signals)
        }

    @Test
    fun `uppercase store package yields s1`() =
        runTest {
            val signals = defaultCollector.collect(anInstalledApp(installerPackageName = "COM.ANDROID.VENDING"))

            assertEquals(S1, signals)
        }

    @Test
    fun `yields only s1 for app with other risk traits`() =
        runTest {
            val app =
                anInstalledApp(
                    installerPackageName = "com.example.store",
                    requestedPermissions = setOf("android.permission.NFC"),
                    services = setOf("com.example.store.PaymentService"),
                )

            assertEquals(S1, defaultCollector.collect(app))
        }

    @Test
    fun `extended list trusts extra store and keeps default`() =
        runTest {
            val collector = InstallSourceCollector(TrustedStores.packageNames + BANK_STORE, emptySet(), emptySet())

            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp(installerPackageName = BANK_STORE)))
            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp(installerPackageName = PLAY_STORE)))
        }

    @Test
    fun `smaller list flags removed store`() =
        runTest {
            val collector = InstallSourceCollector(TrustedStores.packageNames - AMAZON_APPSTORE, emptySet(), emptySet())

            assertEquals(S1, collector.collect(anInstalledApp(installerPackageName = AMAZON_APPSTORE)))
        }

    @Test
    fun `later change to given set does not affect collector`() =
        runTest {
            val given = mutableSetOf(BANK_STORE)
            val collector = InstallSourceCollector(given, emptySet(), emptySet())

            given.remove(BANK_STORE)

            assertEquals(emptySet<Signal>(), collector.collect(anInstalledApp(installerPackageName = BANK_STORE)))
        }

    @Test
    fun `default list has exactly the expected stores`() {
        assertEquals(EXPECTED_TRUSTED_STORES.toSet(), TrustedStores.packageNames)
    }

    @Test
    fun `default list rejects mutation through cast`() {
        val packageNames = TrustedStores.packageNames as MutableSet<String>

        assertThrows(UnsupportedOperationException::class.java) {
            packageNames.add("com.example.store")
        }
    }

    private companion object {
        val S1: Set<Signal> = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)
        const val BANK_STORE = "com.example.bankstore"
        const val PLAY_STORE = "com.android.vending"
        const val AMAZON_APPSTORE = "com.amazon.venezia"
    }
}
