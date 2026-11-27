package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.valossa515.tapguard.core.data.database.entity.UserDecisionEntity

@Dao
interface UserDecisionDao {
    @Insert
    suspend fun insert(decision: UserDecisionEntity)

    @Query("SELECT * FROM user_decision WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): UserDecisionEntity?

    /** Pacotes marcados como confiáveis: `decision` igual a `TRUSTED`, com comparação exata. */
    @Query("SELECT packageName FROM user_decision WHERE decision = 'TRUSTED'")
    suspend fun getTrustedPackageNames(): List<String>
}
