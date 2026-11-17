package io.github.valossa515.tapguard.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Alerta emitido (especificação, seção 9). [type] guarda `RISKY_APP` ou `DEFAULT_PAYMENT_CHANGED`;
 * [level], quando houver, `GREEN`, `YELLOW` ou `RED`. Datas em milissegundos; [readAt] é nulo até
 * o alerta ser lido.
 */
@Entity(tableName = "alert_event")
data class AlertEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val packageName: String?,
    val versionCode: Long?,
    val level: String?,
    val createdAt: Long,
    val readAt: Long?,
)
