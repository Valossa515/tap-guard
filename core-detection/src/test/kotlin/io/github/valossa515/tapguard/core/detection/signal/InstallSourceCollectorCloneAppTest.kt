package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorCloneAppTest(
    private val installer: String,
) {
    private val collector =
        InstallSourceCollector(TrustedStores.packageNames, setOf("com.example.bank"), KnownCloneApps.packageNames)

    @Test
    fun `known bank copied by clone app yields no signal`() =
        runTest {
            val app = anInstalledApp(packageName = "com.example.bank", installerPackageName = installer)

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `other app copied by clone app yields s1`() =
        runTest {
            val app = anInstalledApp(packageName = "com.example.other", installerPackageName = installer)

            assertEquals(setOf(Signal.UNTRUSTED_INSTALL_SOURCE), collector.collect(app))
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun installers(): List<String> = EXPECTED_KNOWN_CLONE_APPS
    }
}
