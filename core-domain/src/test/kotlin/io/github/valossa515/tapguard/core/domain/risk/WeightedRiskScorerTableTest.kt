package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tabela de combinações do plano da SPEC-017 (`.agent/plans/spec-017-motor-score.md`): cada peso
 * sozinho (P), as regras 3 e 4 com os limites 29/30 e 59/60 (C) e a regra 2 (T). Os sinais são
 * escritos como na seção 5 e o resultado esperado é escrito à mão: o teste não lê o
 * [RiskScoreConfig], então mudar um peso derruba casos.
 */
class WeightedRiskScorerTableTest {
    private val scorer = WeightedRiskScorer()

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleSignalWeights")
    fun `single signal weights`(case: Case) = assertCase(case)

    @ParameterizedTest(name = "{0}")
    @MethodSource("adjustmentRules3And4")
    fun `adjustment rules 3 and 4`(case: Case) = assertCase(case)

    @ParameterizedTest(name = "{0}")
    @MethodSource("trustedAppRule2")
    fun `trusted app rule 2`(case: Case) = assertCase(case)

    private fun assertCase(case: Case) {
        val app = anInstalledApp(packageName = APP_PACKAGE)
        val signals = signalsOf(case.signals)

        val result = scorer.score(app, signals, ScoringContext(trustedPackages = case.trustedPackages))

        assertEquals(case.score, result.score, "score")
        assertEquals(case.level, result.level, "level")
        assertEquals(signals, result.signals, "signals")
    }

    data class Case(
        val id: String,
        val signals: String,
        val score: Int,
        val level: RiskLevel,
        val trustedPackages: Set<String> = emptySet(),
    ) {
        override fun toString(): String = "$id: [$signals] trusted=$trustedPackages -> $score $level"
    }

    companion object {
        private const val APP_PACKAGE = "com.example.app"

        /** S1 a S9 da seção 5, escritos à mão. */
        private val SIGNAL_CODES: Map<String, Signal> =
            mapOf(
                "S1" to Signal.UNTRUSTED_INSTALL_SOURCE,
                "S2" to Signal.NFC_PERMISSION,
                "S3" to Signal.CARD_EMULATION_SERVICE,
                "S4" to Signal.ACCESSIBILITY_SERVICE_ENABLED,
                "S5" to Signal.OVERLAY_PERMISSION,
                "S6" to Signal.RECENTLY_INSTALLED_OR_UPDATED,
                "S7" to Signal.UNKNOWN_DEFAULT_PAYMENT_APP,
                "S8" to Signal.SMS_PERMISSION,
                "S9" to Signal.BANK_LOOKALIKE,
            )

        private const val ALL_BUT_S1 = "S2 S3 S4 S5 S6 S7 S8 S9"
        private const val ALL_NINE = "S1 $ALL_BUT_S1"

        private fun signalsOf(codes: String): Set<Signal> =
            codes
                .split(" ")
                .filter { it.isNotEmpty() }
                .map { SIGNAL_CODES.getValue(it) }
                .toSet()

        private fun trusted(vararg packages: String): Set<String> = packages.toSet()

        @JvmStatic
        fun singleSignalWeights(): List<Case> =
            listOf(
                Case("P0", "", 0, RiskLevel.GREEN),
                Case("P1", "S1", 30, RiskLevel.YELLOW),
                Case("P2", "S2", 15, RiskLevel.GREEN),
                Case("P3", "S3", 20, RiskLevel.GREEN),
                Case("P4", "S4", 25, RiskLevel.GREEN),
                Case("P5", "S5", 10, RiskLevel.GREEN),
                Case("P6", "S6", 10, RiskLevel.GREEN),
                Case("P7", "S7", 30, RiskLevel.YELLOW),
                Case("P8", "S8", 10, RiskLevel.GREEN),
                Case("P9", "S9", 20, RiskLevel.GREEN),
            )

        @JvmStatic
        fun adjustmentRules3And4(): List<Case> =
            listOf(
                Case("C01 rule 3 caps 60 at 29", "S2 S3 S4", 29, RiskLevel.GREEN),
                Case("C02 largest sum without S1 S7 S9", "S2 S3 S4 S5 S6 S8", 29, RiskLevel.GREEN),
                Case("C03 rule 3 without NFC", "S4 S5", 29, RiskLevel.GREEN),
                Case("C04 S7 exception capped by rule 4", "S7 S2 S3", 59, RiskLevel.YELLOW),
                Case("C05 S7 exception below 59", "S7 S4", 55, RiskLevel.YELLOW),
                Case("C06 S9 exception reaches 30", "S9 S6", 30, RiskLevel.YELLOW),
                Case("C07 S9 exception capped by rule 4", "S9 S2 S3 S4", 59, RiskLevel.YELLOW),
                Case("C08 S7 and S9 together", "S7 S9", 50, RiskLevel.YELLOW),
                Case("C09 store app is never red", ALL_BUT_S1, 59, RiskLevel.YELLOW),
                Case("C10 S1 and S2 below 60", "S1 S2", 45, RiskLevel.YELLOW),
                Case("C11 S1 and S3 below 60", "S1 S3", 50, RiskLevel.YELLOW),
                Case("C12 exactly 60 with NFC", "S1 S3 S6", 60, RiskLevel.RED),
                Case("C13 S7 counts as NFC", "S1 S7", 60, RiskLevel.RED),
                Case("C14 S2 alone is enough", "S1 S2 S5 S8", 65, RiskLevel.RED),
                Case("C15 exactly 60 without NFC", "S1 S5 S6 S8", 59, RiskLevel.YELLOW),
                Case("C16 rule 4 far above cap", "S1 S4 S5 S6", 59, RiskLevel.YELLOW),
                Case("C17 cap 100 then 59", "S1 S4 S5 S6 S8 S9", 59, RiskLevel.YELLOW),
                Case("C18 S9 with S1 and NFC", "S1 S9 S2", 65, RiskLevel.RED),
                Case("C19 cap 100", ALL_NINE, 100, RiskLevel.RED),
            )

        @JvmStatic
        fun trustedAppRule2(): List<Case> =
            listOf(
                Case("T1 red becomes green", "S1 S2 S3", 65, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T2 rule 4 still caps", "S1 S4 S8", 59, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T3 rule 3 still caps", "S2 S3", 29, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T4 S7 exception and rule 4", "S7 S2 S3", 59, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T5 maximum score", ALL_NINE, 100, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T6 no signals", "", 0, RiskLevel.GREEN, trusted(APP_PACKAGE)),
                Case("T7 other package", "S1 S2 S3", 65, RiskLevel.RED, trusted("com.other.app")),
                Case("T8 exact match", "S1 S2 S3", 65, RiskLevel.RED, trusted("COM.EXAMPLE.APP")),
            )
    }
}
