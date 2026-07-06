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
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/** A forged sub-1 Challenge nav arg that must clamp to level 1 (T-03-09). */
private const val BELOW_MIN_CHALLENGE_LEVEL = 0

/** One timer tick (1s) of virtual time, plus a margin so the `delay(1000)` fires. */
private const val ONE_TICK_MILLIS = 1_001L

/** Two seconds of virtual time. */
private const val TWO_SECONDS_MILLIS = 2_000L

/** Five seconds of virtual time. */
private const val FIVE_SECONDS_MILLIS = 5_000L

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
) : SettingsRepository {
    private val state = MutableStateFlow(hintsEnabled)

    override val hintsEnabled: Flow<Boolean> = state.asStateFlow()

    var lastWritten: Boolean? = null
        private set

    override suspend fun setHintsEnabled(enabled: Boolean) {
        lastWritten = enabled
        state.value = enabled
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
