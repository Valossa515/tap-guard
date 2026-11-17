package io.github.valossa515.tapguard.ui.components

import androidx.compose.ui.graphics.Color
import io.github.valossa515.tapguard.ui.theme.DarkGreenContainer
import io.github.valossa515.tapguard.ui.theme.DarkGreenContent
import io.github.valossa515.tapguard.ui.theme.DarkRedContainer
import io.github.valossa515.tapguard.ui.theme.DarkRedContent
import io.github.valossa515.tapguard.ui.theme.DarkYellowContainer
import io.github.valossa515.tapguard.ui.theme.DarkYellowContent
import io.github.valossa515.tapguard.ui.theme.LightGreenContainer
import io.github.valossa515.tapguard.ui.theme.LightGreenContent
import io.github.valossa515.tapguard.ui.theme.LightRedContainer
import io.github.valossa515.tapguard.ui.theme.LightRedContent
import io.github.valossa515.tapguard.ui.theme.LightYellowContainer
import io.github.valossa515.tapguard.ui.theme.LightYellowContent

/** Cores de um nível: [container] é o fundo e [content] é a cor do texto e do ícone. */
data class RiskLevelColors(
    val container: Color,
    val content: Color,
)

/** Cores de cada nível, em claro ou escuro, a partir da paleta fixa do tema. Função pura. */
fun riskLevelColors(
    level: RiskLevelUi,
    darkTheme: Boolean,
): RiskLevelColors =
    if (darkTheme) {
        when (level) {
            RiskLevelUi.GREEN -> RiskLevelColors(DarkGreenContainer, DarkGreenContent)
            RiskLevelUi.YELLOW -> RiskLevelColors(DarkYellowContainer, DarkYellowContent)
            RiskLevelUi.RED -> RiskLevelColors(DarkRedContainer, DarkRedContent)
        }
    } else {
        when (level) {
            RiskLevelUi.GREEN -> RiskLevelColors(LightGreenContainer, LightGreenContent)
            RiskLevelUi.YELLOW -> RiskLevelColors(LightYellowContainer, LightYellowContent)
            RiskLevelUi.RED -> RiskLevelColors(LightRedContainer, LightRedContent)
        }
    }
