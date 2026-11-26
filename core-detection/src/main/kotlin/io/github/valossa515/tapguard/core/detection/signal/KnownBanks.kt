package io.github.valossa515.tapguard.core.detection.signal

import java.util.Collections

/**
 * Apps de banco conhecidos para o sinal S9: nome de exibição e pacote oficial de cada um.
 * A SPEC-018 também deriva de [apps] os pacotes distintos para o S1 (SPEC-019), na composição
 * Hilt: só o pacote conhecido junto com instalador de clonagem conhecido dispensa o sinal.
 *
 * Critério de corte (ADR-020): as 14 instituições com mais clientes no ranking do Banco Central
 * (dez/2025), todas acima de 25 milhões, e os apps oficiais delas com o nome da marca no título,
 * achados na Google Play com o desenvolvedor do banco. Ficaram para depois, com o pacote já
 * conferido: Banco Pan, Sicoob, Sicredi, Banrisul, Safra e BV.
 *
 * Os nomes vêm do título da Play (a parte antes de `:`) ou do nome do desenvolvedor. O rótulo que
 * aparece no aparelho pode ser outro e fica para conferir no teste ponta a ponta (SPEC-022).
 *
 * [apps] não pode ser alterado, nem por cast para [MutableSet]. Quem usa o SDK monta o próprio
 * conjunto a partir dele: por exemplo, `KnownBanks.apps + KnownBankApp("Banco X", "com.bancox")`
 * para ampliar ou `KnownBanks.apps - app` para reduzir.
 *
 * Limitação (ADR-017): o nome de pacote é único no aparelho, não no mundo. Num aparelho sem o app
 * do banco, outro app pode usar o pacote oficial e não recebe S9 (defesa planejada na issue #65).
 */
object KnownBanks {
    val apps: Set<KnownBankApp> =
        Collections.unmodifiableSet(
            linkedSetOf(
                // Caixa. Fontes: Google Play ("CAIXA", "CAIXA Tem", "Cartões CAIXA");
                // desenvolvedor Caixa Econômica Federal.
                KnownBankApp("CAIXA", "br.com.gabba.Caixa"),
                KnownBankApp("CAIXA Tem", "br.gov.caixa.tem"),
                KnownBankApp("Cartões CAIXA", "br.gov.caixa.cartoes"),
                // Nubank. Fonte: Google Play ("Nubank: conta, cartão e mais").
                KnownBankApp("Nubank", "com.nu.production"),
                // Bradesco. Fontes: Google Play ("Bradesco: Conta, Cartão e Pix!", "Bradesco Cartões: Faturas e +",
                // "Bradesco Empresas: Conta PJ", "Bradesco Net Empresa: Conta PJ", "next: Conta Digital e Cartão").
                KnownBankApp("Bradesco", "com.bradesco"),
                KnownBankApp("Bradesco Cartões", "br.com.bradesco.cartoes"),
                KnownBankApp("Bradesco Empresas", "br.com.bradesco.pdpjapp"),
                KnownBankApp("Bradesco Net Empresa", "br.com.bradesco.netempresa"),
                KnownBankApp("next", "br.com.bradesco.next"),
                // Itaú. Fontes: Google Play ("Banco Itaú: Conta, Cartão e +", "Itaú Empresas: Conta PJ",
                // "Personnalité: banco e cartão"); desenvolvedor Itaú Unibanco S. A.
                KnownBankApp("Itaú", "com.itau"),
                KnownBankApp("Itaú Empresas", "com.itau.empresas"),
                KnownBankApp("Personnalité", "com.itau.pers"),
                // Banco do Brasil. Fontes: Google Play ("BB: Banco, Conta, Pix, Cartão");
                // desenvolvedor Banco do Brasil SA (apelido "Banco do Brasil").
                KnownBankApp("BB", "br.com.bb.android"),
                KnownBankApp("Banco do Brasil", "br.com.bb.android"),
                // Santander. Fontes: Google Play ("Santander Brasil", "Santander Empresas");
                // desenvolvedor Banco Santander (Brasil) S.A. (apelido "Santander").
                KnownBankApp("Santander Brasil", "com.santander.app"),
                KnownBankApp("Santander", "com.santander.app"),
                KnownBankApp("Santander Empresas", "com.santandermovelempresarial.app"),
                // Mercado Pago. Fonte: Google Play ("Mercado Pago: Banco Digital"; desenvolvedor Mercado Libre).
                KnownBankApp("Mercado Pago", "com.mercadopago.wallet"),
                // PicPay. Fonte: Google Play ("Banco PicPay: Cartão, Pix e +").
                KnownBankApp("PicPay", "com.picpay"),
                // Inter. Fontes: Google Play ("Inter: Conta, Cartão e Pix", "Inter Empresas: Conta PJ");
                // desenvolvedor Banco Inter SA.
                KnownBankApp("Inter", "br.com.intermedium"),
                KnownBankApp("Inter Empresas", "br.com.Inter.CDPro"),
                // PagBank. Fonte: Google Play ("Banco PagBank: Conta, Tap On +"; desenvolvedor PagBank).
                KnownBankApp("PagBank", "br.com.uol.ps.myaccount"),
                // C6 Bank. Fonte: Google Play ("C6 Bank: Cartão, Conta e Mais!").
                KnownBankApp("C6 Bank", "com.c6bank.app"),
                // BTG Pactual. Fontes: Google Play ("BTG Pactual Banking", "BTG Pactual Empresas: Conta PJ",
                // "BTG Pactual Investimentos"); desenvolvedor Banco BTG Pactual S.A. (apelido "BTG Pactual").
                KnownBankApp("BTG Pactual Banking", "com.btg.pactual.banking"),
                KnownBankApp("BTG Pactual", "com.btg.pactual.banking"),
                KnownBankApp("BTG Pactual Empresas", "com.btg.business"),
                KnownBankApp("BTG Pactual Investimentos", "com.btg.pactual.digital.mobile"),
                // 99Pay. Fonte: Google Play ("99Pay"; desenvolvedor 99 Tecnologia LTDA).
                KnownBankApp("99Pay", "com.pay99.wallet"),
                // Neon. Fonte: Google Play ("Neon cartão de crédito e conta").
                KnownBankApp("Neon", "br.com.neon"),
            ),
        )
}
