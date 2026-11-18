package io.github.valossa515.tapguard.core.domain.risk

/**
 * Nível de risco de um app, em ordem crescente de gravidade. Os nomes são os valores gravados em
 * `app_snapshot.level` e `alert_event.level`: renomear muda o banco. A faixa de score de cada
 * nível fica no motor de score (SPEC-017).
 */
enum class RiskLevel {
    GREEN,
    YELLOW,
    RED,
}
