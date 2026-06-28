package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end Compose flow test for the playable English-33 board, run JVM-side via
 * Robolectric (no emulator). It drives the real [GameScreen] over a directly
 * constructed [GameViewModel] (the only thing it skips vs. [GameRoute] is the
 * `hiltViewModel()` lookup) and proves the walking-skeleton loop:
 *
 *  - tap a peg → its legal destination highlights → tap the hole → the jumped peg
 *    is removed (pegCount −1) — GAME-01 / GAME-03;
 *  - a played-out line reaches a terminal position and the status-tiered game-over
 *    dialog ("Play Again") appears — D-05 / GAME-02;
 *  - the top-bar restart shows the mid-game confirm and resets to the 32-peg start —
 *    GAME-05 / D-06.
 *
 * Assertions read the [PegCountKey]/[GameStatusKey]/[LegalTargetCountKey] semantics
 * the screen exposes, so they are deterministic rather than pixel-based. Cell taps
 * use [percentOffsetForCell]: the board canvas is square ([BoardMetrics.BOARD_ASPECT_RATIO]),
 * so a cell center is at fraction `(col+0.5)/GRID, (row+0.5)/GRID` of the canvas.
 *
 * The Robolectric runtime SDK is pinned via `@Config(sdk=…)` independently of the
 * app's `compileSdk 36` (Android 16 has no Robolectric android-all jar yet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class GameFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun tappingPegThenHolePerformsTheJumpAndRemovesAPeg() {
        startGame(Boards.english33Initial)

        // The English-33 start has 32 pegs (33 holes, center empty).
        assertPegCount(INITIAL_PEG_COUNT)

        // Select the peg left-of-center; its only legal destination is the center hole.
        tapCell(row = 3, col = 1)
        assertLegalTargetCount(1)

        // Tap the highlighted center hole → the jumped peg is removed.
        tapCell(row = 3, col = 3)
        assertPegCount(INITIAL_PEG_COUNT - 1)
    }

    @Test
    fun reachingATerminalPositionShowsTheGameOverDialog() {
        // A two-peg line that resolves to a single peg in one jump → Won ("Perfect!").
        val nearWin =
            Board(spec = Boards.English33, pegs = 0L)
                .withPeg(Position(row = 3, col = 1))
                .withPeg(Position(row = 3, col = 2))
        startGame(nearWin)

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        assertStatus(GameStatus.Won)
        composeRule
            .onNodeWithText(context.getString(R.string.game_over_perfect_headline))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.play_again))
            .assertIsDisplayed()
    }

    @Test
    fun midGameRestartConfirmsThenResetsToTheInitialBoard() {
        val vm = startGame(Boards.english33Initial)

        // Make one move so restart is mid-game (history non-empty, still Playing).
        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)
        assertPegCount(INITIAL_PEG_COUNT - 1)

        // Top-bar restart → the mid-game confirm dialog appears (D-06).
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.restart_board))
            .performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithText(context.getString(R.string.restart_confirm_title))
            .assertIsDisplayed()

        // Confirm → the board resets to the 32-peg center-empty start.
        composeRule
            .onNodeWithText(context.getString(R.string.restart_board))
            .performClick()
        composeRule.waitForIdle()
        assertPegCount(INITIAL_PEG_COUNT)
    }

    private fun startGame(initialBoard: Board): GameViewModel {
        val viewModel = GameViewModel(RulesEngine, initialBoard)
        composeRule.setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                GameScreen(
                    state = state,
                    onCellTap = viewModel::onCellTap,
                    onRestart = viewModel::onRestart,
                    onConfirmRestart = viewModel::onConfirmRestart,
                    onDismissRestart = viewModel::onDismissRestart,
                    onAnimationFinished = viewModel::onAnimationFinished,
                )
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    private fun tapCell(
        row: Int,
        col: Int,
    ) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(percentOffsetForCell(row = row, col = col))
        }
        composeRule.waitForIdle()
    }

    private fun assertPegCount(expected: Int) {
        composeRule
            .onNodeWithTag(GAME_STATE_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(PegCountKey, expected))
    }

    private fun assertLegalTargetCount(expected: Int) {
        composeRule
            .onNodeWithTag(GAME_STATE_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(LegalTargetCountKey, expected))
    }

    private fun assertStatus(expected: GameStatus) {
        composeRule
            .onNodeWithTag(GAME_STATE_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(GameStatusKey, expected.name))
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The English-33 start: 33 holes minus the empty center. */
private const val INITIAL_PEG_COUNT = 32

/** A cell center as a fraction of the square board canvas (grid centered, origin 0). */
private fun androidx.compose.ui.test.TouchInjectionScope.percentOffsetForCell(
    row: Int,
    col: Int,
) = percentOffset(
    x = (col + BoardMetrics.CELL_CENTER_FRACTION) / BoardMetrics.GRID_DIMENSION,
    y = (row + BoardMetrics.CELL_CENTER_FRACTION) / BoardMetrics.GRID_DIMENSION,
)
