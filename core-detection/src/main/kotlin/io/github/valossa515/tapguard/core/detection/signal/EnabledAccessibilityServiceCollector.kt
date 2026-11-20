package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector

/**
 * Coletor do sinal S4 (tem serviço de acessibilidade ativo). Devolve
 * [Signal.ACCESSIBILITY_SERVICE_ENABLED] quando [InstalledApp.enabledAccessibilityServices] não está
 * vazio. Quando está, devolve um conjunto vazio. Nunca devolve outro sinal.
 *
 * O critério vem do `AppInventory`: os serviços que o usuário ligou, lidos de
 * `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, inclusive um que caiu e ainda vai reconectar.
 * Serviço só declarado, que o usuário não ligou, não conta. O inventário não cruza com
 * [InstalledApp.services]: um serviço ligado conta mesmo que a classe não esteja entre os
 * declarados (ADR-019).
 *
 * Não chama o Android: os serviços ligados já vêm do `AppInventory`.
 */
class EnabledAccessibilityServiceCollector : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (app.enabledAccessibilityServices.isNotEmpty()) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        val DETECTED: Set<Signal> = setOf(Signal.ACCESSIBILITY_SERVICE_ENABLED)
    }
}
