package io.github.valossa515.tapguard.ui.home

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

/** T2 Início (vazia). Sai para a T3, a T5, a T7 e a T8. */
@Composable
fun HomeScreen(
    onScanNowClick: () -> Unit,
    onTestCardClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = onScanNowClick) {
            Text(text = stringResource(id = R.string.home_scan_now))
        }
        Button(onClick = onTestCardClick) {
            Text(text = stringResource(id = R.string.home_test_card))
        }
        Button(onClick = onEmergencyClick) {
            Text(text = stringResource(id = R.string.home_emergency))
        }
        Button(onClick = onSettingsClick) {
            Text(text = stringResource(id = R.string.home_settings))
        }
    }
}
