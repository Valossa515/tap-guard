package io.github.valossa515.tapguard.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma varredura (especificação, seção 9). [trigger] guarda `MANUAL`, `APP_OPEN` ou `PERIODIC`.
 * Datas em milissegundos. [finishedAt] é nulo enquanto a varredura não termina.
 */
@Entity(tableName = "scan_run")
data class ScanRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long?,
    val trigger: String,
    val totalApps: Int,
    val redCount: Int,
    val yellowCount: Int,
)
