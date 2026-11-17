package io.github.valossa515.tapguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val LightColors: ColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightBackground,
        onSurface = LightOnBackground,
    )

internal val DarkColors: ColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkBackground,
        onSurface = DarkOnBackground,
    )

/**
 * Tema do Tap Guard: paleta fixa que segue o modo claro ou escuro do sistema.
 * Não usa cores dinâmicas.
 */
@Composable
fun TapGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
