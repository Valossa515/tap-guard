package io.github.valossa515.tapguard.core.domain.scan

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskResult

/** Um app avaliado numa varredura: o app e o seu risco. */
data class ScannedApp(
    val app: InstalledApp,
    val risk: RiskResult,
)
