package io.github.valossa515.tapguard.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskLevelColorsTest {
    @Test
    fun `three levels have distinct colors in light mode`() {
        val colors = RiskLevelUi.entries.map { riskLevelColors(it, darkTheme = false) }

        assertEquals(3, colors.map { it.container }.distinct().size)
        assertEquals(3, colors.map { it.content }.distinct().size)
    }

    @Test
    fun `three levels have distinct colors in dark mode`() {
        val colors = RiskLevelUi.entries.map { riskLevelColors(it, darkTheme = true) }

        assertEquals(3, colors.map { it.container }.distinct().size)
        assertEquals(3, colors.map { it.content }.distinct().size)
    }

    @Test
    fun `each level content on container has contrast of at least 4,5 to 1 in light mode`() {
        RiskLevelUi.entries.forEach { level ->
            val colors = riskLevelColors(level, darkTheme = false)

            val ratio = contrastRatio(colors.content, colors.container)

            assertTrue("$level claro: contraste $ratio", ratio >= MIN_AA_CONTRAST)
        }
    }

    @Test
    fun `each level content on container has contrast of at least 4,5 to 1 in dark mode`() {
        RiskLevelUi.entries.forEach { level ->
            val colors = riskLevelColors(level, darkTheme = true)

            val ratio = contrastRatio(colors.content, colors.container)

            assertTrue("$level escuro: contraste $ratio", ratio >= MIN_AA_CONTRAST)
        }
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val MIN_AA_CONTRAST = 4.5
        const val LUMINANCE_OFFSET = 0.05
    }
}
