package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanRunDaoTest {
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
    fun `insert generates id and get by id returns the row`() =
        runTest {
            val run = scanRun(startedAt = 1_000, finishedAt = 2_000)

            val id = database.scanRunDao().insert(run)

            assertTrue(id > 0)
            assertEquals(run.copy(id = id), database.scanRunDao().getById(id))
        }

    @Test
    fun `observe all emits inserted rows newest first`() =
        runTest {
            val older = database.scanRunDao().insert(scanRun(startedAt = 1_000, finishedAt = 1_500))
            val newer = database.scanRunDao().insert(scanRun(startedAt = 5_000, finishedAt = 5_500))

            val rows = database.scanRunDao().observeAll().first()

            assertEquals(listOf(newer, older), rows.map { it.id })
        }

    @Test
    fun `optional finished at round trips as null`() =
        runTest {
            val id = database.scanRunDao().insert(scanRun(startedAt = 1_000, finishedAt = null))

            assertNull(database.scanRunDao().getById(id)?.finishedAt)
        }

    private fun scanRun(
        startedAt: Long,
        finishedAt: Long?,
    ) = ScanRunEntity(
        startedAt = startedAt,
        finishedAt = finishedAt,
        trigger = "MANUAL",
        totalApps = 120,
        redCount = 1,
        yellowCount = 3,
    )
}
