package io.github.valossa515.tapguard.ui.components

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import io.github.valossa515.tapguard.ui.theme.TapGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RiskLevelIndicatorTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `green shows check icon and exact phrase in light mode`() {
        show(RiskLevelUi.GREEN)

        compose.onNodeWithText(GREEN_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(GREEN_ICON).assertIsDisplayed()
    }

    @Test
    fun `yellow shows warning icon and exact phrase in light mode`() {
        show(RiskLevelUi.YELLOW)

        compose.onNodeWithText(YELLOW_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(YELLOW_ICON).assertIsDisplayed()
    }

    @Test
    fun `red shows error icon and exact phrase in light mode`() {
        show(RiskLevelUi.RED)

        compose.onNodeWithText(RED_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(RED_ICON).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `green shows check icon and exact phrase in dark mode`() {
        show(RiskLevelUi.GREEN)

        compose.onNodeWithText(GREEN_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(GREEN_ICON).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `yellow shows warning icon and exact phrase in dark mode`() {
        show(RiskLevelUi.YELLOW)

        compose.onNodeWithText(YELLOW_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(YELLOW_ICON).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `red shows error icon and exact phrase in dark mode`() {
        show(RiskLevelUi.RED)

        compose.onNodeWithText(RED_TEXT).assertIsDisplayed()
        compose.onNodeWithContentDescription(RED_ICON).assertIsDisplayed()
    }

    @Test
    fun `forced dark theme on light system uses dark level colors`() {
        show(RiskLevelUi.GREEN, forcedDarkTheme = true)

        assertEquals(riskLevelColors(RiskLevelUi.GREEN, darkTheme = true).content, textColor(GREEN_TEXT))
    }

    private fun show(
        level: RiskLevelUi,
        forcedDarkTheme: Boolean? = null,
    ) {
        Robolectric
            .buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .setContent {
                if (forcedDarkTheme == null) {
                    TapGuardTheme { RiskLevelIndicator(level) }
                } else {
                    TapGuardTheme(darkTheme = forcedDarkTheme) { RiskLevelIndicator(level) }
                }
            }
    }

    private fun textColor(text: String): Color {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        return layouts
            .single()
            .layoutInput
            .style
            .color
    }

    private companion object {
        // Frases exatas da especificação (seção 10).
        const val GREEN_TEXT = "Nada suspeito encontrado."
        const val YELLOW_TEXT = "Encontramos apps que merecem atenção."
        const val RED_TEXT = "Encontramos um app com perfil de golpe. Veja o que fazer."
        const val GREEN_ICON = "Nível verde"
        const val YELLOW_ICON = "Nível amarelo"
        const val RED_ICON = "Nível vermelho"
    }
}
