package io.github.valossa515.tapguard.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TapGuardThemeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `light color scheme in light mode`() {
        val scheme = schemeShown()

        assertEquals(LightBackground, scheme.background)
        assertEquals(LightPrimary, scheme.primary)
    }

    @Test
    @Config(qualifiers = "+night")
    fun `dark color scheme in night mode`() {
        val scheme = schemeShown()

        assertEquals(DarkBackground, scheme.background)
        assertEquals(DarkPrimary, scheme.primary)
    }

    private fun schemeShown(): ColorScheme {
        var scheme: ColorScheme? = null
        Robolectric
            .buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .setContent { TapGuardTheme { scheme = MaterialTheme.colorScheme } }
        compose.waitForIdle()
        return checkNotNull(scheme)
    }
}
