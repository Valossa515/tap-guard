package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity
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

    @Test
    fun `delete older than latest keeps the ten highest ids`() =
        runTest {
            val ids = (1..12).map { database.scanRunDao().insert(scanRun(startedAt = it * 1_000L, finishedAt = null)) }

            database.scanRunDao().deleteOlderThanLatest(10)

            assertEquals(ids.drop(2), remainingIds())
        }

    @Test
    fun `delete older than latest keeps the newest run even with the oldest start time`() =
        runTest {
            val older =
                (1..10).map { database.scanRunDao().insert(scanRun(startedAt = 5_000L + it, finishedAt = null)) }
            // Relógio do aparelho atrasado: a varredura gravada por último começou "antes" de todas.
            val newest = database.scanRunDao().insert(scanRun(startedAt = 1L, finishedAt = 2L))

            database.scanRunDao().deleteOlderThanLatest(10)

            assertEquals(older.drop(1) + newest, remainingIds())
        }

    @Test
    fun `delete older than latest with fewer runs than kept deletes nothing`() =
        runTest {
            val ids = (1..3).map { database.scanRunDao().insert(scanRun(startedAt = it * 1_000L, finishedAt = null)) }

            database.scanRunDao().deleteOlderThanLatest(10)

            assertEquals(ids, remainingIds())
        }

    @Test
    fun `runs removed by retention take their snapshots with them`() =
        runTest {
            val ids = (1..11).map { database.scanRunDao().insert(scanRun(startedAt = it * 1_000L, finishedAt = null)) }
            ids.forEach { database.appSnapshotDao().insertAll(listOf(snapshot(it))) }

            database.scanRunDao().deleteOlderThanLatest(10)

            assertTrue(database.appSnapshotDao().getByScanId(ids.first()).isEmpty())
            ids.drop(1).forEach { assertEquals(1, database.appSnapshotDao().getByScanId(it).size) }
        }

    private fun remainingIds(): List<Long> {
        val cursor = database.openHelper.writableDatabase.query("SELECT id FROM scan_run ORDER BY id")
        return cursor.use { generateSequence { if (it.moveToNext()) it.getLong(0) else null }.toList() }
    }

    private fun snapshot(scanId: Long) =
        AppSnapshotEntity(
            scanId = scanId,
            packageName = "com.example.app",
            label = "App",
            versionCode = 1,
            installer = null,
            firstInstallTime = 100,
            lastUpdateTime = 200,
            signals = 0,
            score = 0,
            level = "GREEN",
        )

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
