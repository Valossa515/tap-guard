package io.github.valossa515.tapguard.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `first read returns the four defaults`() =
        runTest {
            val settings = settingsIn(this)

            assertFalse(settings.onboardingDone.first())
            assertTrue(settings.monitoringEnabled.first())
            assertEquals(6, settings.scanIntervalHours.first())
            assertNull(settings.lastDefaultPaymentComponent.first())
        }

    @Test
    fun `written values are read back`() {
        assumeFalse(WINDOWS_SKIP_REASON, IS_WINDOWS)
        runTest {
            val settings = settingsIn(this)

            settings.setOnboardingDone(true)
            settings.setMonitoringEnabled(false)
            settings.setScanIntervalHours(12)
            settings.setLastDefaultPaymentComponent(COMPONENT)

            assertTrue(settings.onboardingDone.first())
            assertFalse(settings.monitoringEnabled.first())
            assertEquals(12, settings.scanIntervalHours.first())
            assertEquals(COMPONENT, settings.lastDefaultPaymentComponent.first())
        }
    }

    @Test
    fun `writing null removes last default payment component`() {
        assumeFalse(WINDOWS_SKIP_REASON, IS_WINDOWS)
        runTest {
            val settings = settingsIn(this)
            settings.setLastDefaultPaymentComponent(COMPONENT)

            settings.setLastDefaultPaymentComponent(null)

            assertNull(settings.lastDefaultPaymentComponent.first())
        }
    }

    private fun settingsIn(test: TestScope): SettingsDataSource =
        SettingsDataSource(
            PreferenceDataStoreFactory.create(scope = test.backgroundScope) {
                File(folder.root, "settings.preferences_pb")
            },
        )

    private companion object {
        const val COMPONENT = "com.example.wallet/.PaymentService"

        // No Windows, renomear o arquivo temporário do DataStore por cima do existente falha na JVM
        // a partir da segunda gravação. No Android e no Linux do CI estes testes rodam.
        val IS_WINDOWS = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        const val WINDOWS_SKIP_REASON = "DataStore real com mais de uma gravação: verificado no CI (Linux)"
    }
}
