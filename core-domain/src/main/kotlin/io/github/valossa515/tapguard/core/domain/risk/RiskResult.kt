package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.unmodifiableCopy

/**
 * Score, nível e sinais de um app, como o [RiskScorer] os devolve. [score] vai de 0 a 100; fora
 * disso lança [IllegalArgumentException]. O nível não é conferido contra o score: pela regra 2 da
 * seção 5, um app confiável fica verde com qualquer score. [signals] é uma cópia que não pode ser
 * alterada.
 */
@ConsistentCopyVisibility
data class RiskResult private constructor(
    val score: Int,
    val level: RiskLevel,
    val signals: Set<Signal>,
) {
    init {
        require(score in MIN_SCORE..MAX_SCORE) { "Score fora de $MIN_SCORE a $MAX_SCORE: $score" }
    }

    companion object {
        private const val MIN_SCORE = 0
        private const val MAX_SCORE = 100

        operator fun invoke(
            score: Int,
            level: RiskLevel,
            signals: Set<Signal>,
        ): RiskResult = RiskResult(score = score, level = level, signals = signals.unmodifiableCopy())
    }
}
