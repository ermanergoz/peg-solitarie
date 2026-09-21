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
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlin.time.TestTimeSource

/**
 * The state contract every in-game sound cue is fired from — the ViewModel half of
 * SOUND-01/SOUND-02. The screen half (which cue actually plays) is `GameScreenSoundTest`.
 *
 * Four truths are pinned here:
 *  - **The jump-pitch ladder is a pure function of the move history** (D-05/D-06): it
 *    climbs only while the *same* peg keeps jumping and restarts at 0 for any other peg.
 *  - **D-08 is free, not implemented**: undoing a jump and replaying it reproduces the
 *    identical ladder step, because a shorter history recomputes the same index. The
 *    assertions are on the recomputed value, never on a stored counter — a counter is
 *    precisely what this design does not have.
 *  - **The four cue ordinals tick exactly once per real event** — including D-05a as amended
 *    (2026-08-28), where a re-tap deselect is a *successful* action that advances its OWN
 *    ordinal, leaves the selection ordinal alone, and must never be counted as a rejected tap.
 *  - **No state rebuild resets an ordinal** (RESEARCH Pitfall 2): a restart, a Challenge
 *    board load and a resume-restore all carry them forward, because a fresh `0` is a
 *    *changed* key for a one-shot effect sitting at a higher value and would fire a cue
 *    the player never triggered.
 *
 * Robolectric backs `SavedStateHandle.toRoute<Gameplay>()`; `Dispatchers.setMain` with a
 * `StandardTestDispatcher` keeps the async board loads and the ticker deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SOUND_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class GameViewModelSoundTest {
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0
    private val progressRepository = InMemoryProgressRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- The jump-pitch ladder (D-05/D-06/D-08) ------------------------------

    @Test
    fun twoJumpsByDifferentPegsBothStartTheLadderAtItsBase() =
        runTest(testDispatcher) {
            // D-06: a run is same-peg only, so two unrelated jumps are two first-jumps.
            val viewModel = viewModelFor(twoUnrelatedJumpsStrip())
            runCurrent()

            jump(viewModel, fromCol = 0, toCol = 2)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(0)

            jump(viewModel, fromCol = 4, toCol = 6)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(0)

            cancelTickers()
        }

    @Test
    fun aSamePegChainClimbsTheLadderOneRungPerJump() =
        runTest(testDispatcher) {
            // D-05: the same peg hopping 0→2→4→6 is a run, and the run index is its rung.
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            jump(viewModel, fromCol = 0, toCol = 2)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(0)

            jump(viewModel, fromCol = 2, toCol = 4)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(1)

            jump(viewModel, fromCol = 4, toCol = 6)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(2)
        }

    @Test
    fun jumpingWithADifferentPegAfterAChainResetsTheLadder() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainThenSeparatePegBoard())
            runCurrent()

            jump(viewModel, fromCol = 0, toCol = 2)
            jump(viewModel, fromCol = 2, toCol = 4)
            jump(viewModel, fromCol = 4, toCol = 6)
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(2)

            // A peg on the detached row cannot continue the run — the ladder restarts.
            viewModel.onCellTap(Position(row = SEPARATE_ROW, col = 0))
            viewModel.onCellTap(Position(row = SEPARATE_ROW, col = 2))
            runCurrent()
            assertThat(runIndexOf(viewModel)).isEqualTo(0)
        }

    @Test
    fun undoingAndReplayingTheThirdJumpReproducesTheIdenticalLadderStep() =
        runTest(testDispatcher) {
            // D-08. The assertion is on the RECOMPUTED value: there is no stored counter
            // to step back, which is the entire point of deriving the index from history.
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            jump(viewModel, fromCol = 0, toCol = 2)
            jump(viewModel, fromCol = 2, toCol = 4)
            jump(viewModel, fromCol = 4, toCol = 6)
            runCurrent()
            val beforeUndo = runIndexOf(viewModel)
            assertThat(beforeUndo).isEqualTo(2)

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.animating).isNull()

            jump(viewModel, fromCol = 4, toCol = 6)
            runCurrent()

            assertThat(runIndexOf(viewModel)).isEqualTo(beforeUndo)
        }

    // ---- The rejected-tap ordinal (SOUND-02, D-05a) --------------------------

    @Test
    fun tappingAnEmptyCellWithNothingSelectedIsARejectedTap() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()
            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(0)

            viewModel.onCellTap(Position(row = 0, col = 2)) // an empty hole
            runCurrent()

            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(1)
            assertThat(viewModel.uiState.value.selectionTick).isEqualTo(0)
            assertThat(viewModel.uiState.value.deselectionTick).isEqualTo(0)
        }

    @Test
    fun tappingAnIllegalDestinationWhileAPegIsSelectedIsARejectedTap() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            viewModel.onCellTap(Position(row = 0, col = 0))
            runCurrent()
            assertThat(viewModel.uiState.value.legalTargets)
                .containsExactly(Position(row = 0, col = 2))

            // col 4 is empty but is NOT a legal destination for the selected peg.
            viewModel.onCellTap(Position(row = 0, col = 4))
            runCurrent()

            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(1)
            // The gameplay no-op is unchanged: the selection survives, the board does not move.
            assertThat(viewModel.uiState.value.selected).isEqualTo(Position(row = 0, col = 0))
            assertThat(viewModel.uiState.value.board.pegs).isEqualTo(chainStrip().pegs)
        }

    @Test
    fun reTappingTheSelectedPegIsItsOwnDeselectionNotARejection() =
        runTest(testDispatcher) {
            // D-05a as amended (2026-08-28): a deselect is a successful action with a cue of
            // its own, so it advances the deselection ordinal, leaves the selection ordinal
            // exactly where the pick-up left it, and must never fire the invalid-tap cue.
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            viewModel.onCellTap(Position(row = 0, col = 0))
            runCurrent()
            assertThat(viewModel.uiState.value.selectionTick).isEqualTo(1)
            assertThat(viewModel.uiState.value.deselectionTick).isEqualTo(0)

            viewModel.onCellTap(Position(row = 0, col = 0))
            runCurrent()

            assertThat(viewModel.uiState.value.selected).isNull()
            // The pick-up ordinal must NOT move — that is the whole point of the split.
            assertThat(viewModel.uiState.value.selectionTick).isEqualTo(1)
            assertThat(viewModel.uiState.value.deselectionTick).isEqualTo(1)
            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(0)
        }

    @Test
    fun theSelectionOrdinalDoesNotAdvanceOnAJump() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            viewModel.onCellTap(Position(row = 0, col = 0)) // select → 1
            viewModel.onCellTap(Position(row = 0, col = 2)) // jump → no tick
            runCurrent()

            assertThat(viewModel.uiState.value.selectionTick).isEqualTo(1)
            assertThat(viewModel.uiState.value.deselectionTick).isEqualTo(0)
            assertThat(viewModel.uiState.value.animating).isNotNull()

            cancelTickers()
        }

    // ---- The undo ordinal (SOUND-02) -----------------------------------------

    @Test
    fun undoTicksOncePerUndoThatShortenedTheHistory() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            jump(viewModel, fromCol = 0, toCol = 2)
            jump(viewModel, fromCol = 2, toCol = 4)
            runCurrent()
            assertThat(viewModel.uiState.value.undoTick).isEqualTo(0)

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.undoTick).isEqualTo(1)

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.undoTick).isEqualTo(2)

            cancelTickers()
        }

    @Test
    fun undoOnAnEmptyHistoryDoesNotTick() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.uiState.value.undoTick).isEqualTo(0)
        }

    @Test
    fun aDeclinedUndoGrantDoesNotTick() =
        runTest(testDispatcher) {
            try {
                val viewModel =
                    viewModelFor(chainStrip(), adGateway = FixedAdGateway(RewardOutcome.Declined))
                runCurrent()

                // Plan 09-08 put a free allowance in front of the gateway (ADS-02), so a
                // decline is only reachable past exhaustion. Each cycle plays one jump and
                // takes it straight back, leaving the board where it started while spending
                // one free undo — and every one of those undos DOES tick, because it really
                // happened.
                repeat(AssistAllowanceLimits.FREE_UNDOS) {
                    jump(viewModel, fromCol = 0, toCol = 2)
                    runCurrent()
                    viewModel.onUndo()
                    runCurrent()
                }
                val afterFreeUndos = viewModel.uiState.value.undoTick
                assertThat(afterFreeUndos).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)

                jump(viewModel, fromCol = 0, toCol = 2)
                runCurrent()
                viewModel.onUndo()
                runCurrent()
                // The allowance is spent, so the tap raised the offer instead of undoing.
                assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)

                viewModel.onAcceptAdOffer()
                runCurrent()

                // The undo never happened, so nothing may be heard.
                assertThat(viewModel.uiState.value.undoTick).isEqualTo(afterFreeUndos)
                assertThat(viewModel.uiState.value.canUndo).isTrue()
            } finally {
                // Load-bearing: the board stays Playing throughout, so a failing assertion
                // that skipped this would leave the eager ticker live and `runTest`'s
                // end-of-test drain would spin on virtual time instead of reporting.
                cancelTickers()
            }
        }

    // ---- No state rebuild fires a phantom cue (RESEARCH Pitfall 2) -----------

    @Test
    fun aRestartCarriesTheCueOrdinalsForwardInsteadOfResettingThem() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            viewModel.onCellTap(Position(row = 0, col = 2)) // rejected tap → 1
            viewModel.onCellTap(Position(row = 0, col = 0)) // select → 1
            viewModel.onCellTap(Position(row = 0, col = 0)) // deselect → 1
            runCurrent()
            jump(viewModel, fromCol = 0, toCol = 2) // select → 2
            runCurrent()
            viewModel.onUndo() // undo → 1
            runCurrent()
            val before = viewModel.uiState.value
            assertThat(before.selectionTick).isGreaterThan(0)
            // This spec is the ONLY automated proof the resetToInitial carry-forward covers
            // the fourth ordinal, so the setup must actually reach a non-zero value for it.
            assertThat(before.deselectionTick).isGreaterThan(0)
            assertThat(before.undoTick).isGreaterThan(0)
            assertThat(before.rejectedTapTick).isGreaterThan(0)

            viewModel.onRestart()
            viewModel.onConfirmRestart()
            runCurrent()

            val after = viewModel.uiState.value
            assertThat(after.board.pegs).isEqualTo(chainStrip().pegs)
            assertThat(after.selectionTick).isEqualTo(before.selectionTick)
            assertThat(after.deselectionTick).isEqualTo(before.deselectionTick)
            assertThat(after.undoTick).isEqualTo(before.undoTick)
            assertThat(after.rejectedTapTick).isEqualTo(before.rejectedTapTick)
        }

    @Test
    fun aChallengeBoardLoadCarriesTheCueOrdinalsForward() =
        runTest(testDispatcher) {
            // The generation is async, so the screen is briefly the empty loading
            // placeholder — where every tap is a rejected tap. The board that lands must
            // not wipe that ordinal, or the effect keyed on it fires on arrival.
            val viewModel = viewModelFor(chainStrip(), challengeLevel = CHALLENGE_LEVEL)
            assertThat(viewModel.uiState.value.isLoading).isTrue()

            viewModel.onCellTap(Position(row = 0, col = 0))
            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(1)

            runCurrent() // the generated board replaces the loading state

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(1)

            cancelTickers()
        }

    @Test
    fun aResumeRestoreCarriesTheCueOrdinalsForward() =
        runTest(testDispatcher) {
            // Seed a real snapshot by playing and leaving, then Continue into it.
            val played = viewModelFor(chainStrip())
            runCurrent()
            jump(played, fromCol = 0, toCol = 2)
            runCurrent()
            played.onLeaveScreen()
            runCurrent()
            cancelTickers()

            val resumed = viewModelFor(chainStrip(), resume = true)
            assertThat(resumed.uiState.value.isLoading).isTrue()

            resumed.onCellTap(Position(row = 0, col = 0))
            assertThat(resumed.uiState.value.rejectedTapTick).isEqualTo(1)

            runCurrent() // the replayed snapshot replaces the loading state

            assertThat(resumed.uiState.value.canUndo).isTrue()
            assertThat(resumed.uiState.value.rejectedTapTick).isEqualTo(1)

            cancelTickers()
        }

    @Test
    fun aFreshGameStartsEveryCueOrdinalAtZero() =
        runTest(testDispatcher) {
            // The other half of Pitfall 2: the carry-forward only works because the
            // baseline really is 0, which is what lets a consumer guard on `> 0`.
            val viewModel = viewModelFor(chainStrip())
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.selectionTick).isEqualTo(0)
            assertThat(state.deselectionTick).isEqualTo(0)
            assertThat(state.undoTick).isEqualTo(0)
            assertThat(state.rejectedTapTick).isEqualTo(0)
            assertThat(state.animating).isNull()
            assertThat(state.status).isEqualTo(GameStatus.Playing)
        }

    // ---- Helpers -------------------------------------------------------------

    /** The ladder step of the jump currently animating — the value the screen pitches on. */
    private fun runIndexOf(viewModel: GameViewModel): Int {
        val transition = viewModel.uiState.value.animating
        assertThat(transition).isNotNull()
        return requireNotNull(transition).runIndex
    }

    /** Selects the peg at [fromCol] then taps [toCol] — one jump along row 0. */
    private fun jump(
        viewModel: GameViewModel,
        fromCol: Int,
        toCol: Int,
    ) {
        viewModel.onCellTap(Position(row = 0, col = fromCol))
        viewModel.onCellTap(Position(row = 0, col = toCol))
    }

    /** Cancels every live `viewModelScope` so a running ticker cannot hang `runTest`. */
    private fun cancelTickers() = viewModelStore.clear()

    private fun viewModelFor(
        board: Board,
        challengeLevel: Int? = null,
        resume: Boolean = false,
        adGateway: AdGateway = FixedAdGateway(RewardOutcome.Granted),
    ): GameViewModel {
        val args =
            buildMap<String, Any?> {
                put(SOUND_LEVEL_ID_ARG, ClassicLevelIds.ENGLISH33)
                if (challengeLevel != null) put(SOUND_CHALLENGE_LEVEL_ARG, challengeLevel)
                put(SOUND_RESUME_ARG, resume)
            }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = SoundLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = SoundSettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = progressRepository,
                        timeSource = TestTimeSource(),
                        defaultDispatcher = testDispatcher,
                        adGateway = adGateway,
                        getHintUseCase = GetHintUseCase(),
                        savedStateHandle = SavedStateHandle(args),
                    ) as T
            }
        return ViewModelProvider(viewModelStore, factory)[
            "vm${viewModelKey++}",
            GameViewModel::class.java,
        ]
    }

    /**
     * A 1x7 strip with pegs at cols 0, 1, 3 and 5. One peg walks the whole board:
     * 0→2, then 2→4, then 4→6 — a three-jump same-peg run (D-06) ending in a lone peg.
     */
    private fun chainStrip(): Board =
        Board(spec = horizontalLine(CHAIN_STRIP_LENGTH), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))
            .withPeg(Position(row = 0, col = 3))
            .withPeg(Position(row = 0, col = 5))

    /**
     * A 1x7 strip with pegs at cols 0, 1, 4 and 5: two jumps (0→2 and 4→6) made by two
     * DIFFERENT pegs, neither continuing the other.
     */
    private fun twoUnrelatedJumpsStrip(): Board =
        Board(spec = horizontalLine(CHAIN_STRIP_LENGTH), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))
            .withPeg(Position(row = 0, col = 4))
            .withPeg(Position(row = 0, col = 5))

    /**
     * [chainStrip] plus a detached 1x3 row two rows below (no shared edge, so no jump can
     * cross between them). The run is played out on row 0, then a peg on the far row jumps
     * — a move that cannot possibly continue the run, so the ladder must restart at 0.
     */
    private fun chainThenSeparatePegBoard(): Board {
        val cells =
            (0 until CHAIN_STRIP_LENGTH).map { Position(row = 0, col = it) } +
                (0 until SEPARATE_ROW_LENGTH).map { Position(row = SEPARATE_ROW, col = it) }
        return Board(spec = BoardSpec(inPlay = cells, target = null), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))
            .withPeg(Position(row = 0, col = 3))
            .withPeg(Position(row = 0, col = 5))
            .withPeg(Position(row = SEPARATE_ROW, col = 0))
            .withPeg(Position(row = SEPARATE_ROW, col = 1))
    }

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { Position(row = 0, col = it) }, target = null)
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SOUND_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg keys (the route's property names). */
private const val SOUND_LEVEL_ID_ARG = "levelId"
private const val SOUND_CHALLENGE_LEVEL_ARG = "challengeLevel"
private const val SOUND_RESUME_ARG = "resume"

/** The 1x7 fixture strip every ladder test walks. */
private const val CHAIN_STRIP_LENGTH = 7

/** A second, unreachable row used to prove a different peg restarts the ladder. */
private const val SEPARATE_ROW = 2
private const val SEPARATE_ROW_LENGTH = 3

/** A small Challenge level — the generation cost is the point, not the difficulty. */
private const val CHALLENGE_LEVEL = 1

/**
 * A [LevelRepository] serving one fixed board through Classic, and the real pure-domain
 * generator through Challenge (as the production source does), so the async load path this
 * spec's carry-forward test depends on is the genuine one.
 */
private class SoundLevelRepository(
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

    override fun loadChallenge(levelNumber: Int): Level {
        val generated = LevelGenerator.generate(levelNumber.coerceAtLeast(1))
        val spec = generated.startingBoard.spec
        return Level(
            id = "challenge-$levelNumber",
            displayName = "challenge-$levelNumber",
            spec = spec,
            initialBoard = generated.startingBoard,
            holeCount = spec.inPlay.size,
            solution = generated.solution,
            generatorVersion = generated.generatorVersion,
        )
    }
}

/**
 * A [ProgressRepository] that keeps snapshots in memory, so the resume carry-forward test
 * round-trips through the production encode → replay path rather than a hand-built state.
 */
private class InMemoryProgressRepository : ProgressRepository {
    private val snapshots = mutableMapOf<String, ResumeSnapshotDto>()
    private val modes = MutableStateFlow<List<String>>(emptyList())

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("GameViewModel never records a result — ResultsViewModel owns that")

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
        snapshots[dto.mode] = dto
        modes.value = snapshots.keys.toList()
    }

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = snapshots[mode]

    override suspend fun clearSnapshot(mode: String) {
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

/** Hints/haptics/sound all on; this spec is about the ordinals, not the gates. */
private object SoundSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** An [AdGateway] that always answers [outcome] — the undo grant is a gate, not a spec here. */
private class FixedAdGateway(
    private val outcome: RewardOutcome,
) : AdGateway {
    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = outcome

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}
