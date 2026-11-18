package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.aRiskResult
import io.github.valossa515.tapguard.core.domain.signal.Signal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RiskResultTest {
    @Test
    fun `score 0 and 100 are accepted`() {
        assertEquals(0, aRiskResult(score = 0).score)
        assertEquals(100, aRiskResult(score = 100, level = RiskLevel.RED).score)
    }

    @Test
    fun `score minus 1 and 101 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { aRiskResult(score = -1) }
        assertThrows(IllegalArgumentException::class.java) { aRiskResult(score = 101) }
    }

    @Test
    fun `level is not checked against score`() {
        val result = RiskResult(score = 65, level = RiskLevel.GREEN, signals = setOf(Signal.NFC_PERMISSION))

        assertEquals(65, result.score)
        assertEquals(RiskLevel.GREEN, result.level)
        assertEquals(setOf(Signal.NFC_PERMISSION), result.signals)
    }

    @Test
    fun `mutating source signals does not change the model`() {
        val signals = mutableSetOf(Signal.NFC_PERMISSION)
        val result = aRiskResult(signals = signals)

        signals.add(Signal.BANK_LOOKALIKE)

        assertEquals(setOf(Signal.NFC_PERMISSION), result.signals)
    }

    @Test
    fun `exposed signals reject add remove and clear through mutable set cast`() {
        val result = aRiskResult(signals = setOf(Signal.NFC_PERMISSION))
        val mutable = result.signals as MutableSet<Signal>

        assertThrows(UnsupportedOperationException::class.java) { mutable.add(Signal.BANK_LOOKALIKE) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.remove(Signal.NFC_PERMISSION) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.clear() }
        assertEquals(setOf(Signal.NFC_PERMISSION), result.signals)
    }
}
