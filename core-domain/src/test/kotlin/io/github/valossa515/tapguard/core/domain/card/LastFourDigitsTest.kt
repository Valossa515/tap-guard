package io.github.valossa515.tapguard.core.domain.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LastFourDigitsTest {
    @Test
    fun `four ascii digits are accepted`() {
        assertEquals(DIGITS, LastFourDigits(DIGITS).value)
    }

    @Test
    fun `three five sixteen digits letters spaces and non ascii digits are rejected`() {
        val invalid = listOf("123", "12345", TEST_CARD_NUMBER, "12a4", " 123", ARABIC_INDIC_DIGITS)

        for (value in invalid) {
            val error = assertThrows(IllegalArgumentException::class.java) { LastFourDigits(value) }

            assertFalse(error.message.orEmpty().contains(value), "a mensagem repete o valor recebido")
        }
    }

    @Test
    fun `toString hides the digits`() {
        assertFalse(LastFourDigits(DIGITS).toString().contains(DIGITS))
    }

    private companion object {
        const val DIGITS = "1234"
        const val TEST_CARD_NUMBER = "4111111111111111"
        const val ARABIC_INDIC_DIGITS = "١٢٣٤"
    }
}
