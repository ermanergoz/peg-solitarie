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
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
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
 * **ADS-06** in the ViewModel: *a hint that reports "no winning move" is never charged
 * against the allowance or an ad* — and its sibling, an undo with nothing to undo.
 *
 * Both entry points used to consult the gateway **first**: `onHintRequested()` requested the
 * grant before it ran the solver, and `onUndo()` requested it before the empty-history guard.
 * While the Slice-1 stub auto-grants for free that is invisible. It stops being invisible the
 * moment an allowance (plan 09-08) and a real rewarded ad (plan 09-10) exist behind the same
 * seam: a player whose position has no winning move would pay a unit — or watch a
 * thirty-second video — to be told "no winning move". An allowance unit could be refunded;
 * **an ad already watched cannot**, which is why the fix is an ordering change rather than a
 * refund, and why it lands before the funnel rather than after it.
 *
 * Every "costs nothing" test asserts on [RecordingAdGateway.lastReason] being `null` — that
 * *the gateway was never consulted*. That is deliberately a stronger claim than "the board did
 * not change": a declined grant also leaves the board untouched, so a board-only assertion
 * would pass just as happily against the very ordering this spec exists to forbid.
 *
 * A solver **budget miss** returns `null` for a different reason than an unwinnable position,
 * and is covered separately: under this ordering it is likewise free, which is the correct and
 * generous reading of ADS-06 — the player was told "no winning move" and must not be charged
 * for it, whatever the reason.
 *
 * The fixtures are tiny single-row strips, so the REAL [GetHintUseCase] and Solver run
 * instantly and no domain component is mocked except in the budget-miss test, where returning
 * `null` *is* the condition under test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ORDERING_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class GameViewModelAssistOrderingTest {
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- Hints: search first, charge second (ADS-06, T-09-23) ----------------

    @Test
    fun aHintThatFindsNoWinningMoveNeverConsultsTheGateway() =
        orderingTest {
            // The whole of ADS-06 in one assertion. The board is movable but unwinnable, so
            // the honest answer is "no winning move" — and delivering that answer must cost
            // nothing at all, because there is no assist to charge for.
            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(unwinnableButMovableBoard(), gateway)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()

            assertThat(gateway.lastReason).isNull()
            assertThat(gateway.requests).isEqualTo(NO_REQUESTS)
            // Not charged means not charged in EITHER currency: no ad, and no allowance unit
            // spent behind an opt-in dialog either (plan 09-08).
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            // The player still got their answer — free, not withheld.
            val state = viewModel.uiState.value
            assertThat(state.noWinningMove).isTrue()
            assertThat(state.solverThinking).isFalse()
        }

    @Test
    fun aHintThatFindsAWinningMoveIsChargedForAndRevealed() =
        orderingTest {
            // The control: inverting the order must not make hints free. A found move is a
            // real assist, so it is still paid for — and still shown.
            //
            // Since plan 09-08 "charged for" means an allowance unit first (ADS-02) and only
            // then an ad, so the proof is in two halves: the first FREE_HINTS searches each
            // reveal and each spend a unit, and the one after that raises the opt-in offer —
            // which can only happen if every preceding found hint really was charged for.
            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()

            repeat(AssistAllowanceLimits.FREE_HINTS) {
                viewModel.onHintRequested()
                runCurrent()
                val free = viewModel.uiState.value
                assertThat(free.hintSource).isEqualTo(at(LEFT_PEG_COL))
                assertThat(free.hintTarget).isEqualTo(at(JUMP_TARGET_COL))
                assertThat(free.pendingAdOffer).isNull()
                assertThat(gateway.lastReason).isNull()
            }

            viewModel.onHintRequested()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.pendingAdOffer).isEqualTo(AdReason.HINT)
            assertThat(state.solverThinking).isFalse()
            assertThat(state.noWinningMove).isFalse()

            // ...and accepting the offer is what finally reaches the gateway, still for the
            // move the search already found.
            viewModel.onAcceptAdOffer()
            runCurrent()
            assertThat(gateway.lastReason).isEqualTo(AdReason.HINT)
            assertThat(viewModel.uiState.value.hintSource).isEqualTo(at(LEFT_PEG_COL))
            assertThat(viewModel.uiState.value.hintTarget).isEqualTo(at(JUMP_TARGET_COL))
        }

    @Test
    fun aHintWhoseSearchMissesItsBudgetIsAlsoFree() =
        orderingTest {
            // `getHintUseCase` also returns null when the budgeted Solver misses its deadline
            // (Phase 4 D-04), on a board that may well be winnable. The player is told "no
            // winning move" either way, so under this ordering the budget miss is free too.
            val missedBudget = mockk<GetHintUseCase>()
            every { missedBudget.invoke(any(), any(), any(), any(), any()) } returns null

            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway, getHintUseCase = missedBudget)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()

            assertThat(gateway.lastReason).isNull()
            assertThat(gateway.requests).isEqualTo(NO_REQUESTS)
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            assertThat(viewModel.uiState.value.noWinningMove).isTrue()
            assertThat(viewModel.uiState.value.solverThinking).isFalse()
        }

    @Test
    fun aHintDeclinedAtTheGatewayRevealsNothingAndLeavesTheControlIdle() =
        orderingTest {
            // The decline path survives the reorder: the move was found and stashed, but the
            // player refused the ad, so nothing is revealed. `solverThinking` must still be
            // cleared or `HintControl` spins for the rest of the game. Since plan 09-08 the
            // decline arrives from an accepted offer rather than from the tap, so the free
            // allowance is spent first.
            val declined = RecordingAdGateway(outcome = RewardOutcome.Declined)
            val viewModel = viewModelFor(winnableStrip(), declined)
            runCurrent()
            exhaustFreeHints(viewModel)

            viewModel.onHintRequested()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            assertThat(declined.lastReason).isEqualTo(AdReason.HINT)
            val state = viewModel.uiState.value
            assertThat(state.solverThinking).isFalse()
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.pendingAdOffer).isNull()
        }

    // ---- Undo: guard first, charge second (T-09-24) --------------------------

    @Test
    fun anUndoWithNothingToUndoNeverConsultsTheGateway() =
        orderingTest {
            // A fresh board has no history, so this undo is a pure no-op — and a no-op must
            // not burn a unit or play an ad.
            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            val fresh = viewModel.uiState.value.board
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            viewModel.onUndo()
            runCurrent()

            assertThat(gateway.lastReason).isNull()
            assertThat(gateway.requests).isEqualTo(NO_REQUESTS)
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
            assertThat(viewModel.uiState.value.canUndo).isFalse()
        }

    @Test
    fun anUndoWithHistoryIsChargedForAndUndoesTheMove() =
        orderingTest {
            // The control: moving the guard above the gateway must not make undo free.
            // "Charged for" is the allowance first and an ad only past it (plan 09-08), so
            // the proof is that FREE_UNDOS undos each really undo, and the one after them
            // raises the offer — which is only reachable if each of them spent a unit.
            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            val fresh = viewModel.uiState.value.board

            repeat(AssistAllowanceLimits.FREE_UNDOS) {
                jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
                runCurrent()
                assertThat(viewModel.uiState.value.canUndo).isTrue()

                viewModel.onUndo()
                runCurrent()

                assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
                assertThat(viewModel.uiState.value.canUndo).isFalse()
                assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
                assertThat(gateway.lastReason).isNull()
            }

            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            viewModel.onUndo()
            runCurrent()
            // Past the allowance the undo is offered, not silently taken.
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(afterJump)

            viewModel.onAcceptAdOffer()
            runCurrent()

            assertThat(gateway.lastReason).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
            assertThat(viewModel.uiState.value.canUndo).isFalse()
        }

    // ---- The CR-01 staleness contract survives the reorder (T-09-25) ---------

    @Test
    fun aHintComputedForASupersededBoardIsNeitherRevealedNorCharged() =
        orderingTest {
            // The search is parked at the dispatcher hop — genuinely in flight — while the
            // player jumps. The answer now belongs to a board that is no longer on screen, so
            // it is discarded; and because the discard happens BEFORE the gateway, the player
            // is not charged for an assist they will never be shown either.
            val gate = OrderingGatedDispatcher()
            val gateway = RecordingAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway, dispatcher = gate)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gate.pending).isEqualTo(ONE_PARKED_SEARCH)

            jump(viewModel, fromCol = LEFT_PEG_COL, toCol = JUMP_TARGET_COL)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            gate.releaseAll()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.hintSource).isNull()
            assertThat(state.hintTarget).isNull()
            assertThat(state.noWinningMove).isFalse()
            assertThat(state.showRescueBanner).isFalse()
            assertThat(state.solverThinking).isFalse()
            assertThat(state.board).isEqualTo(afterJump)
            assertThat(gateway.lastReason).isNull()
            assertThat(state.pendingAdOffer).isNull()
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Spends every free hint (ADS-02) so the next found move reaches the opt-in offer.
     *
     * Each of these hints is genuinely delivered and genuinely charged — an allowance unit
     * rather than an ad — so the gateway stays untouched throughout, which the callers rely
     * on when they later assert `lastReason`.
     */
    private fun TestScope.exhaustFreeHints(viewModel: GameViewModel) {
        repeat(AssistAllowanceLimits.FREE_HINTS) {
            viewModel.onHintRequested()
            runCurrent()
        }
        check(viewModel.uiState.value.pendingAdOffer == null) { "a free hint must raise no offer" }
    }

    /**
     * Runs [body] on the test dispatcher and ALWAYS cancels the ViewModel store afterwards.
     *
     * The `finally` is load-bearing rather than tidy, and the same idiom as
     * `GameViewModelHintStalenessTest.hintTest` / `GameViewModelAdClockTest.adClockTest`:
     * the undo fixtures play a move, which starts the eager per-second ticker, so a failing
     * assertion that skipped the cancel would leave that ticker live and `runTest`'s
     * end-of-test drain would spin on virtual time forever instead of reporting the failure.
     */
    private fun orderingTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                viewModelStore.clear()
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

    private fun viewModelFor(
        board: Board,
        adGateway: AdGateway,
        getHintUseCase: GetHintUseCase = GetHintUseCase(),
        dispatcher: CoroutineDispatcher = testDispatcher,
    ): GameViewModel {
        // Classic, with no `challengeLevel` arg: the Challenge generation path also hops
        // through `defaultDispatcher`, so keeping it unreachable means the gated variant
        // holds exactly one runnable and it is always the hint search.
        val args = mapOf<String, Any?>(ORDERING_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = OrderingLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = OrderingSettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = mockk(relaxed = true),
                        timeSource = TestTimeSource(),
                        defaultDispatcher = dispatcher,
                        adGateway = adGateway,
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
     * A 1x5 strip with pegs at cols 0, 1 and 3. Its only opening jump is 0→2, and the
     * position has a genuine one-peg winning line (0→2 then 2→4), so the real Solver returns
     * a move here and the "a found hint is still charged for" control is meaningful.
     */
    private fun winnableStrip(): Board =
        Board(spec = horizontalLine(STRIP_LENGTH), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(THIRD_PEG_COL))

    /**
     * Two mutually unreachable 1x3 rows (no shared edge, so no jump can cross between them),
     * each with pegs at cols 0 and 1. Four pegs in two components can only ever reduce to
     * two, so no one-peg line exists and the hint here is an honest `null` — while the board
     * is still movable, which is what keeps the "no winning move" answer a real answer rather
     * than a dead board.
     */
    private fun unwinnableButMovableBoard(): Board {
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
        row: Int = ORDERING_FIXTURE_ROW,
    ): Position = Position(row = row, col = col)
}

/**
 * A background dispatcher that PARKS what it is handed instead of running it, so the
 * staleness test can hold the hint search at the `withContext(defaultDispatcher)` boundary,
 * mutate the board, and only then let the search proceed. Single-threaded by construction:
 * [releaseAll] runs the queued blocks inline on the caller's (test) thread.
 */
private class OrderingGatedDispatcher : CoroutineDispatcher() {
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

/**
 * An [AdGateway] that records whether it was consulted at all.
 *
 * [lastReason] staying `null` is the ADS-06 assertion this whole spec turns on: it
 * distinguishes "the gateway was never asked" from "the gateway declined", which a
 * board-state assertion cannot do.
 */
private class RecordingAdGateway(
    private val outcome: RewardOutcome = RewardOutcome.Granted,
) : AdGateway {
    var lastReason: AdReason? = null
        private set

    var requests = 0
        private set

    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        lastReason = reason
        requests++
        return outcome
    }

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}

/** A [LevelRepository] serving one fixed board through Classic. */
private class OrderingLevelRepository(
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

/** Hints/haptics/sound all on; this spec is about the ordering, not the gates. */
private object OrderingSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ORDERING_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg key (the route's property name). */
private const val ORDERING_LEVEL_ID_ARG = "levelId"

/** The single row every fixture is laid out on. */
private const val ORDERING_FIXTURE_ROW = 0

/** The 1x5 strip the winnable fixture uses. */
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

/** The extra peg that gives the winnable fixture its one-peg line. */
private const val THIRD_PEG_COL = 3

/** What [RecordingAdGateway.requests] must read when the gateway was never consulted. */
private const val NO_REQUESTS = 0

/** The single hint search the gated dispatcher holds while the board is mutated. */
private const val ONE_PARKED_SEARCH = 1
