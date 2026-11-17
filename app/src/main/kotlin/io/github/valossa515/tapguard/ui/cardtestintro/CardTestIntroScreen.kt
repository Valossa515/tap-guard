package io.github.valossa515.tapguard.ui.cardtestintro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.valossa515.tapguard.R

/** T5 Teste do cartão: explicação (vazia). Sai para a T6. */
@Composable
fun CardTestIntroScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.card_test_intro_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = onContinueClick) {
            Text(text = stringResource(id = R.string.card_test_intro_continue))
        }
    }
}
