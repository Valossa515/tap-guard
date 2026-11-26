package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorTrustedStoreTest(
    private val installer: String,
) {
    @Test
    fun `store installer yields no signal`() =
        runTest {
            val collector = InstallSourceCollector(TrustedStores.packageNames, setOf("com.example.bank"), emptySet())

            val signals = collector.collect(anInstalledApp(installerPackageName = installer))

            assertEquals(emptySet<Signal>(), signals)
            assertEquals(
                emptySet<Signal>(),
                collector.collect(anInstalledApp(packageName = "com.example.bank", installerPackageName = installer)),
            )
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun installers(): List<String> = EXPECTED_TRUSTED_STORES
    }
}
