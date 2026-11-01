package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose UI test (`createComposeRule`, JVM, no emulator) for the stateless
 * [NoWinBanner] leaf (ASST-03, D-06/D-08/D-09). It proves the hoisted [message] is rendered
 * and that each of the three actions (Undo · Restart · Home) invokes its handler exactly
 * once. The banner is a pure render of hoisted state/handlers, so the assertions are
 * deterministic (located by text) rather than pixel-based.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NoWinBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun rendersTheMessageAndInvokesEachActionExactlyOnce() {
        var undoCount = 0
        var restartCount = 0
        var homeCount = 0
        val message = app.getString(R.string.nowin_message)

        composeRule.setContent {
            AppTheme {
                NoWinBanner(
                    message = message,
                    onUndo = { undoCount++ },
                    onRestart = { restartCount++ },
                    onHome = { homeCount++ },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(message).assertIsDisplayed()

        composeRule.onNodeWithText(app.getString(R.string.undo_action)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.rescue_restart)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.action_home)).performClick()

        assertThat(undoCount).isEqualTo(1)
        assertThat(restartCount).isEqualTo(1)
        assertThat(homeCount).isEqualTo(1)
    }

    /**
     * Criterion 23k, its Compose half (D-04f): the banner's message is a polite live region, so
     * TalkBack speaks a proven dead end — and a stuck board — as it speaks the Undecided notice;
     * and the three actions are outside that region, neither carrying one nor inside one, so
     * they are read when they are focused and never announced with the message.
     */
    @Test
    fun theBannersMessageIsAPoliteLiveRegionAndItsActionsAreOutsideIt() {
        val messages = listOf(app.getString(R.string.nowin_message), app.getString(R.string.stuck_message))
        val shown = mutableStateOf(messages.first())
        composeRule.setContent {
            AppTheme { NoWinBanner(message = shown.value, onUndo = {}, onRestart = {}, onHome = {}) }
        }

        messages.forEach { message ->
            composeRule.runOnUiThread { shown.value = message }
            composeRule.waitForIdle()

            val messageNode = composeRule.onNodeWithText(message, useUnmergedTree = true).fetchSemanticsNode()
            assertWithMessage("the live region on \"%s\"", message)
                .that(messageNode.config.getOrNull(SemanticsProperties.LiveRegion))
                .isEqualTo(LiveRegionMode.Polite)
            RESCUE_ACTIONS.map(app::getString).forEach { action ->
                val actionNode = composeRule.onNodeWithText(action, useUnmergedTree = true).fetchSemanticsNode()
                assertWithMessage("the live regions on and above the %s action", action)
                    .that(actionNode.liveRegionsOnAndAbove())
                    .isEmpty()
            }
        }
    }

    // ---- D-SB-3 / D-SB-4: the Challenge proven-dead banner can be put away, and only it (30j) ----

    /**
     * GDD GD-SB-19, ux-spec D-SB-4: of the rescue banners only the Challenge proven-dead one carries
     * a Dismiss action — the stuck banner in both modes (nothing to play beneath it) and Classic's
     * proven-dead banner (unchanged, R8) carry none, and a stuck board's banner stays undismissable
     * even when a Hint has also proved it dead.
     */
    @Test
    fun onlyTheChallengeProvenDeadBannerCarriesTheDismissAction() {
        val cases =
            listOf(
                BannerCase(isChallenge = true, stuck = false, noWinningMove = true, dismissable = true),
                BannerCase(isChallenge = true, stuck = true, noWinningMove = false, dismissable = false),
                BannerCase(isChallenge = true, stuck = true, noWinningMove = true, dismissable = false),
                BannerCase(isChallenge = false, stuck = false, noWinningMove = true, dismissable = false),
                BannerCase(isChallenge = false, stuck = true, noWinningMove = false, dismissable = false),
            )
        val shown = mutableStateOf(cases.first())
        composeRule.setContent {
            AppTheme {
                val case = shown.value
                RescueBanner(
                    state =
                        GameUiState
                            .initial(fivePegBoard())
                            .copy(stuck = case.stuck, noWinningMove = case.noWinningMove),
                    isChallenge = case.isChallenge,
                    onUndo = {},
                    onRestart = {},
                    onHome = {},
                    onDismissProvenDead = {},
                )
            }
        }

        cases.forEach { case ->
            composeRule.runOnUiThread { shown.value = case }
            composeRule.waitForIdle()

            val dismissActions =
                composeRule
                    .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss), useUnmergedTree = true)
                    .fetchSemanticsNodes()
            assertWithMessage("a Dismiss action on %s", case)
                .that(dismissActions.isNotEmpty())
                .isEqualTo(case.dismissable)
        }
    }

    @Test
    fun aDismissableBannerGoesOnATapOutsideItsButtonsOrOnTalkBacksDismissAndNeverOnAButton() {
        var dismissals = 0
        var undos = 0
        val message = app.getString(R.string.nowin_message)
        composeRule.setContent {
            AppTheme {
                NoWinBanner(
                    message = message,
                    onUndo = { undos++ },
                    onRestart = {},
                    onHome = {},
                    onDismiss = { dismissals++ },
                )
            }
        }
        composeRule.waitForIdle()

        // TalkBack's way: the message node offers Dismiss, and no click and no role (D-04g's idiom).
        val messageNode = composeRule.onNodeWithText(message, useUnmergedTree = true)
        val config = messageNode.fetchSemanticsNode().config
        assertThat(config.contains(SemanticsActions.Dismiss)).isTrue()
        assertThat(config.contains(SemanticsActions.OnClick)).isFalse()
        assertThat(config.getOrNull(SemanticsProperties.Role)).isNull()
        messageNode.performSemanticsAction(SemanticsActions.Dismiss)
        assertThat(dismissals).isEqualTo(1)
        // A tap on the message, then one in the banner's top padding (above the text, inside the
        // rounded shape, which also bounds its hit area): both outside the three buttons.
        messageNode.performClick()
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).performTouchInput { click(Offset(x = centerX, y = 2f)) }
        assertThat(dismissals).isEqualTo(3)
        // A button keeps its own tap.
        composeRule.onNodeWithText(app.getString(R.string.undo_action)).performClick()
        assertThat(undos).isEqualTo(1)
        assertThat(dismissals).isEqualTo(3)
    }

    @Test
    fun aBannerWithoutADismissalCallbackHasNoDismissActionAndATapOnItsMessageChangesNothing() {
        val message = app.getString(R.string.stuck_message)
        var undos = 0
        composeRule.setContent {
            AppTheme { NoWinBanner(message = message, onUndo = { undos++ }, onRestart = {}, onHome = {}) }
        }
        composeRule.waitForIdle()

        val messageNode = composeRule.onNodeWithText(message, useUnmergedTree = true)
        assertThat(messageNode.fetchSemanticsNode().config.contains(SemanticsActions.Dismiss)).isFalse()
        messageNode.performClick()
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()
        assertThat(undos).isEqualTo(0)
    }

    /** Five isolated pegs on the English-33 footprint: a terminal board past the Challenge threshold. */
    private fun fivePegBoard(): Board =
        listOf(Position(2, 0), Position(4, 0), Position(2, 6), Position(4, 6), Position(6, 2))
            .fold(Board(spec = Boards.English33, pegs = 0L)) { board, position -> board.withPeg(position) }

    /** One rescue-banner situation: the mode, the two flags, and whether it may be put away. */
    private data class BannerCase(
        val isChallenge: Boolean,
        val stuck: Boolean,
        val noWinningMove: Boolean,
        val dismissable: Boolean,
    )
}

/** Every live region on this node and on each of its ancestors, the node's own first. */
private fun SemanticsNode.liveRegionsOnAndAbove(): List<LiveRegionMode> =
    generateSequence(this) { it.parent }
        .mapNotNull { it.config.getOrNull(SemanticsProperties.LiveRegion) }
        .toList()

/** The banner's three actions, in their on-screen order (D-09). */
private val RESCUE_ACTIONS = listOf(R.string.undo_action, R.string.rescue_restart, R.string.action_home)

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
