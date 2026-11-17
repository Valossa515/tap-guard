package io.github.valossa515.tapguard.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.dao.AlertEventDao
import io.github.valossa515.tapguard.core.data.database.dao.AppSnapshotDao
import io.github.valossa515.tapguard.core.data.database.dao.ScanRunDao
import io.github.valossa515.tapguard.core.data.database.dao.UserDecisionDao
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
class DataModuleTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var database: Provider<TapGuardDatabase>

    @Inject
    lateinit var scanRunDao: ScanRunDao

    @Inject
    lateinit var appSnapshotDao: AppSnapshotDao

    @Inject
    lateinit var userDecisionDao: UserDecisionDao

    @Inject
    lateinit var alertEventDao: AlertEventDao

    @Inject
    lateinit var dataStore: Provider<DataStore<Preferences>>

    @Before
    fun setUp() = hilt.inject()

    @Test
    fun `injects database daos and datastore`() {
        assertNotNull(database.get())
        assertNotNull(scanRunDao)
        assertNotNull(appSnapshotDao)
        assertNotNull(userDecisionDao)
        assertNotNull(alertEventDao)
        assertNotNull(dataStore.get())
    }

    @Test
    fun `database and datastore are singletons`() {
        assertSame(database.get(), database.get())
        assertSame(dataStore.get(), dataStore.get())
    }
}
