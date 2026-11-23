package io.github.valossa515.tapguard.core.detection.signal

/**
 * Carteiras que a lista padrão deve conter, escritas à mão. Os testes não leem [KnownWallets] para
 * montar o esperado: assim, tirar uma carteira da lista de produção derruba o teste.
 */
internal val EXPECTED_KNOWN_WALLETS: List<String> =
    listOf(
        "com.google.android.apps.walletnfcrel",
        "com.samsung.android.spay",
        "com.nu.production",
        "com.itau",
        "br.com.bb.android",
        "br.com.intermedium",
    )
