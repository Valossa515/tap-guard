package io.github.valossa515.tapguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.valossa515.tapguard.navigation.TapGuardNavHost
import io.github.valossa515.tapguard.ui.theme.TapGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema transparentes em todas as versões (o Android 15+ já força isso com targetSdk 36);
        // o conteúdo respeita as barras pelo safeDrawingPadding do NavHost.
        enableEdgeToEdge()
        setContent {
            TapGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TapGuardNavHost(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
