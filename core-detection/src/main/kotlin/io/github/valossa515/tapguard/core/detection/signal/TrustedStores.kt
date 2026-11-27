package io.github.valossa515.tapguard.core.detection.signal

import java.util.Collections

/**
 * Lojas confiáveis para o sinal S1: um app instalado por um destes pacotes não recebe S1.
 *
 * Ficam fora (ADR-017):
 * - os instaladores de pacote do sistema (`com.google.android.packageinstaller`,
 *   `com.android.packageinstaller` e os equivalentes de fabricante) e `com.android.shell`, que
 *   aparecem quando um APK é instalado à mão ou por adb;
 * - F-Droid, porque o cliente aceita repositórios de terceiros;
 * - Aurora Store, cliente não oficial da Play que aparece como instaladora de tudo o que baixa;
 * - apps de clonagem (Smart Switch, Mi Mover, Clone Phone), que copiam também um APK de golpe.
 *   [KnownCloneApps] permite uma exceção separada no S1 somente para pacotes de banco conhecidos.
 *
 * [packageNames] não pode ser alterado, nem por cast para [MutableSet]. Quem usa o SDK monta o
 * próprio conjunto a partir dele e o passa ao [InstallSourceCollector]: por exemplo,
 * `TrustedStores.packageNames + "com.banco.loja"` para ampliar ou
 * `TrustedStores.packageNames - "com.amazon.venezia"` para reduzir.
 */
object TrustedStores {
    val packageNames: Set<String> =
        Collections.unmodifiableSet(
            linkedSetOf(
                // Google Play. Fontes: verificação de desenvolvedores do Google (2026); especificação, seção 5.
                "com.android.vending",
                // Galaxy Store (Samsung). Fontes: verificação do Google; seção 5; Talsec.
                "com.sec.android.app.samsungapps",
                // Xiaomi GetApps. Fontes: verificação do Google; seção 5; Talsec.
                "com.xiaomi.mipicks",
                // Xiaomi GetApps, variante (Xiaomi Market). Fontes: Talsec; APKMirror (GetApps 4.88).
                "com.xiaomi.market",
                // OPPO App Market (OPPO, realme, OnePlus). Fontes: verificação do Google; Talsec.
                "com.heytap.market",
                // OPPO App Market, variante. Fonte: Talsec.
                "com.oppo.market",
                // vivo V-Appstore. Fontes: verificação do Google; Talsec.
                "com.vivo.appstore",
                // HONOR App Market. Fontes: verificação do Google; APKMirror.
                "com.hihonor.appmarket",
                // Palm Store (Tecno, Infinix, itel). Fontes: verificação do Google; Talsec.
                "com.transsnet.store",
                // Huawei AppGallery. Fontes: seção 5; Talsec.
                "com.huawei.appmarket",
                // Amazon Appstore (fechada fora dos aparelhos Fire em 20/08/2025). Fontes: seção 5; Talsec.
                "com.amazon.venezia",
            ),
        )
}
