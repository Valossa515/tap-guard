package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BankNameSkeletonTest(
    private val label: String,
    private val expectedSkeleton: String,
) {
    @Test
    fun `label yields expected skeleton`() {
        assertEquals(expectedSkeleton, bankNameSkeleton(label))
    }

    companion object {
        /** Rótulo e esqueleto esperado, escritos à mão. Letras que não se distinguem na tela vão em escape. */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf("NU BANK", "nubank"),
                arrayOf("Itáú", "itau"),
                arrayOf("Nubank: conta, cartão e mais", "nubank"),
                arrayOf("Itaú | Token", "itau"),
                arrayOf("Next - Launcher", "next"),
                // Meia-risca e travessão.
                arrayOf("Caixa \u2013 Organizador", "caixa"),
                arrayOf("Caixa \u2014 Organizador", "caixa"),
                // Separador no início não corta.
                arrayOf(": Nubank", "nubank"),
                // Largura cheia.
                arrayOf("\uFF2E\uFF55\uFF42\uFF41\uFF4E\uFF4B", "nubank"),
                arrayOf("01!34@5\$7", "oiieaasst"),
                arrayOf("Bradesc0", "bradesco"),
                arrayOf("Calxa", "caixa"),
                // "а" cirílico.
                arrayOf("Nub\u0430nk", "nubank"),
                // Cirílico: а е о р с у х і ӏ ѕ ј к м н т в.
                arrayOf(
                    "\u0430\u0435\u043E\u0440\u0441\u0443\u0445\u0456\u04CF\u0455\u0458\u043A\u043C\u043D\u0442\u0432",
                    "aeopcyxiisjkmhtb",
                ),
                // Grego: α ο ρ ι κ ν τ.
                arrayOf("\u03B1\u03BF\u03C1\u03B9\u03BA\u03BD\u03C4", "aopiknt"),
                arrayOf("rnercado", "mercado"),
                arrayOf("vvave", "wave"),
                // Espaço de largura zero.
                arrayOf("Ita\u200Bú", "itau"),
                // Emoji de banco e pontuação.
                arrayOf("\uD83C\uDFE6 ...?", ""),
                arrayOf("", ""),
            )
    }
}
