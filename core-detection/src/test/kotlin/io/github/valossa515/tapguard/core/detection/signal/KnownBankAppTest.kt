package io.github.valossa515.tapguard.core.detection.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KnownBankAppTest {
    @Test
    fun `filled name and package create the entry`() {
        val app = KnownBankApp(DISPLAY_NAME, PACKAGE_NAME)

        assertEquals(DISPLAY_NAME, app.displayName)
        assertEquals(PACKAGE_NAME, app.packageName)
    }

    @Test
    fun `empty display name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { KnownBankApp("", PACKAGE_NAME) }
    }

    @Test
    fun `blank display name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { KnownBankApp(BLANK, PACKAGE_NAME) }
    }

    @Test
    fun `empty package name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { KnownBankApp(DISPLAY_NAME, "") }
    }

    @Test
    fun `blank package name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { KnownBankApp(DISPLAY_NAME, BLANK) }
    }

    private companion object {
        const val DISPLAY_NAME = "Banco Exemplo"
        const val PACKAGE_NAME = "com.example.bank"
        const val BLANK = "   "
    }
}
