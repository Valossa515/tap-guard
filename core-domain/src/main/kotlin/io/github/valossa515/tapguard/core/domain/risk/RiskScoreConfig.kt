package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.signal.Signal

/**
 * Único arquivo de configuração do motor de score (seção 5 da especificação): o peso de cada sinal,
 * o teto da soma, os tetos das regras de ajuste 3 e 4 e o início de cada faixa.
 *
 * Os pesos são um ponto de partida para calibrar com testes. Mudar um valor aqui exige atualizar, na
 * mesma spec, os cinco exemplos da seção 5 e a tabela de testes do [WeightedRiskScorer]
 * (`backlog_rules.md`). As regras em si (quais sinais contam como NFC, as exceções da regra 3) ficam
 * no [WeightedRiskScorer].
 */
internal object RiskScoreConfig {
    /** S1: instalado fora de uma loja confiável. */
    const val UNTRUSTED_INSTALL_SOURCE_WEIGHT = 30

    /** S2: declara a permissão de NFC. */
    const val NFC_PERMISSION_WEIGHT = 15

    /** S3: declara serviço de emulação de cartão (HCE). */
    const val CARD_EMULATION_SERVICE_WEIGHT = 20

    /** S4: tem serviço de acessibilidade ativo. */
    const val ACCESSIBILITY_SERVICE_ENABLED_WEIGHT = 25

    /** S5: pede permissão de sobreposição de tela. */
    const val OVERLAY_PERMISSION_WEIGHT = 10

    /** S6: instalado ou atualizado nos últimos 7 dias. */
    const val RECENTLY_INSTALLED_OR_UPDATED_WEIGHT = 10

    /** S7: é o app de pagamento padrão e não está na lista de carteiras conhecidas. */
    const val UNKNOWN_DEFAULT_PAYMENT_APP_WEIGHT = 30

    /** S8: pede leitura ou recebimento de SMS. */
    const val SMS_PERMISSION_WEIGHT = 10

    /** S9: nome parecido com o de um banco conhecido, mas pacote diferente do oficial. */
    const val BANK_LOOKALIKE_WEIGHT = 20

    /** A soma dos pesos é limitada a este valor antes das regras de ajuste. */
    const val MAX_SCORE = 100

    /** Regra 3: teto do app instalado por loja confiável sem S7 nem S9. */
    const val TRUSTED_STORE_CAP = 29

    /** Regra 4: teto do app sem S1 junto com um sinal de NFC (S2, S3 ou S7). */
    const val NO_NFC_SIGNAL_CAP = 59

    /** Início da faixa amarela (30 a 59). Abaixo dele, verde (0 a 29). */
    const val YELLOW_FROM = 30

    /** Início da faixa vermelha (60 em diante). */
    const val RED_FROM = 60

    fun weightOf(signal: Signal): Int =
        when (signal) {
            Signal.UNTRUSTED_INSTALL_SOURCE -> UNTRUSTED_INSTALL_SOURCE_WEIGHT
            Signal.NFC_PERMISSION -> NFC_PERMISSION_WEIGHT
            Signal.CARD_EMULATION_SERVICE -> CARD_EMULATION_SERVICE_WEIGHT
            Signal.ACCESSIBILITY_SERVICE_ENABLED -> ACCESSIBILITY_SERVICE_ENABLED_WEIGHT
            Signal.OVERLAY_PERMISSION -> OVERLAY_PERMISSION_WEIGHT
            Signal.RECENTLY_INSTALLED_OR_UPDATED -> RECENTLY_INSTALLED_OR_UPDATED_WEIGHT
            Signal.UNKNOWN_DEFAULT_PAYMENT_APP -> UNKNOWN_DEFAULT_PAYMENT_APP_WEIGHT
            Signal.SMS_PERMISSION -> SMS_PERMISSION_WEIGHT
            Signal.BANK_LOOKALIKE -> BANK_LOOKALIKE_WEIGHT
        }

    fun levelFor(score: Int): RiskLevel =
        when {
            score >= RED_FROM -> RiskLevel.RED
            score >= YELLOW_FROM -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }
}
