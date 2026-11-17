package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.valossa515.tapguard.core.data.database.entity.AlertEventEntity

@Dao
interface AlertEventDao {
    @Insert
    suspend fun insert(alert: AlertEventEntity): Long

    @Query("SELECT * FROM alert_event WHERE id = :id")
    suspend fun getById(id: Long): AlertEventEntity?
}
