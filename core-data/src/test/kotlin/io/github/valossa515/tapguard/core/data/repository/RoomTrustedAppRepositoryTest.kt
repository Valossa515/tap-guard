package io.github.valossa515.tapguard.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.UserDecisionEntity
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
class RoomTrustedAppRepositoryTest {
    private lateinit var database: TapGuardDatabase
    private lateinit var repository: RoomTrustedAppRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TapGuardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomTrustedAppRepository(database.userDecisionDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `returns only packages whose decision is exactly TRUSTED`() =
        runTest {
            decide("com.example.bank", "TRUSTED")
            decide("com.example.game", "TRUSTED")
            decide("com.example.other", "trusted")

            assertEquals(setOf("com.example.bank", "com.example.game"), repository.trustedPackages())
        }

    @Test
    fun `returns an empty set when there is no decision`() =
        runTest {
            assertTrue(repository.trustedPackages().isEmpty())
        }

    private suspend fun decide(
        packageName: String,
        decision: String,
    ) = database.userDecisionDao().insert(
        UserDecisionEntity(packageName = packageName, decision = decision, decidedAt = 3_000),
    )
}
