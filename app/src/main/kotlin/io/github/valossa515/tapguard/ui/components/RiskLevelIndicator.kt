package io.github.valossa515.tapguard.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.valossa515.tapguard.R
import io.github.valossa515.tapguard.ui.theme.LocalTapGuardDarkTheme

/**
 * Mostra um nível de risco sempre com cor, ícone e frase (RNF-06): o nível nunca é
 * comunicado só pela cor. As cores seguem o modo do `TapGuardTheme` em volta.
 */
@Composable
fun RiskLevelIndicator(
    level: RiskLevelUi,
    modifier: Modifier = Modifier,
) {
    val colors = riskLevelColors(level, LocalTapGuardDarkTheme.current)
    Row(
        modifier =
            modifier
                .background(color = colors.container, shape = RoundedCornerShape(size = 12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = level.iconRes()),
            contentDescription = stringResource(id = level.iconDescriptionRes()),
            tint = colors.content,
        )
        Text(
            text = stringResource(id = level.textRes()),
            color = colors.content,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@DrawableRes
private fun RiskLevelUi.iconRes(): Int =
    when (this) {
        RiskLevelUi.GREEN -> R.drawable.ic_level_green
        RiskLevelUi.YELLOW -> R.drawable.ic_level_yellow
        RiskLevelUi.RED -> R.drawable.ic_level_red
    }

@StringRes
private fun RiskLevelUi.iconDescriptionRes(): Int =
    when (this) {
        RiskLevelUi.GREEN -> R.string.risk_level_green_icon_description
        RiskLevelUi.YELLOW -> R.string.risk_level_yellow_icon_description
        RiskLevelUi.RED -> R.string.risk_level_red_icon_description
    }

@StringRes
private fun RiskLevelUi.textRes(): Int =
    when (this) {
        RiskLevelUi.GREEN -> R.string.risk_level_green_text
        RiskLevelUi.YELLOW -> R.string.risk_level_yellow_text
        RiskLevelUi.RED -> R.string.risk_level_red_text
    }
