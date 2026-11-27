package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRunDao {
    @Insert
    suspend fun insert(scanRun: ScanRunEntity): Long

    @Query("SELECT * FROM scan_run WHERE id = :id")
    suspend fun getById(id: Long): ScanRunEntity?

    @Query("SELECT * FROM scan_run ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ScanRunEntity>>

    /**
     * Mantém as [keep] varreduras de maior `id` e apaga as demais. O `id` autoincremento segue a
     * ordem de gravação, então a retenção não depende do relógio do aparelho. Os `app_snapshot` das
     * varreduras apagadas caem pela cascata da chave estrangeira.
     */
    @Query("DELETE FROM scan_run WHERE id NOT IN (SELECT id FROM scan_run ORDER BY id DESC LIMIT :keep)")
    suspend fun deleteOlderThanLatest(keep: Int)
}
