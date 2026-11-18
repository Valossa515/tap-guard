package io.github.valossa515.tapguard.core.domain.risk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ScoringContextTest {
    @Test
    fun `holds the trusted packages`() {
        assertEquals(setOf(TRUSTED), ScoringContext(trustedPackages = setOf(TRUSTED)).trustedPackages)
    }

    @Test
    fun `mutating source trusted packages does not change the model`() {
        val trusted = mutableSetOf(TRUSTED)
        val context = ScoringContext(trustedPackages = trusted)

        trusted.add(OTHER)

        assertEquals(setOf(TRUSTED), context.trustedPackages)
    }

    @Test
    fun `exposed trusted packages reject add remove and clear through mutable set cast`() {
        val context = ScoringContext(trustedPackages = setOf(TRUSTED))
        val mutable = context.trustedPackages as MutableSet<String>

        assertThrows(UnsupportedOperationException::class.java) { mutable.add(OTHER) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.remove(TRUSTED) }
        assertThrows(UnsupportedOperationException::class.java) { mutable.clear() }
        assertEquals(setOf(TRUSTED), context.trustedPackages)
    }

    private companion object {
        const val TRUSTED = "com.example.trusted"
        const val OTHER = "com.example.other"
    }
}
