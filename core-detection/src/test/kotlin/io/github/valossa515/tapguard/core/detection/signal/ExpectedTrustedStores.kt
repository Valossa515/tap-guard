package io.github.valossa515.tapguard.core.detection.signal

/**
 * Lojas que a lista padrão deve conter, escritas à mão. Os testes não leem [TrustedStores]: assim,
 * tirar uma loja da lista de produção derruba o caso dela em vez de sumir com ele.
 */
internal val EXPECTED_TRUSTED_STORES: List<String> =
    listOf(
        "com.android.vending",
        "com.sec.android.app.samsungapps",
        "com.xiaomi.mipicks",
        "com.xiaomi.market",
        "com.heytap.market",
        "com.oppo.market",
        "com.vivo.appstore",
        "com.hihonor.appmarket",
        "com.transsnet.store",
        "com.huawei.appmarket",
        "com.amazon.venezia",
    )
