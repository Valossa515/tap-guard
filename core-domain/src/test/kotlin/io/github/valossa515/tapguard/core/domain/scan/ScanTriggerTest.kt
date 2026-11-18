package io.github.valossa515.tapguard.core.domain.scan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScanTriggerTest {
    @Test
    fun `entries are manual app open and periodic`() {
        assertEquals(listOf("MANUAL", "APP_OPEN", "PERIODIC"), ScanTrigger.entries.map { it.name })
    }
}
