package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KnownCloneAppsTest {
    @Test
    fun `default list has exactly the expected clone apps`() {
        assertEquals(EXPECTED_KNOWN_CLONE_APPS.toSet(), KnownCloneApps.packageNames)
    }

    @Test
    fun `default list rejects mutation through cast and preserves contents`() {
        val packageNames = KnownCloneApps.packageNames as MutableSet<String>

        assertThrows(UnsupportedOperationException::class.java) {
            packageNames.add("com.example.clone")
        }
        assertEquals(EXPECTED_KNOWN_CLONE_APPS.toSet(), KnownCloneApps.packageNames)
    }
}
