package io.github.valossa515.tapguard.core.domain.scan

/** Caso de uso da varredura: inventário, coletores e score de todos os apps. */
interface ScanDevice {
    suspend operator fun invoke(trigger: ScanTrigger): ScanReport
}
