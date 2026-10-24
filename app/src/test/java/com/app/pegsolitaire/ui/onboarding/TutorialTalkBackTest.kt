package com.app.pegsolitaire.ui.onboarding

import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.components.BoardCellPositionKey
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * TB-9's JVM half (D-TB as amended by D-TBa items 3 and 6, `docs/ux-spec.md` §11.1, "The tutorial"):
 * the step line on opening and on each step change, the coach's hint line, the done line, the focus
 * moves to step 2's and step 3's board stop and to "Got it" — none on the first step, and none while
 * touch exploration is off — and a step cleared by a jump **action** advancing exactly as the taps do
 * (GD-TB-5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_TALKBACK_SDK], qualifiers = TUTORIAL_TALKBACK_QUALIFIERS)
class TutorialTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved() {
        open(touchExploration = true)

        assertChannel("Step 1 of 3. Tap a peg, then tap the empty hole two cells away.")
        assertThat(boardStop().fetchSemanticsNode().config[SemanticsProperties.Focused]).isFalse()
    }

    @Test
    fun clearingAStepSpeaksTheNextStepsLineAndMovesFocusToItsBoardStop() {
        val tutorial = open(touchExploration = true)

        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        assertChannel("Step 2 of 3. Jumps work up, down, left and right — never diagonally.")
        boardStop().assertIsFocused()
    }

    @Test
    fun withTouchExplorationOffAStepChangeMovesNoFocus() {
        val tutorial = open(touchExploration = false)

        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        assertChannel("Step 2 of 3. Jumps work up, down, left and right — never diagonally.")
        assertThat(boardStop().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused)).isNull()
    }

    @Test
    fun aJumpActionClearsTheFirstStepAndAdvancesItAsTheTwoTapsDo() {
        val tutorial = open(touchExploration = true)
        val move = RulesEngine.legalMoves(tutorial.uiState.value.board.board).single()

        val action =
            cell(move.from).fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        composeRule.runOnIdle { action.action() }
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        assertThat(tutorial.uiState.value.stepIndex).isEqualTo(1)
        assertChannel("Step 2 of 3. Jumps work up, down, left and right — never diagonally.")
    }

    @Test
    fun threeNonProgressingTapsBringTheCoachWhoseHintIsSpoken() {
        val tutorial = open(touchExploration = false)
        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        repeat(THREE) { activate(T2_EMPTY_HOLE) }

        assertThat(tutorial.uiState.value.coachTick).isEqualTo(1)
        assertChannel("Hint: row 1, column 2, jump down to row 3, column 2.")
    }

    @Test
    fun anIdleNudgeBringsTheCoachWhoseHintIsSpoken() {
        open(touchExploration = false)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TutorialAnim.NUDGE_DELAY_MS + NUDGE_SLACK_MS))
        composeRule.waitForIdle()

        assertChannel("Hint: row 1, column 1, jump right to row 1, column 3.")
    }

    @Test
    fun completingTheTutorialSpeaksTheDoneLineAndMovesFocusToGotIt() {
        val tutorial = open(touchExploration = true)
        repeat(STEPS) {
            while (tutorial.uiState.value.board.board.pegCount > 1) playTheOnlyMove(tutorial)
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }

        assertThat(tutorial.uiState.value.completed).isTrue()
        assertChannel("That's the whole game. Go clear a board.")
        composeRule.onNodeWithText("Got it").assertIsFocused()
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private fun open(touchExploration: Boolean): TutorialViewModel {
        val manager = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(touchExploration)
        val tutorial =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent { AppTheme { TutorialRouteUnderTest(tutorial) } }
        composeRule.waitForIdle()
        return tutorial
    }

    /** Activates the first legal move's two cells, as TalkBack's double-taps would. */
    private fun playTheOnlyMove(tutorial: TutorialViewModel) {
        val move = RulesEngine.legalMoves(tutorial.uiState.value.board.board).first()
        activate(move.from)
        activate(move.to)
    }

    private fun activate(position: Position) {
        cell(position).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    private fun cell(position: Position): SemanticsNodeInteraction =
        composeRule.onNode(SemanticsMatcher.expectValue(BoardCellPositionKey, position))

    private fun boardStop(): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(BOARD_STOP_PREFIX, substring = true)

    private fun assertChannel(line: String) {
        val canvas = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().config
        assertThat(canvas.getOrNull(SemanticsProperties.ContentDescription)?.single()?.replace(ZERO_WIDTH_SPACE, ""))
            .isEqualTo(line)
    }

    private companion object {
        /** T2's top-left corner: an empty hole while nothing is held, a non-progressing tap. */
        val T2_EMPTY_HOLE = Position(row = 0, col = 0)

        const val THREE = 3
        const val STEPS = 3
        const val NUDGE_SLACK_MS = 50L
        const val BOARD_STOP_PREFIX = "Board, "
        const val ZERO_WIDTH_SPACE = "​"
    }
}

private const val TUTORIAL_TALKBACK_SDK = 34
private const val TUTORIAL_TALKBACK_QUALIFIERS = "w400dp-h800dp-hdpi"
