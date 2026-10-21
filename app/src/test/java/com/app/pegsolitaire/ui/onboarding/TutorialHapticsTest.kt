package com.app.pegsolitaire.ui.onboarding

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.app.pegsolitaire.testing.RecordingHapticFeedback
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
 * What the tutorial feels like (D-01j, WT-15, WT-16's haptic half; tech-lead ruling 2026-10-18).
 *
 * A completed tutorial jump fires the game's own `ContextClick`, through the shared
 * `JumpHapticEffect` and under the same persisted Haptics switch, read through the
 * `HapticsSetting` seam. A wrong tap in the tutorial fires nothing at all: the redirect is the
 * whole answer there (D-01j(2)), and the reject haptic stays private to the gameplay screen.
 *
 * Driven through the real [TutorialScreen] over a real [TutorialViewModel], with a recording
 * haptic engine swapped in through `LocalHapticFeedback`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_HAPTICS_ROBOLECTRIC_SDK])
class TutorialHapticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val haptics = RecordingHapticFeedback()

    @Test
    fun aCompletedTutorialJumpFiresExactlyOneContextClickWhileHapticsAreOn() {
        val viewModel = startTutorial(hapticsOn = true)
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()

        tapCell(move.from)
        // A selection is not a jump, and nothing is felt for it.
        assertThat(haptics.events).isEmpty()

        tapCell(move.to)
        assertThat(haptics.events).containsExactly(HapticFeedbackType.ContextClick)
    }

    @Test
    fun noHapticFiresOnATutorialJumpWhileHapticsAreOff() {
        val viewModel = startTutorial(hapticsOn = false)
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()

        tapCell(move.from)
        tapCell(move.to)

        // The jump still happened: the switch gates the feedback, never the game.
        assertThat(viewModel.uiState.value.board.board.pegCount).isEqualTo(TutorialUiState.STEP_COMPLETE_PEGS)
        assertThat(haptics.events).isEmpty()
    }

    /**
     * The 7c race on the tutorial's call site (Ruling 2, test 9): with the OS animator scale at
     * zero the snap path sets and clears `animating` in one frame, and the keyed effect must
     * still see the move id. `GameFlowTest` holds the gameplay call site.
     */
    @Test
    fun aCompletedTutorialJumpStillFiresTheHapticWhenTheOsAnimationScaleIsZero() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            ANIMATIONS_OFF,
        )
        val viewModel = startTutorial(hapticsOn = true)
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()

        tapCell(move.from)
        tapCell(move.to)

        assertThat(haptics.events).containsExactly(HapticFeedbackType.ContextClick)
    }

    @Test
    fun noWrongTapInTheTutorialFiresAnyHapticWhileHapticsAreOn() {
        val viewModel = startTutorial(hapticsOn = true)
        playTheOnlyMove(viewModel)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)
        haptics.events.clear()

        tapCell(T2_EMPTY_HOLE) // T-W1: an empty hole, nothing held
        tapCell(T2_MOVABLE_PEG) // a selection, so the next two taps are made holding a peg
        tapCell(T2_IMMOVABLE_PEG) // T-W2: a different peg while one is held
        tapCell(T2_ILLEGAL_HOLE) // T-W3: a hole that is not a destination while one is held

        // The taps landed: the held peg is still held, so none of them moved anything.
        assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_MOVABLE_PEG)
        assertThat(haptics.events).isEmpty()
    }

    private fun startTutorial(hapticsOn: Boolean): TutorialViewModel {
        val viewModel =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = hapticsOn), TestApplicationScope)
        composeRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                AppTheme {
                    TutorialRouteUnderTest(viewModel)
                }
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    private fun playTheOnlyMove(viewModel: TutorialViewModel) {
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
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

    private companion object {
        /** Every tutorial footprint sits in a 3-cell square grid. */
        const val TUTORIAL_GRID = 3

        /** The OS animator duration scale a player with reduced motion has set (criterion 25). */
        const val ANIMATIONS_OFF = 0f

        /** T2's only movable peg. */
        val T2_MOVABLE_PEG = Position(row = 0, col = 1)

        /** T2's middle peg, which has no legal jump. */
        val T2_IMMOVABLE_PEG = Position(row = 1, col = 1)

        /** An in-play empty hole on T2 with nothing pointing at it. */
        val T2_EMPTY_HOLE = Position(row = 0, col = 0)

        /** An in-play empty hole on T2 that is never a legal destination. */
        val T2_ILLEGAL_HOLE = Position(row = 2, col = 2)
    }
}

private const val TUTORIAL_HAPTICS_ROBOLECTRIC_SDK = 34
