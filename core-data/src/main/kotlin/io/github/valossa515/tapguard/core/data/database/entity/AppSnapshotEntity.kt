package io.github.valossa515.tapguard.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Estado de um app numa varredura (especificação, seção 9). Chave: [scanId] + [packageName].
 * Apagar a varredura apaga os snapshots dela. [signals] é a máscara de bits de S1 (bit 0) a
 * S9 (bit 8); [level] guarda `GREEN`, `YELLOW` ou `RED`. Datas em milissegundos.
 */
@Entity(
    tableName = "app_snapshot",
    primaryKeys = ["scanId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = ScanRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AppSnapshotEntity(
    val scanId: Long,
    val packageName: String,
    val label: String,
    val versionCode: Long,
    val installer: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val signals: Int,
    val score: Int,
    val level: String,
)
