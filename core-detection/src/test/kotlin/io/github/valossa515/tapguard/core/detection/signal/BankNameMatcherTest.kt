package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankNameMatcherTest {
    @Test
    fun `prefix and suffixes are removed including glued and repeated`() {
        val matcher = BankNameMatcher(listOf("Itaú"))

        listOf("Banco Itaú", "ItauToken", "Itaú Cartões Empresas", "Itaú PJ Mobile Oficial Digital")
            .forEach { assertTrue(it, matcher.matches(it)) }
        assertFalse(matcher.matches("Banco Meu Itaú"))
    }

    @Test
    fun `every loaded prefix and suffix is removed`() {
        val matcher = BankNameMatcher(listOf("Itaú"))

        assertEquals(3, BANK_NAME_PREFIXES.size)
        assertEquals(11, BANK_NAME_SUFFIXES.size)
        BANK_NAME_PREFIXES.forEach { assertTrue(it, matcher.matches("$it Itaú")) }
        BANK_NAME_SUFFIXES.forEach { assertTrue(it, matcher.matches("Banco Itaú $it")) }
    }

    @Test
    fun `five letter name needs equal skeleton`() {
        val matcher = BankNameMatcher(listOf("Caixa"))

        assertTrue(matcher.matches("CAIXA"))
        assertFalse(matcher.matches("Baixa"))
        assertFalse(matcher.matches("Caixas"))
    }

    @Test
    fun `six letter name tolerates one edit but not two`() {
        val matcher = BankNameMatcher(listOf("Nubank"))

        assertTrue(matcher.matches("Nubamk"))
        assertTrue(matcher.matches("Nubnak"))
        assertFalse(matcher.matches("Nibamk"))
    }

    @Test
    fun `nine letter name tolerates one edit but not two`() {
        val matcher = BankNameMatcher(listOf("Santander"))

        assertTrue(matcher.matches("Santandr"))
        assertFalse(matcher.matches("Santadr"))
    }

    @Test
    fun `ten letter name tolerates two edits but not three`() {
        val matcher = BankNameMatcher(listOf(TEN_LETTER_NAME))

        assertTrue(matcher.matches("Brazilcard"))
        assertTrue(matcher.matches("Brazilcart"))
        assertFalse(matcher.matches("Brazilkart"))
    }

    @Test
    fun `label with empty skeleton never matches`() {
        val matcher = BankNameMatcher(listOf("Nubank"))

        assertFalse(matcher.matches(EMOJI_AND_PUNCTUATION))
        assertFalse(matcher.matches(""))
    }

    @Test
    fun `bank name with empty skeleton is ignored`() {
        val matcher = BankNameMatcher(listOf(EMOJI_AND_PUNCTUATION, "Nubank"))

        assertTrue(matcher.matches("Nubank"))
        assertFalse(matcher.matches(EMOJI_AND_PUNCTUATION))
        assertFalse(matcher.matches(""))
    }

    @Test
    fun `empty name list never matches`() {
        assertFalse(BankNameMatcher(emptyList()).matches("Nubank"))
    }

    /** Lê a lista de produção de propósito: uma entrada que colida com outra instituição derruba o teste. */
    @Test
    fun `known bank names only match hand-written same-bank pairs`() {
        val apps = KnownBanks.apps

        val matchingPairs =
            apps
                .flatMap { app ->
                    apps
                        .filter { it.packageName != app.packageName }
                        .filter { BankNameMatcher(listOf(it.displayName)).matches(app.displayName) }
                        .map { app.displayName to it.displayName }
                }.toSet()

        assertEquals(SAME_BANK_PAIRS, matchingPairs)
    }

    private companion object {
        /** Nome fictício com esqueleto de 10 letras (`brasiicard`). */
        const val TEN_LETTER_NAME = "Brasilcard"

        /** Emoji de banco e pontuação. */
        const val EMOJI_AND_PUNCTUATION = "\uD83C\uDFE6 ...?"

        /** Pares (rótulo, nome) de apps do mesmo banco que casam, escritos à mão; os pacotes estão na lista. */
        val SAME_BANK_PAIRS =
            setOf(
                "Itaú Empresas" to "Itaú",
                "Bradesco Cartões" to "Bradesco",
                "Bradesco Empresas" to "Bradesco",
                "Santander Empresas" to "Santander",
                "Inter Empresas" to "Inter",
                "BTG Pactual Empresas" to "BTG Pactual",
            )
    }
}
