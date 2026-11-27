package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector
import java.util.Collections

/**
 * Coletor do S1: devolve vazio para instalador em [trustedInstallers] ou para pacote em
 * [knownBankPackages] E instalador em [knownCloneApps]. Nos demais casos, inclusive instalador
 * nulo, devolve somente [Signal.UNTRUSTED_INSTALL_SOURCE] (ADR-017, SPEC-019).
 *
 * Comparações exatas, com maiúsculas e minúsculas. Os três conjuntos são obrigatórios e copiados
 * separadamente: alterá-los depois não muda o coletor. Bancos ou clonadores vazios desligam somente
 * a exceção; lojas vazias ainda permitem banco com clonador. Todos vazios dão S1 a qualquer app.
 * A SPEC-018 monta no Hilt [TrustedStores.packageNames], os pacotes de [KnownBanks.apps]
 * deduplicados e [KnownCloneApps.packageNames]. O SDK pode passar conjuntos próprios.
 *
 * Não chama o `PackageManager`: o instalador já vem do `AppInventory`.
 *
 * Limitações (ADR-017): com shell ou root (adb, Shizuku), um app pode ser registrado como instalado
 * por uma loja da lista; e, num aparelho sem uma das lojas, outro app pode usar o nome de pacote
 * dela. A exceção mantém o risco de pacote de banco ou clonador emprestado (issues #57 e #65).
 */
class InstallSourceCollector(
    trustedInstallers: Set<String>,
    knownBankPackages: Set<String>,
    knownCloneApps: Set<String>,
) : SignalCollector {
    private val trustedInstallers: Set<String> = Collections.unmodifiableSet(LinkedHashSet(trustedInstallers))
    private val knownBankPackages: Set<String> = Collections.unmodifiableSet(LinkedHashSet(knownBankPackages))
    private val knownCloneApps: Set<String> = Collections.unmodifiableSet(LinkedHashSet(knownCloneApps))

    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (
            app.installerPackageName in trustedInstallers ||
            (app.packageName in knownBankPackages && app.installerPackageName in knownCloneApps)
        ) {
            emptySet()
        } else {
            UNTRUSTED
        }

    private companion object {
        val UNTRUSTED: Set<Signal> = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)
    }
}
