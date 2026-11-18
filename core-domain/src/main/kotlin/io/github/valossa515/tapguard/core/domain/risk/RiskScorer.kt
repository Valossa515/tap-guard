package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal

/** Motor de score: calcula score e nível de um app a partir dos sinais (seção 5). */
interface RiskScorer {
    fun score(
        app: InstalledApp,
        signals: Set<Signal>,
        ctx: ScoringContext,
    ): RiskResult
}
