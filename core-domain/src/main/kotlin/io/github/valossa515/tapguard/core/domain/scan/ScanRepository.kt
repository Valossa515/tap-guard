package io.github.valossa515.tapguard.core.domain.scan

/**
 * Histórico de varreduras: onde o [ScanDevice] grava cada [ScanReport]. A gravação é atômica: a
 * varredura entra inteira ou não entra. A implementação pode aplicar uma política de retenção na
 * mesma operação; a do app mantém as 10 varreduras mais recentes pela ordem de gravação. Uma falha
 * é propagada para quem chama, sem nada gravado pela metade.
 */
interface ScanRepository {
    suspend fun save(report: ScanReport)
}
