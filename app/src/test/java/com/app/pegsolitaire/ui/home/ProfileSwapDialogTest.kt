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
 * [ProfileSwapDialog] leaf (CLOUD-02, D-12 to D-15).
 *
 * This dialog guards the only destructive path in the app, so the assertions here are not
 * about layout:
 *  - **Dismissal is "Keep this device's progress", never the wipe (T-13-42).** Wiring
 *    `onDismissRequest` to the wrong lambda — or to nothing — is invisible on screen, and
 *    would either erase a player's progress because they pressed back, or leave the
 *    question standing with no push and no pull able to run (D-15). This is the assertion
 *    with real teeth, and it is driven through the dialog's own window rather than by
 *    calling the lambda, so it is the *wiring* under test.
 *  - **Each action forwards exactly once and never the other one**, so a swapped pair
 *    cannot silently replace a device's progress with a stranger's.
 *  - **The body states what is destroyed (D-14, T-13-44)**, asserted against `strings.xml`
 *    rather than hand-typed copy so the four wiped categories cannot drift out of it.
 *  - **An unknown display name degrades to neutral phrasing, never to "null"** — Phase 11
 *    D-14 makes a failed name lookup a degraded name, not a failed sign-in.
 *
 * The dismissal is driven through the real dialog window rather than a fake: Compose hosts
 * the content in a [ComponentDialog], so dispatching back on that dialog's own
 * `onBackPressedDispatcher` is exactly the path a player's back press takes. The host
 * Activity's dispatcher would not do — a dialog window handles its own back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PROFILE_SWAP_ROBOLECTRIC_SDK])
class ProfileSwapDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private fun setDialog(
        accountName: String? = ARRIVING_ACCOUNT_NAME,
        onKeepLocal: () -> Unit = {},
        onUseCloud: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                ProfileSwapDialog(
                    accountName = accountName,
                    onKeepLocal = onKeepLocal,
                    onUseCloud = onUseCloud,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun bothAnswersAndTheirConsequencesAreOnScreenBeforeAnythingIsDecided() {
        setDialog()

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.profile_swap_title)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.profile_swap_keep)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.profile_swap_use_cloud)).assertIsDisplayed()
    }

    @Test
    fun theBodyNamesTheArrivingAccountWhenItIsKnown() {
        setDialog(accountName = ARRIVING_ACCOUNT_NAME)

        val body = app.getString(R.string.profile_swap_body, ARRIVING_ACCOUNT_NAME)
        assertThat(body).contains(ARRIVING_ACCOUNT_NAME)
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @Test
    fun theBodyEnumeratesEverythingTheReplacePathDestroys() {
        // D-14 / T-13-44 is a copy contract, not a layout one: the body must name all four
        // categories the wipe deletes. Asserted on the resources themselves — and on BOTH
        // bodies — so a rewrite that quietly drops the in-progress games fails here.
        val named = app.getString(R.string.profile_swap_body, ARRIVING_ACCOUNT_NAME)
        val fallback = app.getString(R.string.profile_swap_body_unknown_account)

        listOf(named, fallback).forEach { body ->
            assertThat(body).contains("level results")
            assertThat(body).contains("play history")
            assertThat(body).contains("Challenge level")
            assertThat(body).contains("in-progress games")
        }
    }

    @Test
    fun anUnknownAccountNameFallsBackToNeutralCopyRatherThanTheWordNull() {
        // Phase 11 D-14: a failed display-name lookup is a degraded NAME, not a failed
        // sign-in. Formatting the nullable straight into the named body would put the
        // literal "null" in front of the player, right above the only wipe in the app.
        setDialog(accountName = null)

        val fallback = app.getString(R.string.profile_swap_body_unknown_account)
        assertThat(fallback).doesNotContain("null")
        composeRule.onNodeWithText(fallback).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.profile_swap_body, "null")).assertDoesNotExist()
    }

    @Test
    fun keepingThisDevicesProgressInvokesOnKeepLocalOnceAndNeverOnUseCloud() {
        var keepCount = 0
        var useCloudCount = 0
        setDialog(onKeepLocal = { keepCount++ }, onUseCloud = { useCloudCount++ })

        composeRule.onNodeWithText(app.getString(R.string.profile_swap_keep)).performClick()
        composeRule.waitForIdle()

        assertThat(keepCount).isEqualTo(1)
        assertThat(useCloudCount).isEqualTo(0)
    }

    @Test
    fun usingTheCloudProgressInvokesOnUseCloudOnceAndNeverOnKeepLocal() {
        var keepCount = 0
        var useCloudCount = 0
        setDialog(onKeepLocal = { keepCount++ }, onUseCloud = { useCloudCount++ })

        composeRule.onNodeWithText(app.getString(R.string.profile_swap_use_cloud)).performClick()
        composeRule.waitForIdle()

        assertThat(useCloudCount).isEqualTo(1)
        assertThat(keepCount).isEqualTo(0)
    }

    @Test
    fun dismissingTheDialogKeepsThisDevicesProgressAndNeverWipesIt() {
        // D-13 / T-13-42: back and tapping outside are an ANSWER — the safe one. Both
        // reach the composable as onDismissRequest, which must be onKeepLocal.
        var keepCount = 0
        var useCloudCount = 0
        setDialog(onKeepLocal = { keepCount++ }, onUseCloud = { useCloudCount++ })

        dispatchBackOnTheDialogWindow()

        assertThat(keepCount).isEqualTo(1)
        assertThat(useCloudCount).isEqualTo(0)
    }

    @Test
    fun dismissingWithAnUnknownAccountNameIsStillTheSafeAnswer() {
        // The degraded-name path is the one most likely to be rebuilt by hand later; the
        // safe default must not be a property of only the happy branch.
        var keepCount = 0
        var useCloudCount = 0
        setDialog(accountName = null, onKeepLocal = { keepCount++ }, onUseCloud = { useCloudCount++ })

        dispatchBackOnTheDialogWindow()

        assertThat(keepCount).isEqualTo(1)
        assertThat(useCloudCount).isEqualTo(0)
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
private const val PROFILE_SWAP_ROBOLECTRIC_SDK = 34

/** The arriving account's display name — never this device's, per [ProfileSwapDialog]. */
private const val ARRIVING_ACCOUNT_NAME = "PegChampion"
