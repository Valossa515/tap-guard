package io.github.valossa515.tapguard.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta fixa (sem cores dinâmicas). Pares texto/fundo com contraste AA ou melhor.
// Valores ARGB como constantes nomeadas; as cores do Compose são montadas a partir deles.

private const val LIGHT_PRIMARY = 0xFF1E5AA8
private const val LIGHT_ON_PRIMARY = 0xFFFFFFFF
private const val LIGHT_BACKGROUND = 0xFFFAFBFD
private const val LIGHT_ON_BACKGROUND = 0xFF1A1C1E

private const val DARK_PRIMARY = 0xFFA8C8FF
private const val DARK_ON_PRIMARY = 0xFF003062
private const val DARK_BACKGROUND = 0xFF111318
private const val DARK_ON_BACKGROUND = 0xFFE2E2E6

internal val LightPrimary = Color(LIGHT_PRIMARY)
internal val LightOnPrimary = Color(LIGHT_ON_PRIMARY)
internal val LightBackground = Color(LIGHT_BACKGROUND)
internal val LightOnBackground = Color(LIGHT_ON_BACKGROUND)

internal val DarkPrimary = Color(DARK_PRIMARY)
internal val DarkOnPrimary = Color(DARK_ON_PRIMARY)
internal val DarkBackground = Color(DARK_BACKGROUND)
internal val DarkOnBackground = Color(DARK_ON_BACKGROUND)
