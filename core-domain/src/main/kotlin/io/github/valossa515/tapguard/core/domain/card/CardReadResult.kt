package io.github.valossa515.tapguard.core.domain.card

import java.time.YearMonth

/** Resultado da leitura do cartão pelo [CardReader]. Falhas esperadas são valores, não exceções. */
sealed interface CardReadResult {
    /**
     * Cartão lido. Não há campo para o número completo: só os quatro últimos dígitos (RNF-02 e
     * ADR-008). [toString] não mostra nenhum dos campos, para nada vazar em log (RF-205).
     */
    data class Success(
        val applicationLabel: String,
        val lastFourDigits: LastFourDigits,
        val expiry: YearMonth,
    ) : CardReadResult {
        override fun toString(): String = "Success(****)"
    }

    /** O cartão não é suportado. */
    data object UnsupportedCard : CardReadResult

    /** A leitura foi interrompida antes de terminar. */
    data object ReadInterrupted : CardReadResult
}
