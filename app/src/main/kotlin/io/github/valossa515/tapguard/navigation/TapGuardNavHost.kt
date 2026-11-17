package io.github.valossa515.tapguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.valossa515.tapguard.ui.alerthistory.AlertHistoryScreen
import io.github.valossa515.tapguard.ui.appdetail.AppDetailScreen
import io.github.valossa515.tapguard.ui.cardtestintro.CardTestIntroScreen
import io.github.valossa515.tapguard.ui.cardtestreading.CardTestReadingScreen
import io.github.valossa515.tapguard.ui.emergency.EmergencyScreen
import io.github.valossa515.tapguard.ui.home.HomeScreen
import io.github.valossa515.tapguard.ui.onboarding.OnboardingScreen
import io.github.valossa515.tapguard.ui.scanresult.ScanResultScreen
import io.github.valossa515.tapguard.ui.settings.SettingsScreen

/** Pacote provisório aberto na T4 a partir da T3 e da T9, até existir varredura (o app isca). */
internal const val PROVISIONAL_PACKAGE_NAME = "io.github.valossa515.tapguard.decoy"

/**
 * Grafo de navegação provisório (especificação, seção 10). Começa sempre na T1, sem persistência.
 * É o único lugar que conhece o NavController: as telas recebem só lambdas.
 */
@Composable
fun TapGuardNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = OnboardingRoute,
        modifier = modifier,
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onContinueClick = {
                    // A T1 sai da pilha: voltar na T2 não reabre o onboarding.
                    navController.navigate(HomeRoute) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onScanNowClick = { navController.navigate(ScanResultRoute) },
                onTestCardClick = { navController.navigate(CardTestIntroRoute) },
                onEmergencyClick = { navController.navigate(EmergencyRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ScanResultRoute> {
            ScanResultScreen(
                onAppClick = { navController.navigate(AppDetailRoute(PROVISIONAL_PACKAGE_NAME)) },
            )
        }
        composable<AppDetailRoute> { entry ->
            AppDetailScreen(packageName = entry.toRoute<AppDetailRoute>().packageName)
        }
        composable<CardTestIntroRoute> {
            CardTestIntroScreen(
                onContinueClick = { navController.navigate(CardTestReadingRoute) },
            )
        }
        composable<CardTestReadingRoute> {
            CardTestReadingScreen(
                // Volta para a T2 que já está na pilha, sem empilhar outra.
                onDoneClick = { navController.popBackStack<HomeRoute>(inclusive = false) },
            )
        }
        composable<EmergencyRoute> {
            EmergencyScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onAlertHistoryClick = { navController.navigate(AlertHistoryRoute) },
            )
        }
        composable<AlertHistoryRoute> {
            AlertHistoryScreen(
                onAlertClick = { navController.navigate(AppDetailRoute(PROVISIONAL_PACKAGE_NAME)) },
            )
        }
    }
}
