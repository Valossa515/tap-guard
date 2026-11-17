package io.github.valossa515.tapguard.navigation

import kotlinx.serialization.Serializable

/** T1 Onboarding. */
@Serializable
data object OnboardingRoute

/** T2 Início. */
@Serializable
data object HomeRoute

/** T3 Resultado. */
@Serializable
data object ScanResultRoute

/** T4 Detalhe do app. */
@Serializable
data class AppDetailRoute(
    val packageName: String,
)

/** T5 Teste do cartão: explicação. */
@Serializable
data object CardTestIntroRoute

/** T6 Teste do cartão: leitura e resultado. */
@Serializable
data object CardTestReadingRoute

/** T7 Emergência. */
@Serializable
data object EmergencyRoute

/** T8 Configurações. */
@Serializable
data object SettingsRoute

/** T9 Histórico de alertas. */
@Serializable
data object AlertHistoryRoute
