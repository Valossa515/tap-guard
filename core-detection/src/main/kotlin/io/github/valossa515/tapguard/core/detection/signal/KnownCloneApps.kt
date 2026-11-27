package io.github.valossa515.tapguard.core.detection.signal

import java.util.Collections

/**
 * Apps de clonagem conhecidos para a exceção estreita do S1 (ADR-017, SPEC-019).
 * Só dispensam S1 quando o pacote copiado também pertence ao conjunto de bancos do coletor.
 * Não são lojas confiáveis: podem copiar também APKs de golpe.
 *
 * [packageNames] não pode ser alterado, nem por cast para [MutableSet]. O consumidor do SDK
 * pode ampliar ou reduzir seu próprio conjunto com `+` ou `-`.
 *
 * Ficam fora ShareMe, SHAREit e SHAREit Lite (compartilhamento de arquivos) e MIUI Backup
 * (restauração de backup). Fontes levantadas na SPEC-017 em 2026-11-25; a atribuição do
 * instalador em aparelho será conferida na SPEC-022. Nomes de pacote não autenticam o app:
 * a limitação do pacote emprestado continua (issues #57 e #65).
 */
object KnownCloneApps {
    val packageNames: Set<String> =
        Collections.unmodifiableSet(
            linkedSetOf(
                // Samsung Smart Switch. Fontes: Talsec, Installation Sources Cookbook; Google Play:
                // https://play.google.com/store/apps/details?id=com.sec.android.easyMover
                "com.sec.android.easyMover",
                // Xiaomi Mi Mover. Fontes: Talsec; Google Play:
                // https://play.google.com/store/apps/details?id=com.miui.huanji
                "com.miui.huanji",
                // OPPO Clone Phone. Fontes: Talsec; Google Play:
                // https://play.google.com/store/apps/details?id=com.coloros.backuprestore
                "com.coloros.backuprestore",
                // OnePlus Clone Phone. Fontes: Talsec; Google Play:
                // https://play.google.com/store/apps/details?id=com.oneplus.backuprestore
                "com.oneplus.backuprestore",
                // Huawei Phone Clone. Fontes: Talsec; APKMirror (Huawei Internet Services):
                // https://apkmirror.com/apk/huawei-internet-services/phone-clone/
                "com.hicloud.android.clone",
                // HONOR Device Clone. Fontes: Talsec; Google Play:
                // https://play.google.com/store/apps/details?id=com.hihonor.android.clone
                "com.hihonor.android.clone",
                // vivo EasyShare. Fontes: Talsec; Google Play:
                // https://play.google.com/store/apps/details?id=com.vivo.easyshare
                "com.vivo.easyshare",
            ),
        )
}
