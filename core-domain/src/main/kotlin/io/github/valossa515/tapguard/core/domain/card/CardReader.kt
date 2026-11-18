package io.github.valossa515.tapguard.core.domain.card

/** Leitor do cartão aproximado. */
interface CardReader {
    suspend fun read(tag: NfcTagHandle): CardReadResult
}
