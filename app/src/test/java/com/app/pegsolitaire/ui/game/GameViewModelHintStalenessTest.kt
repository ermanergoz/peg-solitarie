package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.domain.usecase.HintOutcome
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
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
import kotlin.coroutines.CoroutineContext
import kotlin.time.TestTimeSource

/**
 * The UI-layer half of DEBT-01's "the hint system tells the truth" — the one truth
 * `08-VERIFICATION.md` recorded as FAILED: **"Every hint the game shows is a legal move on
 * the board the player is actually looking at."**
 *
 * `08-REVIEW.md` CR-01 names the mechanism. `onHintRequested` launches `hintJob`, which may
 * legitimately burn the full 1.5 s deadline (the documented off-book Diamond-41 worst case,
 * where the pagoda prune is provably vacuous). The board stays fully tappable throughout —
 * `GameScreen` disables only the Hint control — yet neither `performJump` nor `onUndo`
 * cancels the search, and the result is applied unconditionally. Worse, the
 * `withContext(defaultDispatcher)` block reads `_uiState.value.board`, `moveHistory.size`
 * and `onRecordedLine` on the BACKGROUND thread, so the three inputs can be mutually
 * inconsistent with each other. The visible failures are a glow pointing at a cell the
 * player has already vacated, and a "no winning move" rescue banner raised over a position
 * that was never searched.
 *
 * The harness is a [GatedDispatcher] injected as the ViewModel's `defaultDispatcher`. It
 * queues the runnable instead of running it, parking the hint coroutine EXACTLY at the
 * dispatcher hop — the real CR-01 race window, with the search genuinely in flight — while
 * the test mutates the board on the main dispatcher. That reproduces a 1.5 s off-book
 * search deterministically in virtual time, with no sleeping and no flake, and every test
 * asserts `gate.pending == 1` first, so "the search really was in flight" is proven rather
 * than assumed.
 *
 * The fixtures are tiny single-row strips, so the REAL `GetHintUseCase` and `Solver` run
 * instantly: no domain component is mocked except in the final control test, where the use
 * case is a recording stub precisely because the inputs it was handed are the assertion.
 *
 * Every drain is followed by `runCurrent()`, never `advanceUntilIdle()`. These fixtures stay
 * `Playing` on purpose (a Stuck board would raise the rescue banner by itself and mask the
 * stale one), so the eager per-second ticker never stops rescheduling and advancing virtual
 * time to idle would spin forever. `runCurrent()` is also the tighter assertion: the
 * `withContext` resumption is scheduled at the CURRENT virtual instant, so no time needs to
 * pass for the result to land.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HINT_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class GameViewModelHintStalenessTest {
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0

    /** The parked background dispatcher every hint search in this spec suspends on. */
    private val gate = GatedDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- A jump during an in-flight search (CR-01, T-08-12-01) ---------------

    @Test
    fun aJumpDuringAnInFlightSearchNeverAppliesTheStaleHint() =
        hintTest {
            val start = playableStrip()
            // A fixture with no legal move could not exercise the race at all.
            assertThat(RulesEngine.legalMoves(start)).isNotEmpty()
            val viewModel = viewModelFor(start)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.solverThinking).isTrue()
            assertThat(gate.pending).isEqualTo(1)

            val boardBeforeJump = viewModel.uiState.value.board
            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            val boardAfterJump = viewModel.uiState.value.board
            assertThat(boardAfterJump).isNotEqualTo(boardBeforeJump)

            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.showRescueBanner).isFalse()
            assertThat(state.board).isEqualTo(boardAfterJump)
        }

    // ---- The cancellation must not lock the control (T-08-12-02) ------------

    @Test
    fun aJumpDuringAnInFlightSearchLeavesTheHintControlUsable() =
        hintTest {
            // Cancelling `hintJob` kills its own `solverThinking = false` write, and
            // `HintControl` renders `enabled = false` while that flag is true — so a fix
            // that forgets to clear it disables Hint for the rest of the game, a regression
            // strictly worse than the bug. The cancelling site owes the clear.
            val viewModel = viewModelFor(playableStrip())
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)

            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            gate.releaseAll()
            runCurrent()

            assertThat(viewModel.uiState.value.solverThinking).isFalse()

            // The control is genuinely usable again: a fresh request resolves to a legal
            // move on the board now on screen.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)
            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.solverThinking).isFalse()
            val hinted =
                RulesEngine
                    .legalMoves(state.board)
                    .firstOrNull { it.from == state.hintSource && it.to == state.hintTarget }
            assertThat(hinted).isNotNull()
        }

    // ---- An undo during an in-flight search (CR-01, T-08-12-01) -------------

    @Test
    fun anUndoDuringAnInFlightSearchNeverAppliesTheStaleHint() =
        hintTest {
            val viewModel = viewModelFor(playableStrip())
            runCurrent()

            val boardBeforeJump = viewModel.uiState.value.board
            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)

            viewModel.onUndo()
            runCurrent()
            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.showRescueBanner).isFalse()
            assertThat(state.solverThinking).isFalse()
            assertThat(state.board).isEqualTo(boardBeforeJump)
            assertThat(state.canUndo).isFalse()
        }

    // ---- A stale NULL is just as untruthful as a stale move -----------------

    @Test
    fun aStaleNullResultNeverRaisesTheRescueBannerOnANewBoard() =
        hintTest {
            // Two mutually unreachable 1x3 components: four pegs that can only ever reduce
            // to two, so the hint on this board is an honest `null`. The jump made during
            // the search leaves the OTHER component still movable, so the post-jump board
            // is Playing — any banner it shows can only have come from the stale search.
            val start = movableButUnwinnableBoard()
            assertThat(RulesEngine.legalMoves(start)).isNotEmpty()
            val viewModel = viewModelFor(start)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)

            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            // The jumped-to position was never searched, so nothing may be claimed of it.
            assertThat(viewModel.uiState.value.stuck).isFalse()

            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.showRescueBanner).isFalse()
        }

    // ---- The control: the fix removes stale hints, not hints ----------------

    @Test
    fun anUndisturbedHintStillResolvesAndTheSearchSeesTheBoardAsOfTheRequest() =
        hintTest {
            val searchedBoards = mutableListOf<Board>()
            val searchedMoveCounts = mutableListOf<Int>()
            val recordingHint = mockk<GetHintUseCase>()
            every { recordingHint.invoke(any(), any(), any(), any(), any()) } answers {
                val searched = firstArg<Board>()
                searchedBoards += searched
                searchedMoveCounts += thirdArg<Int>()
                RulesEngine.legalMoves(searched).firstOrNull()?.let { HintOutcome.Found(it) } ?: HintOutcome.ProvenDead
            }

            // Half 1 — left undisturbed, the hint resolves, and the inputs the search was
            // handed are the ones that were true AT REQUEST TIME.
            val undisturbed = viewModelFor(playableStrip(), getHintUseCase = recordingHint)
            runCurrent()
            val boardAtRequest = undisturbed.uiState.value.board

            undisturbed.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)
            gate.releaseAll()
            runCurrent()

            assertThat(searchedBoards).containsExactly(boardAtRequest)
            assertThat(searchedMoveCounts).containsExactly(NO_MOVES_PLAYED)
            val expected = RulesEngine.legalMoves(boardAtRequest).first()
            val hintedState = undisturbed.uiState.value
            assertThat(hintedState.hintSource).isEqualTo(expected.from)
            assertThat(hintedState.hintTarget).isEqualTo(expected.to)
            assertThat(hintedState.solverThinking).isFalse()
            cancelTickers()

            // Half 2 — jumped during the search, the background block must never have seen
            // the post-jump board: either it never ran (cancelled), or it ran against the
            // board captured on the main thread at request time.
            val disturbed = viewModelFor(playableStrip(), getHintUseCase = recordingHint)
            runCurrent()

            disturbed.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)

            val beforeJump = disturbed.uiState.value.board
            jump(disturbed, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            val afterJump = disturbed.uiState.value.board
            assertThat(afterJump).isNotEqualTo(beforeJump)

            gate.releaseAll()
            runCurrent()

            assertThat(searchedBoards).doesNotContain(afterJump)
        }

    // ---- The async board-install window (WR-01) ------------------------------

    @Test
    fun aHintDuringTheChallengeLoadWindowIsRefusedNotAnsweredWithTheRescueBanner() =
        hintTest {
            // The only board on screen during a Challenge load is the loading placeholder
            // (`pegs = 0`), which the Solver answers with an instant honest `null` — and
            // the placeholder is still the board when that result lands, so without the
            // isLoading guard the stale-result guard PASSES and the "no winning move"
            // rescue banner is raised over the loading indicator (WR-01).
            val board = playableStrip()
            val viewModel = challengeViewModelFor(board)
            runCurrent()
            // The generation is genuinely in flight: parked at the dispatcher hop.
            assertThat(viewModel.uiState.value.isLoading).isTrue()
            assertThat(gate.pending).isEqualTo(1)

            viewModel.onHintRequested()
            runCurrent()

            // Refused outright: no spinner raised, and no second runnable was parked —
            // the one in the gate is still the level load, not a hint search.
            assertThat(viewModel.uiState.value.solverThinking).isFalse()
            assertThat(gate.pending).isEqualTo(1)

            gate.releaseAll()
            runCurrent()

            // The install lands on a clean state: no banner over a position never played.
            val installed = viewModel.uiState.value
            assertThat(installed.isLoading).isFalse()
            assertThat(installed.noWinningMove).isFalse()
            assertThat(installed.showRescueBanner).isFalse()
            assertThat(installed.board).isEqualTo(board)

            // And the control is genuinely usable once a real board exists: a fresh
            // request resolves to a legal move on the installed board.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(1)
            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.solverThinking).isFalse()
            val hinted =
                RulesEngine
                    .legalMoves(state.board)
                    .firstOrNull { it.from == state.hintSource && it.to == state.hintTarget }
            assertThat(hinted).isNotNull()
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Runs [body] on the test dispatcher and ALWAYS cancels the ViewModel store afterwards.
     *
     * The `finally` is load-bearing rather than tidy. These fixtures deliberately stay
     * `Playing` (a Stuck board would raise the rescue banner on its own and mask the stale
     * one), so the eager per-second ticker keeps rescheduling. A failing assertion that
     * skipped the cancel would leave that ticker live, and `runTest`'s own end-of-test drain
     * would then spin forever instead of reporting the failure — a RED spec that hangs
     * instead of going red is worse than no spec at all.
     */
    private fun hintTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                cancelTickers()
            }
        }

    /** Selects the peg at [fromCol] then taps [toCol] — one jump along the fixture row. */
    private fun jump(
        viewModel: GameViewModel,
        fromCol: Int,
        toCol: Int,
    ) {
        viewModel.onCellTap(at(fromCol))
        viewModel.onCellTap(at(toCol))
    }

    /** Cancels every live `viewModelScope` so a running ticker cannot hang `runTest`. */
    private fun cancelTickers() = viewModelStore.clear()

    private fun viewModelFor(
        board: Board,
        getHintUseCase: GetHintUseCase = GetHintUseCase(),
    ): GameViewModel {
        // Classic, with no `challengeLevel` arg: the Challenge generation path also hops
        // through `defaultDispatcher`, so keeping it unreachable means the gate holds
        // exactly one runnable and it is always the hint search. The WR-01 spec that
        // needs the OPPOSITE — the gate holding the level load — uses
        // [challengeViewModelFor] instead.
        val args = mapOf<String, Any?>(HINT_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)
        return viewModelWith(HintLevelRepository(board), getHintUseCase, args)
    }

    /**
     * A Challenge ViewModel whose level load is parked at the [gate] — the WR-01 window:
     * `isLoading` is true, the board on screen is the loading placeholder, and the one
     * runnable the gate holds is the generation, not a hint search.
     */
    private fun challengeViewModelFor(board: Board): GameViewModel {
        val args =
            mapOf<String, Any?>(
                HINT_LEVEL_ID_ARG to "",
                HINT_CHALLENGE_LEVEL_ARG to HINT_CHALLENGE_LEVEL,
            )
        return viewModelWith(ChallengeHintLevelRepository(board), GetHintUseCase(), args)
    }

    private fun viewModelWith(
        levelRepository: LevelRepository,
        getHintUseCase: GetHintUseCase,
        args: Map<String, Any?>,
    ): GameViewModel {
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = levelRepository,
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = HintSettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = mockk(relaxed = true),
                        timeSource = TestTimeSource(),
                        defaultDispatcher = gate,
                        adGateway = HintAdGateway,
                        getHintUseCase = getHintUseCase,
                        savedStateHandle = SavedStateHandle(args),
                    ) as T
            }
        return ViewModelProvider(viewModelStore, factory)[
            "vm${viewModelKey++}",
            GameViewModel::class.java,
        ]
    }

    /**
     * A 1x5 strip with pegs at cols 0, 1 and 3. Its only opening jump is 0→2, which leaves
     * {2, 3} — still holding the legal jump 2→4 — so the board stays `Playing` with
     * `canUndo` true after the jump, and the position has a genuine one-peg winning line
     * for the control test to resolve.
     */
    private fun playableStrip(): Board =
        Board(spec = horizontalLine(STRIP_LENGTH), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(THIRD_PEG_COL))

    /**
     * Two mutually unreachable 1x3 rows (no shared edge, so no jump can cross between
     * them), each with pegs at cols 0 and 1. Four pegs in two components can only ever
     * reduce to two, so no one-peg line exists and a hint here is an honest `null`. The
     * opening jump 0→2 on the first row leaves the second row still movable, so the
     * post-jump board is `Playing` rather than `Stuck` — which is what makes any rescue
     * banner it shows attributable to the stale search and nothing else.
     */
    private fun movableButUnwinnableBoard(): Board {
        val cells =
            (0 until COMPONENT_LENGTH).map { at(it) } +
                (0 until COMPONENT_LENGTH).map { at(it, row = DETACHED_ROW) }
        return Board(spec = BoardSpec(inPlay = cells, target = null), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(LEFT_PEG_COL, row = DETACHED_ROW))
            .withPeg(at(ADJACENT_PEG_COL, row = DETACHED_ROW))
    }

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { at(it) }, target = null)

    private fun at(
        col: Int,
        row: Int = FIXTURE_ROW,
    ): Position = Position(row = row, col = col)
}

/**
 * A background dispatcher that PARKS what it is handed instead of running it, so a test can
 * hold the hint coroutine at the `withContext(defaultDispatcher)` boundary — the exact
 * CR-01 race window — mutate the board on the main dispatcher, and only then let the search
 * proceed. Single-threaded by construction: [releaseAll] runs the queued blocks inline on
 * the caller's thread, which is the test thread.
 */
private class GatedDispatcher : CoroutineDispatcher() {
    private val parked = ArrayDeque<Runnable>()

    /** How many blocks are currently held — a search in flight is `1`, not an assumption. */
    val pending: Int get() = parked.size

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        parked.addLast(block)
    }

    /** Drains the queue. A block whose job was cancelled meanwhile unwinds instead of running. */
    fun releaseAll() {
        while (parked.isNotEmpty()) {
            parked.removeFirst().run()
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val HINT_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg key (the route's property name). */
private const val HINT_LEVEL_ID_ARG = "levelId"

/** The `Gameplay` route Challenge arg key (the route's property name). */
private const val HINT_CHALLENGE_LEVEL_ARG = "challengeLevel"

/** The (in-bounds) Challenge level the WR-01 loading-window spec plays. */
private const val HINT_CHALLENGE_LEVEL = 1

/** The single row every fixture is laid out on. */
private const val FIXTURE_ROW = 0

/** The 1x5 strip the playable fixture uses. */
private const val STRIP_LENGTH = 5

/** The 1x3 footprint of each unreachable component in the unwinnable fixture. */
private const val COMPONENT_LENGTH = 3

/** The second component's row — two rows down, so no vertical jump can bridge them. */
private const val DETACHED_ROW = 2

/** The jumping peg. */
private const val LEFT_PEG_COL = 0

/** The peg it jumps over. */
private const val ADJACENT_PEG_COL = 1

/** The empty cell it lands on. */
private const val JUMP_TARGET_COL = 2

/** The extra peg that keeps [GameViewModelHintStalenessTest] playable after the jump. */
private const val THIRD_PEG_COL = 3

/** The move count at the moment a hint is requested on a freshly loaded board. */
private const val NO_MOVES_PLAYED = 0

/**
 * A [LevelRepository] serving one fixed board through Classic. `loadChallenge` is an
 * `error(...)` on purpose: this spec must never reach the Challenge path, because that path
 * hops through the same gated dispatcher and would put a second runnable in the queue.
 */
private class HintLevelRepository(
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

    override fun loadChallenge(levelNumber: Int): Level =
        error("This spec is Classic-only — the Challenge load would occupy the hint gate")
}

/**
 * A [LevelRepository] serving one fixed board through *Challenge*, for the WR-01
 * loading-window spec — where the gate deliberately holds the level load, so the test can
 * tap Hint while `isLoading` is true and the placeholder board is on screen. `solution` is
 * left `null` on purpose: the post-install control assertion must exercise the Solver path,
 * not the recorded line.
 */
private class ChallengeHintLevelRepository(
    private val board: Board,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level =
        error("This spec is Challenge-only — Classic would install a board synchronously")

    override fun loadChallenge(levelNumber: Int): Level =
        Level(
            id = "challenge-$levelNumber",
            displayName = "challenge-$levelNumber",
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )
}

/** Hints/haptics/sound all on; this spec is about the search, not the gates. */
private object HintSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Always grants, so the grant is never the thing under test here. */
private object HintAdGateway : AdGateway {
    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}
