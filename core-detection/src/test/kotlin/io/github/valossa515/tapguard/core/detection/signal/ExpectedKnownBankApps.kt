package io.github.valossa515.tapguard.core.detection.signal

/**
 * Apps de banco que a lista padrão deve conter, escritos à mão. Os testes não leem [KnownBanks] para
 * montar o esperado: assim, tirar ou trocar uma entrada da lista de produção derruba o teste.
 */
internal val EXPECTED_KNOWN_BANK_APPS: List<KnownBankApp> =
    listOf(
        KnownBankApp("CAIXA", "br.com.gabba.Caixa"),
        KnownBankApp("CAIXA Tem", "br.gov.caixa.tem"),
        KnownBankApp("Cartões CAIXA", "br.gov.caixa.cartoes"),
        KnownBankApp("Nubank", "com.nu.production"),
        KnownBankApp("Bradesco", "com.bradesco"),
        KnownBankApp("Bradesco Cartões", "br.com.bradesco.cartoes"),
        KnownBankApp("Bradesco Empresas", "br.com.bradesco.pdpjapp"),
        KnownBankApp("Bradesco Net Empresa", "br.com.bradesco.netempresa"),
        KnownBankApp("next", "br.com.bradesco.next"),
        KnownBankApp("Itaú", "com.itau"),
        KnownBankApp("Itaú Empresas", "com.itau.empresas"),
        KnownBankApp("Personnalité", "com.itau.pers"),
        KnownBankApp("BB", "br.com.bb.android"),
        KnownBankApp("Banco do Brasil", "br.com.bb.android"),
        KnownBankApp("Santander Brasil", "com.santander.app"),
        KnownBankApp("Santander", "com.santander.app"),
        KnownBankApp("Santander Empresas", "com.santandermovelempresarial.app"),
        KnownBankApp("Mercado Pago", "com.mercadopago.wallet"),
        KnownBankApp("PicPay", "com.picpay"),
        KnownBankApp("Inter", "br.com.intermedium"),
        KnownBankApp("Inter Empresas", "br.com.Inter.CDPro"),
        KnownBankApp("PagBank", "br.com.uol.ps.myaccount"),
        KnownBankApp("C6 Bank", "com.c6bank.app"),
        KnownBankApp("BTG Pactual Banking", "com.btg.pactual.banking"),
        KnownBankApp("BTG Pactual", "com.btg.pactual.banking"),
        KnownBankApp("BTG Pactual Empresas", "com.btg.business"),
        KnownBankApp("BTG Pactual Investimentos", "com.btg.pactual.digital.mobile"),
        KnownBankApp("99Pay", "com.pay99.wallet"),
        KnownBankApp("Neon", "br.com.neon"),
    )
