package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KnownWalletsTest {
    @Test
    fun `default list has exactly the expected wallets`() {
        assertEquals(EXPECTED_KNOWN_WALLETS.toSet(), KnownWallets.packageNames)
    }

    @Test
    fun `default list rejects mutation through cast`() {
        val packageNames = KnownWallets.packageNames as MutableSet<String>

        assertThrows(UnsupportedOperationException::class.java) {
            packageNames.add("com.example.wallet")
        }
    }
}
