package io.github.valossa515.tapguard.core.domain.scan

import io.github.valossa515.tapguard.core.domain.unmodifiableCopy
import java.time.Instant

/**
 * Resultado de uma varredura, como o [ScanDevice] o devolve. [finishedAt] anterior a [startedAt]
 * lança [IllegalArgumentException]. [apps] é uma cópia na ordem recebida que não pode ser alterada.
 */
@ConsistentCopyVisibility
data class ScanReport private constructor(
    val trigger: ScanTrigger,
    val startedAt: Instant,
    val finishedAt: Instant,
    val apps: List<ScannedApp>,
) {
    init {
        require(!finishedAt.isBefore(startedAt)) { "Fim da varredura ($finishedAt) antes do início ($startedAt)" }
    }

    companion object {
        operator fun invoke(
            trigger: ScanTrigger,
            startedAt: Instant,
            finishedAt: Instant,
            apps: List<ScannedApp>,
        ): ScanReport =
            ScanReport(
                trigger = trigger,
                startedAt = startedAt,
                finishedAt = finishedAt,
                apps = apps.unmodifiableCopy(),
            )
    }
}
