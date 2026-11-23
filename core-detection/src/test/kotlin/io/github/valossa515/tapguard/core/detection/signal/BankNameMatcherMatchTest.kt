package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Nomes de banco escritos à mão, um por instituição da tabela do plano. Os testes da comparação não
 * leem [KnownBanks] de produção: assim, mexer na lista não muda o que eles documentam.
 */
internal val HAND_WRITTEN_BANK_NAMES: List<String> =
    listOf(
        "Nubank",
        "Itaú",
        "Bradesco",
        "CAIXA",
        "next",
        "Banco do Brasil",
        "Santander",
        "Mercado Pago",
        "PicPay",
        "Inter",
        "PagBank",
        "C6 Bank",
        "Neon",
    )

@RunWith(Parameterized::class)
class BankNameMatcherMatchTest(
    private val label: String,
) {
    @Test
    fun `label matches a bank name`() {
        assertTrue(BankNameMatcher(HAND_WRITTEN_BANK_NAMES).matches(label))
    }

    companion object {
        /** Imitadores da tabela do plano e, separados, os falsos positivos aceitos (ADR-020). */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun labels(): List<String> =
            listOf(
                "Nubank",
                "NUBANK ",
                "Nu Bank",
                "Nubank: conta, cartão e mais",
                // "а" cirílico.
                "Nub\u0430nk",
                // Largura cheia.
                "\uFF2E\uFF55\uFF42\uFF41\uFF4E\uFF4B",
                "Nubnak",
                "Nubamk",
                "Itau",
                "Itáú",
                "1taú",
                "Ltaú",
                "Itaú Token",
                "ItauToken",
                "Itaú | Token",
                "Banco Itaú",
                "Itaú Cartões Empresas",
                // Espaço de largura zero.
                "Itaú\u200B",
                "Bradesc0",
                "Brandesco",
                "Bradesco App",
                "Meu Bradesco",
                "Calxa",
                "Santandr",
                "PicPay Segurança",
                "Pic Pay",
                "PicPey",
                "Mercad0 Pag0",
                "Banco Brasil",
                "Banco PagBank: Conta, Tap On +",
                "C6 Bank: Cartão, Conta e Mais!",
                // Falsos positivos aceitos: nome curto igual de outro desenvolvedor (um clube, uma loja de roupas).
                "Inter",
                "NEXT",
                // Falsos positivos aceitos: o corte no separador deixa só o nome do banco.
                "Next - Launcher",
                "Caixa \u2013 Organizador",
            )
    }
}
