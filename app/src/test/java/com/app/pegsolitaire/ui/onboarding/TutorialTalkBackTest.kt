package com.app.pegsolitaire.ui.onboarding

import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
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
import com.app.pegsolitaire.testing.assertInTouchMode
import com.app.pegsolitaire.testing.descriptionChangesSent
import com.app.pegsolitaire.testing.nodeInfoHandedOut
import com.app.pegsolitaire.testing.traversalBeforeOf
import com.app.pegsolitaire.testing.turnOnATouchExplorationService
import com.app.pegsolitaire.ui.components.ANNOUNCEMENT_ARM_GAP_MS
import com.app.pegsolitaire.ui.components.ANNOUNCEMENT_REGION_TEST_TAG
import com.app.pegsolitaire.ui.components.BoardCellPositionKey
import com.app.pegsolitaire.ui.components.FOCUS_MOVE_GAP_MS
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * TB-9's JVM half (D-TB as amended by D-TBa items 3 and 6, `docs/ux-spec.md` §11.1, "The tutorial"):
 * the step line on opening and on each step change, the coach's hint line, the done line, the focus
 * moves to step 2's and step 3's board stop and to "Got it" — none on the first step, and none while
 * touch exploration is off — and a step cleared by a jump **action** advancing exactly as the taps do
 * (GD-TB-5).
 */
@RunWith(RobolectricTestRunner::class)
// A phone's state: Robolectric starts a window in touch mode only with native graphics (TouchMode.kt).
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [TUTORIAL_TALKBACK_SDK], qualifiers = TUTORIAL_TALKBACK_QUALIFIERS)
class TutorialTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved() {
        open(touchExploration = true)
        // Loop 3: the channel speaks once armed (defect ruling §1, mechanism 5).
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + FRAME_MS)

        assertChannel("Tap a peg, then tap the empty hole two cells away.")
        assertThat(boardStop().fetchSemanticsNode().config[SemanticsProperties.Focused]).isFalse()
    }

    @Test
    fun clearingAStepSpeaksTheNextStepsLineAndMovesFocusToItsBoardStop() {
        val tutorial = open(touchExploration = true)

        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        assertChannel("Step 2 of 3. Jumps work up, down, left and right — never diagonally.")
        settleFocusMove()
        boardStop().assertIsFocused()
    }

    /**
     * Loop 1, F-2: step 2 played by double-taps leaves input focus on the board stop, so step 3's
     * move must still be a change — focus lost, then regained — or TalkBack is sent nothing.
     */
    @Test
    fun aStepAfterADoubleTappedStepMovesFocusToItsBoardStopAgain() {
        val tutorial = open(touchExploration = true)
        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        settleFocusMove()
        boardStop().assertIsFocused()
        playTheOnlyMove(tutorial)

        composeRule.mainClock.autoAdvance = false
        idlePastTheStepBeat()
        val trace =
            List(TRACE_FRAMES) {
                composeRule.mainClock.advanceTimeByFrame()
                boardStop().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
            }

        assertThat(tutorial.uiState.value.stepIndex).isEqualTo(2)
        assertThat(trace).contains(false)
        assertThat(trace.last()).isTrue()
        assertThat(trace.indexOf(false)).isLessThan(trace.lastIndexOf(true))
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
        settleFocusMove()
        composeRule.onNodeWithText("Got it").assertIsFocused()
    }

    /**
     * Defect ruling §1 (B-1): T2 fills the 3 × 3 grid, so its nine cells tile the canvas, and a node
     * that later-drawn stops cover is pruned from what a service is handed. The channel must be a
     * leaf no cell covers, so step 2's line reaches TalkBack.
     */
    @Test
    fun onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine() {
        turnOnATouchExplorationService()
        val tutorial = open(touchExploration = true)
        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        val channel = checkNotNull(composeRule.nodeInfoHandedOut(channelNode())) { "the channel is pruned on T2" }
        assertThat(channel.liveRegion).isEqualTo(View.ACCESSIBILITY_LIVE_REGION_POLITE)
        assertThat(channel.childCount).isEqualTo(0)
        assertThat(channel.contentDescription?.toString()?.replace(ZERO_WIDTH_SPACE, "")).isEqualTo(STEP_TWO_LINE)
    }

    /** Defect ruling §1 (B-1): after the first step swap every line is still sent to the service. */
    @Test
    fun everyLineAfterTheFirstStepSwapIsSentToTheService() {
        turnOnATouchExplorationService()
        val tutorial = open(touchExploration = true)
        playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        letTheServiceHear()
        repeat(THREE) {
            activate(T2_EMPTY_HOLE)
            letTheServiceHear()
        }
        playTheOnlyMove(tutorial)
        letTheServiceHear()
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        while (tutorial.uiState.value.board.board.pegCount > 1) playTheOnlyMove(tutorial)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        letTheServiceHear()

        assertThat(tutorial.uiState.value.completed).isTrue()
        assertThat(descriptionChangesSent())
            .containsAtLeast(STEP_TWO_LINE, NO_PEG_LINE, T2_COACH_LINE, T2_JUMP_LINE, DONE_LINE)
            .inOrder()
    }

    /**
     * Defect ruling §1, mechanism 5: a line set in a new screen's first frames can arrive with its
     * node and never be sent, so the channel holds it until it is armed, then puts it on.
     */
    @Test
    fun theFirstStepsLineIsHeldUntilTheChannelIsArmedAndIsThenSent() {
        turnOnATouchExplorationService()
        composeRule.mainClock.autoAdvance = false
        open(touchExploration = true)
        composeRule.mainClock.advanceTimeByFrame()

        assertThat(channelText()).isNull()

        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + FRAME_MS)
        assertThat(channelText()).isEqualTo(STEP_ONE_LINE)
        letTheServiceHear()
        assertThat(descriptionChangesSent()).contains(STEP_ONE_LINE)
    }

    /**
     * Defect ruling §1, mechanism 3 ("Not a stop on opening"): while it holds a line the channel is
     * a speaking leaf at the screen's top-left corner, the place Compose reads first. It is read
     * last instead — one stop leads to it and none follows it — so a new screen's first focus does
     * not land on the step-1 line.
     */
    @Test
    fun whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder() {
        turnOnATouchExplorationService()
        open(touchExploration = true)
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + FRAME_MS)
        assertThat(channelText()).isEqualTo(STEP_ONE_LINE)
        val channel = channelNode()

        val readJustBefore = everyNode().filter { composeRule.traversalBeforeOf(it) == channel.id }

        assertThat(readJustBefore.map { it.id }).hasSize(1)
        assertThat(composeRule.traversalBeforeOf(channel)).isNull()
    }

    /**
     * Defect ruling §3 (B-3): the board stop is a leaf with something to say, so Compose puts it in
     * its reading order — after Skip, before the first cell — and TalkBack does not read it first.
     */
    @Test
    fun theBoardStopIsReadAfterSkipAndBeforeTheFirstCell() {
        turnOnATouchExplorationService()
        open(touchExploration = true)
        val stop = boardStop().fetchSemanticsNode()
        val skip = composeRule.onNodeWithText("Skip tutorial").fetchSemanticsNode()

        assertThat(composeRule.traversalBeforeOf(skip)).isEqualTo(stop.id)
        assertThat(
            composeRule.traversalBeforeOf(stop),
        ).isEqualTo(cell(Position(row = 0, col = 0)).fetchSemanticsNode().id)
    }

    /** Defect ruling §7 (B-7): in the tutorial too, a selection under touch exploration puts no line. */
    @Test
    fun underTouchExplorationASelectionPutsNoLineAndItsPutDownStillDoes() {
        open(touchExploration = true)
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + PAST_THE_HOLD_MS)

        activate(Position(row = 0, col = 0))
        assertThat(channelText()).isNull()

        activate(Position(row = 0, col = 0))
        assertChannel("Peg put down, row 1, column 1.")
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    /** The board's channel: the tutorial's one live region, found by what it is, not by its tag. */
    private fun channelNode(): SemanticsNode =
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNode()

    /** Every node of the screen but its root, unmerged: each one a stop could be. */
    private fun everyNode(): List<SemanticsNode> =
        composeRule
            .onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filterNot { it.isRoot }

    private fun channelText(): String? =
        channelNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.single()
            ?.replace(ZERO_WIDTH_SPACE, "")

    /** Lets Compose's accessibility delegate send what changed: its batches are 100 ms apart. */
    private fun letTheServiceHear() {
        composeRule.mainClock.advanceTimeBy(SERVICE_BATCHES_MS)
        composeRule.waitForIdle()
    }

    private fun open(touchExploration: Boolean): TutorialViewModel {
        val manager = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(touchExploration)
        val tutorial =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent { AppTheme { TutorialRouteUnderTest(tutorial) } }
        composeRule.waitForIdle()
        composeRule.assertInTouchMode()
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

    /** Lets a focus move finish: its gap, then a frame. */
    private fun settleFocusMove() {
        composeRule.mainClock.advanceTimeBy(FOCUS_MOVE_GAP_MS + FRAME_MS)
        composeRule.waitForIdle()
    }

    private fun assertChannel(line: String) {
        val channel = composeRule.onNodeWithTag(ANNOUNCEMENT_REGION_TEST_TAG).fetchSemanticsNode().config
        assertThat(channel.getOrNull(SemanticsProperties.ContentDescription)?.single()?.replace(ZERO_WIDTH_SPACE, ""))
            .isEqualTo(line)
    }

    private companion object {
        /** T2's top-left corner: an empty hole while nothing is held, a non-progressing tap. */
        val T2_EMPTY_HOLE = Position(row = 0, col = 0)

        const val THREE = 3
        const val STEPS = 3
        const val NUDGE_SLACK_MS = 50L
        const val FRAME_MS = 16L

        /** Frames enough for a step's install, its focus move's clear, gap and request. */
        const val TRACE_FRAMES = 30
        const val BOARD_STOP_PREFIX = "Board, "

        /** Two of the accessibility delegate's 100 ms batches and a frame. */
        const val SERVICE_BATCHES_MS = 216L

        /** Past the step line's one-second hold, so the channel is empty before the selection. */
        const val PAST_THE_HOLD_MS = 1_200L

        /**
         * The first step's caption alone (brief A2.4; the device-2 ruling §3): the pips' own first
         * focus already says "Step 1 of 3", so the opening line does not say it again.
         */
        const val STEP_ONE_LINE = "Tap a peg, then tap the empty hole two cells away."
        const val STEP_TWO_LINE = "Step 2 of 3. Jumps work up, down, left and right — never diagonally."
        const val NO_PEG_LINE = "No peg here."
        const val T2_COACH_LINE = "Hint: row 1, column 2, jump down to row 3, column 2."
        const val T2_JUMP_LINE = "Row 1, column 2 to row 3, column 2. Removed row 2, column 2. 1 peg left."
        const val DONE_LINE = "That's the whole game. Go clear a board."
        const val ZERO_WIDTH_SPACE = "​"
    }
}

private const val TUTORIAL_TALKBACK_SDK = 34
private const val TUTORIAL_TALKBACK_QUALIFIERS = "w400dp-h800dp-hdpi"
