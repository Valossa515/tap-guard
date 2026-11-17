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

// Níveis de risco: fundo (container) e texto/ícone (content), em claro e escuro.

private const val LIGHT_GREEN_CONTAINER = 0xFFD7F5DD
private const val LIGHT_GREEN_CONTENT = 0xFF0B5D1E
private const val LIGHT_YELLOW_CONTAINER = 0xFFFFF1C2
private const val LIGHT_YELLOW_CONTENT = 0xFF5C4300
private const val LIGHT_RED_CONTAINER = 0xFFFFDAD6
private const val LIGHT_RED_CONTENT = 0xFF8C1D18

private const val DARK_GREEN_CONTAINER = 0xFF0F3D1A
private const val DARK_GREEN_CONTENT = 0xFFA6E9B4
private const val DARK_YELLOW_CONTAINER = 0xFF3F2E00
private const val DARK_YELLOW_CONTENT = 0xFFFFE08A
private const val DARK_RED_CONTAINER = 0xFF5C1410
private const val DARK_RED_CONTENT = 0xFFFFB4AB

internal val LightGreenContainer = Color(LIGHT_GREEN_CONTAINER)
internal val LightGreenContent = Color(LIGHT_GREEN_CONTENT)
internal val LightYellowContainer = Color(LIGHT_YELLOW_CONTAINER)
internal val LightYellowContent = Color(LIGHT_YELLOW_CONTENT)
internal val LightRedContainer = Color(LIGHT_RED_CONTAINER)
internal val LightRedContent = Color(LIGHT_RED_CONTENT)

internal val DarkGreenContainer = Color(DARK_GREEN_CONTAINER)
internal val DarkGreenContent = Color(DARK_GREEN_CONTENT)
internal val DarkYellowContainer = Color(DARK_YELLOW_CONTAINER)
internal val DarkYellowContent = Color(DARK_YELLOW_CONTENT)
internal val DarkRedContainer = Color(DARK_RED_CONTAINER)
internal val DarkRedContent = Color(DARK_RED_CONTENT)
