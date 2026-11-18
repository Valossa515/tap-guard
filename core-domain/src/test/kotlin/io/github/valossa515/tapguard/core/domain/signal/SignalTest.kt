package io.github.valossa515.tapguard.core.domain.signal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignalTest {
    @Test
    fun `nine entries with bits 0 to 8 in S1 to S9 order`() {
        val expected =
            listOf(
                Signal.UNTRUSTED_INSTALL_SOURCE to 0,
                Signal.NFC_PERMISSION to 1,
                Signal.CARD_EMULATION_SERVICE to 2,
                Signal.ACCESSIBILITY_SERVICE_ENABLED to 3,
                Signal.OVERLAY_PERMISSION to 4,
                Signal.RECENTLY_INSTALLED_OR_UPDATED to 5,
                Signal.UNKNOWN_DEFAULT_PAYMENT_APP to 6,
                Signal.SMS_PERMISSION to 7,
                Signal.BANK_LOOKALIKE to 8,
            )

        assertEquals(expected, Signal.entries.map { it to it.bit })
    }

    @Test
    fun `bits are distinct`() {
        val distinctBits = Signal.entries.map { it.bit }.toSet()

        assertEquals(Signal.entries.size, distinctBits.size)
    }
}
