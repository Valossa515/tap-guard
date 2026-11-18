package io.github.valossa515.tapguard.core.domain.signal

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp

/** Coletor de sinais: devolve os sinais encontrados num app. */
interface SignalCollector {
    suspend fun collect(app: InstalledApp): Set<Signal>
}
