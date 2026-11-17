package io.github.valossa515.tapguard.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.entity.UserDecisionEntity
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
class UserDecisionDaoTest {
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
    fun `insert and get by package name returns the decision`() =
        runTest {
            val decision = UserDecisionEntity(packageName = "com.example.bank", decision = "TRUSTED", decidedAt = 3_000)

            database.userDecisionDao().insert(decision)

            assertEquals(decision, database.userDecisionDao().getByPackageName("com.example.bank"))
        }
}
