package io.github.valossa515.tapguard.core.domain.risk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Faixas da seção 5: verde de 0 a 29, amarelo de 30 a 59, vermelho de 60 em diante. */
class RiskScoreConfigTest {
    @Test
    fun `band limits`() {
        assertEquals(RiskLevel.GREEN, RiskScoreConfig.levelFor(0))
        assertEquals(RiskLevel.GREEN, RiskScoreConfig.levelFor(29))
        assertEquals(RiskLevel.YELLOW, RiskScoreConfig.levelFor(30))
        assertEquals(RiskLevel.YELLOW, RiskScoreConfig.levelFor(59))
        assertEquals(RiskLevel.RED, RiskScoreConfig.levelFor(60))
        assertEquals(RiskLevel.RED, RiskScoreConfig.levelFor(100))
    }
}
