package io.github.valossa515.tapguard.core.domain.risk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RiskLevelTest {
    @Test
    fun `severity order is green yellow red`() {
        assertTrue(RiskLevel.GREEN < RiskLevel.YELLOW)
        assertTrue(RiskLevel.YELLOW < RiskLevel.RED)
    }

    @Test
    fun `names match persisted values`() {
        assertEquals(listOf("GREEN", "YELLOW", "RED"), RiskLevel.entries.map { it.name })
    }
}
