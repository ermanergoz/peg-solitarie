package com.app.pegsolitaire.ui.onboarding

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
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
import org.robolectric.annotation.Config

/**
 * The one Compose UI test per screen's main flow (rules/testing.md), for the tutorial.
 *
 * It finds the screen by [TUTORIAL_SCREEN_TEST_TAG] and the board by
 * [BOARD_CANVAS_TEST_TAG] — **never by button copy**, which is the defect the phase's two
 * retargeted tests exist to remove. A screen located by the words on its buttons breaks
 * whenever the copy changes and says nothing about the behaviour.
 *
 * Two of its tests supply the window insets Robolectric leaves empty, which is the blind
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

    /** Asserts the screen offers no way to skip past a step without playing it. */
    private fun assertNoAdvanceButton() {
        ADVANCE_WORDS.forEach { word ->
            assertThat(composeRule.onAllNodesWithText(word, substring = true).fetchSemanticsNodes())
                .isEmpty()
        }
    }

    private fun startTutorial(): TutorialViewModel {
        val viewModel = TutorialViewModel(SilentOnboardingRepository(), TestApplicationScope)
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
