package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KnownBanksTest {
    @Test
    fun `default list has exactly the expected bank apps`() {
        assertEquals(EXPECTED_KNOWN_BANK_APPS.toSet(), KnownBanks.apps)
    }

    @Test
    fun `default list rejects mutation through cast`() {
        val apps = KnownBanks.apps as MutableSet<KnownBankApp>

        assertThrows(UnsupportedOperationException::class.java) {
            apps.add(KnownBankApp("Banco Exemplo", "com.example.bank"))
        }
    }
}
