package io.github.valossa515.tapguard.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.valossa515.tapguard.MainActivity
import io.github.valossa515.tapguard.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TapGuardNavHostTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app starts on onboarding`() {
        assertShown(R.string.onboarding_title)
    }

    @Test
    fun `onboarding goes to home`() {
        click(R.string.onboarding_continue)

        assertShown(R.string.home_title)
    }

    @Test
    fun `home goes to scan result`() {
        goToHome()

        click(R.string.home_scan_now)

        assertShown(R.string.scan_result_title)
    }

    @Test
    fun `home goes to card test intro`() {
        goToHome()

        click(R.string.home_test_card)

        assertShown(R.string.card_test_intro_title)
    }

    @Test
    fun `home goes to emergency`() {
        goToHome()

        click(R.string.home_emergency)

        assertShown(R.string.emergency_title)
    }

    @Test
    fun `home goes to settings`() {
        goToHome()

        click(R.string.home_settings)

        assertShown(R.string.settings_title)
        compose.onNodeWithText(text(R.string.settings_alert_history)).assertIsDisplayed()
    }

    @Test
    fun `scan result goes to app detail with provisional package name`() {
        goToHome()
        click(R.string.home_scan_now)

        click(R.string.scan_result_provisional_app)

        assertShown(R.string.app_detail_title)
        compose.onNodeWithText(PROVISIONAL_PACKAGE_TEXT).assertIsDisplayed()
    }

    @Test
    fun `card test intro goes to card test reading`() {
        goToHome()
        click(R.string.home_test_card)

        click(R.string.card_test_intro_continue)

        assertShown(R.string.card_test_reading_title)
    }

    @Test
    fun `settings goes to alert history then app detail with provisional package name`() {
        goToHome()
        click(R.string.home_settings)

        click(R.string.settings_alert_history)
        assertShown(R.string.alert_history_title)
        click(R.string.alert_history_provisional_alert)

        assertShown(R.string.app_detail_title)
        compose.onNodeWithText(PROVISIONAL_PACKAGE_TEXT).assertIsDisplayed()
    }

    @Test
    fun `back on home after onboarding closes the app`() {
        goToHome()

        pressBack()

        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun `card test reading returns to existing home and back closes the app`() {
        goToHome()
        click(R.string.home_test_card)
        click(R.string.card_test_intro_continue)

        click(R.string.card_test_reading_done)
        assertShown(R.string.home_title)
        pressBack()

        assertTrue(compose.activity.isFinishing)
    }

    private fun goToHome() {
        click(R.string.onboarding_continue)
        assertShown(R.string.home_title)
    }

    private fun click(
        @StringRes label: Int,
    ) {
        compose.onNodeWithText(text(label)).performClick()
        compose.waitForIdle()
    }

    private fun assertShown(
        @StringRes title: Int,
    ) {
        compose.onNodeWithText(text(title)).assertIsDisplayed()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun text(
        @StringRes id: Int,
    ): String = compose.activity.getString(id)

    private companion object {
        const val PROVISIONAL_PACKAGE_TEXT = "Pacote: io.github.valossa515.tapguard.decoy"
    }
}
