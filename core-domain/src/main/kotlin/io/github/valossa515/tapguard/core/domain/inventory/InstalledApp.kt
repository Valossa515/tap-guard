package io.github.valossa515.tapguard.core.domain.inventory

import io.github.valossa515.tapguard.core.domain.unmodifiableCopy
import java.time.Instant

/**
 * App instalado que não é do sistema, como o [AppInventory] o devolve. Não há campo de app do
 * sistema: o inventário já não os devolve (regra 1 da seção 5).
 *
 * [installerPackageName] é nulo quando o sistema não informa o instalador. [services] guarda os
 * nomes de classe dos serviços declarados. [cardEmulationServices] guarda os nomes de classe dos
 * serviços de emulação de cartão (HCE) no próprio aparelho: os que respondem à ação
 * `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e exigem a permissão
 * `android.permission.BIND_NFC_SERVICE`, inclusive desligados. Off-host e NFC-F ficam fora
 * (ADR-018). Os dois conjuntos vêm de consultas diferentes ao sistema.
 *
 * [enabledAccessibilityServices] guarda os nomes de classe completos dos serviços de acessibilidade
 * do app que o usuário ligou (`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`), inclusive um que
 * caiu. Vem de outra leitura do sistema e não é validado como parte de [services] (ADR-019). As
 * coleções são cópias que não podem ser alteradas.
 */
@ConsistentCopyVisibility
data class InstalledApp private constructor(
    val packageName: String,
    val label: String,
    val versionCode: Long,
    val installerPackageName: String?,
    val firstInstallTime: Instant,
    val lastUpdateTime: Instant,
    val requestedPermissions: Set<String>,
    val services: Set<String>,
    val cardEmulationServices: Set<String>,
    val enabledAccessibilityServices: Set<String>,
) {
    companion object {
        /**
         * Única entrada pública; copia as coleções. Espelha o construtor da data class, que o
         * detekt já isenta da `LongParameterList` (`ignoreDataClasses`).
         */
        @Suppress("LongParameterList")
        operator fun invoke(
            packageName: String,
            label: String,
            versionCode: Long,
            installerPackageName: String?,
            firstInstallTime: Instant,
            lastUpdateTime: Instant,
            requestedPermissions: Set<String>,
            services: Set<String>,
            cardEmulationServices: Set<String>,
            enabledAccessibilityServices: Set<String>,
        ): InstalledApp =
            InstalledApp(
                packageName = packageName,
                label = label,
                versionCode = versionCode,
                installerPackageName = installerPackageName,
                firstInstallTime = firstInstallTime,
                lastUpdateTime = lastUpdateTime,
                requestedPermissions = requestedPermissions.unmodifiableCopy(),
                services = services.unmodifiableCopy(),
                cardEmulationServices = cardEmulationServices.unmodifiableCopy(),
                enabledAccessibilityServices = enabledAccessibilityServices.unmodifiableCopy(),
            )
    }
}
