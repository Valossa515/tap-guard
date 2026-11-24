package io.github.valossa515.tapguard.core.detection.inventory

import android.content.ComponentName
import android.nfc.cardemulation.CardEmulation
import android.os.RemoteException
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/*
 * Shadows do Robolectric para os cenários do pagamento padrão que o ShadowCardEmulation não simula.
 * Cada um entra por @Config(shadows = [...]) no método de teste e não vaza para o teste seguinte
 * (Robolectric 4.18, APIs 26 a 36). O método do shadow usa os dois parâmetros: o detekt
 * (UnusedParameter) recusa parâmetro sem uso, e o Robolectric precisa da assinatura completa.
 */

/**
 * Serviço NFC morto: a partir da API 36, o `CardEmulation` do módulo NFC tenta de novo e chama
 * `rethrowAsRuntimeException` (`callServiceReturn`, android-16.0.0_r1), o que dá uma
 * `RuntimeException` com causa [RemoteException]. `throw RuntimeException(...)` seria recusado pelo
 * detekt (`TooGenericExceptionThrown`).
 */
@Implements(CardEmulation::class)
class DeadNfcServiceCardEmulationShadow {
    @Implementation
    fun isDefaultServiceForCategory(
        service: ComponentName,
        category: String,
    ): Boolean = throw RemoteException("NFC service died asking $service in $category").rethrowAsRuntimeException()
}

/**
 * App sem `android.permission.NFC`: o serviço NFC chama `NfcPermissions.enforceUserPermissions`, que
 * lança [SecurityException] (todas as tags lidas, de android-8.0.0_r1 a android-16.0.0_r1).
 */
@Implements(CardEmulation::class)
class MissingNfcPermissionCardEmulationShadow {
    @Implementation
    fun isDefaultServiceForCategory(
        service: ComponentName,
        category: String,
    ): Boolean = throw SecurityException("android.permission.NFC required to ask $service in $category")
}

/**
 * Papel de carteira (API 35+, flag `wallet_role_enabled`): o serviço NFC responde `true` para todo
 * serviço registrado do app dono do papel. Aqui o dono é [HOLDER_PACKAGE], e a resposta só vale na
 * categoria `payment`, a que o inventário pergunta.
 */
@Implements(CardEmulation::class)
class WalletRoleCardEmulationShadow {
    @Implementation
    fun isDefaultServiceForCategory(
        service: ComponentName,
        category: String,
    ): Boolean = category == "payment" && service.packageName == HOLDER_PACKAGE

    companion object {
        const val HOLDER_PACKAGE = "com.example.a.user"
    }
}
