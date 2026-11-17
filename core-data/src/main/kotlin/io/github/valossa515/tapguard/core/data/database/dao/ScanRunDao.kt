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
}
