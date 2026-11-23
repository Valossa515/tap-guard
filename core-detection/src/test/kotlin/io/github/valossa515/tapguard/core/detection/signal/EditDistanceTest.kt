package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Test

class EditDistanceTest {
    @Test
    fun `equal strings have distance zero`() {
        assertEquals(0, editDistance("nubank", "nubank"))
    }

    @Test
    fun `one substitution has distance one`() {
        assertEquals(1, editDistance("nubank", "nubamk"))
    }

    @Test
    fun `one insertion or deletion has distance one`() {
        assertEquals(1, editDistance("bradesco", "brandesco"))
        assertEquals(1, editDistance("santander", "santandr"))
    }

    @Test
    fun `adjacent transposition has distance one`() {
        assertEquals(1, editDistance("nubank", "nubnak"))
    }

    @Test
    fun `empty string distance is the other length`() {
        assertEquals(4, editDistance("", "itau"))
        assertEquals(4, editDistance("itau", ""))
        assertEquals(0, editDistance("", ""))
    }

    @Test
    fun `two edits have distance two`() {
        assertEquals(2, editDistance("bancobrasii", "bancodobrasii"))
    }
}
