package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BankNameMatcherNoMatchTest(
    private val label: String,
) {
    @Test
    fun `label matches no bank name`() {
        assertFalse(BankNameMatcher(HAND_WRITTEN_BANK_NAMES).matches(label))
    }

    companion object {
        /** Apps legítimos da tabela do plano e, separadas, as imitações que escapam (ADR-020). */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun labels(): List<String> =
            listOf(
                "Baixa",
                "Caixinha",
                "Caixa de Ferramentas",
                "Next Launcher",
                "Neon Light",
                "Itaú Shop",
                "Mercado Livre",
                "Bradesco Seguros",
                "C6",
                // Imitações que escapam: palavras fora da lista fechada de prefixos e sufixos.
                "Atualização Nubank",
                "Nubank Wallpapers",
                "Inter&Co",
                "Neon Pay",
            )
    }
}
