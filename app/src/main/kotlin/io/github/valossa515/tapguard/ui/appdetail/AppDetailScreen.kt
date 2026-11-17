package io.github.valossa515.tapguard.ui.appdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.valossa515.tapguard.R

/** T4 Detalhe do app (vazia). Mostra o pacote recebido pela rota. */
@Composable
fun AppDetailScreen(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.app_detail_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = stringResource(id = R.string.app_detail_package_name, packageName))
    }
}
