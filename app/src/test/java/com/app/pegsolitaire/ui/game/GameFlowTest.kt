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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.TestTimeSource

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
    fun reachingATerminalPositionEmitsTheFinishEventInsteadOfADialog() {
        // A two-peg line that resolves to a single peg in one jump → Won.
        val nearWin =
            Board(spec = Boards.English33, pegs = 0L)
                .withPeg(Position(row = 3, col = 1))
                .withPeg(Position(row = 3, col = 2))
        val viewModel = startGame(nearWin)

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        assertStatus(GameStatus.Won)

        // D-05: the Phase-1 game-over dialog is gone — the VM emits a single finish
        // event (carrying the ScoreCalculator-consistent result) for GameRoute to turn
        // into a Results navigation; no in-screen overlay appears.
        val result = runBlocking { viewModel.finished.first() }
        assertThat(result.pegsRemaining).isEqualTo(1)
        assertThat(result.stars).isEqualTo(ScoreCalculator.stars(pegsRemaining = 1))
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.elapsed_time))
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
        val viewModel =
            GameViewModel(
                rulesEngine = RulesEngine,
                levelRepository = SingleBoardLevelRepository(initialBoard),
                scoreCalculator = ScoreCalculator,
                settingsRepository = HintsOnSettingsRepository,
                timeSource = TestTimeSource(),
                defaultDispatcher = Dispatchers.Unconfined,
                adGateway = NoOpAdGateway(),
                savedStateHandle = SavedStateHandle(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)),
            )
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
                    onToggleHints = viewModel::onToggleHints,
                    onNavigateBack = {},
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

/** The `Gameplay(levelId)` route arg key (the route's property name). */
private const val LEVEL_ID_ARG = "levelId"

/** The English-33 start: 33 holes minus the empty center. */
private const val INITIAL_PEG_COUNT = 32

/** This flow drives the English-33 board, which occupies a 7×7 lattice. */
private const val ENGLISH33_GRID_DIMENSION = 7

/** A cell center as a fraction of the square board canvas (grid centered, origin 0). */
private fun androidx.compose.ui.test.TouchInjectionScope.percentOffsetForCell(
    row: Int,
    col: Int,
) = percentOffset(
    x = (col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID_DIMENSION,
    y = (row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID_DIMENSION,
)

/**
 * A minimal [LevelRepository] that always loads the one [board] it was built with,
 * under [ClassicLevelIds.ENGLISH33] — lets the flow test drive [GameViewModel] over
 * the new levelId seam without the Android-resource-backed real source.
 */
private class SingleBoardLevelRepository(
    private val board: Board,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level =
        Level(
            id = levelId,
            displayName = levelId,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )

    override fun loadChallenge(levelNumber: Int): Level = error("Challenge is a Phase-3 stub")
}

/** A fixed hints-on [SettingsRepository]; this flow test never toggles the setting. */
private object HintsOnSettingsRepository : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit
}
