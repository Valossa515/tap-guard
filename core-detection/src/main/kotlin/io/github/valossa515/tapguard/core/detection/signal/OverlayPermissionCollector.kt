package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector

/**
 * Coletor do sinal S5 (pede permissão de sobreposição de tela). Devolve
 * [Signal.OVERLAY_PERMISSION] quando [InstalledApp.requestedPermissions] contém
 * `android.permission.SYSTEM_ALERT_WINDOW`. Quando não contém, devolve um conjunto vazio. Nunca
 * devolve outro sinal.
 *
 * Vale a permissão declarada, não a concedida: a seção 5 diz "pede". A concessão, que ficaria no
 * `AppOpsManager`, não é lida. A comparação é exata, com maiúsculas e minúsculas (ADR-019).
 *
 * Não chama o `PackageManager`: as permissões já vêm do `AppInventory`.
 */
class OverlayPermissionCollector : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (OVERLAY_PERMISSION in app.requestedPermissions) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        const val OVERLAY_PERMISSION = "android.permission.SYSTEM_ALERT_WINDOW"
        val DETECTED: Set<Signal> = setOf(Signal.OVERLAY_PERMISSION)
    }
}
