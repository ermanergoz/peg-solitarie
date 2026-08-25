package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

/**
 * Turbine + virtual-time coverage of every documented `GameViewModel` behavior.
 *
 * Phase-1 gameplay (D-02..D-06, GAME-02/04/05, the T-03-01 legal-move control) is
 * preserved verbatim; this plan adds the 02-05 behaviors:
 *  - the played board is loaded from the `Gameplay(levelId)` route via the
 *    `LevelRepository` (CLAS-02) — no hardcoded English-33 inject;
 *  - a visible elapsed timer starts on the first move, ticks on the injected
 *    `TestTimeSource`, and freezes at a terminal status (D-04);
 *  - exactly one `finished` `ScoreResult` is emitted on Won/Stuck (SCOR-02, Pitfall 4).
 *
 * Robolectric backs `SavedStateHandle.toRoute<Gameplay>()`; `Dispatchers.setMain`
 * with a `StandardTestDispatcher` makes the `viewModelScope` ticker deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
// The single behavior spec for GameViewModel (gameplay + timer + undo + hint); kept
// together so one file exhaustively documents the VM's contract.
@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent/advanceUntilIdle/advanceTimeBy virtual-time control
class GameViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    // Owns every VM built in a test so [tearDown] (and the in-body cancellation a few
    // ever-Playing tests need) can clear it: clearing the store cancels each
    // `viewModelScope`, stopping the eager per-second ticker so `runTest` does not
    // hang draining an infinite `delay`-loop on a board that never reaches terminal.
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0

    // A peg above the empty center; its only legal jump lands in the center hole.
    private val pegAboveCenter = Position(row = 1, col = 3)
    private val centerHole = Position(row = 3, col = 3)
    private val pegLeftOfCenter = Position(row = 3, col = 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- New 02-05 behaviors -------------------------------------------------

    @Test
    fun loadsTheBoardSelectedByLevelId() =
        runTest(testDispatcher) {
            val english = viewModelFor(Boards.english33Initial, levelId = ClassicLevelIds.ENGLISH33)
            assertThat(english.levelId).isEqualTo(ClassicLevelIds.ENGLISH33)
            assertThat(english.uiState.value.board).isEqualTo(Boards.english33Initial)

            val tiny = wonInOneJumpBoard()
            val other = viewModelFor(tiny, levelId = OTHER_LEVEL_ID)
            assertThat(other.levelId).isEqualTo(OTHER_LEVEL_ID)
            assertThat(other.uiState.value.board).isEqualTo(tiny)
        }

    @Test
    fun challengeLevelArgLoadsAGeneratedSolvableBoardAndIsExposed() =
        runTest(testDispatcher) {
            // The `board` passed here is unused for Challenge — the VM branches on the
            // route arg and loads via loadChallenge (the generator), not loadClassic.
            val viewModel =
                viewModelFor(Boards.english33Initial, challengeLevel = FIRST_CHALLENGE_LEVEL)
            runCurrent()

            assertThat(viewModel.challengeLevel).isEqualTo(FIRST_CHALLENGE_LEVEL)
            // A generated board is real and playable: non-empty and reducible (the
            // generator deals solvable-by-construction boards).
            assertThat(viewModel.uiState.value.board.pegCount).isGreaterThan(1)
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)

            cancelTickers() // the generated board stays Playing; stop any eager ticker.
        }

    @Test
    fun challengeLevelBelowOneIsClampedWithoutCrashing() =
        runTest(testDispatcher) {
            // A forged `challengeLevel = 0` nav arg must clamp to level 1, not crash (T-03-09).
            val viewModel =
                viewModelFor(Boards.english33Initial, challengeLevel = BELOW_MIN_CHALLENGE_LEVEL)
            runCurrent()

            assertThat(viewModel.challengeLevel).isEqualTo(BELOW_MIN_CHALLENGE_LEVEL)
            assertThat(viewModel.uiState.value.board.pegCount).isGreaterThan(1)
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)

            cancelTickers()
        }

    @Test
    fun elapsedSecondsStartsAtZeroThenIncreasesAfterTheFirstMove() =
        runTest(testDispatcher) {
            val timeSource = TestTimeSource()
            val viewModel = viewModelFor(Boards.english33Initial, timeSource = timeSource)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(0)

            // No move yet → advancing the clock must NOT start the timer.
            timeSource += 5.seconds
            advanceTimeBy(TWO_SECONDS_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(0)

            // First move starts the timer (mark taken now, at t = 5s).
            viewModel.onCellTap(pegAboveCenter)
            viewModel.onCellTap(centerHole)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(0)

            timeSource += 3.seconds // t = 8s → 3s elapsed since the mark
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(3)

            cancelTickers() // board stays Playing; stop the eager ticker so runTest can finish.
        }

    @Test
    fun timerFreezesAtTerminalStatus() =
        runTest(testDispatcher) {
            val timeSource = TestTimeSource()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource = timeSource)
            runCurrent()

            // Move A keeps the game Playing → the timer starts and ticks.
            viewModel.onCellTap(Position(row = 0, col = 2))
            viewModel.onCellTap(Position(row = 0, col = 4))
            runCurrent()
            timeSource += 2.seconds
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(2)

            // Move B reaches Stuck → the ticker stops.
            viewModel.onCellTap(Position(row = 0, col = 0))
            viewModel.onCellTap(Position(row = 0, col = 2))
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)
            val frozen = viewModel.uiState.value.elapsedSeconds

            // Advancing the clock after terminal does not move the timer.
            timeSource += 10.seconds
            advanceTimeBy(FIVE_SECONDS_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(frozen)
        }

    @Test
    fun emitsExactlyOneFinishedEventWithScoreCalculatorConsistentResult() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(wonInOneJumpBoard())
            viewModel.finished.test {
                expectNoEvents()

                viewModel.onCellTap(Position(row = 0, col = 0))
                viewModel.onCellTap(Position(row = 0, col = 2))
                runCurrent()

                val result = awaitItem()
                assertThat(result.pegsRemaining).isEqualTo(1)
                assertThat(result.pegsCleared).isEqualTo(1)
                assertThat(result.stars).isEqualTo(ScoreCalculator.stars(pegsRemaining = 1))
                assertThat(result.score)
                    .isEqualTo(
                        ScoreCalculator.score(
                            pegsCleared = 1,
                            elapsedSeconds = result.elapsedSeconds,
                        ),
                    )

                // No re-emission on subsequent state reads (Pitfall 4).
                viewModel.onAnimationFinished()
                runCurrent()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Persisted hint toggle ----------------------------------------------

    @Test
    fun showHintsReflectsTheSettingAndSelectionStillComputesTargets() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(Boards.english33Initial, settings = FakeSettingsRepository(hintsEnabled = true))
            runCurrent()
            assertThat(viewModel.uiState.value.showHints).isTrue()

            viewModel.onCellTap(pegAboveCenter)
            runCurrent()
            assertThat(viewModel.uiState.value.legalTargets).containsExactly(centerHole)
            assertThat(viewModel.uiState.value.showHints).isTrue()
        }

    @Test
    fun onToggleHintsFlipsShowHintsAndPersistsTheNewValue() =
        runTest(testDispatcher) {
            val settings = FakeSettingsRepository(hintsEnabled = true)
            val viewModel = viewModelFor(Boards.english33Initial, settings = settings)
            runCurrent()
            assertThat(viewModel.uiState.value.showHints).isTrue()

            viewModel.onToggleHints()
            runCurrent()

            assertThat(settings.lastWritten).isFalse()
            assertThat(viewModel.uiState.value.showHints).isFalse()
        }

    @Test
    fun hintsOffHidesShowHintsButTargetsAreStillComputedOnSelection() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(Boards.english33Initial, settings = FakeSettingsRepository(hintsEnabled = false))
            runCurrent()
            assertThat(viewModel.uiState.value.showHints).isFalse()

            // The gate is render-only: selection/jump logic is unchanged when hints are off.
            viewModel.onCellTap(pegAboveCenter)
            runCurrent()
            assertThat(viewModel.uiState.value.legalTargets).containsExactly(centerHole)
            assertThat(viewModel.uiState.value.showHints).isFalse()
        }

    // ---- Undo (ASST-02, D-05/D-07) ------------------------------------------

    @Test
    fun undoRestoresTheExactPriorBoardAndDropsTheLastMove() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            runCurrent()
            val initialBoard = viewModel.uiState.value.board
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            viewModel.onCellTap(pegAboveCenter)
            viewModel.onCellTap(centerHole)
            runCurrent()
            assertThat(viewModel.uiState.value.board).isNotEqualTo(initialBoard)
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.board).isEqualTo(initialBoard)
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            cancelTickers() // board stays Playing after undo; stop the eager ticker.
        }

    @Test
    fun repeatedUndoWalksAllTheWayBackToTheInitialBoardAndRescuesAStuckGame() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(twoMoveStuckLine())
            runCurrent()
            val initialBoard = viewModel.uiState.value.board

            // Move A keeps the game Playing.
            viewModel.onCellTap(Position(row = 0, col = 2))
            viewModel.onCellTap(Position(row = 0, col = 4))
            runCurrent()
            val afterMoveA = viewModel.uiState.value.board

            // Move B strands two non-adjacent pegs → Stuck.
            viewModel.onCellTap(Position(row = 0, col = 0))
            viewModel.onCellTap(Position(row = 0, col = 2))
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)

            // First undo rescues the Stuck board back to a playable position.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.board).isEqualTo(afterMoveA)
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            // Second undo returns to the initial board; nothing left to undo.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.board).isEqualTo(initialBoard)
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            cancelTickers() // board is Playing again; stop the eager ticker.
        }

    @Test
    fun undoWithNoMovesIsANoOp() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            runCurrent()
            val before = viewModel.uiState.value
            assertThat(before.canUndo).isFalse()

            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.uiState.value.board).isEqualTo(before.board)
            assertThat(viewModel.uiState.value.canUndo).isFalse()
        }

    @Test
    fun undoDoesNotTouchTheElapsedTimer() =
        runTest(testDispatcher) {
            val timeSource = TestTimeSource()
            val viewModel = viewModelFor(Boards.english33Initial, timeSource = timeSource)
            runCurrent()

            // First move starts the timer; advance real time so it reads 4s.
            viewModel.onCellTap(pegAboveCenter)
            viewModel.onCellTap(centerHole)
            runCurrent()
            timeSource += 4.seconds
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(4)

            // Undo must NOT reset the timer (D-07).
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(4)

            // And the ticker keeps counting real time after the undo.
            timeSource += 2.seconds
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(6)

            cancelTickers()
        }

    @Test
    fun undoRequestsTheUndoGrantThroughTheAdGateway() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()
            val viewModel = viewModelFor(Boards.english33Initial, adGateway = gateway)
            runCurrent()

            viewModel.onCellTap(pegAboveCenter)
            viewModel.onCellTap(centerHole)
            runCurrent()

            viewModel.onUndo()
            runCurrent()

            assertThat(gateway.lastReason).isEqualTo(AdReason.UNDO)

            cancelTickers()
        }

    @Test
    fun undoIsSkippedWhenTheGrantIsDenied() =
        runTest(testDispatcher) {
            val deniedGateway = FakeAdGateway(grant = false)
            val viewModel = viewModelFor(Boards.english33Initial, adGateway = deniedGateway)
            runCurrent()

            viewModel.onCellTap(pegAboveCenter)
            viewModel.onCellTap(centerHole)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            viewModel.onUndo()
            runCurrent()

            // Grant denied → the undo does not proceed; the board is unchanged.
            assertThat(viewModel.uiState.value.board).isEqualTo(afterJump)
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            cancelTickers()
        }

    // ---- Stuck-rescue state machine (ASST-03, D-06/D-08) --------------------

    @Test
    fun reachingADeadEndSetsTheRescueFlagAndEmitsNoFinishedEvent() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(twoMoveStuckLine())
            viewModel.finished.test {
                expectNoEvents()

                // Move A keeps the game Playing; Move B strands two pegs → Stuck.
                viewModel.onCellTap(Position(row = 0, col = 2))
                viewModel.onCellTap(Position(row = 0, col = 4))
                runCurrent()
                viewModel.onCellTap(Position(row = 0, col = 0))
                viewModel.onCellTap(Position(row = 0, col = 2))
                runCurrent()

                val state = viewModel.uiState.value
                assertThat(state.status).isEqualTo(GameStatus.Stuck)
                assertThat(state.stuck).isTrue()
                assertThat(state.showRescueBanner).isTrue()
                assertThat(state.noWinningMove).isFalse()
                // D-06: Stuck no longer emits a finished event (it is rescuable, not
                // auto-navigating); the board also stays interactive (2 pegs remain).
                assertThat(state.board.pegCount).isEqualTo(2)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun wonEmitsFinishedAndNeverSetsTheRescueFlag() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(wonInOneJumpBoard())
            viewModel.finished.test {
                expectNoEvents()

                viewModel.onCellTap(Position(row = 0, col = 0))
                viewModel.onCellTap(Position(row = 0, col = 2))
                runCurrent()

                // Won still routes to Results (emits exactly once) and shows no banner.
                awaitItem()
                val state = viewModel.uiState.value
                assertThat(state.status).isEqualTo(GameStatus.Won)
                assertThat(state.stuck).isFalse()
                assertThat(state.showRescueBanner).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun undoingOutOfAStuckBoardResumesTheTickerAndClearsTheRescueFlag() =
        runTest(testDispatcher) {
            val timeSource = TestTimeSource()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource = timeSource)
            runCurrent()

            // Move A starts the timer (Playing) and it ticks to 2s.
            viewModel.onCellTap(Position(row = 0, col = 2))
            viewModel.onCellTap(Position(row = 0, col = 4))
            runCurrent()
            timeSource += 2.seconds
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(2)

            // Move B reaches Stuck → the ticker freezes.
            viewModel.onCellTap(Position(row = 0, col = 0))
            viewModel.onCellTap(Position(row = 0, col = 2))
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)
            timeSource += 5.seconds
            advanceTimeBy(FIVE_SECONDS_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(2) // frozen

            // Undo rescues to Playing → the rescue flag clears and the ticker RESUMES,
            // reflecting the real elapsed time since the (never-reset) mark (D-07).
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)
            assertThat(viewModel.uiState.value.stuck).isFalse()
            assertThat(viewModel.uiState.value.showRescueBanner).isFalse()

            timeSource += 1.seconds
            advanceTimeBy(ONE_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(EIGHT_SECONDS)

            cancelTickers()
        }

    @Test
    fun stuckThenUndoThenReSolveToWonEmitsExactlyOneFinishedEvent() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(forkBoardWithStuckTrap())
            viewModel.finished.test {
                expectNoEvents()

                // A wrong line strands a peg → Stuck (no emission; the guard is untouched).
                jump(viewModel, from = Position(row = 2, col = 2), to = Position(row = 2, col = 4))
                jump(viewModel, from = Position(row = 3, col = 3), to = Position(row = 3, col = 1))
                runCurrent()
                assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)
                expectNoEvents()

                // Undo all the way back to the initial board (finishedEmitted must reset).
                viewModel.onUndo()
                runCurrent()
                viewModel.onUndo()
                runCurrent()
                assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)
                assertThat(viewModel.uiState.value.canUndo).isFalse()

                // Re-solve to one peg → a finished event fires exactly once (guard reset).
                jump(viewModel, from = Position(row = 3, col = 3), to = Position(row = 3, col = 1))
                jump(viewModel, from = Position(row = 2, col = 3), to = Position(row = 2, col = 1))
                jump(viewModel, from = Position(row = 3, col = 1), to = Position(row = 1, col = 1))
                runCurrent()

                val result = awaitItem()
                assertThat(result.pegsRemaining).isEqualTo(1)
                assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun noWinningMoveIsUndoneBackToAPlayableRescueFreeBoard() =
        runTest(testDispatcher) {
            // twoMoveStuckLine is Playing but unwinnable: its only line strands two pegs, so a
            // hint folds to no-win, raising the rescue banner with a still-Playing status.
            val viewModel = viewModelFor(twoMoveStuckLine())
            runCurrent()

            // Make the one available first move (still Playing) so there is a move to undo.
            viewModel.onCellTap(Position(row = 0, col = 2))
            viewModel.onCellTap(Position(row = 0, col = 4))
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Playing)

            viewModel.onHintRequested()
            // runCurrent (not advanceUntilIdle): the board is still Playing after the move, so
            // the 1s timer ticker is live — advanceUntilIdle would advance virtual time forever
            // and spin. runCurrent drains the queued off-thread hint job (which folds to no-win)
            // without touching the pending ticker delay.
            runCurrent()
            assertThat(viewModel.uiState.value.noWinningMove).isTrue()
            assertThat(viewModel.uiState.value.showRescueBanner).isTrue()

            // Undo clears both the no-win flag and the derived rescue banner (D-08).
            viewModel.onUndo()
            runCurrent()
            val state = viewModel.uiState.value
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.stuck).isFalse()
            assertThat(state.showRescueBanner).isFalse()

            cancelTickers()
        }

    // ---- Hint (ASST-01/ASST-03, D-01/D-03/D-04/D-10) ------------------------

    @Test
    fun hintOnTheRecordedLineReturnsTheRecordedMoveWithoutSolverLatency() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(Boards.english33Initial, challengeLevel = FIRST_CHALLENGE_LEVEL)
            runCurrent() // finish the async Challenge load (board + recorded solution)

            viewModel.onHintRequested()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // The generator is deterministic, so the recorded first move here is exactly
            // the one the VM captured; proving hintSource/hintTarget == solution[0] proves
            // the recorded branch was taken (no Solver search).
            val recordedFirst = LevelGenerator.generate(FIRST_CHALLENGE_LEVEL).solution.first()
            assertThat(state.hintSource).isEqualTo(recordedFirst.from)
            assertThat(state.hintTarget).isEqualTo(recordedFirst.to)
            assertThat(state.solverThinking).isFalse()
            assertThat(state.noWinningMove).isFalse()

            cancelTickers()
        }

    @Test
    fun hintOnClassicSetsSolverThinkingThenResolvesToALegalHighlightAndAJumpClearsIt() =
        runTest(testDispatcher) {
            // A tiny solvable Classic strip keeps the Solver fallback instant.
            val viewModel = viewModelFor(wonInOneJumpBoard())
            runCurrent()

            viewModel.onHintRequested()
            // solverThinking is raised synchronously, before the off-thread search runs.
            assertThat(viewModel.uiState.value.solverThinking).isTrue()

            advanceUntilIdle()
            val hinted = viewModel.uiState.value
            assertThat(hinted.solverThinking).isFalse()
            assertThat(hinted.hintSource).isEqualTo(Position(row = 0, col = 0))
            assertThat(hinted.hintTarget).isEqualTo(Position(row = 0, col = 2))
            // The highlighted move is a legal move on the current board (gameplay authority).
            val hintedMove =
                RulesEngine
                    .legalMoves(hinted.board)
                    .firstOrNull { it.from == hinted.hintSource && it.to == hinted.hintTarget }
            assertThat(hintedMove).isNotNull()

            // Performing a jump clears the shown highlight.
            viewModel.onCellTap(Position(row = 0, col = 0))
            viewModel.onCellTap(Position(row = 0, col = 2))
            runCurrent()
            val afterJump = viewModel.uiState.value
            assertThat(afterJump.hintSource).isNull()
            assertThat(afterJump.hintTarget).isNull()
        }

    @Test
    fun hintRequestsTheHintGrantThroughTheAdGateway() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()
            val viewModel = viewModelFor(wonInOneJumpBoard(), adGateway = gateway)
            runCurrent()

            viewModel.onHintRequested()
            advanceUntilIdle()

            assertThat(gateway.lastReason).isEqualTo(AdReason.HINT)
        }

    @Test
    fun hintDeniedByTheGatewayDoesNotProceedAndClearsThinking() =
        runTest(testDispatcher) {
            val denied = FakeAdGateway(grant = false)
            val viewModel = viewModelFor(wonInOneJumpBoard(), adGateway = denied)
            runCurrent()

            viewModel.onHintRequested()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.solverThinking).isFalse()
        }

    @Test
    fun hintOnAnUnsolvablePositionSetsNoWinningMove() =
        runTest(testDispatcher) {
            // Two pegs separated by an empty hole: no legal jump → Solver returns null.
            val spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null)
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 2))
            val viewModel = viewModelFor(board)
            runCurrent()

            viewModel.onHintRequested()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.noWinningMove).isTrue()
            assertThat(state.hintSource).isNull()
            assertThat(state.solverThinking).isFalse()
        }

    @Test
    fun aSecondHintRequestCancelsThePriorSearchSoOnlyOneProceeds() =
        runTest(testDispatcher) {
            // The gateway gates every grant until released, so both hint jobs launch and
            // suspend; only the SECOND (uncancelled) job may proceed past the gate.
            val gateway = GatedAdGateway()
            val viewModel = viewModelFor(wonInOneJumpBoard(), adGateway = gateway)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gateway.started).isEqualTo(1)

            // Second request cancels the first in-flight job before releasing the gate.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(gateway.started).isEqualTo(2)

            gateway.release()
            advanceUntilIdle()

            // Exactly one job proceeded past the grant — the prior search was cancelled.
            assertThat(gateway.proceeded).isEqualTo(1)
            assertThat(viewModel.uiState.value.hintSource).isEqualTo(Position(row = 0, col = 0))
            assertThat(viewModel.uiState.value.solverThinking).isFalse()

            cancelTickers()
        }

    @Test
    fun deviatingFromTheRecordedLineFlipsOnRecordedLine() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(Boards.english33Initial, challengeLevel = CHALLENGE_LEVEL_WITH_CHOICES)
            runCurrent()
            assertThat(viewModel.isOnRecordedLine()).isTrue()

            val recordedFirst =
                LevelGenerator.generate(CHALLENGE_LEVEL_WITH_CHOICES).solution.first()
            // Pick a LEGAL move that is NOT the recorded first move — a genuine deviation.
            val deviating =
                RulesEngine.legalMoves(viewModel.uiState.value.board).first { it != recordedFirst }
            viewModel.onCellTap(deviating.from)
            viewModel.onCellTap(deviating.to)
            runCurrent()

            assertThat(viewModel.isOnRecordedLine()).isFalse()

            cancelTickers()
        }

    @Test
    fun undoRecomputesOnRecordedLineAndClearsTheHint() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(Boards.english33Initial, challengeLevel = CHALLENGE_LEVEL_WITH_CHOICES)
            runCurrent()

            val recordedFirst =
                LevelGenerator.generate(CHALLENGE_LEVEL_WITH_CHOICES).solution.first()
            val deviating =
                RulesEngine.legalMoves(viewModel.uiState.value.board).first { it != recordedFirst }
            viewModel.onCellTap(deviating.from)
            viewModel.onCellTap(deviating.to)
            runCurrent()
            assertThat(viewModel.isOnRecordedLine()).isFalse()

            // Undo walks back to the start, so the moveHistory prefix matches the line again.
            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.isOnRecordedLine()).isTrue()
            val state = viewModel.uiState.value
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()

            cancelTickers()
        }

    // ---- Preserved Phase-1 behaviors ----------------------------------------

    @Test
    fun selectingPegHighlightsItsLegalDestinations() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.selected).isNull()
                assertThat(initial.legalTargets).isEmpty()

                viewModel.onCellTap(pegAboveCenter)

                val selected = awaitItem()
                assertThat(selected.selected).isEqualTo(pegAboveCenter)
                assertThat(selected.legalTargets).containsExactly(centerHole)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reTappingTheSelectedPegDeselectsIt() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()

                viewModel.onCellTap(pegAboveCenter)

                val deselected = awaitItem()
                assertThat(deselected.selected).isNull()
                assertThat(deselected.legalTargets).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tappingADifferentPegWhileSelectedIsANoOp() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                val selected = awaitItem()

                // A DIFFERENT peg while one is already selected must not switch selection.
                viewModel.onCellTap(pegLeftOfCenter)

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tappingALegalDestinationPerformsTheJumpAndAnimates() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            val pegsBefore = Boards.english33Initial.pegCount
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()

                viewModel.onCellTap(centerHole)

                val jumped = awaitItem()
                assertThat(jumped.board.pegCount).isEqualTo(pegsBefore - 1)
                assertThat(jumped.selected).isNull()
                assertThat(jumped.legalTargets).isEmpty()
                assertThat(jumped.status).isEqualTo(GameStatus.Playing)
                val transition = jumped.animating
                assertThat(transition).isNotNull()
                requireNotNull(transition)
                assertThat(transition.from).isEqualTo(pegAboveCenter)
                assertThat(transition.to).isEqualTo(centerHole)
                assertThat(transition.removed).isEqualTo(Position(row = 2, col = 3))
                assertThat(transition.id).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
            cancelTickers() // board stays Playing; stop the eager ticker so runTest can finish.
        }

    @Test
    fun animationFinishedClearsTheTransition() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                assertThat(awaitItem().animating).isNotNull()

                viewModel.onAnimationFinished()

                assertThat(awaitItem().animating).isNull()
                cancelAndIgnoreRemainingEvents()
            }
            cancelTickers() // board stays Playing; stop the eager ticker so runTest can finish.
        }

    @Test
    fun tappingABlockedCellWhileSelectedIsANoOp() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                val selected = awaitItem()

                // A 2x2 corner coordinate is outside the English-33 footprint.
                viewModel.onCellTap(Position(row = 0, col = 0))

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aDestinationNotInLegalMovesIsNeverApplied() =
        runTest(testDispatcher) {
            // A 1x5 strip: pegs at the two left cells, the rest empty. Selecting
            // the leftmost peg yields exactly one legal target (col 2); tapping a
            // farther empty hole (col 4) is a forged, illegal destination.
            val strip = Position(row = 0, col = 0)
            val board =
                Board(spec = horizontalLine(length = 5), pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
            val viewModel = viewModelFor(board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(strip)
                val selected = awaitItem()
                assertThat(selected.legalTargets).containsExactly(Position(row = 0, col = 2))

                // Forge: an empty hole that is NOT a legal destination.
                viewModel.onCellTap(Position(row = 0, col = 4))

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                assertThat(viewModel.uiState.value.board.pegCount).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun clearingTheBoardToOnePegReportsWon() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(wonInOneJumpBoard())

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))

                val won = awaitItem()
                assertThat(won.status).isEqualTo(GameStatus.Won)
                assertThat(won.board.pegCount).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reachingADeadEndWithMultiplePegsReportsStuck() =
        runTest(testDispatcher) {
            // Strip cols 0,1,2 plus an isolated cell (2,0). After the only jump,
            // the lone peg at (2,0) survives with no neighbors → Stuck (2 pegs).
            val spec =
                BoardSpec(
                    inPlay =
                        listOf(
                            Position(row = 0, col = 0),
                            Position(row = 0, col = 1),
                            Position(row = 0, col = 2),
                            Position(row = 2, col = 0),
                        ),
                    target = null,
                )
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
                    .withPeg(Position(row = 2, col = 0))
            val viewModel = viewModelFor(board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))

                val stuck = awaitItem()
                assertThat(stuck.status).isEqualTo(GameStatus.Stuck)
                assertThat(stuck.board.pegCount).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun midGameRestartAsksForConfirmationThenResets() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                val afterJump = awaitItem()
                viewModel.onAnimationFinished()
                awaitItem()

                viewModel.onRestart()

                val confirming = awaitItem()
                assertThat(confirming.confirmRestart).isTrue()
                // Board is NOT reset yet — still the post-jump board.
                assertThat(confirming.board).isEqualTo(afterJump.board)

                viewModel.onConfirmRestart()

                val reset = awaitItem()
                assertThat(reset.confirmRestart).isFalse()
                assertThat(reset.board).isEqualTo(Boards.english33Initial)
                assertThat(reset.selected).isNull()
                assertThat(reset.status).isEqualTo(GameStatus.Playing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissingTheRestartConfirmationKeepsTheGame() =
        runTest(testDispatcher) {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                val afterJump = awaitItem()
                viewModel.onAnimationFinished()
                awaitItem()
                viewModel.onRestart()
                awaitItem()

                viewModel.onDismissRestart()

                val kept = awaitItem()
                assertThat(kept.confirmRestart).isFalse()
                assertThat(kept.board).isEqualTo(afterJump.board)
                cancelAndIgnoreRemainingEvents()
            }
            cancelTickers() // board stays Playing; stop the eager ticker so runTest can finish.
        }

    @Test
    fun restartFromAnEndStateResetsWithoutConfirmation() =
        runTest(testDispatcher) {
            val board = wonInOneJumpBoard()
            val viewModel = viewModelFor(board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))
                assertThat(awaitItem().status).isEqualTo(GameStatus.Won)

                viewModel.onRestart()

                val reset = awaitItem()
                assertThat(reset.confirmRestart).isFalse()
                assertThat(reset.status).isEqualTo(GameStatus.Playing)
                assertThat(reset.board).isEqualTo(board)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Helpers -------------------------------------------------------------

    /** Selects [from] then taps [to] — the two-tap gesture the VM turns into one jump. */
    private fun jump(
        viewModel: GameViewModel,
        from: Position,
        to: Position,
    ) {
        viewModel.onCellTap(from)
        viewModel.onCellTap(to)
    }

    private fun englishViewModel(): GameViewModel = viewModelFor(Boards.english33Initial)

    private fun viewModelFor(
        board: Board,
        levelId: String = ClassicLevelIds.ENGLISH33,
        challengeLevel: Int? = null,
        timeSource: TimeSource = TestTimeSource(),
        settings: SettingsRepository = FakeSettingsRepository(),
        adGateway: AdGateway = FakeAdGateway(),
    ): GameViewModel {
        val args =
            buildMap<String, Any?> {
                put(LEVEL_ID_ARG, levelId)
                if (challengeLevel != null) put(CHALLENGE_LEVEL_ARG, challengeLevel)
            }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = FakeLevelRepository(boardsById = mapOf(levelId to board)),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = settings,
                        timeSource = timeSource,
                        defaultDispatcher = testDispatcher,
                        adGateway = adGateway,
                        getHintUseCase = GetHintUseCase(),
                        savedStateHandle = SavedStateHandle(args),
                    ) as T
            }
        return ViewModelProvider(viewModelStore, factory)["vm${viewModelKey++}", GameViewModel::class.java]
    }

    /**
     * Stops every running ticker by clearing the store (cancels each `viewModelScope`).
     * A few tests leave the board mid-game (status stays `Playing`), so the eager
     * per-second ticker would otherwise loop forever and hang `runTest` cleanup; those
     * tests call this before returning so the scheduler can go idle.
     */
    private fun cancelTickers() = viewModelStore.clear()

    /** A 1x3 strip (pegs at cols 0,1; hole at col 2): one jump leaves a lone peg → Won. */
    private fun wonInOneJumpBoard(): Board =
        Board(spec = horizontalLine(length = 3), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))

    /**
     * A 1x5 strip filled cols 0..3 (hole at col 4). Two jumps
     * (col2→col4, then col0→col2) strand two non-adjacent pegs → Stuck.
     */
    private fun twoMoveStuckLine(): Board =
        Board(spec = horizontalLine(length = 5), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))
            .withPeg(Position(row = 0, col = 2))
            .withPeg(Position(row = 0, col = 3))

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(
            inPlay = (0 until length).map { Position(row = 0, col = it) },
            target = null,
        )

    /**
     * A small board that is solvable to one peg BUT has a losing fork, so a test can drive it
     * into Stuck, undo, then re-solve to Won on the same board (proving the finished guard is
     * reset). Cells form a 2×2 peg block (rows 2–3, cols 2–3) over an L of empty holes plus a
     * spare hole at (2,4).
     *
     * Winning line: (3,3)→(3,1), (2,3)→(2,1), (3,1)→(1,1) ⇒ one peg (Won).
     * Losing fork:  (2,2)→(2,4), then (3,3)→(3,1) strands two pegs ⇒ Stuck.
     */
    private fun forkBoardWithStuckTrap(): Board {
        val cells =
            listOf(
                Position(row = 1, col = 1),
                Position(row = 2, col = 1),
                Position(row = 3, col = 1),
                Position(row = 2, col = 2),
                Position(row = 2, col = 3),
                Position(row = 3, col = 2),
                Position(row = 3, col = 3),
                Position(row = 2, col = 4),
            )
        return Board(spec = BoardSpec(inPlay = cells, target = null), pegs = 0L)
            .withPeg(Position(row = 2, col = 2))
            .withPeg(Position(row = 2, col = 3))
            .withPeg(Position(row = 3, col = 2))
            .withPeg(Position(row = 3, col = 3))
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The `Gameplay(levelId)` route arg key (the route's property name). */
private const val LEVEL_ID_ARG = "levelId"

/** The `Gameplay(challengeLevel)` route arg key (the route's property name). */
private const val CHALLENGE_LEVEL_ARG = "challengeLevel"

/** A second valid catalog id used to prove board selection is levelId-driven. */
private const val OTHER_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** The first Challenge level (the on-ramp), used to prove the challenge load path. */
private const val FIRST_CHALLENGE_LEVEL = 1

/**
 * A Challenge level whose generated board offers several legal opening moves, so a
 * genuine deviation from the recorded first move is always available (the level-1
 * on-ramp can be too small to deviate from).
 */
private const val CHALLENGE_LEVEL_WITH_CHOICES = 10

/** A forged sub-1 Challenge nav arg that must clamp to level 1 (T-03-09). */
private const val BELOW_MIN_CHALLENGE_LEVEL = 0

/** One timer tick (1s) of virtual time, plus a margin so the `delay(1000)` fires. */
private const val ONE_TICK_MILLIS = 1_001L

/** Two seconds of virtual time. */
private const val TWO_SECONDS_MILLIS = 2_000L

/** Five seconds of virtual time. */
private const val FIVE_SECONDS_MILLIS = 5_000L

/** Total real elapsed after a rescue-undo resumes the timer (2s + 5s frozen + 1s). */
private const val EIGHT_SECONDS = 8

/**
 * A minimal in-memory [LevelRepository] for ViewModel tests: returns the board
 * registered under each id, so `loadClassic(levelId)` is deterministic without the
 * Android-resource-backed real source.
 */
private class FakeLevelRepository(
    private val boardsById: Map<String, Board>,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level {
        val board = boardsById.getValue(levelId)
        return Level(
            id = levelId,
            displayName = levelId,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )
    }

    override fun loadChallenge(levelNumber: Int): Level {
        // Route Challenge loads through the real pure-domain generator (as the
        // production GeneratedChallengeLevelSource does) so the VM's challenge path
        // is exercised against a genuine solvable-by-construction board.
        val clamped = levelNumber.coerceAtLeast(1)
        val generated = LevelGenerator.generate(clamped)
        val spec = generated.startingBoard.spec
        return Level(
            id = "challenge-$clamped",
            displayName = "challenge-$clamped",
            spec = spec,
            initialBoard = generated.startingBoard,
            holeCount = spec.inPlay.size,
            solution = generated.solution,
            generatorVersion = generated.generatorVersion,
        )
    }
}

/**
 * An in-memory [SettingsRepository] backed by a `MutableStateFlow` so a write both
 * records the last-written value ([lastWritten]) and re-emits through [hintsEnabled] —
 * letting the collected flow drive `showHints` exactly as the DataStore-backed impl does.
 */
private class FakeSettingsRepository(
    hintsEnabled: Boolean = true,
    soundOn: Boolean = true,
    hapticsOn: Boolean = true,
) : SettingsRepository {
    private val state = MutableStateFlow(hintsEnabled)
    private val soundState = MutableStateFlow(soundOn)
    private val hapticsState = MutableStateFlow(hapticsOn)

    override val hintsEnabled: Flow<Boolean> = state.asStateFlow()

    override val soundOn: Flow<Boolean> = soundState.asStateFlow()

    override val hapticsOn: Flow<Boolean> = hapticsState.asStateFlow()

    var lastWritten: Boolean? = null
        private set

    override suspend fun setHintsEnabled(enabled: Boolean) {
        lastWritten = enabled
        state.value = enabled
    }

    override suspend fun setSoundOn(on: Boolean) {
        soundState.value = on
    }

    override suspend fun setHapticsOn(on: Boolean) {
        hapticsState.value = on
    }
}

/**
 * A fake [AdGateway] that records the last requested [AdReason] and returns a
 * configurable [grant] (default `true`, matching the Slice-1 auto-grant stub), so the
 * VM tests assert both that undo requests the grant and that a denial skips the undo.
 */
private class FakeAdGateway(
    private val grant: Boolean = true,
) : AdGateway {
    var lastReason: AdReason? = null
        private set

    override suspend fun requestRewarded(reason: AdReason): Boolean {
        lastReason = reason
        return grant
    }

    override fun showInterstitial() = Unit
}

/**
 * An [AdGateway] whose [requestRewarded] suspends on a shared gate until [release],
 * counting how many grants [started] (entered the gate) and how many [proceeded] past
 * it. A cancelled hint job's suspended `await` throws before [proceeded] increments, so
 * `proceeded == 1` after two rapid requests proves the first search was cancelled (no
 * overlapping Solver work — the logic half of D-10).
 */
private class GatedAdGateway : AdGateway {
    private val gate = CompletableDeferred<Unit>()

    var started = 0
        private set

    var proceeded = 0
        private set

    fun release() {
        gate.complete(Unit)
    }

    override suspend fun requestRewarded(reason: AdReason): Boolean {
        started++
        gate.await()
        proceeded++
        return true
    }

    override fun showInterstitial() = Unit
}
