package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorConfigurationTest(
    private val stores: Set<String>,
    private val banks: Set<String>,
    private val cloners: Set<String>,
    private val app: InstalledApp,
    private val expected: Set<Signal>,
) {
    @Test
    fun `configuration uses exact package conjunction`() =
        runTest {
            val collector = InstallSourceCollector(stores, banks, cloners)

            assertEquals(expected, collector.collect(app))
        }

    companion object {
        private const val STORE = "com.example.store"
        private const val BANK = "com.example.bank"
        private const val CLONE = "com.example.clone"
        private const val EXTRA_BANK = "com.example.extra.bank"
        private const val EXTRA_CLONE = "com.example.extra.clone"
        private val STORES = setOf(STORE)
        private val BANKS = setOf(BANK)
        private val CLONERS = setOf(CLONE)
        private val NONE = emptySet<String>()
        private val CLEAR = emptySet<Signal>()
        private val S1 = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)

        private fun app(
            bank: String = BANK,
            installer: String = CLONE,
        ): InstalledApp = anInstalledApp(packageName = bank, installerPackageName = installer, label = "Banco Fictício")

        @JvmStatic
        @Parameterized.Parameters(name = "configuration {index}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(STORES, BANKS, CLONERS, app(), CLEAR),
                arrayOf(STORES, BANKS, CLONERS, app(bank = "COM.EXAMPLE.BANK"), S1),
                arrayOf(STORES, BANKS, CLONERS, app(installer = "COM.EXAMPLE.CLONE"), S1),
                arrayOf(STORES, BANKS, CLONERS, app(bank = "com.example.other"), S1),
                arrayOf(STORES, NONE, CLONERS, app(), S1),
                arrayOf(STORES, BANKS, NONE, app(), S1),
                arrayOf(STORES, NONE, CLONERS, app(installer = STORE), CLEAR),
                arrayOf(STORES, BANKS, NONE, app(installer = STORE), CLEAR),
                arrayOf(NONE, BANKS, CLONERS, app(), CLEAR),
                arrayOf(NONE, NONE, NONE, app(), S1),
                arrayOf(STORES, BANKS, CLONERS, app(installer = STORE), CLEAR),
                arrayOf(STORES, BANKS, CLONERS, app(EXTRA_BANK, EXTRA_CLONE), S1),
                arrayOf(STORES, BANKS + EXTRA_BANK, CLONERS + EXTRA_CLONE, app(EXTRA_BANK, EXTRA_CLONE), CLEAR),
                arrayOf(STORES, BANKS - BANK, CLONERS, app(), S1),
                arrayOf(STORES, BANKS, CLONERS - CLONE, app(), S1),
            )
    }
}
