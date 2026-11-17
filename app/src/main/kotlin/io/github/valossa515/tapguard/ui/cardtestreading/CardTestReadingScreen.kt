package io.github.valossa515.tapguard.ui.cardtestreading

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

/** T6 Teste do cartão: leitura e resultado (vazia). Volta para a T2. */
@Composable
fun CardTestReadingScreen(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.card_test_reading_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = onDoneClick) {
            Text(text = stringResource(id = R.string.card_test_reading_done))
        }
    }
}
