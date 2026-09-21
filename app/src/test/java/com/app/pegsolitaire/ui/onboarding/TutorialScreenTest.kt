package com.app.pegsolitaire.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
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
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_SCREEN_ROBOLECTRIC_SDK])
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

    private fun pipNodeCount(stepNumber: Int): Int {
        val description =
            RuntimeEnvironment
                .getApplication()
                .getString(R.string.tutorial_step_progress, stepNumber, TutorialUiState.STEP_COUNT)
        return composeRule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().size
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
    }
}

private const val TUTORIAL_SCREEN_ROBOLECTRIC_SDK = 34
