package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.AlertEventEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlertEventDaoTest {
    private lateinit var database: TapGuardDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TapGuardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `insert with all fields and get by id returns the row`() =
        runTest {
            val alert =
                AlertEventEntity(
                    type = "RISKY_APP",
                    packageName = "com.example.decoy",
                    versionCode = 42,
                    level = "RED",
                    createdAt = 4_000,
                    readAt = 5_000,
                )

            val id = database.alertEventDao().insert(alert)

            assertEquals(alert.copy(id = id), database.alertEventDao().getById(id))
        }

    @Test
    fun `optional fields round trip as null`() =
        runTest {
            val alert =
                AlertEventEntity(
                    type = "DEFAULT_PAYMENT_CHANGED",
                    packageName = null,
                    versionCode = null,
                    level = null,
                    createdAt = 4_000,
                    readAt = null,
                )

            val id = database.alertEventDao().insert(alert)

            assertEquals(alert.copy(id = id), database.alertEventDao().getById(id))
        }
}
