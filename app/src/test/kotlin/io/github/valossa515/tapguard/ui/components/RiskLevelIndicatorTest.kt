package io.github.valossa515.tapguard.ui.components

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.valossa515.tapguard.ui.theme.TapGuardTheme
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

    private fun show(level: RiskLevelUi) {
        Robolectric
            .buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .setContent { TapGuardTheme { RiskLevelIndicator(level) } }
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
