package io.github.valossa515.tapguard.core.detection.signal

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import io.github.valossa515.tapguard.core.domain.signal.Signal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InstallSourceCollectorRejectedCloneTest(
    private val installer: String?,
) {
    @Test
    fun `known bank with manual or excluded installer yields s1`() =
        runTest {
            val collector =
                InstallSourceCollector(
                    TrustedStores.packageNames,
                    setOf("com.example.bank"),
                    KnownCloneApps.packageNames,
                )
            val app = anInstalledApp(packageName = "com.example.bank", installerPackageName = installer)

            assertEquals(setOf(Signal.UNTRUSTED_INSTALL_SOURCE), collector.collect(app))
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun installers(): List<String?> =
            listOf(
                null,
                "com.example.unknown",
                "com.android.shell",
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.miui.packageinstaller",
                "com.xiaomi.midrop",
                "com.lenovo.anyshare.gps",
                "shareit.lite",
                "com.miui.backup",
            )
    }
}
