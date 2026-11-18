package io.github.valossa515.tapguard.core.domain.card

/**
 * Os quatro últimos dígitos do cartão: exatamente quatro dígitos ASCII, de 0 a 9. Qualquer outro
 * valor lança [IllegalArgumentException], com mensagem que não repete o valor recebido (pode ser um
 * número completo). [toString] não mostra os dígitos, para nada vazar em log (RF-205).
 */
@JvmInline
value class LastFourDigits(
    val value: String,
) {
    init {
        require(value.length == LENGTH && value.all { it in '0'..'9' }) {
            "Esperados exatamente $LENGTH dígitos de 0 a 9"
        }
    }

    override fun toString(): String = "LastFourDigits(****)"

    private companion object {
        const val LENGTH = 4
    }
}
