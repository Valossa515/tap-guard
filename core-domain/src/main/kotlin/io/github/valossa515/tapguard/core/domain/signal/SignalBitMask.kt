package io.github.valossa515.tapguard.core.domain.signal

import io.github.valossa515.tapguard.core.domain.unmodifiableCopy

/** Máscara de bits dos sinais, como gravada em `app_snapshot.signals`. */
fun Set<Signal>.toBitMask(): Int = fold(0) { mask, signal -> mask or (1 shl signal.bit) }

/**
 * Sinais de uma máscara de bits. Lança [IllegalArgumentException] se a máscara for negativa ou
 * tiver bit fora dos nove sinais.
 */
fun signalsFromBitMask(mask: Int): Set<Signal> {
    require(mask and ALL_SIGNALS_MASK.inv() == 0) { "Máscara de sinais inválida: $mask" }
    return Signal.entries
        .filter { mask and (1 shl it.bit) != 0 }
        .toSet()
        .unmodifiableCopy()
}

private val ALL_SIGNALS_MASK: Int = Signal.entries.toSet().toBitMask()
