package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector

/**
 * Coletor do sinal S3 (declara serviço de emulação de cartão, HCE). Devolve
 * [Signal.CARD_EMULATION_SERVICE] quando [InstalledApp.cardEmulationServices] não está vazio.
 * Quando está, devolve um conjunto vazio. Nunca devolve outro sinal e não depende do S2.
 *
 * O critério vem do `AppInventory`: serviços que respondem à ação
 * `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e exigem
 * `android.permission.BIND_NFC_SERVICE`, inclusive desligados. Off-host e NFC-F ficam fora: no
 * off-host os comandos vão para o chip seguro e não passam pelo app, e NFC-F (FeliCa) não é o
 * padrão dos cartões EMV no Brasil. O critério é mais largo que o do sistema NFC de propósito (não
 * exige serviço exportado, meta-data nem a permissão `NFC` no pacote): um falso positivo raro é
 * melhor que deixar de avisar (ADR-018).
 *
 * Não chama o `PackageManager`.
 */
class CardEmulationServiceCollector : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (app.cardEmulationServices.isNotEmpty()) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        val DETECTED: Set<Signal> = setOf(Signal.CARD_EMULATION_SERVICE)
    }
}
