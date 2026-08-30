package com.app.pegsolitaire.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose UI test (`createComposeRule`, JVM, no emulator) for the stateless
 * [AdOfferDialog] leaf (ADS-01, D-06/D-07/D-10).
 *
 * It proves the four things AdMob's rewarded-inventory policy and D-10 actually require:
 * the reward and its count are disclosed before any ad can play (formatted from
 * [AssistAllowanceLimits], never a hand-typed sentence, so the assertion cannot drift from
 * the policy); each [AdReason] selects its own copy; the decline is a real, clickable peer
 * of the accept rather than decoration; and the loading state disables both actions while
 * announcing itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class AdOfferDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private fun setDialog(
        reason: AdReason = AdReason.HINT,
        loading: Boolean = false,
        onAccept: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                AdOfferDialog(
                    reason = reason,
                    loading = loading,
                    onAccept = onAccept,
                    onDismiss = onDismiss,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun hintReasonShowsTheHintCopyFormattedWithTheFreeHintAllowance() {
        setDialog(reason = AdReason.HINT)

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_hint_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                app.getString(R.string.ad_offer_hint_body, AssistAllowanceLimits.FREE_HINTS),
            ).assertIsDisplayed()
    }

    @Test
    fun undoReasonShowsTheUndoCopyFormattedWithTheFreeUndoAllowance() {
        setDialog(reason = AdReason.UNDO)

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_undo_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                app.getString(R.string.ad_offer_undo_body, AssistAllowanceLimits.FREE_UNDOS),
            ).assertIsDisplayed()
    }

    @Test
    fun acceptInvokesOnAcceptOnceAndNeverOnDismiss() {
        var acceptCount = 0
        var dismissCount = 0
        setDialog(onAccept = { acceptCount++ }, onDismiss = { dismissCount++ })

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_watch)).performClick()

        assertThat(acceptCount).isEqualTo(1)
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun declineInvokesOnDismissOnceAndNeverOnAccept() {
        var acceptCount = 0
        var dismissCount = 0
        setDialog(onAccept = { acceptCount++ }, onDismiss = { dismissCount++ })

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_decline)).performClick()

        assertThat(dismissCount).isEqualTo(1)
        assertThat(acceptCount).isEqualTo(0)
    }

    @Test
    fun loadingDisablesBothActionsAndDropsTheAcceptLabel() {
        setDialog(loading = true)

        composeRule
            .onNodeWithContentDescription(app.getString(R.string.ad_offer_loading))
            .assertIsNotEnabled()
        composeRule.onNodeWithText(app.getString(R.string.ad_offer_watch)).assertDoesNotExist()
        composeRule.onNodeWithText(app.getString(R.string.ad_offer_decline)).assertIsNotEnabled()
    }

    @Test
    fun loadingAnnouncesItselfThroughTheSpinnerContentDescription() {
        setDialog(loading = true)

        composeRule
            .onNodeWithContentDescription(app.getString(R.string.ad_offer_loading))
            .assertIsDisplayed()
    }

    @Test
    fun theSurfaceCarriesTheTestTagInBothStates() {
        setDialog(loading = false)
        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theSurfaceCarriesTheTestTagWhileLoading() {
        setDialog(loading = true)
        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
