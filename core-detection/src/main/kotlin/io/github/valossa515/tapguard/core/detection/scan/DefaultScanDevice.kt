package io.github.valossa515.tapguard.core.detection.scan

import io.github.valossa515.tapguard.core.domain.inventory.AppInventory
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskScorer
import io.github.valossa515.tapguard.core.domain.risk.ScoringContext
import io.github.valossa515.tapguard.core.domain.risk.TrustedAppRepository
import io.github.valossa515.tapguard.core.domain.risk.WeightedRiskScorer
import io.github.valossa515.tapguard.core.domain.scan.ScanDevice
import io.github.valossa515.tapguard.core.domain.scan.ScanReport
import io.github.valossa515.tapguard.core.domain.scan.ScanRepository
import io.github.valossa515.tapguard.core.domain.scan.ScanTrigger
import io.github.valossa515.tapguard.core.domain.scan.ScannedApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Clock
import java.util.Collections

/**
 * [ScanDevice] da seção 7 (ADR-023). Em cada varredura, nesta ordem:
 * 1. o início vem do [clock];
 * 2. o [inventory] devolve os apps de usuário (ele troca sozinho para o dispatcher de I/O);
 * 3. os apps confiáveis são lidos uma vez de [trustedApps] e viram o [ScoringContext] (regra 2);
 * 4. no [dispatcher], para cada app na ordem do inventário, os [collectors] rodam em sequência, a
 *    união dos sinais vai para o [WeightedRiskScorer] e o app vira um [ScannedApp];
 * 5. o fim é o maior entre o início e o agora do [clock], porque o relógio do aparelho pode voltar
 *    durante a varredura;
 * 6. o [ScanReport] é gravado em [scans] e devolvido.
 *
 * O cancelamento é conferido antes de cada app: cancelada antes da gravação, a varredura não grava
 * nada. Não há `try`: um erro do inventário, de um coletor, da leitura dos confiáveis ou da gravação
 * chega a quem chama, e nada fica gravado pela metade (a gravação é atômica, [ScanRepository]).
 *
 * A lista de [collectors] é copiada. O motor é criado aqui (ADR-022). Quem usa o SDK fornece as
 * duas portas, [TrustedAppRepository] e [ScanRepository]; uma implementação em memória basta. Não
 * usa API do Android.
 */
class DefaultScanDevice(
    private val inventory: AppInventory,
    collectors: List<SignalCollector>,
    private val trustedApps: TrustedAppRepository,
    private val scans: ScanRepository,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) : ScanDevice {
    private val collectors: List<SignalCollector> = Collections.unmodifiableList(ArrayList(collectors))
    private val scorer: RiskScorer = WeightedRiskScorer()

    override suspend fun invoke(trigger: ScanTrigger): ScanReport {
        val startedAt = clock.instant()
        val apps = inventory.installedApps()
        val context = ScoringContext(trustedApps.trustedPackages())
        val scanned =
            withContext(dispatcher) {
                apps.map { app ->
                    ensureActive()
                    ScannedApp(app = app, risk = scorer.score(app, signalsOf(app), context))
                }
            }
        val report =
            ScanReport(
                trigger = trigger,
                startedAt = startedAt,
                finishedAt = maxOf(startedAt, clock.instant()),
                apps = scanned,
            )
        scans.save(report)
        return report
    }

    private suspend fun signalsOf(app: InstalledApp): Set<Signal> {
        val signals = mutableSetOf<Signal>()
        collectors.forEach { signals += it.collect(app) }
        return signals
    }
}
