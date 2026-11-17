package io.github.valossa515.tapguard.core.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A versão 1 nasce do schema commitado em `core-data/schemas`. Toda versão nova ganha um teste aqui. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TapGuardDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(TEST_DATABASE),
            driver = AndroidSQLiteDriver(),
            databaseClass = TapGuardDatabase::class,
        )

    @Test
    fun `version 1 is created and validated from the exported schema`() {
        helper.createDatabase(1).close()

        helper.runMigrationsAndValidate(1, emptyList()).close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
