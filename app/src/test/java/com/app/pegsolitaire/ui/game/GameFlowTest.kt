package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
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

    /** Captures every haptic the screen fires, so the SET-02 gate is asserted, not assumed. */
    private val haptics = RecordingHapticFeedback()

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
    fun reachingAStuckPositionSurfacesTheRescueBannerOverTheStillPresentBoard() {
        // Two adjacent pegs that resolve in one jump, plus an isolated peg with no
        // neighbour: after the jump two pegs remain with no legal move → Stuck (D-06),
        // which is now rescuable in-board rather than auto-navigating.
        val stuckBoard =
            Board(spec = Boards.English33, pegs = 0L)
                .withPeg(Position(row = 3, col = 1))
                .withPeg(Position(row = 3, col = 2))
                .withPeg(Position(row = 0, col = 3))
        startGame(stuckBoard)

        // A Playing board shows no rescue banner.
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertDoesNotExist()

        // Drive the only line into the dead end.
        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)
        assertStatus(GameStatus.Stuck)

        // D-08: the non-blocking stuck banner surfaces over the STILL-PRESENT, interactive
        // board (the board canvas remains displayed beneath the overlay, not replaced).
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.stuck_message))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun aWonGameNeverSurfacesTheRescueBanner() {
        // A one-jump win → Won routes to Results; it must not raise the rescue banner.
        val nearWin =
            Board(spec = Boards.English33, pegs = 0L)
                .withPeg(Position(row = 3, col = 1))
                .withPeg(Position(row = 3, col = 2))
        startGame(nearWin)

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        assertStatus(GameStatus.Won)
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertDoesNotExist()
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

    // ---- Haptics on jump, gated by the persisted setting (SET-02, D-15) ------

    @Test
    fun aJumpFiresExactlyOneLightHapticWhileHapticsAreOn() {
        val viewModel = startGame(Boards.english33Initial, settings = AllOnSettingsRepository)
        assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        // Nothing fires before a move — the haptic is per jump, not per composition.
        assertThat(haptics.events).isEmpty()

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        assertThat(haptics.events).containsExactly(HapticFeedbackType.ContextClick)
    }

    @Test
    fun noHapticFiresOnAJumpWhileHapticsAreOff() {
        val viewModel =
            startGame(Boards.english33Initial, settings = FixedSettingsRepository(hapticsOn = false))
        assertThat(viewModel.uiState.value.hapticsOn).isFalse()

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        // The jump still happened — only the feedback is silenced (the switch is a gate,
        // never a gameplay change).
        assertPegCount(INITIAL_PEG_COUNT - 1)
        assertThat(haptics.events).isEmpty()
    }

    private fun startGame(
        initialBoard: Board,
        settings: SettingsRepository = AllOnSettingsRepository,
    ): GameViewModel {
        val viewModel =
            GameViewModel(
                rulesEngine = RulesEngine,
                levelRepository = SingleBoardLevelRepository(initialBoard),
                scoreCalculator = ScoreCalculator,
                settingsRepository = settings,
                // Classic-only flow test: the Challenge ladder is never touched here.
                challengeProgress = mockk(relaxed = true),
                timeSource = TestTimeSource(),
                defaultDispatcher = Dispatchers.Unconfined,
                adGateway = NoOpAdGateway(),
                getHintUseCase = GetHintUseCase(),
                savedStateHandle = SavedStateHandle(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)),
            )
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    GameScreen(
                        state = state,
                        onCellTap = viewModel::onCellTap,
                        onRestart = viewModel::onRestart,
                        onConfirmRestart = viewModel::onConfirmRestart,
                        onDismissRestart = viewModel::onDismissRestart,
                        onAnimationFinished = viewModel::onAnimationFinished,
                        onToggleHints = viewModel::onToggleHints,
                        onUndo = viewModel::onUndo,
                        onHint = viewModel::onHintRequested,
                        onHome = {},
                        onNavigateBack = {},
                    )
                }
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

/**
 * A fixed [SettingsRepository] whose flags never change; this flow test never toggles a
 * setting from the UI, it just starts a game with the flags it wants to prove.
 */
private class FixedSettingsRepository(
    hintsEnabled: Boolean = true,
    soundOn: Boolean = true,
    hapticsOn: Boolean = true,
) : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(hintsEnabled)

    override val soundOn: Flow<Boolean> = flowOf(soundOn)

    override val hapticsOn: Flow<Boolean> = flowOf(hapticsOn)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** The default all-on settings (hints + sound + haptics), matching the fail-open defaults. */
private val AllOnSettingsRepository = FixedSettingsRepository()

/**
 * A [HapticFeedback] that records every fire instead of buzzing, so the SET-02 gate is
 * asserted JVM-side. The physical *feel* of the tick is device-only and is covered by
 * the end-of-phase manual checkpoint (VALIDATION Manual-Only).
 */
private class RecordingHapticFeedback : HapticFeedback {
    val events = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        events += hapticFeedbackType
    }
}
