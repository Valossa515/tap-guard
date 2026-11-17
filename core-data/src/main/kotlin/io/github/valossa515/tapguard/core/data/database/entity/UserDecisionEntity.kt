package io.github.valossa515.tapguard.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** App marcado pelo usuário (especificação, seção 9). [decision] guarda `TRUSTED`. Data em milissegundos. */
@Entity(tableName = "user_decision")
data class UserDecisionEntity(
    @PrimaryKey val packageName: String,
    val decision: String,
    val decidedAt: Long,
)
