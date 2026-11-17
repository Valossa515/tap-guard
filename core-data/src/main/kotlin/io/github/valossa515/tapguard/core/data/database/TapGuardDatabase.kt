package io.github.valossa515.tapguard.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.valossa515.tapguard.core.data.database.dao.AlertEventDao
import io.github.valossa515.tapguard.core.data.database.dao.AppSnapshotDao
import io.github.valossa515.tapguard.core.data.database.dao.ScanRunDao
import io.github.valossa515.tapguard.core.data.database.dao.UserDecisionDao
import io.github.valossa515.tapguard.core.data.database.entity.AlertEventEntity
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import io.github.valossa515.tapguard.core.data.database.entity.UserDecisionEntity

/**
 * Banco local do Tap Guard (especificação, seção 9). Toda mudança de versão traz uma `Migration`
 * e um teste de migração; o schema de cada versão fica em `core-data/schemas`, no repositório.
 * Nenhum dado de cartão entra aqui.
 */
@Database(
    entities = [
        ScanRunEntity::class,
        AppSnapshotEntity::class,
        UserDecisionEntity::class,
        AlertEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TapGuardDatabase : RoomDatabase() {
    abstract fun scanRunDao(): ScanRunDao

    abstract fun appSnapshotDao(): AppSnapshotDao

    abstract fun userDecisionDao(): UserDecisionDao

    abstract fun alertEventDao(): AlertEventDao

    companion object {
        const val DATABASE_NAME = "tapguard.db"
    }
}
