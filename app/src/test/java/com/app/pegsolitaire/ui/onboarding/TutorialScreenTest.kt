package com.app.pegsolitaire.ui.onboarding

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The one Compose UI test per screen's main flow (rules/testing.md), for the tutorial.
 *
 * It finds the screen by [TUTORIAL_SCREEN_TEST_TAG], its header and body by
 * [TUTORIAL_HEADER_TEST_TAG] and [TUTORIAL_BODY_TEST_TAG], and the board by
 * [BOARD_CANVAS_TEST_TAG] — **never by button copy**, which is the defect the phase's two
 * retargeted tests exist to remove. A screen located by the words on its buttons breaks
 * whenever the copy changes and says nothing about the behaviour.
 *
 * Three of its tests supply the window insets Robolectric leaves empty, which is the blind
 * spot that let a visible-but-untappable "Skip" reach a device (see [applySystemBarInsets]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_SCREEN_ROBOLECTRIC_SDK], qualifiers = TALL_PHONE_QUALIFIER)
class TutorialScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theTutorialAdvancesOnlyWhenThePlayerCompletesARealJumpAndNeverByAButton() {
        val viewModel = startTutorial()
        composeRule.onNodeWithTag(TUTORIAL_SCREEN_TEST_TAG).assertIsDisplayed()

        // Criterion 1: there is no Next/Continue affordance at ANY step. The only way
        // forward is a real jump on the real rules engine.
        assertNoAdvanceButton()

        // Step 1 → 2 by completing the jump, and by nothing else.
        assertThat(viewModel.uiState.value.stepIndex).isEqualTo(FIRST_STEP)
        playTheOnlyMove(viewModel)
        assertNoAdvanceButton()
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.stepIndex).isEqualTo(SECOND_STEP)

        // Step 2 → 3, same way.
        playTheOnlyMove(viewModel)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.stepIndex).isEqualTo(THIRD_STEP)
        assertNoAdvanceButton()
    }

    @Test
    fun skipIsReachableOnEveryStep() {
        val viewModel = startTutorial()
        val skip = label(R.string.onboarding_skip)

        // Criterion 5: on every step, not only the first. A tutorial the player cannot leave
        // is worse than no tutorial (D-01d).
        repeat(TutorialUiState.STEP_COUNT) { step ->
            composeRule.onNodeWithText(skip).performScrollTo().assertIsDisplayed()
            if (step < TutorialUiState.STEP_COUNT - 1) {
                while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                    playTheOnlyMove(viewModel)
                }
                idlePastTheStepBeat()
                composeRule.waitForIdle()
            }
        }
    }

    /**
     * Criterion 5 again, and the half [skipIsReachableOnEveryStep] cannot see: a control that
     * is *displayed* is not necessarily a control that can be *pressed*.
     *
     * A Compose test composes with **zero** window insets, so every node lands where the
     * layout puts it and a screen drawing under the status bar looks identical to one that
     * respects it. That blind spot is exactly how a tutorial whose visible "Skip" was a no-op
     * — 99 px of a 126 px target inside a 142 px system-bar inset, the label's own centre
     * dead — reached a device with a green suite. So this test **supplies** the insets
     * Robolectric leaves empty ([applySystemBarInsets]) and then asserts the geometry: the
     * whole of Skip's touch target, not merely its label, clears the top inset on every step.
     *
     * It fails on a screen with no inset modifier, which is the property worth owning.
     */
    @Test
    fun skipsWholeTouchTargetClearsTheSystemBarsOnEveryStep() {
        val viewModel = startTutorial()
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)
        val skip = label(R.string.onboarding_skip)

        repeat(TutorialUiState.STEP_COUNT) { step ->
            // The pips share the header row, and they were drawn over the system clock too.
            assertThat(nodeTop(hasContentDescription(stepDescription(step + 1)))).isAtLeast(statusBarInset)
            assertThat(nodeTop(hasText(skip))).isAtLeast(statusBarInset)
            if (step < TutorialUiState.STEP_COUNT - 1) {
                while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                    playTheOnlyMove(viewModel)
                }
                idlePastTheStepBeat()
                composeRule.waitForIdle()
            }
        }
    }

    /**
     * The founder's ask (2026-11-06, amended the same day): the header — the step pips and Skip —
     * stays at the top, and the rest of the tutorial sits in the middle of the height below it.
     *
     * On a tall phone the bands fit with room to spare. The header keeps its place, the column's
     * own padding below the status bar, and the body — board, caption and footer — splits the rest
     * evenly: the gap from the header down to the body equals the gap from the body down to the
     * navigation bar, on every step and again once the star and the CTA show in the footer.
     *
     * The insets are supplied, and they are deliberately unequal, so a body centred in the window
     * rather than between the header and the navigation bar is told apart. The room to spare is
     * asserted rather than assumed: on a window the bands filled, both gaps would be fixed spacing
     * whatever the arrangement, and their equality would prove nothing.
     */
    @Test
    fun theHeaderStaysAtTheTopAndTheBodyIsCentredBelowItOnEveryStep() {
        val viewModel = startTutorial()
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)

        repeat(TutorialUiState.STEP_COUNT) {
            assertTheHeaderIsPinnedAndTheBodyCentredBelowIt()
            while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                playTheOnlyMove(viewModel)
            }
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }

        // Completed: the footer now shows the star and "Got it", and the body is still centred.
        assertThat(viewModel.uiState.value.completed).isTrue()
        assertTheHeaderIsPinnedAndTheBodyCentredBelowIt()
    }

    /**
     * Loop 1 (the founder on the emulator, 2026-11-06): the board must not move when the star and
     * "Got it" appear.
     *
     * The body is centred below the header, so a footer that arrived with room of its own would make
     * the body taller and lift the board by half of what it added. The footer keeps its full height
     * from the first step instead, and the board's bounds — the canvas fills the slab, so it moves
     * only when the slab does — are the same at the start of every step and after completion. On the
     * tall window, where the body has slack to be centred in.
     */
    @Test
    fun theBoardHoldsItsPlaceWhenTheStarAndGotItAppear() {
        val viewModel = startTutorial()
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)
        val beforeCompletion = mutableListOf<DpRect>()

        repeat(TutorialUiState.STEP_COUNT) {
            beforeCompletion += composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).getBoundsInRoot()
            while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                playTheOnlyMove(viewModel)
            }
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }

        assertThat(viewModel.uiState.value.completed).isTrue()
        val afterCompletion = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).getBoundsInRoot()
        beforeCompletion.forEach { before ->
            assertThat(before.left.value).isWithin(SAME_PLACE_TOLERANCE_DP).of(afterCompletion.left.value)
            assertThat(before.top.value).isWithin(SAME_PLACE_TOLERANCE_DP).of(afterCompletion.top.value)
            assertThat(before.right.value).isWithin(SAME_PLACE_TOLERANCE_DP).of(afterCompletion.right.value)
            assertThat(before.bottom.value).isWithin(SAME_PLACE_TOLERANCE_DP).of(afterCompletion.bottom.value)
        }
    }

    /**
     * Loop 1's other half: the footer's reserved room is room, not a control. Before completion no
     * node of the merged semantics tree carries "Got it", so TalkBack has nothing to land on; and a
     * tap where it waits presses nothing. After completion both probes reach it, which is what proves
     * they were aimed at the button.
     *
     * The merged tree is the oracle because, like the nodes a service is handed, it leaves out a
     * subtree under `clearAndSetSemantics`; the unmerged tree keeps such nodes as replaced semantics.
     */
    @Test
    fun theWaitingGotItIsNeitherAnnouncedNorPressableBeforeCompletion() {
        var completed by mutableStateOf(false)
        var presses = 0
        composeRule.setContent {
            AppTheme {
                TutorialScreen(
                    state = if (completed) TutorialUiState.initial().completed() else TutorialUiState.initial(),
                    onCellTap = {},
                    onAnimationFinished = {},
                    onSkip = {},
                    onDone = { presses++ },
                )
            }
        }
        val gotIt = label(R.string.onboarding_got_it)

        assertThat(composeRule.onAllNodesWithText(gotIt).fetchSemanticsNodes()).isEmpty()
        tapWhereGotItSits()
        assertThat(presses).isEqualTo(0)

        completed = true
        composeRule.waitForIdle()
        assertThat(composeRule.onAllNodesWithText(gotIt).fetchSemanticsNodes()).hasSize(1)
        tapWhereGotItSits()
        assertThat(presses).isEqualTo(1)
    }

    /**
     * Loop 1, the third property: the waiting "Got it" takes no focus. Under touch exploration the CTA
     * asks to be focusable — completion moves focus to it — so this is the case where a laid-out,
     * invisible button could still be reached. Focus is moved forward through the whole screen more
     * times than it has stops, and it never lands on "Got it" before completion.
     */
    @Test
    fun theWaitingGotItTakesNoFocusBeforeCompletion() {
        shadowOf(RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java))
            .setTouchExplorationEnabled(true)
        lateinit var focusManager: FocusManager
        composeRule.setContent {
            focusManager = LocalFocusManager.current
            AppTheme {
                TutorialScreen(
                    state = TutorialUiState.initial(),
                    onCellTap = {},
                    onAnimationFinished = {},
                    onSkip = {},
                    onDone = {},
                )
            }
        }
        val focusedGotIt = isFocused() and hasAnyDescendant(hasText(label(R.string.onboarding_got_it)))

        repeat(FOCUS_MOVES) {
            composeRule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
            assertThat(composeRule.onAllNodes(focusedGotIt, useUnmergedTree = true).fetchSemanticsNodes()).isEmpty()
        }
    }

    /**
     * The other edge of the same contract: the completion CTA is not under the navigation bar.
     *
     * ## Why this window, and not the one this test used to be on (A-10)
     *
     * The intent was always to measure on a window short enough that the column overflows, so
     * that `performScrollTo` presses the CTA against the viewport's bottom edge and the padding
     * is the only thing that can keep it off the bar. `w360dp-h640dp` is **not** that window,
     * and the re-audit proved it: with `safeDrawingPadding()` deleted this test still passed. The
     * numbers say why — the four bands measure ≈600dp, so on a 640dp window with the insets
     * ignored the column does not overflow **at all** (scrollable overflow 0.0dp), the scroll is
     * a no-op, and the CTA sits at `532..584` with 32dp of air under a 616dp floor. It passed
     * for exactly the wrong reason its own KDoc claimed it could not.
     *
     * [OVERFLOWING_WINDOW_QUALIFIER] is the shortest qualifier at which the property is
     * *measurable*: ≈600dp of content overflows it whether or not the insets are honoured, so the
     * scrolled CTA lands on the bottom edge of whichever viewport it is given — the inset one at
     * 536dp, or the whole window at 560dp. The 24dp between those two is the navigation bar, and
     * it is the whole of what this test measures. Measured both ways:
     *
     * | `safeDrawingPadding()` | overflow | CTA after the scroll | floor | verdict |
     * |---|---|---|---|---|
     * | present | 120.0dp | `484..536` | 536 | pass |
     * | deleted | 40.0dp | `508..560` | 536 | **fail by 24dp** |
     *
     * The precondition is asserted rather than assumed, because assuming it is what went wrong:
     * the scroll must actually **move** the CTA. On a window with room to spare it would not, and
     * a no-op scroll measures nothing.
     */
    @Test
    @Config(qualifiers = OVERFLOWING_WINDOW_QUALIFIER)
    fun theCompletionButtonClearsTheNavigationBar() {
        val viewModel = startTutorial()
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)

        repeat(TutorialUiState.STEP_COUNT) {
            while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                playTheOnlyMove(viewModel)
            }
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }

        val ctaLabel = label(R.string.onboarding_got_it)
        val before = composeRule.onNodeWithText(ctaLabel).getBoundsInRoot()
        val cta = composeRule.onNodeWithText(ctaLabel).performScrollTo().getBoundsInRoot()
        val floor = composeRule.onRoot().getBoundsInRoot().bottom - navigationBarInset

        // The column really overflows, so the scroll really presses the CTA against the edge.
        assertThat(cta.top).isNotEqualTo(before.top)
        assertThat(cta.bottom).isAtMost(floor)
    }

    @Test
    fun theStepPipRowAnnouncesItselfAsOneNodeSayingStepNOfThree() {
        val viewModel = startTutorial()

        // The pips are decorative individually, so the row speaks ONCE — "Step 1 of 3" —
        // rather than announcing three anonymous dots to TalkBack.
        composeRule
            .onNodeWithTag(TUTORIAL_SCREEN_TEST_TAG)
            .assertIsDisplayed()
        assertThat(pipNodeCount(stepNumber = FIRST_STEP + 1)).isEqualTo(ONE_NODE)

        playTheOnlyMove(viewModel)
        idlePastTheStepBeat()
        composeRule.waitForIdle()

        // And it advances with the step.
        assertThat(pipNodeCount(stepNumber = SECOND_STEP + 1)).isEqualTo(ONE_NODE)
        assertThat(pipNodeCount(stepNumber = FIRST_STEP + 1)).isEqualTo(NO_NODE)
    }

    private fun pipNodeCount(stepNumber: Int): Int =
        composeRule.onAllNodes(hasContentDescription(stepDescription(stepNumber))).fetchSemanticsNodes().size

    private fun stepDescription(stepNumber: Int): String =
        RuntimeEnvironment
            .getApplication()
            .getString(R.string.tutorial_step_progress, stepNumber, TutorialUiState.STEP_COUNT)

    private val statusBarInset: Dp get() = STATUS_BAR_INSET_DP.dp

    private val navigationBarInset: Dp get() = NAVIGATION_BAR_INSET_DP.dp

    private fun nodeTop(matcher: SemanticsMatcher): Dp {
        val node = composeRule.onNode(matcher).performScrollTo()
        return node.getBoundsInRoot().top
    }

    /**
     * The header sits the column's padding below the status bar, and the gap from the header down to
     * the body equals the gap from the body down to the navigation bar.
     */
    private fun assertTheHeaderIsPinnedAndTheBodyCentredBelowIt() {
        val window = composeRule.onRoot().getBoundsInRoot()
        val header = composeRule.onNodeWithTag(TUTORIAL_HEADER_TEST_TAG).getBoundsInRoot()
        val body = composeRule.onNodeWithTag(TUTORIAL_BODY_TEST_TAG).getBoundsInRoot()
        val columnPadding = with(composeRule.density) { dimensionPx(R.dimen.spacing_md).toDp() }

        assertThat((header.top - (window.top + statusBarInset)).value)
            .isWithin(CENTRING_TOLERANCE_DP)
            .of(columnPadding.value)

        val gapAbove = body.top - header.bottom
        val gapBelow = (window.bottom - navigationBarInset) - body.bottom
        // Room to spare, so the arrangement — not the fixed spacing — is what places the body.
        assertThat(gapAbove + gapBelow).isAtLeast(ROOM_TO_SPARE_DP.dp)
        assertThat(gapAbove.value).isWithin(CENTRING_TOLERANCE_DP).of(gapBelow.value)
    }

    private fun dimensionPx(id: Int): Int = RuntimeEnvironment.getApplication().resources.getDimensionPixelSize(id)

    /** "Got it" is the body's last band, so a tap just above the body's bottom edge lands on it. */
    private fun tapWhereGotItSits() {
        composeRule.onNodeWithTag(TUTORIAL_BODY_TEST_TAG).performTouchInput {
            click(Offset(centerX, bottom - GOT_IT_TAP_INSET_DP.dp.toPx()))
        }
        composeRule.waitForIdle()
    }

    /** Asserts the screen offers no way to skip past a step without playing it. */
    private fun assertNoAdvanceButton() {
        ADVANCE_WORDS.forEach { word ->
            assertThat(composeRule.onAllNodesWithText(word, substring = true).fetchSemanticsNodes())
                .isEmpty()
        }
    }

    private fun startTutorial(): TutorialViewModel {
        val viewModel =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent {
            AppTheme {
                TutorialRouteUnderTest(viewModel)
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    private fun playTheOnlyMove(viewModel: TutorialViewModel) {
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).first()
        tapCell(move.from)
        tapCell(move.to)
    }

    private fun tapCell(position: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (position.col + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                    y = (position.row + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    private fun label(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    private companion object {
        const val FIRST_STEP = 0
        const val SECOND_STEP = 1
        const val THIRD_STEP = 2
        const val ONE_NODE = 1
        const val NO_NODE = 0

        /** Every tutorial footprint sits in a 3-cell square grid. */
        const val TUTORIAL_GRID = 3

        /** Copy a tap-to-advance affordance would have to use; none may exist (criterion 1). */
        val ADVANCE_WORDS = listOf("Next", "Continue", "Done")

        /**
         * The top inset under test, in dp — the 142 px cutout band a `Pixel_9a` at 420 dpi
         * reports, rounded up onto the grid so the number is a floor rather than one device's
         * exact reading (`qa-engineer.md` F-1: `frame=[0,0][1080,142]` ≈ 54 dp).
         */
        const val STATUS_BAR_INSET_DP = 56

        /** A gesture-navigation bar, the shallower of the two bottom cases. */
        const val NAVIGATION_BAR_INSET_DP = 24

        /**
         * The least room the tall window must leave around the body for the centring test to mean
         * anything. An overflowing column leaves only fixed spacing — the `spacing_lg` band gap under
         * the header and the `spacing_md` padding under the body, 40dp together — so anything well past
         * that is slack the arrangement places.
         */
        const val ROOM_TO_SPARE_DP = 64

        /**
         * One pixel of rounding — the centring offset and each inset are rounded to whole pixels, and
         * a pixel is 1dp at the test's default density. The error under test is the whole slack,
         * hundreds of dp on [TALL_PHONE_QUALIFIER].
         */
        const val CENTRING_TOLERANCE_DP = 1f

        /** "The same place", for the board before and after completion: half a pixel at the test's default density. */
        const val SAME_PLACE_TOLERANCE_DP = 0.5f

        /** Up from the body's bottom edge into "Got it", well inside the button's 40dp height. */
        const val GOT_IT_TAP_INSET_DP = 12

        /**
         * Forward focus moves, more than the first step has stops (Skip, the board stop, three cells
         * and "Got it" if it could take focus), so the walk wraps and visits every one of them.
         */
        const val FOCUS_MOVES = 12
    }
}

private const val TUTORIAL_SCREEN_ROBOLECTRIC_SDK = 34

/**
 * A 6.7-inch-class window, so the inset assertions read against a viewport that can hold the
 * board — Robolectric's default 320x470px window would make every geometry claim a statement
 * about a screen no player has.
 */
private const val TALL_PHONE_QUALIFIER = "w411dp-h915dp"

/**
 * The shortest window at which the CTA's bottom-edge clearance is **measurable**: the tutorial's
 * four bands measure ≈600dp, so this column overflows whether or not the window insets are
 * honoured, and the scrolled CTA therefore lands on the bottom edge of whichever viewport it was
 * given. `w360dp-h640dp` cannot measure it — ≈600dp of content fits a 640dp window, so with the
 * insets ignored the column does not overflow at all (A-10, and the measured table on
 * [TutorialScreenTest.theCompletionButtonClearsTheNavigationBar]).
 */
private const val OVERFLOWING_WINDOW_QUALIFIER = "w360dp-h560dp"
