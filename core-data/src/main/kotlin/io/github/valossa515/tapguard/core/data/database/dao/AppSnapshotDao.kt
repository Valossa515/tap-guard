package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity

@Dao
interface AppSnapshotDao {
    @Insert
    suspend fun insertAll(snapshots: List<AppSnapshotEntity>)

    @Query("SELECT * FROM app_snapshot WHERE scanId = :scanId")
    suspend fun getByScanId(scanId: Long): List<AppSnapshotEntity>
}
