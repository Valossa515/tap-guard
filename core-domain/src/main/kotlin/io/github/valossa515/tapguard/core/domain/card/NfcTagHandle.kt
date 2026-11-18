package io.github.valossa515.tapguard.core.domain.card

/**
 * Marcador opaco do cartão aproximado, entregue ao [CardReader]. O domínio não conhece a `Tag` do
 * Android nem os comandos ao cartão: o `core-nfc` embrulha a `Tag` na própria implementação desta
 * interface e a desembrulha no [CardReader] (SPEC-041).
 */
interface NfcTagHandle
