package io.github.valossa515.tapguard.core.domain.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.YearMonth

class CardReadResultTest {
    private val success = CardReadResult.Success(LABEL, LastFourDigits(DIGITS), EXPIRY)

    @Test
    fun `exhaustive when covers success unsupported card and read interrupted`() {
        val results = listOf(success, CardReadResult.UnsupportedCard, CardReadResult.ReadInterrupted)

        val outcomes =
            results.map { result ->
                when (result) {
                    is CardReadResult.Success -> "success"
                    CardReadResult.UnsupportedCard -> "unsupported"
                    CardReadResult.ReadInterrupted -> "interrupted"
                }
            }

        assertEquals(listOf("success", "unsupported", "interrupted"), outcomes)
    }

    @Test
    fun `success holds label last four digits and expiry`() {
        assertEquals(LABEL, success.applicationLabel)
        assertEquals(LastFourDigits(DIGITS), success.lastFourDigits)
        assertEquals(EXPIRY, success.expiry)
    }

    @Test
    fun `success toString hides digits expiry and label`() {
        val text = success.toString()

        assertFalse(text.contains(DIGITS))
        assertFalse(text.contains("2030"))
        assertFalse(text.contains(LABEL))
    }

    private companion object {
        const val LABEL = "CARTAO TESTE"
        const val DIGITS = "1234"
        val EXPIRY: YearMonth = YearMonth.of(2030, 12)
    }
}
