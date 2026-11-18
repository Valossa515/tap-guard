package io.github.valossa515.tapguard.core.domain.signal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SignalBitMaskTest {
    @Test
    fun `all 512 combinations round trip`() {
        for (mask in 0 until COMBINATIONS) {
            assertEquals(mask, signalsFromBitMask(mask).toBitMask())
        }
    }

    @Test
    fun `empty set is 0 and all signals is 511`() {
        assertEquals(0, emptySet<Signal>().toBitMask())
        assertEquals(emptySet<Signal>(), signalsFromBitMask(0))
        assertEquals(ALL_SIGNALS, Signal.entries.toSet().toBitMask())
        assertEquals(Signal.entries.toSet(), signalsFromBitMask(ALL_SIGNALS))
    }

    @Test
    fun `single signal mask is 1 shifted by its bit`() {
        for (signal in Signal.entries) {
            val mask = setOf(signal).toBitMask()

            assertEquals(1 shl signal.bit, mask)
            assertEquals(setOf(signal), signalsFromBitMask(mask))
        }
    }

    @Test
    fun `negative and above bit 8 masks are rejected`() {
        for (mask in listOf(-1, COMBINATIONS, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { signalsFromBitMask(mask) }
        }
    }

    @Test
    fun `decoded set rejects add through mutable set cast`() {
        val decoded = signalsFromBitMask(setOf(Signal.NFC_PERMISSION).toBitMask()) as MutableSet<Signal>

        assertThrows(UnsupportedOperationException::class.java) { decoded.add(Signal.BANK_LOOKALIKE) }
        assertEquals(setOf(Signal.NFC_PERMISSION), decoded)
    }

    private companion object {
        const val COMBINATIONS = 512
        const val ALL_SIGNALS = 511
    }
}
