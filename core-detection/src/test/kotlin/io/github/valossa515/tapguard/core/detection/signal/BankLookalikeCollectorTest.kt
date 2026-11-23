package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BankLookalikeCollectorTest {
    private val collector = BankLookalikeCollector(TEST_BANK_APPS)

    @Test
    fun `official package yields nothing`() =
        runTest {
            val signals = collector.collect(app(NUBANK_PACKAGE, "Nubank"))

            assertEquals(emptySet<Signal>(), signals)
        }

    /** "Itaú Empresas" casa com "Itaú" depois de tirar o sufixo, mas o pacote é de uma entrada da lista. */
    @Test
    fun `package of another app of the same bank yields nothing`() =
        runTest {
            val signals = collector.collect(app(ITAU_EMPRESAS_PACKAGE, "Itaú Empresas"))

            assertEquals(emptySet<Signal>(), signals)
        }

    @Test
    fun `bank name with other package yields s9`() =
        runTest {
            val signals = collector.collect(app(OTHER_PACKAGE, "Nubank"))

            assertEquals(S9, signals)
        }

    @Test
    fun `package differing by suffix or case yields s9`() =
        runTest {
            listOf("$NUBANK_PACKAGE.fake", NUBANK_PACKAGE.uppercase()).forEach { packageName ->
                assertEquals(packageName, S9, collector.collect(app(packageName, "Nubank")))
            }
        }

    /**
     * Limitação do pacote emprestado (ADR-017, ADR-020, #65): num aparelho sem o Nubank, um app de golpe
     * instalado à mão com o pacote oficial não recebe S9. Quem o pega é o S1 (instalador nulo).
     */
    @Test
    fun `borrowed official package yields nothing`() =
        runTest {
            val app =
                anInstalledApp(
                    packageName = NUBANK_PACKAGE,
                    label = "Nubank",
                    installerPackageName = null,
                    cardEmulationServices = setOf("com.nu.production.PayService"),
                )

            assertEquals(emptySet<Signal>(), collector.collect(app))
        }

    @Test
    fun `unrelated name yields nothing`() =
        runTest {
            val signals = collector.collect(app(OTHER_PACKAGE, "Calculadora"))

            assertEquals(emptySet<Signal>(), signals)
        }

    @Test
    fun `extended list flags fictitious bank imitator`() =
        runTest {
            val extended = BankLookalikeCollector(TEST_BANK_APPS + KnownBankApp("Banco Fictício", FICTITIOUS_PACKAGE))
            val official = app(FICTITIOUS_PACKAGE, "Banco Fictício")

            assertEquals(S9, extended.collect(app(OTHER_PACKAGE, "Banco Ficticio")))
            assertEquals(emptySet<Signal>(), extended.collect(official))
            assertEquals(S9, extended.collect(app(OTHER_PACKAGE, "Nubank")))
        }

    @Test
    fun `empty list never yields s9`() =
        runTest {
            val empty = BankLookalikeCollector(emptySet())

            assertEquals(emptySet<Signal>(), empty.collect(app(OTHER_PACKAGE, "Nubank")))
        }

    @Test
    fun `later change to given set does not affect collector`() =
        runTest {
            val given = TEST_BANK_APPS.toMutableSet()
            val copied = BankLookalikeCollector(given)

            given.clear()

            assertEquals(S9, copied.collect(app(OTHER_PACKAGE, "Nubank")))
            assertEquals(emptySet<Signal>(), copied.collect(app(NUBANK_PACKAGE, "Nubank")))
        }

    @Test
    fun `yields only s9 for app with other risk traits`() =
        runTest {
            val app =
                anInstalledApp(
                    packageName = OTHER_PACKAGE,
                    label = "Nubank",
                    installerPackageName = null,
                    requestedPermissions = setOf("android.permission.NFC"),
                    cardEmulationServices = setOf("com.fake.app.PayService"),
                )

            assertEquals(S9, collector.collect(app))
        }

    private companion object {
        val S9: Set<Signal> = setOf(Signal.BANK_LOOKALIKE)
        const val NUBANK_PACKAGE = "com.nu.production"
        const val ITAU_EMPRESAS_PACKAGE = "com.itau.empresas"
        const val FICTITIOUS_PACKAGE = "com.example.bancoficticio"
        const val OTHER_PACKAGE = "com.fake.app"

        fun app(
            packageName: String,
            label: String,
        ) = anInstalledApp(packageName = packageName, label = label)

        /** Lista pequena escrita à mão; os testes não leem [KnownBanks]. */
        val TEST_BANK_APPS: Set<KnownBankApp> =
            setOf(
                KnownBankApp("Nubank", NUBANK_PACKAGE),
                KnownBankApp("Itaú", "com.itau"),
                KnownBankApp("Itaú Empresas", ITAU_EMPRESAS_PACKAGE),
            )
    }
}
