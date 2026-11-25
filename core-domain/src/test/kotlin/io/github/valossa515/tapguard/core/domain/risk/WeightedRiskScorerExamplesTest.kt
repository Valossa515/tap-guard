package io.github.valossa515.tapguard.core.domain.risk

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Os cinco exemplos da seção 5 da especificação, com o resultado esperado escrito à mão. */
class WeightedRiskScorerExamplesTest {
    private val scorer = WeightedRiskScorer()
    private val app = anInstalledApp(packageName = "com.example.app")
    private val noTrustedPackages = ScoringContext(trustedPackages = emptySet())

    @Test
    fun `example 1 APK outside store with NFC and HCE is 65 red`() {
        val signals = setOf(Signal.UNTRUSTED_INSTALL_SOURCE, Signal.NFC_PERMISSION, Signal.CARD_EMULATION_SERVICE)

        val result = scorer.score(app, signals, noTrustedPackages)

        assertEquals(RiskResult(score = 65, level = RiskLevel.RED, signals = signals), result)
    }

    @Test
    fun `example 2 same app installed yesterday with accessibility is 100 red`() {
        val signals =
            setOf(
                Signal.UNTRUSTED_INSTALL_SOURCE,
                Signal.NFC_PERMISSION,
                Signal.CARD_EMULATION_SERVICE,
                Signal.ACCESSIBILITY_SERVICE_ENABLED,
                Signal.RECENTLY_INSTALLED_OR_UPDATED,
            )

        val result = scorer.score(app, signals, noTrustedPackages)

        assertEquals(RiskResult(score = 100, level = RiskLevel.RED, signals = signals), result)
    }

    @Test
    fun `example 3 bank app from store with NFC and HCE is capped at 29 green`() {
        val signals = setOf(Signal.NFC_PERMISSION, Signal.CARD_EMULATION_SERVICE)

        val result = scorer.score(app, signals, noTrustedPackages)

        assertEquals(RiskResult(score = 29, level = RiskLevel.GREEN, signals = signals), result)
    }

    @Test
    fun `example 4 game installed by APK without NFC is 30 yellow`() {
        val signals = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)

        val result = scorer.score(app, signals, noTrustedPackages)

        assertEquals(RiskResult(score = 30, level = RiskLevel.YELLOW, signals = signals), result)
    }

    @Test
    fun `example 5 app outside store with accessibility and SMS is capped at 59 yellow`() {
        val signals =
            setOf(Signal.UNTRUSTED_INSTALL_SOURCE, Signal.ACCESSIBILITY_SERVICE_ENABLED, Signal.SMS_PERMISSION)

        val result = scorer.score(app, signals, noTrustedPackages)

        assertEquals(RiskResult(score = 59, level = RiskLevel.YELLOW, signals = signals), result)
    }
}
