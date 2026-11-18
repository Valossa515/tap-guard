package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.unmodifiableCopy

/**
 * O que muda a cada varredura e o [RiskScorer] não sabe sozinho: os pacotes que o usuário marcou
 * como confiáveis (regra 2 da seção 5). Os pesos são configuração da implementação do motor, e a
 * regra 3 (loja confiável) sai da ausência de S1. [trustedPackages] é uma cópia que não pode ser
 * alterada.
 */
@ConsistentCopyVisibility
data class ScoringContext private constructor(
    val trustedPackages: Set<String>,
) {
    companion object {
        operator fun invoke(trustedPackages: Set<String>): ScoringContext =
            ScoringContext(trustedPackages = trustedPackages.unmodifiableCopy())
    }
}
