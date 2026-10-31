package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.HomeProgress
import com.app.pegsolitaire.data.ImportOutcome
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.domain.usecase.HintOutcome
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
 * The in-progress resume contract (PERS-03, D-04/D-05/D-06) at the ViewModel layer.
 *
 * Four truths are pinned here, each of which a device playtest would otherwise be the
 * only witness to:
 *  - **Leaving a Playing game saves; a finished one never does** (Pitfall 4) — a stale
 *    Continue must not survive a solve.
 *  - **Continue restores by replay, never by regeneration** (D-06) — a counting
 *    [LevelRepository] proves the generator is not called on the resume path, and the
 *    rebuilt board, the full history (undo works immediately) and the elapsed time all
 *    come back exactly as they were left.
 *  - **A corrupt or absent snapshot degrades to the normal load** (T-05-03) — Continue
 *    never crashes on untrusted stored state.
 *  - **The snapshot is cleared on solve and on restart** (D-05), and leaving to Home
 *    deliberately does not clear it.
 *
 * The [RecordingProgressRepository] stand-in stores real [ResumeSnapshotDto]s, so both
 * halves of the round trip run through the production `SnapshotMapper` encode/rebuild
 * path rather than a hand-built shortcut.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESUME_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent / advanceTimeBy virtual-time control
class GameViewModelResumeTest {
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0
    private val progressRepository = RecordingProgressRepository()
    private lateinit var levelRepository: CountingLevelRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- Save on leave (D-04, Pitfall 4) -------------------------------------

    @Test
    fun leavingAPlayingGameSavesTheHistoryAndTheElapsedTime() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            val viewModel = viewModelFor(sixPegStrip(), timeSource = time)
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            tickTo(time, PLAYED_SECONDS)

            viewModel.onLeaveScreen()
            runCurrent()

            val saved = progressRepository.snapshots[ProgressPersistenceKeys.MODE_CLASSIC]
            assertThat(saved).isNotNull()
            assertThat(saved!!.moves).hasSize(1)
            assertThat(saved.initialPegs).isEqualTo(sixPegStrip().pegs)
            assertThat(saved.elapsedMs).isEqualTo(PLAYED_SECONDS * MILLIS_IN_SECOND)
            // Leaving to Home keeps the game resumable — only a solve/restart clears (D-05).
            assertThat(progressRepository.clears).isEmpty()
            cancelTickers()
        }

    @Test
    fun leavingASolvedGameSavesNothing() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(wonInOneJumpBoard())
            jump(viewModel, fromCol = 0, toCol = WIN_JUMP_TO_COL)
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)

            viewModel.onLeaveScreen()
            runCurrent()

            // A finished board must never become a Continue affordance (Pitfall 4).
            assertThat(progressRepository.saves).isEmpty()
        }

    // ---- Restore on Continue (D-06) ------------------------------------------

    @Test
    fun continueRestoresTheExactBoardAndTheElapsedTime() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            val left = playTwoMovesAndLeave(time)

            val resumed = viewModelFor(sixPegStrip(), resume = true)
            runCurrent()
            val state = resumed.uiState.value

            assertThat(state.board.pegs).isEqualTo(left.board.pegs)
            assertThat(state.board.spec.inPlay).containsExactlyElementsIn(left.board.spec.inPlay)
            assertThat(state.elapsedSeconds).isEqualTo(PLAYED_SECONDS)
            assertThat(state.status).isEqualTo(GameStatus.Playing)
            assertThat(state.canUndo).isTrue()
            assertThat(state.isLoading).isFalse()
            cancelTickers()
        }

    @Test
    fun undoStillWorksImmediatelyAfterAResume() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            val left = playTwoMovesAndLeave(time)

            val resumed = viewModelFor(sixPegStrip(), resume = true)
            runCurrent()

            resumed.onUndo()
            runCurrent()

            // The restored history is real history: one undo steps back to the board as it
            // stood after the FIRST move, and a second move remains undoable.
            assertThat(resumed.uiState.value.board.pegs).isEqualTo(left.afterFirstMovePegs)
            assertThat(resumed.uiState.value.canUndo).isTrue()
            cancelTickers()
        }

    @Test
    fun resumingAChallengeGameNeverCallsTheGenerator() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            val viewModel =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, timeSource = time)
            runCurrent() // the async Challenge load
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            tickTo(time, PLAYED_SECONDS)
            viewModel.onLeaveScreen()
            runCurrent()
            cancelTickers()
            val loadsBeforeResume = levelRepository.challengeLoads
            assertThat(loadsBeforeResume).isEqualTo(1)

            val resumed =
                viewModelFor(
                    sixPegStrip(),
                    challengeLevel = PLAYED_CHALLENGE_LEVEL,
                    resume = true,
                )
            runCurrent()

            // The board came off disk by replay — the O(level) generator was never asked
            // for it again (D-06).
            assertThat(levelRepository.challengeLoads).isEqualTo(loadsBeforeResume)
            assertThat(resumed.uiState.value.canUndo).isTrue()
            assertThat(resumed.uiState.value.isLoading).isFalse()
            cancelTickers()
        }

    // ---- Restored Challenge identity (WR-03) ---------------------------------

    @Test
    fun aRestoredChallengeGameTakesItsLevelFromTheSnapshotStampNotTheRoute() =
        runTest(testDispatcher) {
            // Save a level-7 Challenge game.
            val time = TestTimeSource()
            val viewModel =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, timeSource = time)
            runCurrent()
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            tickTo(time, PLAYED_SECONDS)
            viewModel.onLeaveScreen()
            runCurrent()
            cancelTickers()

            // Cold start: Home's ladder flow has not emitted yet, so the Continue tap can
            // navigate with the stale initial level — the snapshot's stamp must win
            // (WR-03): it names the level the restored board actually is.
            val resumed =
                viewModelFor(sixPegStrip(), challengeLevel = STALE_ROUTE_LEVEL, resume = true)
            runCurrent()

            assertThat(resumed.challengeLevel).isEqualTo(PLAYED_CHALLENGE_LEVEL)

            // Leaving again re-saves under the stamped level too, not the stale route's —
            // otherwise one background trip would corrupt the snapshot's own provenance.
            resumed.onLeaveScreen()
            runCurrent()
            val saved = progressRepository.snapshots[ProgressPersistenceKeys.MODE_CHALLENGE]
            assertThat(saved!!.challengeLevel).isEqualTo(PLAYED_CHALLENGE_LEVEL)
            assertThat(saved.levelId).isEqualTo(PLAYED_CHALLENGE_LEVEL.toString())
            cancelTickers()
        }

    @Test
    fun aForgedSnapshotStampStillWinsTheRouteButIsItselfClamped() =
        runTest(testDispatcher) {
            // WARN-03, stored-state side. The stamp beats the route arg (WR-03), so if the
            // stamp were trusted verbatim the getter would be unclamped again by another
            // door — this time one that survives a reboot.
            val time = TestTimeSource()
            val viewModel =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, timeSource = time)
            runCurrent()
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            viewModel.onLeaveScreen()
            runCurrent()
            cancelTickers()

            progressRepository.tamperWithChallengeLevel(
                ProgressPersistenceKeys.MODE_CHALLENGE,
                forgedLevel = Int.MAX_VALUE,
            )

            val resumed =
                viewModelFor(sixPegStrip(), challengeLevel = STALE_ROUTE_LEVEL, resume = true)
            runCurrent()

            assertThat(resumed.challengeLevel).isEqualTo(GeneratorConstants.MAX_CHALLENGE_LEVEL)
            assertThat(resumed.challengeLevel).isNotEqualTo(STALE_ROUTE_LEVEL)
            cancelTickers()
        }

    // ---- In-flight restore window (WR-01) ------------------------------------

    @Test
    fun leavingDuringAnInFlightRestoreNeverOverwritesTheStoredSnapshot() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            playTwoMovesAndLeave(time)
            val savesBefore = progressRepository.saves.size

            val resumed = viewModelFor(sixPegStrip(), resume = true)
            // The restore has not resolved yet: the screen is loading, not a fresh board.
            assertThat(resumed.uiState.value.isLoading).isTrue()

            // An ON_STOP in this window (player backgrounds right after tapping Continue)
            // must save NOTHING — the stored snapshot is the very game being restored.
            resumed.onLeaveScreen()
            runCurrent()

            assertThat(progressRepository.saves).hasSize(savesBefore)
            val stored = progressRepository.snapshots[ProgressPersistenceKeys.MODE_CLASSIC]
            assertThat(stored!!.moves).hasSize(2)
            // ...and the restore then lands intact, history and all.
            assertThat(resumed.uiState.value.isLoading).isFalse()
            assertThat(resumed.uiState.value.canUndo).isTrue()
            cancelTickers()
        }

    @Test
    fun tapsDuringAnInFlightRestoreAreIgnoredNotSilentlyDiscarded() =
        runTest(testDispatcher) {
            val time = TestTimeSource()
            val left = playTwoMovesAndLeave(time)

            val resumed = viewModelFor(sixPegStrip(), resume = true)
            // A fast tap before the restore resolves must not play a move on a board the
            // restore would then silently replace (WR-01): the loading placeholder has no
            // tappable cells, so the tap is a no-op.
            resumed.onCellTap(Position(row = 0, col = FIRST_JUMP_FROM_COL))
            runCurrent()

            assertThat(resumed.uiState.value.board.pegs).isEqualTo(left.board.pegs)
            assertThat(resumed.uiState.value.selected).isNull()
            cancelTickers()
        }

    // ---- Fail-safe restore (T-05-03/T-05-04) ---------------------------------

    @Test
    fun anAbsentSnapshotFallsBackToTheNormalLoad() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, resume = true)
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.board.pegs).isEqualTo(sixPegStrip().pegs)
            assertThat(state.canUndo).isFalse()
            assertThat(levelRepository.challengeLoads).isEqualTo(1)
            cancelTickers()
        }

    @Test
    fun aTamperedSnapshotFallsBackToTheNormalLoadInsteadOfCrashing() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, timeSource = TestTimeSource())
            runCurrent()
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            viewModel.onLeaveScreen()
            runCurrent()
            cancelTickers()
            progressRepository.tamperWithMoves(ProgressPersistenceKeys.MODE_CHALLENGE)

            val resumed =
                viewModelFor(sixPegStrip(), challengeLevel = PLAYED_CHALLENGE_LEVEL, resume = true)
            runCurrent()

            // A forged history is rejected wholesale (T-05-04) and Continue degrades to a
            // fresh load rather than throwing (T-05-03).
            assertThat(resumed.uiState.value.canUndo).isFalse()
            assertThat(resumed.uiState.value.board.pegs).isEqualTo(sixPegStrip().pegs)
            // WR-02: the unusable row is purged, so the Continue affordance disappears
            // instead of forever restarting a fresh board from a phantom tile.
            assertThat(progressRepository.clears).contains(ProgressPersistenceKeys.MODE_CHALLENGE)
            cancelTickers()
        }

    @Test
    fun continueWithAnUnknownClassicLevelIdRestoresTheSnapshotInsteadOfCrashing() =
        runTest(testDispatcher) {
            // CR-01: the resume route's levelId comes from the stored blob — an id the
            // catalog no longer knows previously hit the fail-fast lookup synchronously in
            // the CONSTRUCTOR, crashing on the Continue tap. A valid snapshot needs no
            // catalog lookup at all: the board on disk wins.
            val time = TestTimeSource()
            val left = playTwoMovesAndLeave(time)

            val resumed = viewModelFor(sixPegStrip(), resume = true, levelId = UNKNOWN_LEVEL_ID)
            runCurrent()

            assertThat(resumed.uiState.value.board.pegs).isEqualTo(left.board.pegs)
            assertThat(resumed.uiState.value.canUndo).isTrue()
            cancelTickers()
        }

    @Test
    fun continueWithAnUnknownClassicLevelIdAndNoSnapshotFallsBackToADefaultBoard() =
        runTest(testDispatcher) {
            // CR-01 defense-in-depth: nothing to restore AND the id is unknown — the
            // fallback load degrades to the default board rather than throwing.
            val viewModel = viewModelFor(sixPegStrip(), resume = true, levelId = UNKNOWN_LEVEL_ID)
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.board.pegs).isEqualTo(Boards.english33Initial.pegs)
            assertThat(state.status).isEqualTo(GameStatus.Playing)
            cancelTickers()
        }

    // ---- Clear on solve / restart (D-05) -------------------------------------

    @Test
    fun solvingClearsTheModesSnapshot() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(wonInOneJumpBoard())

            jump(viewModel, fromCol = 0, toCol = WIN_JUMP_TO_COL)
            runCurrent()

            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)
            assertThat(progressRepository.clears).contains(ProgressPersistenceKeys.MODE_CLASSIC)
        }

    @Test
    fun restartingClearsTheModesSnapshot() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(sixPegStrip())
            jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
            runCurrent()

            viewModel.onRestart()
            viewModel.onConfirmRestart()
            runCurrent()

            assertThat(progressRepository.clears).contains(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(viewModel.uiState.value.canUndo).isFalse()
            cancelTickers()
        }

    // ---- The Undecided notice is never part of the snapshot (GD-HT-2) -------

    /**
     * AC-15 and criterion 23g's resume half: the notice is transient. A game left — and its
     * process killed — while the notice is up comes back on its board with neither the
     * notice nor the rescue banner.
     *
     * The body runs inside `try`/`finally`: both boards are mid-game, so their tickers are
     * live, and a failed assertion that skipped [cancelTickers] would leave `runTest`'s
     * end-of-test drain spinning on virtual time instead of reporting the failure.
     */
    @Test
    fun theUndecidedNoticeIsNotRestoredOnResume() =
        runTest(testDispatcher) {
            try {
                val undecided = mockk<GetHintUseCase>()
                every { undecided.invoke(any(), any(), any(), any(), any()) } returns HintOutcome.Undecided
                val left = viewModelFor(sixPegStrip(), getHintUseCase = undecided)
                jump(left, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
                runCurrent()
                left.onHintRequested()
                runCurrent()
                check(left.uiState.value.hintUndecided) { "the notice must be up when the game is left" }
                val boardLeft = left.uiState.value.board
                left.onLeaveScreen()
                runCurrent()
                cancelTickers()

                val resumed = viewModelFor(sixPegStrip(), resume = true)
                runCurrent()

                val state = resumed.uiState.value
                assertThat(state.isLoading).isFalse()
                assertThat(state.board.pegs).isEqualTo(boardLeft.pegs)
                assertThat(state.hintUndecided).isFalse()
                assertThat(state.noWinningMove).isFalse()
                assertThat(state.showRescueBanner).isFalse()
            } finally {
                cancelTickers()
            }
        }

    // ---- Helpers -------------------------------------------------------------

    /** The board + history state a game was left in, captured before the snapshot write. */
    private data class LeftGame(
        val board: Board,
        val afterFirstMovePegs: Long,
    )

    /**
     * Plays two jumps on [sixPegStrip] (leaving the board still Playing), lets the timer
     * reach [PLAYED_SECONDS], leaves the screen, and returns what was on screen — the
     * expectation a resume must reproduce exactly.
     */
    private fun TestScope.playTwoMovesAndLeave(time: TestTimeSource): LeftGame {
        val viewModel = viewModelFor(sixPegStrip(), timeSource = time)
        jump(viewModel, fromCol = FIRST_JUMP_FROM_COL, toCol = FIRST_JUMP_TO_COL)
        runCurrent()
        val afterFirstMove = viewModel.uiState.value.board
        jump(viewModel, fromCol = SECOND_JUMP_FROM_COL, toCol = SECOND_JUMP_TO_COL)
        tickTo(time, PLAYED_SECONDS)
        val left = viewModel.uiState.value
        check(left.status == GameStatus.Playing) { "the fixture must still be Playing when left" }

        viewModel.onLeaveScreen()
        runCurrent()
        cancelTickers()
        return LeftGame(board = left.board, afterFirstMovePegs = afterFirstMove.pegs)
    }

    /** Advances the monotonic source by [seconds] and lets the per-second ticker write it. */
    private fun TestScope.tickTo(
        time: TestTimeSource,
        seconds: Int,
    ) {
        runCurrent()
        time += seconds.seconds
        advanceTimeBy(TICK_ADVANCE_MILLIS)
        runCurrent()
    }

    /** Selects the peg at [fromCol] then taps [toCol] — one jump along the single row. */
    private fun jump(
        viewModel: GameViewModel,
        fromCol: Int,
        toCol: Int,
    ) {
        viewModel.onCellTap(Position(row = 0, col = fromCol))
        viewModel.onCellTap(Position(row = 0, col = toCol))
    }

    /** Cancels every live `viewModelScope` so a still-running ticker cannot hang `runTest`. */
    private fun cancelTickers() = viewModelStore.clear()

    private fun viewModelFor(
        board: Board,
        challengeLevel: Int? = null,
        resume: Boolean = false,
        levelId: String = ClassicLevelIds.ENGLISH33,
        timeSource: TimeSource = TestTimeSource(),
        getHintUseCase: GetHintUseCase = GetHintUseCase(),
    ): GameViewModel {
        if (!this::levelRepository.isInitialized) {
            levelRepository = CountingLevelRepository(board)
        }
        val args =
            buildMap<String, Any?> {
                put(RESUME_LEVEL_ID_ARG, levelId)
                if (challengeLevel != null) put(RESUME_CHALLENGE_LEVEL_ARG, challengeLevel)
                put(RESUME_ARG, resume)
            }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = levelRepository,
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = AllOnSettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = progressRepository,
                        timeSource = timeSource,
                        defaultDispatcher = testDispatcher,
                        adGateway = GrantingAds,
                        getHintUseCase = getHintUseCase,
                        savedStateHandle = SavedStateHandle(args),
                    ) as T
            }
        return ViewModelProvider(viewModelStore, factory)["vm${viewModelKey++}", GameViewModel::class.java]
    }

    /**
     * A 1x7 strip filled cols 0..5 (hole at col 6). Two jumps (4→6, then 2→4) leave the
     * board still Playing with two moves of history — the shape a resume must reproduce.
     */
    private fun sixPegStrip(): Board =
        (0 until STRIP_PEG_COUNT).fold(Board(spec = horizontalLine(STRIP_LENGTH), pegs = 0L)) { board, col ->
            board.withPeg(Position(row = 0, col = col))
        }

    /** A 1x3 strip (pegs at cols 0,1; hole at col 2): one jump leaves a lone peg → Won. */
    private fun wonInOneJumpBoard(): Board =
        Board(spec = horizontalLine(WIN_STRIP_LENGTH), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { Position(row = 0, col = it) }, target = null)
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RESUME_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg keys (the route's property names). */
private const val RESUME_LEVEL_ID_ARG = "levelId"
private const val RESUME_CHALLENGE_LEVEL_ARG = "challengeLevel"
private const val RESUME_ARG = "resume"

/** The Challenge level the resume fixture plays — deliberately not 1. */
private const val PLAYED_CHALLENGE_LEVEL = 7

/** A levelId no catalog board has ever had — the CR-01 stale/tampered resume id. */
private const val UNKNOWN_LEVEL_ID = "ghost-board"

/**
 * The cold-start route level for a Challenge Continue: Home's ladder StateFlow initial
 * value before the first DataStore emission lands — NOT the level the snapshot holds.
 */
private const val STALE_ROUTE_LEVEL = 1

/** The 1x7 fixture strip and the six pegs on it (hole at the last column). */
private const val STRIP_LENGTH = 7
private const val STRIP_PEG_COUNT = 6

/** The two jumps the fixture plays along that strip, in order. */
private const val FIRST_JUMP_FROM_COL = 4
private const val FIRST_JUMP_TO_COL = 6
private const val SECOND_JUMP_FROM_COL = 2
private const val SECOND_JUMP_TO_COL = 4

/** A 1x3 strip is the smallest board with exactly one winning jump. */
private const val WIN_STRIP_LENGTH = 3
private const val WIN_JUMP_TO_COL = 2

/** The elapsed time the fixture accrues before leaving; restored exactly (SC3). */
private const val PLAYED_SECONDS = 30

/** One tick plus a margin, so the ticker's `delay(1s)` actually fires under virtual time. */
private const val TICK_ADVANCE_MILLIS = 1_100L

/** `ScoreResult`/`GameUiState` count whole seconds; the snapshot stores milliseconds. */
private const val MILLIS_IN_SECOND = 1_000L

/**
 * A [ProgressRepository] that stores real snapshots in memory and records every
 * save/clear, so both halves of the round trip run the production encode → rebuild path.
 *
 * `recordResult` deliberately throws: recording a solved play belongs to
 * `ResultsViewModel` (05-05), and this spec is the guard that `GameViewModel` never
 * takes that job back.
 */
private class RecordingProgressRepository : ProgressRepository {
    val snapshots = mutableMapOf<String, ResumeSnapshotDto>()
    val saves = mutableListOf<ResumeSnapshotDto>()
    val clears = mutableListOf<String>()
    private val modes = MutableStateFlow<List<String>>(emptyList())

    /** Replaces a stored snapshot's history with an illegal move (the T-05-04 forgery). */
    fun tamperWithMoves(mode: String) {
        val stored = requireNotNull(snapshots[mode]) { "nothing stored for $mode" }
        snapshots[mode] = stored.copy(moves = listOf(FORGED_MOVE_TUPLE.copyOf()))
    }

    /**
     * Rewrites a stored snapshot's provenance stamp to an out-of-range level — the
     * WARN-03 forgery on the *stored state* side. A rooted device or a restored backup
     * can carry any value here, so the stamp is no more trusted than the route arg.
     */
    fun tamperWithChallengeLevel(
        mode: String,
        forgedLevel: Int,
    ) {
        val stored = requireNotNull(snapshots[mode]) { "nothing stored for $mode" }
        snapshots[mode] = stored.copy(challengeLevel = forgedLevel)
    }

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("GameViewModel never records a result — ResultsViewModel owns that (05-05)")

    /** Unused by this screen — GameViewModel never reads the achievement aggregate. */
    override suspend fun aggregate(): ProgressAggregate = ProgressAggregate()

    /** Unused by this screen — no per-board leaderboard read happens here. */
    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeHomeProgress(): Flow<HomeProgress> = flowOf(HomeProgress.UNKNOWN)

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) {
        saves += dto
        snapshots[dto.mode] = dto
        modes.value = snapshots.keys.toList()
    }

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = snapshots[mode]

    override suspend fun clearSnapshot(mode: String) {
        clears += mode
        snapshots -= mode
        modes.value = snapshots.keys.toList()
    }

    override fun observeResumeModes(): Flow<List<String>> = modes

    /** Unreachable from here — gameplay never calls cloud save (CLOUD-01/CLOUD-04). */
    override suspend fun exportProgress(ladder: Int): CloudProgress = error("gameplay never calls cloud save")

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("gameplay never calls cloud save")

    override suspend fun resetAllProgress() = error("gameplay never calls cloud save")
}

/** A move tuple that is well-formed but describes a jump no board could ever allow. */
private val FORGED_MOVE_TUPLE = intArrayOf(9, 9, 9, 9, 9, 9)

/**
 * A [LevelRepository] serving one fixed board through both load paths while counting the
 * Challenge loads — the proof that a resume rebuilds by replay, never by regeneration.
 * `loadClassic` fails fast on an unknown id exactly like the production catalog source,
 * so the CR-01 fail-safe (an untrusted resume id must never crash the ViewModel) is
 * exercised against the real contract rather than a never-throwing stand-in.
 */
private class CountingLevelRepository(
    private val board: Board,
) : LevelRepository {
    var challengeLoads = 0
        private set

    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level {
        require(levelId == ClassicLevelIds.ENGLISH33) { "Unknown Classic level id: $levelId" }
        return levelOf(levelId)
    }

    override fun loadChallenge(levelNumber: Int): Level {
        challengeLoads++
        return levelOf("challenge-$levelNumber")
    }

    private fun levelOf(id: String): Level =
        Level(
            id = id,
            displayName = id,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )
}

/** Hints/haptics on; this spec is about the snapshot, not the feedback gates. */
private object AllOnSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** The auto-grant stub's behavior: every rewarded request answers [RewardOutcome.Granted]. */
private object GrantingAds : AdGateway {
    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}
