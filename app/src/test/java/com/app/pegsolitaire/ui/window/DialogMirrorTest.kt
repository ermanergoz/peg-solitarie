package com.app.pegsolitaire.ui.window

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.window.DialogWindowProvider
import com.app.pegsolitaire.R
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.ui.game.AD_OFFER_DIALOG_TEST_TAG
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.home.PLAY_GAMES_PROMPT_DIALOG_TEST_TAG
import com.app.pegsolitaire.ui.home.PROFILE_SWAP_DIALOG_TEST_TAG
import com.app.pegsolitaire.ui.home.PlayGamesPromptDialog
import com.app.pegsolitaire.ui.home.ProfileSwapDialog
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.window.SystemBarsState.Hidden
import com.app.pegsolitaire.ui.window.SystemBarsState.Shown
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Criterion 30c (docs/ux-spec.md D-05g part 2, §6.2 "Dialogs mirror it"; docs/tech-design.md §3.8
 * (c)): a dialog is a window of its own, and a window that fits the system bars shows them while it
 * is open. So every dialog takes the bar state of the screen it opens over, for its own window, when
 * it opens, and gives nothing back when it closes.
 *
 * Over the board ([Hidden]): the restart confirm and the ad offer, idle and loading. On Home
 * ([Shown]): the Play Games opt-in prompt and the profile-swap dialog, which open nowhere else today
 * and mirror for the rule's uniformity. Results hosts no dialog. The state is provided as the host
 * provides it, and the seam is the recording fake; each dialog's window is read from its root view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DIALOG_MIRROR_ROBOLECTRIC_SDK])
class DialogMirrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val bars = RecordingSystemBars()

    @Test
    fun theRestartConfirmOverTheBoardHidesItsBarsAndGivesNothingBackOnClosing() {
        val state = mutableStateOf(GameUiState.initial().copy(confirmRestart = true))
        showGame(state)

        assertTheDialogAskedFor(Hidden, onNodeWithText(R.string.restart_confirm_title), "the restart confirm")

        close(state) { it.copy(confirmRestart = false) }
        assertNothingShownOnClosing("the restart confirm")
    }

    @Test
    fun theAdOfferOverTheBoardHidesItsBarsAndGivesNothingBackOnClosing() {
        val state = mutableStateOf(GameUiState.initial().copy(pendingAdOffer = AdReason.HINT))
        showGame(state)

        assertTheDialogAskedFor(Hidden, composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG), "the ad offer")

        close(state) { it.copy(pendingAdOffer = null) }
        assertNothingShownOnClosing("the ad offer")
    }

    @Test
    fun theLoadingAdOfferOverTheBoardHidesItsBars() {
        val state = mutableStateOf(GameUiState.initial().copy(pendingAdOffer = AdReason.UNDO, adOfferLoading = true))
        showGame(state)

        assertTheDialogAskedFor(Hidden, composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG), "the loading ad offer")
    }

    @Test
    fun thePlayGamesPromptOnHomeShowsItsBars() {
        show(barsState = Shown) { PlayGamesPromptDialog(onAccept = {}, onDecline = {}) }

        assertTheDialogAskedFor(
            Shown,
            composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG),
            "the opt-in prompt",
        )
    }

    @Test
    fun theProfileSwapDialogOnHomeShowsItsBars() {
        show(barsState = Shown) {
            ProfileSwapDialog(accountName = ACCOUNT_NAME, onKeepLocal = {}, onUseCloud = {})
        }

        assertTheDialogAskedFor(Shown, composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG), "the profile swap")
    }

    private fun showGame(state: MutableState<GameUiState>) {
        show(barsState = Hidden) {
            GameScreen(
                state = state.value,
                onCellTap = {},
                onRestart = {},
                onConfirmRestart = {},
                onDismissRestart = {},
                onAnimationFinished = {},
                onToggleHints = {},
                onUndo = {},
                onHint = {},
                onAcceptAdOffer = {},
                onDismissAdOffer = {},
                onHome = {},
                onNavigateBack = {},
            )
        }
    }

    private fun show(
        barsState: SystemBarsState,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalSystemBars provides bars,
                LocalSystemBarsState provides remember { mutableStateOf(barsState) },
            ) {
                AppTheme { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun close(
        state: MutableState<GameUiState>,
        change: (GameUiState) -> GameUiState,
    ) {
        bars.clear()
        composeRule.runOnUiThread { state.value = change(state.value) }
        composeRule.waitForIdle()
    }

    /** Every request named the dialog's own window, never the activity's, and asked for [expected]. */
    private fun assertTheDialogAskedFor(
        expected: SystemBarsState,
        dialogNode: SemanticsNodeInteraction,
        dialog: String,
    ) {
        val dialogWindow = windowOf(dialogNode)
        assertWithMessage("whether $dialog has a window of its own")
            .that(dialogWindow)
            .isNotSameInstanceAs(composeRule.activity.window)
        val requests = requestsFor(dialogWindow)
        assertWithMessage("the bar requests for the window of $dialog").that(requests).isNotEmpty()
        assertWithMessage("the bar requests for the window of $dialog").that(requests.toSet()).containsExactly(expected)
        assertWithMessage("bar requests naming any other window while $dialog is open")
            .that(bars.records.filter { it.window !== dialogWindow })
            .isEmpty()
    }

    /** The states asked of [window], in order. */
    private fun requestsFor(window: Window): List<SystemBarsState> =
        bars.records
            .filter { it.window === window }
            .map { it.state }

    private fun assertNothingShownOnClosing(dialog: String) {
        assertWithMessage("the bar requests made by closing $dialog")
            .that(bars.records.map { it.state })
            .doesNotContain(Shown)
    }

    /** The window a dialog's content is composed in: its root view's parent provides it. */
    private fun windowOf(node: SemanticsNodeInteraction): Window {
        val root = node.fetchSemanticsNode().root as ViewRootForTest
        val provider = root.view.parent as? DialogWindowProvider
        return checkNotNull(provider) { "the node is not in a dialog window" }.window
    }

    private fun onNodeWithText(
        @StringRes id: Int,
    ): SemanticsNodeInteraction = composeRule.onNodeWithText(composeRule.activity.getString(id))
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val DIALOG_MIRROR_ROBOLECTRIC_SDK = 34

private const val ACCOUNT_NAME = "Ada"
