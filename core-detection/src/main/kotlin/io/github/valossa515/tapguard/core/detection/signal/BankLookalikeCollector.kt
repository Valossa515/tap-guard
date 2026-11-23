package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import java.util.Collections

/**
 * Coletor do sinal S9 (nome parecido com o de um banco conhecido, mas pacote diferente do oficial).
 * Devolve [Signal.BANK_LOOKALIKE] quando o [InstalledApp.label] casa com o nome de alguma entrada de
 * [knownBankApps] e o [InstalledApp.packageName] não é o pacote de nenhuma delas. Fora isso, devolve
 * um conjunto vazio. Nunca devolve outro sinal.
 *
 * - **O que conta:** nome igual ou quase igual, pela comparação de nomes do S9 (esqueleto, variantes
 *   e distância dentro do limite; ADR-020).
 * - **Pacote da lista não gera S9**, de qualquer entrada e não só da que casou: um app oficial cujo
 *   nome lembra outro app do mesmo banco ("Itaú Empresas" e "Itaú") fica sem S9. A comparação do
 *   pacote é exata, com maiúsculas e minúsculas.
 * - O conjunto recebido é copiado: mudá-lo depois não altera o coletor. Um conjunto vazio nunca gera
 *   S9. A lista padrão é [KnownBanks.apps]; quem usa o SDK amplia ou reduz com `+` ou `-`.
 *
 * Comportamentos aceitos (ADR-020): nome curto igual de outro desenvolvedor ("Inter", "NEXT") e o
 * corte no separador ("Next - Launcher") geram S9; palavras fora da lista fechada ("Atualização
 * Nubank") escapam.
 *
 * Limitação do pacote emprestado (ADR-017 e ADR-020): num aparelho sem o app do banco, um app com o
 * pacote oficial e o nome do banco não recebe S9. Instalado à mão, ele recebe S1. A defesa pelo
 * certificado de assinatura está na issue #65.
 *
 * Não chama o `PackageManager`: rótulo e pacote já vêm do `AppInventory`.
 */
class BankLookalikeCollector(
    knownBankApps: Set<KnownBankApp>,
) : SignalCollector {
    private val packageNames: Set<String> =
        Collections.unmodifiableSet(knownBankApps.mapTo(LinkedHashSet()) { it.packageName })
    private val matcher = BankNameMatcher(knownBankApps.map { it.displayName })

    override suspend fun collect(app: InstalledApp): Set<Signal> =
        when {
            app.packageName in packageNames -> emptySet()
            matcher.matches(app.label) -> DETECTED
            else -> emptySet()
        }

    private companion object {
        val DETECTED: Set<Signal> = setOf(Signal.BANK_LOOKALIKE)
    }
}
