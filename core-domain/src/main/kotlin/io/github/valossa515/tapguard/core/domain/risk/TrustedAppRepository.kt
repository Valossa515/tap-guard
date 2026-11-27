package io.github.valossa515.tapguard.core.domain.risk

/**
 * Apps que o usuário marcou como confiáveis (regra 2 da seção 5). Os pacotes viram o
 * [ScoringContext.trustedPackages] de cada varredura; a comparação com o pacote do app é exata.
 * Marcar e desfazer entram na SPEC-033.
 */
interface TrustedAppRepository {
    suspend fun trustedPackages(): Set<String>
}
