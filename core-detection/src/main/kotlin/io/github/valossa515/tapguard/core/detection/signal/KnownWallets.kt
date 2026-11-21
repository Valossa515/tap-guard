package io.github.valossa515.tapguard.core.detection.signal

import java.util.Collections

/**
 * Carteiras conhecidas para o sinal S7: um destes pacotes pode ser o app de pagamento por
 * aproximação padrão sem gerar S7. Quem compara é o coletor de S7 (SPEC-016).
 *
 * Critério (ADR-020): só entram apps com fonte de que fazem o pagamento por aproximação. Além da
 * Carteira do Google e da Samsung Wallet, entram os apps de banco que fazem Pix por aproximação no
 * próprio app. Ficam fora Mercado Pago, PicPay, Bradesco, Caixa, Santander e C6, que fazem o Pix por
 * aproximação pela Carteira do Google. Um banco que lançar a função no próprio app recebe S7 até
 * entrar na lista.
 *
 * [packageNames] não pode ser alterado, nem por cast para [MutableSet]. Quem usa o SDK monta o
 * próprio conjunto a partir dele: por exemplo, `KnownWallets.packageNames + "com.banco.app"`.
 *
 * Limitação (ADR-017): o nome de pacote é único no aparelho, não no mundo. Num aparelho sem um
 * destes apps, outro app pode usar o mesmo pacote (defesa planejada na issue #65).
 */
object KnownWallets {
    val packageNames: Set<String> =
        Collections.unmodifiableSet(
            linkedSetOf(
                // Carteira do Google. Fontes: Google Play ("Carteira do Google"); especificação, seção 5.
                "com.google.android.apps.walletnfcrel",
                // Samsung Wallet. Fontes: Google Play ("Samsung Wallet (Samsung Pay)"); seção 5; TechTudo.
                "com.samsung.android.spay",
                // Nubank, carteira padrão com Pix por aproximação. Fontes: Google Play; TechTudo (07/04/2026).
                "com.nu.production",
                // Itaú, Pix por aproximação no app. Fontes: Google Play; Mobile Time (27/01/2026).
                "com.itau",
                // Banco do Brasil, serviço Pix por Aproximação escolhido nas configurações de NFC.
                // Fontes: Google Play; TechTudo (03/08/2026).
                "br.com.bb.android",
                // Inter, Pix por aproximação no Super App 26.1.1 ou mais recente.
                // Fontes: Google Play; TechTudo (03/08/2026).
                "br.com.intermedium",
            ),
        )
}
