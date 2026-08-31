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
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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

/**
 * **ADSF-01** in virtual time: *the game clock pauses for the full duration of any ad
 * presentation and resumes afterwards.*
 *
 * The requirement is a scoring guarantee, not a cosmetic one. Score decays with elapsed
 * time, so every second an ad is on screen is score the player paid for a reward they were
 * told was free. `GameViewModel` therefore cancels its ticker, folds the seconds already
 * earned into `elapsedBaseSeconds`, and resumes on a **fresh** `TimeSource` mark — the same
 * rebase `applyRestored()` uses for a resumed snapshot.
 *
 * The harness is the injected [kotlin.time.TestTimeSource] plus [AdClockGatedAdGateway],
 * whose `requestRewarded` suspends until released. Holding that gate while advancing the
 * time source *is* an ad presentation of a known duration, with no device, no SDK and no
 * flake: whatever the clock reads on the far side of the gate is the whole assertion.
 *
 * Every test asserts on `uiState.value.elapsedSeconds` — the observable the score is
 * computed from — never on a private field, and no test-only accessor was added to
 * `GameViewModel` for this spec.
 *
 * Every test runs through [adClockTest], which cancels the ViewModel store in a `finally`.
 * Most of these fixtures finish `Playing`, so the eager per-second ticker would otherwise
 * keep rescheduling and `runTest`'s end-of-test drain would spin forever — and a spec that
 * hangs instead of going red on a regression is worse than no spec at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [AD_CLOCK_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent/advanceTimeBy virtual-time control
class GameViewModelAdClockTest {
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

    // ---- 1. The clock does not advance across an ad --------------------------

    @Test
    fun theClockDoesNotAdvanceWhileARewardedAdIsBeingPresented() =
        adClockTest {
            val timeSource = TestTimeSource()
            val gateway = AdClockGatedAdGateway()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource, gateway)
            runCurrent()

            playOpeningMove(viewModel)
            timeSource += PLAY_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            val beforeAd = viewModel.uiState.value.elapsedSeconds
            assertThat(beforeAd).isEqualTo(PLAY_SECONDS)

            // The undo's rewarded request is now in flight and held at the gate — this is
            // the ad being on screen.
            viewModel.onUndo()
            runCurrent()
            assertThat(gateway.started).isEqualTo(ONE_REQUEST)

            timeSource += AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_LONG_ADVANCE_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(beforeAd)

            // ...and it is still unchanged once the ad is dismissed and the grant lands.
            gateway.release()
            runCurrent()
            assertThat(gateway.proceeded).isEqualTo(ONE_REQUEST)
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(beforeAd)
        }

    // ---- 2. ...and it resumes afterwards -------------------------------------

    @Test
    fun theClockResumesFromWhereItStoppedOnceTheAdIsDismissed() =
        adClockTest {
            // A clock that never restarts would satisfy test 1 perfectly and hand the player
            // a permanently free game, so "it stopped" is only half of ADSF-01.
            val timeSource = TestTimeSource()
            val gateway = AdClockGatedAdGateway()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource, gateway)
            runCurrent()

            playOpeningMove(viewModel)
            timeSource += PLAY_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            val beforeAd = viewModel.uiState.value.elapsedSeconds

            viewModel.onUndo()
            runCurrent()
            timeSource += AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_LONG_ADVANCE_MILLIS)
            gateway.release()
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(beforeAd)

            // Real play resumes: the clock grows by exactly the interval that passed, which
            // is only true if the resumed ticker measures from a mark taken AFTER the ad.
            timeSource += POST_AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds)
                .isEqualTo(beforeAd + POST_AD_SECONDS)
        }

    // ---- 3. The rescue-banner path (RESEARCH Pitfall 10) ---------------------

    /**
     * The **Pitfall 10** regression test, and the one that pins the
     * `if (frozenForAd) return` guard at the top of `resumeTickerIfNeeded()` together with
     * the unconditional fresh mark in `unfreezeClockAfterAd()`.
     *
     * The D-08 rescue path is the dangerous one: the board is frozen at `Stuck`, the rescue
     * banner's Undo routes through the very same `onUndo()` funnel, and that funnel *ends*
     * by calling `resumeTickerIfNeeded()`. That resume reuses the pre-ad `startMark` and
     * only checks `tickerJob != null` — so a `startMark` that still predates the ad makes
     * the resumed ticker charge the player for the whole ad, one statement after the freeze
     * spent the ad protecting them from exactly that.
     *
     * Remove the guard, or move the `startMark = timeSource.markNow()` in
     * `unfreezeClockAfterAd()` behind its `status != Playing` early return, and **this test
     * fails** with an elapsed time inflated by [AD_SECONDS].
     */
    @Test
    fun aRescueUndoTakenAfterAnAdDoesNotReAddTheAdsDuration() =
        adClockTest {
            val timeSource = TestTimeSource()
            val gateway = AdClockGatedAdGateway()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource, gateway)
            runCurrent()

            playOpeningMove(viewModel)
            timeSource += PLAY_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            val realPlayTime = viewModel.uiState.value.elapsedSeconds
            assertThat(realPlayTime).isEqualTo(PLAY_SECONDS)

            // The dead end: the terminal freeze stops the ticker and raises the banner.
            playStuckingMove(viewModel)
            val stuckState = viewModel.uiState.value
            assertThat(stuckState.status).isEqualTo(GameStatus.Stuck)
            assertThat(stuckState.showRescueBanner).isTrue()
            assertThat(stuckState.elapsedSeconds).isEqualTo(realPlayTime)

            // The banner's Undo — the same funnel, with an ad in front of it.
            viewModel.onUndo()
            runCurrent()
            assertThat(gateway.started).isEqualTo(ONE_REQUEST)
            timeSource += AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_LONG_ADVANCE_MILLIS)
            gateway.release()
            runCurrent()

            // Rescued, and billed for the play only — never for the ad.
            val rescued = viewModel.uiState.value
            assertThat(rescued.status).isEqualTo(GameStatus.Playing)
            assertThat(rescued.showRescueBanner).isFalse()
            assertThat(rescued.elapsedSeconds).isEqualTo(realPlayTime)

            // The rescued ticker is alive and measuring from after the ad.
            timeSource += POST_AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds)
                .isEqualTo(realPlayTime + POST_AD_SECONDS)
        }

    // ---- 4. Freezing a clock that never started ------------------------------

    @Test
    fun freezingAClockThatNeverStartedIsInertAndTheTimerStillStartsOnTheFirstJump() =
        adClockTest {
            // The timer starts on the FIRST move (D-04), so a hint taken before any jump
            // freezes a clock with no mark. Rebasing there would invent elapsed time out of
            // an uninitialised mark, so the freeze must decline outright.
            val timeSource = TestTimeSource()
            val gateway = AdClockGatedAdGateway()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource, gateway)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(NO_TIME_ELAPSED)

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gateway.started).isEqualTo(ONE_REQUEST)

            timeSource += AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_LONG_ADVANCE_MILLIS)
            runCurrent()
            gateway.release()
            runCurrent()
            assertThat(gateway.proceeded).isEqualTo(ONE_REQUEST)
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(NO_TIME_ELAPSED)

            // And the timer still behaves normally afterwards: it starts on the first jump
            // and counts from THERE, not from the ad.
            playOpeningMove(viewModel)
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(NO_TIME_ELAPSED)
            timeSource += POST_AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(POST_AD_SECONDS)
        }

    // ---- 5. A finished game must not resume its ticker -----------------------

    @Test
    fun anAdThatReturnsAfterTheGameIsOverDoesNotResurrectTheClock() =
        adClockTest {
            // The ad's return is an SDK callback whose timing the app does not control, so
            // the unfreeze must be correct for a board that finished while the ad was up.
            val timeSource = TestTimeSource()
            val gateway = AdClockGatedAdGateway()
            val viewModel = viewModelFor(twoMoveStuckLine(), timeSource, gateway)
            runCurrent()

            playOpeningMove(viewModel)
            timeSource += PLAY_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_TICK_MILLIS)
            runCurrent()
            val beforeAd = viewModel.uiState.value.elapsedSeconds

            viewModel.onHintRequested()
            runCurrent()
            assertThat(gateway.started).isEqualTo(ONE_REQUEST)

            // The game ends while the ad is still up.
            playStuckingMove(viewModel)
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)

            gateway.release()
            runCurrent()

            // The clock stays frozen at its terminal reading no matter how much time passes.
            timeSource += AD_SECONDS.seconds
            advanceTimeBy(AD_CLOCK_LONG_ADVANCE_MILLIS)
            runCurrent()
            assertThat(viewModel.uiState.value.elapsedSeconds).isEqualTo(beforeAd)
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Runs [body] on the test dispatcher and ALWAYS cancels the ViewModel store afterwards.
     *
     * The `finally` is load-bearing rather than tidy, and this spec learned it the hard way:
     * a failing clock assertion that skipped the cancel left the resumed ticker live, and
     * `runTest`'s own end-of-test drain then spun on virtual time forever instead of
     * reporting the failure. Same idiom as `GameViewModelHintStalenessTest.hintTest`.
     */
    private fun adClockTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                cancelTickers()
            }
        }

    /** Jump col 2 over col 3 into the hole at col 4 — the board stays `Playing`. */
    private fun playOpeningMove(viewModel: GameViewModel) {
        viewModel.onCellTap(at(OPENING_FROM_COL))
        viewModel.onCellTap(at(OPENING_TO_COL))
    }

    /** Jump col 0 over col 1 into col 2 — strands two non-adjacent pegs ⇒ `Stuck`. */
    private fun playStuckingMove(viewModel: GameViewModel) {
        viewModel.onCellTap(at(STUCKING_FROM_COL))
        viewModel.onCellTap(at(STUCKING_TO_COL))
    }

    /** Cancels every live `viewModelScope` so a running ticker cannot hang `runTest`. */
    private fun cancelTickers() = viewModelStore.clear()

    private fun viewModelFor(
        board: Board,
        timeSource: TestTimeSource,
        adGateway: AdGateway,
    ): GameViewModel {
        val args = mapOf<String, Any?>(AD_CLOCK_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = AdClockLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = AdClockSettings,
                        // This spec is the clock, not the ladder or the snapshot; relaxed
                        // stand-ins absorb the advance-on-solve / save-on-leave calls.
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = mockk(relaxed = true),
                        timeSource = timeSource,
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
     * A 1x5 strip filled cols 0..3 (hole at col 4). Jump col2→col4 keeps it `Playing` with
     * `canUndo` true; the follow-up col0→col2 strands two non-adjacent pegs ⇒ `Stuck`, which
     * is the D-08 rescue-banner state the Pitfall-10 test needs.
     */
    private fun twoMoveStuckLine(): Board =
        Board(spec = horizontalLine(STRIP_LENGTH), pegs = 0L)
            .withPeg(at(STUCKING_FROM_COL))
            .withPeg(at(FILLED_COL_ONE))
            .withPeg(at(OPENING_FROM_COL))
            .withPeg(at(FILLED_COL_THREE))

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { at(it) }, target = null)

    private fun at(col: Int): Position = Position(row = AD_CLOCK_ROW, col = col)
}

/**
 * An [AdGateway] whose [requestRewarded] suspends on a shared gate until [release] — the
 * ADSF-01 harness. The span between `started` incrementing and [release] IS the ad
 * presentation, and the test advances the time source across it.
 *
 * [started] and [proceeded] make "the ad really was on screen" an assertion rather than an
 * assumption; a request cancelled while parked never increments [proceeded].
 */
private class AdClockGatedAdGateway : AdGateway {
    private val gate = CompletableDeferred<Unit>()

    var started = 0
        private set

    var proceeded = 0
        private set

    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    fun release() {
        gate.complete(Unit)
    }

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        started++
        gate.await()
        proceeded++
        return RewardOutcome.Granted
    }

    override fun showInterstitial() = Unit
}

/** Serves the one fixture board through Classic; this spec never touches Challenge. */
private class AdClockLevelRepository(
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
        error("This spec is Classic-only — the clock behaviour is mode-independent")
}

/** Hints/sound/haptics all on; this spec is about the clock, not the gates. */
private object AdClockSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val AD_CLOCK_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg key (the route's property name). */
private const val AD_CLOCK_LEVEL_ID_ARG = "levelId"

/** The single row every fixture is laid out on. */
private const val AD_CLOCK_ROW = 0

/** The 1x5 strip both fixtures share. */
private const val STRIP_LENGTH = 5

/** The jumping peg of the stucking move, and the strip's leftmost filled cell. */
private const val STUCKING_FROM_COL = 0

/** The peg the stucking move jumps over. */
private const val FILLED_COL_ONE = 1

/** The jumping peg of the opening move, and the cell the stucking move lands on. */
private const val OPENING_FROM_COL = 2

/** Where the stucking move lands — also the peg the opening move jumps over. */
private const val STUCKING_TO_COL = OPENING_FROM_COL

/** The peg the opening move jumps over. */
private const val FILLED_COL_THREE = 3

/** The empty cell the opening move lands on. */
private const val OPENING_TO_COL = 4

/** Real play time banked before any ad is requested. */
private const val PLAY_SECONDS = 4

/** How long each simulated ad presentation lasts — deliberately far longer than the play. */
private const val AD_SECONDS = 30

/** Real play time banked after the ad is dismissed. */
private const val POST_AD_SECONDS = 5

/** The reading of a clock that has not started, and of one an ad never touched. */
private const val NO_TIME_ELAPSED = 0

/** Exactly one rewarded request reached (and passed) the gate. */
private const val ONE_REQUEST = 1

/** Just past one ticker period (1_000 ms), so a pending tick is guaranteed to have run. */
private const val AD_CLOCK_TICK_MILLIS = 1_001L

/** A scheduler advance long enough to cover several ticker periods during an ad. */
private const val AD_CLOCK_LONG_ADVANCE_MILLIS = 5_000L
