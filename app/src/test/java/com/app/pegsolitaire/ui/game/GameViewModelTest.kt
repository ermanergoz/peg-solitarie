package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
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
        timeSource: TimeSource = TestTimeSource(),
    ): GameViewModel =
        GameViewModel(
            rulesEngine = RulesEngine,
            levelRepository = FakeLevelRepository(boardsById = mapOf(levelId to board)),
            scoreCalculator = ScoreCalculator,
            timeSource = timeSource,
            savedStateHandle = SavedStateHandle(mapOf(LEVEL_ID_ARG to levelId)),
        )

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

/** A second valid catalog id used to prove board selection is levelId-driven. */
private const val OTHER_LEVEL_ID = ClassicLevelIds.EUROPEAN37

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

    override fun loadChallenge(levelNumber: Int): Level = error("Challenge is a Phase-3 stub")
}
