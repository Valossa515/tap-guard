package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import java.util.Collections

/**
 * Coletor do sinal S7 (é o app de pagamento padrão e não está na lista de carteiras conhecidas).
 * Devolve [Signal.UNKNOWN_DEFAULT_PAYMENT_APP] quando [InstalledApp.defaultPaymentServices] não está
 * vazio e o [InstalledApp.packageName] não está em [knownWallets]. Fora isso, devolve um conjunto
 * vazio. Nunca devolve outro sinal.
 *
 * - **O que conta:** o sistema diz que um serviço de HCE do app é o padrão de pagamento (até a API 34,
 *   o componente escolhido nas configurações de NFC; da API 35 em diante, com o papel de carteira, o
 *   app dono do papel). O critério vem do `AppInventory` (ADR-021).
 * - A comparação do pacote é exata, com maiúsculas e minúsculas. O conjunto recebido é copiado: mudá-lo
 *   depois não altera o coletor. Um conjunto vazio faz todo app padrão receber S7. A lista padrão é
 *   [KnownWallets.packageNames]; quem usa o SDK amplia ou reduz com `+` ou `-`.
 * - Serviço padrão de um app do sistema ou do próprio app não gera S7 em nenhum app: o inventário não
 *   lista esses pacotes (regra 1 da seção 5).
 *
 * Limitação do pacote emprestado (ADR-017 e ADR-020): num aparelho sem a carteira, um app com o pacote
 * dela não recebe S7. Instalado à mão, ele recebe S1. A defesa pelo certificado de assinatura está na
 * issue #65.
 *
 * Não chama o Android: o serviço padrão já vem do `AppInventory`.
 */
class DefaultPaymentAppCollector(
    knownWallets: Set<String>,
) : SignalCollector {
    private val knownWallets: Set<String> = Collections.unmodifiableSet(LinkedHashSet(knownWallets))

    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (app.defaultPaymentServices.isNotEmpty() && app.packageName !in knownWallets) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        val DETECTED: Set<Signal> = setOf(Signal.UNKNOWN_DEFAULT_PAYMENT_APP)
    }
}
