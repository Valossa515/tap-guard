package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import java.time.Clock
import java.time.Duration

/**
 * Coletor do sinal S6 (instalado ou atualizado nos últimos 7 dias). Devolve
 * [Signal.RECENTLY_INSTALLED_OR_UPDATED] quando o mais recente entre
 * [InstalledApp.firstInstallTime] e [InstalledApp.lastUpdateTime] não é anterior ao agora do
 * [clock] menos 7 dias (168 horas corridas, sem fuso). Quando é anterior, devolve um conjunto
 * vazio. Nunca devolve outro sinal.
 *
 * O limite é inclusivo: um app atualizado há exatamente 7 dias conta. Um horário no futuro também
 * conta. Com o relógio do aparelho atrasado, todos os apps ganham S6: sozinho ele vale 10 e o app
 * fica verde, mas somado a outros sinais pode mudar o nível (ADR-019).
 *
 * O agora vem só do [clock], recebido no construtor.
 */
class RecentInstallOrUpdateCollector(
    private val clock: Clock,
) : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> {
        val latest = maxOf(app.firstInstallTime, app.lastUpdateTime)
        return if (!latest.isBefore(clock.instant().minus(WINDOW))) {
            DETECTED
        } else {
            emptySet()
        }
    }

    private companion object {
        val WINDOW: Duration = Duration.ofDays(7)
        val DETECTED: Set<Signal> = setOf(Signal.RECENTLY_INSTALLED_OR_UPDATED)
    }
}
