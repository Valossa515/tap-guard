package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorCopiedSetsTest(
    private val changedSet: String,
) {
    @Test
    fun `later removal and addition do not change accepted or rejected app`() =
        runTest {
            val stores = mutableSetOf(STORE)
            val banks = mutableSetOf(BANK)
            val cloners = mutableSetOf(CLONE)
            val collector = InstallSourceCollector(stores, banks, cloners)
            val accepted =
                anInstalledApp(
                    packageName = BANK,
                    installerPackageName = if (changedSet == "stores") STORE else CLONE,
                )
            val rejected =
                anInstalledApp(
                    packageName = if (changedSet == "banks") EXTRA else BANK,
                    installerPackageName = if (changedSet == "banks") CLONE else EXTRA,
                )

            when (changedSet) {
                "stores" -> {
                    stores.remove(STORE)
                    stores.add(EXTRA)
                }

                "banks" -> {
                    banks.remove(BANK)
                    banks.add(EXTRA)
                }

                "cloners" -> {
                    cloners.remove(CLONE)
                    cloners.add(EXTRA)
                }

                else -> {
                    error("Unknown test set")
                }
            }

            assertEquals(emptySet<Signal>(), collector.collect(accepted))
            assertEquals(setOf(Signal.UNTRUSTED_INSTALL_SOURCE), collector.collect(rejected))
        }

    companion object {
        private const val STORE = "com.example.store"
        private const val BANK = "com.example.bank"
        private const val CLONE = "com.example.clone"
        private const val EXTRA = "com.example.extra"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun sets(): List<String> = listOf("stores", "banks", "cloners")
    }
}
