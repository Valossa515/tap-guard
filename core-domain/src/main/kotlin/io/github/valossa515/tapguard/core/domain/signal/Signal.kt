package io.github.valossa515.tapguard.core.domain.signal

/**
 * Sinal de perfil de golpe encontrado num app (especificação, seção 5).
 *
 * [bit] é a posição do sinal na máscara gravada em `app_snapshot.signals`: S1 no bit 0 até S9 no
 * bit 8. A posição não muda depois de publicada. O peso de cada sinal não fica aqui: fica no
 * arquivo de configuração do motor de score (SPEC-017).
 */
enum class Signal(
    val bit: Int,
) {
    /** S1: instalado fora de uma loja confiável. */
    UNTRUSTED_INSTALL_SOURCE(bit = 0),

    /** S2: declara a permissão de NFC. */
    NFC_PERMISSION(bit = 1),

    /** S3: declara serviço de emulação de cartão (HCE). */
    CARD_EMULATION_SERVICE(bit = 2),

    /** S4: tem serviço de acessibilidade ativo. */
    ACCESSIBILITY_SERVICE_ENABLED(bit = 3),

    /** S5: pede permissão de sobreposição de tela. */
    OVERLAY_PERMISSION(bit = 4),

    /** S6: instalado ou atualizado nos últimos 7 dias. */
    RECENTLY_INSTALLED_OR_UPDATED(bit = 5),

    /** S7: é o app de pagamento padrão e não está na lista de carteiras conhecidas. */
    UNKNOWN_DEFAULT_PAYMENT_APP(bit = 6),

    /** S8: pede leitura ou recebimento de SMS. */
    SMS_PERMISSION(bit = 7),

    /** S9: nome parecido com o de um banco conhecido, mas pacote diferente do oficial. */
    BANK_LOOKALIKE(bit = 8),
}
