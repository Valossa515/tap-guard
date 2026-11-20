package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector

/**
 * Coletor do sinal S8 (pede leitura ou recebimento de SMS). Devolve [Signal.SMS_PERMISSION]
 * quando [InstalledApp.requestedPermissions] contém `android.permission.READ_SMS` ou
 * `android.permission.RECEIVE_SMS`; as duas juntas geram um só S8. Quando não contém nenhuma,
 * devolve um conjunto vazio. Nunca devolve outro sinal.
 *
 * A comparação é exata, com maiúsculas e minúsculas. Não contam (ADR-019):
 * `android.permission.SEND_SMS`, `android.permission.RECEIVE_MMS`,
 * `android.permission.RECEIVE_WAP_PUSH`, `android.permission.READ_CELL_BROADCASTS` e
 * `android.permission.BROADCAST_SMS`.
 *
 * Não chama o `PackageManager`: as permissões já vêm do `AppInventory`.
 */
class SmsPermissionCollector : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (READ_SMS in app.requestedPermissions || RECEIVE_SMS in app.requestedPermissions) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        const val READ_SMS = "android.permission.READ_SMS"
        const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
        val DETECTED: Set<Signal> = setOf(Signal.SMS_PERMISSION)
    }
}
