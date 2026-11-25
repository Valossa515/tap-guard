package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal

/**
 * Motor de score da seção 5: soma os pesos dos sinais ([RiskScoreConfig]), limita a soma a 100 e
 * aplica as regras de ajuste nesta ordem:
 *
 * 1. Apps do sistema são ignorados: cumprida pelo `AppInventory`, que não os devolve.
 * 2. App com o pacote em [ScoringContext.trustedPackages] (comparação exata) fica verde. A regra só
 *    decide o nível: o score continua passando pelas regras 3 e 4.
 * 3. Sem S1 (loja confiável), sem S7 e sem S9, o score é limitado a 29.
 * 4. Sem S1 junto com um sinal de NFC (S2, S3 ou S7), o score é limitado a 59.
 *
 * As regras 3 e 4 são tetos, então a ordem entre elas não muda o resultado. Sem S1 a regra 4 sempre
 * vale: um app sem S1 nunca fica vermelho, nem com S7 ou S9. O nível sai da faixa do score final,
 * salvo pela regra 2. [RiskResult.signals] são os sinais recebidos. Do [InstalledApp], só o pacote é
 * lido.
 */
class WeightedRiskScorer : RiskScorer {
    override fun score(
        app: InstalledApp,
        signals: Set<Signal>,
        ctx: ScoringContext,
    ): RiskResult {
        val sum = minOf(signals.sumOf(RiskScoreConfig::weightOf), RiskScoreConfig.MAX_SCORE)
        val afterTrustedStoreRule =
            if (isTrustedStoreCapped(signals)) minOf(sum, RiskScoreConfig.TRUSTED_STORE_CAP) else sum
        val score =
            if (canBeRed(signals)) {
                afterTrustedStoreRule
            } else {
                minOf(afterTrustedStoreRule, RiskScoreConfig.NO_NFC_SIGNAL_CAP)
            }
        val level = if (app.packageName in ctx.trustedPackages) RiskLevel.GREEN else RiskScoreConfig.levelFor(score)
        return RiskResult(score = score, level = level, signals = signals)
    }

    private fun isTrustedStoreCapped(signals: Set<Signal>): Boolean =
        Signal.UNTRUSTED_INSTALL_SOURCE !in signals && signals.none { it in TRUSTED_STORE_EXCEPTIONS }

    private fun canBeRed(signals: Set<Signal>): Boolean {
        val outsideTrustedStore = Signal.UNTRUSTED_INSTALL_SOURCE in signals
        return outsideTrustedStore && signals.any { it in NFC_SIGNALS }
    }

    private companion object {
        /** Regra 3: com S7 ou S9, o app de loja confiável não tem o teto de 29. */
        val TRUSTED_STORE_EXCEPTIONS: Set<Signal> =
            setOf(Signal.UNKNOWN_DEFAULT_PAYMENT_APP, Signal.BANK_LOOKALIKE)

        /** Regra 4: sinais de NFC que, junto com S1, permitem o vermelho. */
        val NFC_SIGNALS: Set<Signal> =
            setOf(Signal.NFC_PERMISSION, Signal.CARD_EMULATION_SERVICE, Signal.UNKNOWN_DEFAULT_PAYMENT_APP)
    }
}
