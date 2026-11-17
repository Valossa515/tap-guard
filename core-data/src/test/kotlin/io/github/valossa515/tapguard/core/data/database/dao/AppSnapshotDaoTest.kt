package io.github.valossa515.tapguard.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSnapshotDaoTest {
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
    fun `insert all and get by scan id returns the rows`() =
        runTest {
            val scanId = insertScanRun()
            val snapshots =
                listOf(
                    snapshot(scanId, "com.example.decoy", installer = null),
                    snapshot(scanId, "com.example.bank", installer = "com.android.vending"),
                )

            database.appSnapshotDao().insertAll(snapshots)

            assertEquals(
                snapshots.sortedBy { it.packageName },
                database.appSnapshotDao().getByScanId(scanId).sortedBy { it.packageName },
            )
        }

    @Test
    fun `duplicate scan id and package name is rejected`() =
        runTest {
            val scanId = insertScanRun()
            database.appSnapshotDao().insertAll(listOf(snapshot(scanId, "com.example.decoy", installer = null)))

            val duplicate = snapshot(scanId, "com.example.decoy", installer = null)

            val result = runCatching { database.appSnapshotDao().insertAll(listOf(duplicate)) }

            assertTrue("esperava violação de chave: $result", result.exceptionOrNull() is SQLiteConstraintException)
        }

    @Test
    fun `orphan scan id is rejected`() =
        runTest {
            val orphan = snapshot(MISSING_SCAN_ID, "com.example.decoy", installer = null)

            val result = runCatching { database.appSnapshotDao().insertAll(listOf(orphan)) }

            assertTrue("esperava violação de FK: $result", result.exceptionOrNull() is SQLiteConstraintException)
        }

    @Test
    fun `deleting the scan run cascades to its snapshots`() =
        runTest {
            val scanId = insertScanRun()
            database.appSnapshotDao().insertAll(listOf(snapshot(scanId, "com.example.decoy", installer = null)))

            // Os DAOs não apagam (só inserir e ler): o teste apaga a varredura por SQL direto.
            database.openHelper.writableDatabase.execSQL("DELETE FROM scan_run WHERE id = $scanId")

            assertTrue(database.appSnapshotDao().getByScanId(scanId).isEmpty())
        }

    private suspend fun insertScanRun(): Long =
        database.scanRunDao().insert(
            ScanRunEntity(
                startedAt = 1_000,
                finishedAt = 2_000,
                trigger = "MANUAL",
                totalApps = 2,
                redCount = 1,
                yellowCount = 0,
            ),
        )

    private fun snapshot(
        scanId: Long,
        packageName: String,
        installer: String?,
    ) = AppSnapshotEntity(
        scanId = scanId,
        packageName = packageName,
        label = "App",
        versionCode = 42,
        installer = installer,
        firstInstallTime = 100,
        lastUpdateTime = 200,
        signals = 0b0_0000_0111,
        score = 65,
        level = "RED",
    )

    private companion object {
        const val MISSING_SCAN_ID = 999L
    }
}
