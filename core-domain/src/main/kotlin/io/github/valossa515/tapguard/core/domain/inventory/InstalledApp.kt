package io.github.valossa515.tapguard.core.domain.inventory

import io.github.valossa515.tapguard.core.domain.unmodifiableCopy
import java.time.Instant

/**
 * App instalado que não é do sistema, como o [AppInventory] o devolve. Não há campo de app do
 * sistema: o inventário já não os devolve (regra 1 da seção 5).
 *
 * [installerPackageName] é nulo quando o sistema não informa o instalador. [services] guarda os
 * nomes de classe dos serviços declarados. As coleções são cópias que não podem ser alteradas.
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
            )
    }
}
