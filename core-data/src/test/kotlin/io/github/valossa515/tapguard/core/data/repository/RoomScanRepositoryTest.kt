package io.github.valossa515.tapguard.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.AppSnapshotEntity
import io.github.valossa515.tapguard.core.data.database.entity.ScanRunEntity
import io.github.valossa515.tapguard.core.domain.aRiskResult
import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import io.github.valossa515.tapguard.core.domain.scan.ScanReport
import io.github.valossa515.tapguard.core.domain.scan.ScanTrigger
import io.github.valossa515.tapguard.core.domain.scan.ScannedApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomScanRepositoryTest {
    private lateinit var database: TapGuardDatabase
    private lateinit var repository: RoomScanRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TapGuardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomScanRepository(database, database.scanRunDao(), database.appSnapshotDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `save writes the scan run columns`() =
        runTest {
            val report =
                report(
                    trigger = ScanTrigger.PERIODIC,
                    apps =
                        listOf(
                            scanned("com.example.red", RiskLevel.RED),
                            scanned("com.example.yellow", RiskLevel.YELLOW),
                            scanned("com.example.green", RiskLevel.GREEN),
                        ),
                )

            repository.save(report)

            assertEquals(
                ScanRunEntity(
                    id = onlyScanId(),
                    startedAt = STARTED_AT_MILLIS,
                    finishedAt = FINISHED_AT_MILLIS,
                    trigger = "PERIODIC",
                    totalApps = 3,
                    redCount = 1,
                    yellowCount = 1,
                ),
                database.scanRunDao().getById(onlyScanId()),
            )
        }

    @Test
    fun `save writes one snapshot per app with every column and the signal mask`() =
        runTest {
            val sideloaded =
                ScannedApp(
                    app =
                        anInstalledApp(
                            packageName = "com.example.decoy",
                            label = "Decoy",
                            installerPackageName = null,
                            firstInstallTime = Instant.ofEpochMilli(100),
                            lastUpdateTime = Instant.ofEpochMilli(200),
                        ),
                    risk = aRiskResult(score = 65, level = RiskLevel.RED, signals = S1_S2_S3),
                )
            val lookalike =
                ScannedApp(
                    app =
                        anInstalledApp(
                            packageName = "com.example.fake",
                            label = "Nubank",
                            installerPackageName = "com.android.vending",
                            firstInstallTime = Instant.ofEpochMilli(300),
                            lastUpdateTime = Instant.ofEpochMilli(400),
                        ),
                    risk = aRiskResult(score = 85, level = RiskLevel.RED, signals = S1_S2_S3 + Signal.BANK_LOOKALIKE),
                )

            repository.save(report(apps = listOf(sideloaded, lookalike)))

            val scanId = onlyScanId()
            assertEquals(
                listOf(
                    AppSnapshotEntity(scanId, "com.example.decoy", "Decoy", 1, null, 100, 200, 7, 65, "RED"),
                    AppSnapshotEntity(
                        scanId,
                        "com.example.fake",
                        "Nubank",
                        1,
                        "com.android.vending",
                        300,
                        400,
                        263,
                        85,
                        "RED",
                    ),
                ),
                database.appSnapshotDao().getByScanId(scanId).sortedBy { it.packageName },
            )
        }

    @Test
    fun `counts follow the final level and a trusted app counts as green`() =
        runTest {
            val apps =
                listOf(
                    scanned("com.example.trusted", RiskLevel.GREEN, score = 65),
                    scanned("com.example.red", RiskLevel.RED, score = 65),
                    scanned("com.example.yellow1", RiskLevel.YELLOW, score = 30),
                    scanned("com.example.yellow2", RiskLevel.YELLOW, score = 59),
                )

            repository.save(report(apps = apps))

            val run = database.scanRunDao().getById(onlyScanId())
            assertEquals(1, run?.redCount)
            assertEquals(2, run?.yellowCount)
        }

    @Test
    fun `trusted app keeps its score with the green level`() =
        runTest {
            repository.save(report(apps = listOf(scanned("com.example.trusted", RiskLevel.GREEN, score = 65))))

            val snapshot = database.appSnapshotDao().getByScanId(onlyScanId()).single()
            assertEquals(65, snapshot.score)
            assertEquals("GREEN", snapshot.level)
        }

    @Test
    fun `empty report is saved with zero counts and no snapshots`() =
        runTest {
            repository.save(report(apps = emptyList()))

            val run = database.scanRunDao().getById(onlyScanId())
            assertEquals(0, run?.totalApps)
            assertEquals(0, run?.redCount)
            assertEquals(0, run?.yellowCount)
            assertEquals(0, countRows("app_snapshot"))
        }

    @Test
    fun `eleventh save keeps the ten most recent scans`() =
        runTest {
            repeat(11) { repository.save(report(apps = listOf(scanned("com.example.app", RiskLevel.GREEN)))) }

            val ids = scanIds()
            assertEquals(10, ids.size)
            assertEquals((2L..11L).toList(), ids)
            assertTrue(database.appSnapshotDao().getByScanId(1).isEmpty())
            assertEquals(10, countRows("app_snapshot"))
        }

    @Test
    fun `failed save rolls back the scan and does not apply retention`() =
        runTest {
            repeat(10) { repository.save(report(apps = listOf(scanned("com.example.app", RiskLevel.GREEN)))) }
            val before = scanIds()
            val duplicate =
                report(
                    apps =
                        listOf(
                            scanned("com.example.same", RiskLevel.GREEN),
                            scanned("com.example.same", RiskLevel.RED),
                        ),
                )

            val result = runCatching { repository.save(duplicate) }

            assertTrue("esperava violação de chave: $result", result.exceptionOrNull() is SQLiteConstraintException)
            assertEquals(before, scanIds())
            assertEquals(10, countRows("app_snapshot"))
        }

    private fun onlyScanId(): Long = scanIds().single()

    private fun scanIds(): List<Long> {
        val cursor = database.openHelper.writableDatabase.query("SELECT id FROM scan_run ORDER BY id")
        return cursor.use { generateSequence { if (it.moveToNext()) it.getLong(0) else null }.toList() }
    }

    private fun countRows(table: String): Int {
        val cursor = database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table")
        return cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    private fun scanned(
        packageName: String,
        level: RiskLevel,
        score: Int = 0,
    ) = ScannedApp(app = anInstalledApp(packageName = packageName), risk = aRiskResult(score = score, level = level))

    private fun report(
        trigger: ScanTrigger = ScanTrigger.MANUAL,
        apps: List<ScannedApp>,
    ) = ScanReport(
        trigger = trigger,
        startedAt = Instant.ofEpochMilli(STARTED_AT_MILLIS),
        finishedAt = Instant.ofEpochMilli(FINISHED_AT_MILLIS),
        apps = apps,
    )

    private companion object {
        const val STARTED_AT_MILLIS = 1_000L
        const val FINISHED_AT_MILLIS = 1_500L
        val S1_S2_S3 = setOf(Signal.UNTRUSTED_INSTALL_SOURCE, Signal.NFC_PERMISSION, Signal.CARD_EMULATION_SERVICE)
    }
}
