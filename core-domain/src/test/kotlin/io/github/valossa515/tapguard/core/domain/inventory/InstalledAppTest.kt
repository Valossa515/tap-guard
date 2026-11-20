package io.github.valossa515.tapguard.core.domain.inventory

import io.github.valossa515.tapguard.core.domain.anInstalledApp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class InstalledAppTest {
    @Test
    fun `fields are read back and installer accepts null`() {
        val app =
            InstalledApp(
                packageName = "com.example.app",
                label = "Example",
                versionCode = 42L,
                installerPackageName = null,
                firstInstallTime = FIRST_INSTALL,
                lastUpdateTime = LAST_UPDATE,
                requestedPermissions = setOf(NFC),
                services = setOf(SERVICE),
                cardEmulationServices = emptySet(),
            )

        assertEquals("com.example.app", app.packageName)
        assertEquals("Example", app.label)
        assertEquals(42L, app.versionCode)
        assertNull(app.installerPackageName)
        assertEquals(FIRST_INSTALL, app.firstInstallTime)
        assertEquals(LAST_UPDATE, app.lastUpdateTime)
        assertEquals(setOf(NFC), app.requestedPermissions)
        assertEquals(setOf(SERVICE), app.services)
    }

    @Test
    fun `mutating source permissions and services does not change the model`() {
        val permissions = mutableSetOf(NFC)
        val services = mutableSetOf(SERVICE)
        val app = anInstalledApp(requestedPermissions = permissions, services = services)

        permissions.add(SMS)
        services.clear()

        assertEquals(setOf(NFC), app.requestedPermissions)
        assertEquals(setOf(SERVICE), app.services)
    }

    @Test
    fun `exposed permissions and services reject add remove and clear through mutable set cast`() {
        val app = anInstalledApp(requestedPermissions = setOf(NFC), services = setOf(SERVICE))

        for (exposed in listOf(app.requestedPermissions, app.services)) {
            val mutable = exposed as MutableSet<String>
            assertThrows(UnsupportedOperationException::class.java) { mutable.add(SMS) }
            assertThrows(UnsupportedOperationException::class.java) { mutable.remove(mutable.first()) }
            assertThrows(UnsupportedOperationException::class.java) { mutable.clear() }
        }
        assertEquals(setOf(NFC), app.requestedPermissions)
        assertEquals(setOf(SERVICE), app.services)
    }

    private companion object {
        const val NFC = "android.permission.NFC"
        const val SMS = "android.permission.READ_SMS"
        const val SERVICE = "com.example.app.PaymentService"
        val FIRST_INSTALL: Instant = Instant.parse("2026-01-01T00:00:00Z")
        val LAST_UPDATE: Instant = Instant.parse("2026-02-01T00:00:00Z")
    }
}
