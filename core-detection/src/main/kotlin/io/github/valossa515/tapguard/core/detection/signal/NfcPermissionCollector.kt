package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import io.github.valossa515.tapguard.core.domain.signal.SignalCollector

/**
 * Coletor do sinal S2 (declara a permissão de NFC). Devolve [Signal.NFC_PERMISSION] quando
 * [InstalledApp.requestedPermissions] contém `android.permission.NFC`, a permissão que o modo
 * leitor exige. Quando não contém, devolve um conjunto vazio. Nunca devolve outro sinal.
 *
 * A comparação é exata, com maiúsculas e minúsculas. Não contam (ADR-018):
 * - `android.permission.NFC_TRANSACTION_EVENT` e `android.permission.NFC_PREFERRED_PAYMENT_INFO`,
 *   que não leem nem emulam cartão sem a `NFC`;
 * - `android.permission.BIND_NFC_SERVICE`, de nível signature: o app a usa para proteger o próprio
 *   serviço de HCE, não para pedir.
 *
 * Não chama o `PackageManager`: as permissões já vêm do `AppInventory`.
 */
class NfcPermissionCollector : SignalCollector {
    override suspend fun collect(app: InstalledApp): Set<Signal> =
        if (NFC_PERMISSION in app.requestedPermissions) {
            DETECTED
        } else {
            emptySet()
        }

    private companion object {
        const val NFC_PERMISSION = "android.permission.NFC"
        val DETECTED: Set<Signal> = setOf(Signal.NFC_PERMISSION)
    }
}
