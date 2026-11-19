package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import java.util.Collections

/**
 * Coletor do sinal S1 (instalado fora de uma loja confiável). Devolve
 * [Signal.UNTRUSTED_INSTALL_SOURCE] quando [InstalledApp.installerPackageName] é nulo (por
 * exemplo, `adb install` ou instalador já desinstalado) ou não está em [trustedInstallers]. Quando
 * está, devolve um conjunto vazio. Nunca devolve outro sinal.
 *
 * A comparação é exata, com maiúsculas e minúsculas. O conjunto recebido é copiado: mudá-lo depois
 * não altera o coletor. Um conjunto vazio faz todo app receber S1. A lista padrão é
 * [TrustedStores.packageNames].
 *
 * Não chama o `PackageManager`: o instalador já vem do `AppInventory`.
 *
 * Limitações (ADR-017): com shell ou root (adb, Shizuku), um app pode ser registrado como instalado
 * por uma loja da lista; e, num aparelho sem uma das lojas, outro app pode usar o nome de pacote
 * dela. Nesse caso o app que instalou recebe S1 e, se ele se desinstalar, o instalador vira nulo.
 */
class InstallSourceCollector(
    trustedInstallers: Set<String>,
) : SignalCollector {
    private val trustedInstallers: Set<String> = Collections.unmodifiableSet(LinkedHashSet(trustedInstallers))

    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (app.installerPackageName in trustedInstallers) emptySet() else UNTRUSTED

    private companion object {
        val UNTRUSTED: Set<Signal> = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)
    }
}
