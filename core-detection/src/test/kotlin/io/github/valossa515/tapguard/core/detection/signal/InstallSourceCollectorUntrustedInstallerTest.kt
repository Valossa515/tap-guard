package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorUntrustedInstallerTest(
    private val installer: String,
) {
    @Test
    fun `untrusted installer yields s1`() =
        runTest {
            val collector = InstallSourceCollector(TrustedStores.packageNames, emptySet(), emptySet())

            val signals = collector.collect(anInstalledApp(installerPackageName = installer))

            assertEquals(setOf(Signal.UNTRUSTED_INSTALL_SOURCE), signals)
        }

    companion object {
        /** Lojas recusadas, app de clonagem e instaladores do sistema (ADR-017). */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun installers(): List<String> =
            listOf(
                "com.example.store",
                "org.fdroid.fdroid",
                "com.aurora.store",
                "com.sec.android.easyMover",
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.miui.packageinstaller",
                "com.android.shell",
            )
    }
}
