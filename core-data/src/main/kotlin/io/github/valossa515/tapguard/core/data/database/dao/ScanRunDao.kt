package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acesso a `scan_run`. É `abstract class`, e não interface como os outros DAOs, para que a retenção
 * valide [deleteOlderThanLatest] antes da consulta, sem expor a consulta sem a validação.
 */
@Dao
abstract class ScanRunDao {
    @Insert
    abstract suspend fun insert(scanRun: ScanRunEntity): Long

    @Query("SELECT * FROM scan_run WHERE id = :id")
    abstract suspend fun getById(id: Long): ScanRunEntity?

    @Query("SELECT * FROM scan_run ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<ScanRunEntity>>

    /**
     * Mantém as [keep] varreduras de maior `id` e apaga as demais. O `id` autoincremento segue a
     * ordem de gravação, então a retenção não depende do relógio do aparelho. Os `app_snapshot` das
     * varreduras apagadas caem pela cascata da chave estrangeira. [keep] menor que 1 lança
     * [IllegalArgumentException] sem apagar nada: no SQLite, `LIMIT 0` apagaria todas e um `LIMIT`
     * negativo não apagaria nenhuma.
     */
    suspend fun deleteOlderThanLatest(keep: Int) {
        require(keep > 0) { KEEP_BELOW_ONE_MESSAGE }
        deleteOutsideLatest(keep)
    }

    @Query("DELETE FROM scan_run WHERE id NOT IN (SELECT id FROM scan_run ORDER BY id DESC LIMIT :keep)")
    protected abstract suspend fun deleteOutsideLatest(keep: Int)

    private companion object {
        const val KEEP_BELOW_ONE_MESSAGE = "A retenção precisa manter pelo menos uma varredura"
    }
}
