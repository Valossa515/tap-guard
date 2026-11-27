package io.github.valossa515.tapguard.core.data.repository

import androidx.room.withTransaction
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.dao.AppSnapshotDao
import io.github.valossa515.tapguard.core.data.database.dao.ScanRunDao
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import io.github.valossa515.tapguard.core.domain.scan.ScanReport
import io.github.valossa515.tapguard.core.domain.scan.ScanRepository
import io.github.valossa515.tapguard.core.domain.scan.ScannedApp
import io.github.valossa515.tapguard.core.domain.signal.toBitMask
import javax.inject.Inject

/**
 * [ScanRepository] sobre o Room. Numa só transação, grava a linha de `scan_run`, um `app_snapshot`
 * por app e aplica a retenção: ficam as 10 varreduras de maior `id` (ordem de gravação, sem depender
 * do relógio), e os snapshots das apagadas caem pela cascata. Uma falha em qualquer passo desfaz os
 * três, e a exceção é propagada.
 *
 * `scan_run.finishedAt` sempre é preenchido, porque a linha só é gravada no fim da varredura.
 * `redCount` e `yellowCount` contam pelo nível final: um app confiável conta como verde. Permissões,
 * serviços e o pagamento padrão não são gravados.
 */
class RoomScanRepository
    @Inject
    constructor(
        private val database: TapGuardDatabase,
        private val scanRunDao: ScanRunDao,
        private val appSnapshotDao: AppSnapshotDao,
    ) : ScanRepository {
        override suspend fun save(report: ScanReport) {
            database.withTransaction {
                val scanId = scanRunDao.insert(report.toScanRunEntity())
                appSnapshotDao.insertAll(report.apps.map { it.toAppSnapshotEntity(scanId) })
                scanRunDao.deleteOlderThanLatest(RETAINED_SCANS)
            }
        }

        private fun ScanReport.toScanRunEntity(): ScanRunEntity =
            ScanRunEntity(
                startedAt = startedAt.toEpochMilli(),
                finishedAt = finishedAt.toEpochMilli(),
                trigger = trigger.name,
                totalApps = apps.size,
                redCount = apps.count { it.risk.level == RiskLevel.RED },
                yellowCount = apps.count { it.risk.level == RiskLevel.YELLOW },
            )

        private fun ScannedApp.toAppSnapshotEntity(scanId: Long): AppSnapshotEntity =
            AppSnapshotEntity(
                scanId = scanId,
                packageName = app.packageName,
                label = app.label,
                versionCode = app.versionCode,
                installer = app.installerPackageName,
                firstInstallTime = app.firstInstallTime.toEpochMilli(),
                lastUpdateTime = app.lastUpdateTime.toEpochMilli(),
                signals = risk.signals.toBitMask(),
                score = risk.score,
                level = risk.level.name,
            )

        private companion object {
            const val RETAINED_SCANS = 10
        }
    }
