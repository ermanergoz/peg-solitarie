package com.app.pegsolitaire.ui.home

import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Robolectric + Compose UI test (`createComposeRule`, JVM, no emulator) for the stateless
 * [PlayGamesPromptDialog] leaf (PGS-01, D-15/D-16).
 *
 * It proves the three things D-16 actually requires of the prompt:
 *  - the offer is fully disclosed before any answer — the body names leaderboards,
 *    achievements and cloud save, and says the game plays the same either way, asserted
 *    against `strings.xml` rather than a hand-typed sentence so the copy cannot drift;
 *  - each action forwards exactly once and never the other one, so a mis-wired callback
 *    cannot silently opt a player in;
 *  - **dismissal counts as "Not now", never as accept.** This is the assertion with real
 *    teeth. Wiring `onDismissRequest` to the wrong lambda — or to nothing — is invisible
 *    on screen, but it would either opt a player in who walked away, or leave the answer
 *    unwritten so the prompt returns on the next launch (the D-17/T-11-08 nag).
 *
 * The dismissal is driven through the real dialog window rather than a fake: Compose hosts
 * the content in a [ComponentDialog], so dispatching back on that dialog's own
 * `onBackPressedDispatcher` is exactly the path a player's back press takes. The host
 * Activity's dispatcher would not do — a dialog window handles its own back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PROMPT_ROBOLECTRIC_SDK])
class PlayGamesPromptDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private fun setDialog(
        onAccept: () -> Unit = {},
        onDecline: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                PlayGamesPromptDialog(onAccept = onAccept, onDecline = onDecline)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun thePromptDisclosesTheOfferAndBothAnswersBeforeAnythingIsDecided() {
        setDialog()

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_title)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_body)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_accept)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_decline)).assertIsDisplayed()
    }

    @Test
    fun theBodyNamesAllThreeBenefitsAndSaysTheGameNeedsNoneOfThem() {
        // D-16 is a copy contract, not a layout one: the same three benefits as the
        // Settings row, plus the plain statement that signing in is optional. Asserted on
        // the resource itself so a future rewrite that quietly drops "either way" fails.
        val body = app.getString(R.string.play_games_prompt_body)

        assertThat(body).contains("leaderboards")
        assertThat(body).contains("achievements")
        assertThat(body).contains("cloud")
        assertThat(body).contains("either way")
    }

    @Test
    fun signingInInvokesOnAcceptOnceAndNeverOnDecline() {
        var acceptCount = 0
        var declineCount = 0
        setDialog(onAccept = { acceptCount++ }, onDecline = { declineCount++ })

        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_accept)).performClick()
        composeRule.waitForIdle()

        assertThat(acceptCount).isEqualTo(1)
        assertThat(declineCount).isEqualTo(0)
    }

    @Test
    fun notNowInvokesOnDeclineOnceAndNeverOnAccept() {
        var acceptCount = 0
        var declineCount = 0
        setDialog(onAccept = { acceptCount++ }, onDecline = { declineCount++ })

        composeRule.onNodeWithText(app.getString(R.string.play_games_prompt_decline)).performClick()
        composeRule.waitForIdle()

        assertThat(declineCount).isEqualTo(1)
        assertThat(acceptCount).isEqualTo(0)
    }

    @Test
    fun dismissingTheDialogCountsAsNotNowAndNeverAsAccept() {
        // D-16: back / tapping outside is an ANSWER — the negative one. Both reach the
        // composable as onDismissRequest, which must be onDecline.
        var acceptCount = 0
        var declineCount = 0
        setDialog(onAccept = { acceptCount++ }, onDecline = { declineCount++ })

        dispatchBackOnTheDialogWindow()

        assertThat(declineCount).isEqualTo(1)
        assertThat(acceptCount).isEqualTo(0)
    }

    /**
     * Presses back on the dialog's own window, the way the platform does. Compose hosts
     * dialog content in a [ComponentDialog] whose `onBackPressedDispatcher` owns the back
     * gesture while it is showing.
     */
    private fun dispatchBackOnTheDialogWindow() {
        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog).isInstanceOf(ComponentDialog::class.java)
        composeRule.runOnUiThread {
            (dialog as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val PROMPT_ROBOLECTRIC_SDK = 34
