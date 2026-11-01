package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.app.pegsolitaire.R
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
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
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
import kotlin.time.TestTimeSource

/**
 * The whole free-assist economy (ADS-01..ADS-05, CONS-04), proven against the Slice-1 stub
 * and the test fakes **before any real ad SDK exists** — which is the entire reason the
 * ROADMAP locked this plan ahead of the SDK swap. The stub's silent auto-grant is exactly
 * what hides a missing opt-in dialog, so every behaviour below is asserted here or nowhere.
 *
 * ## Why nothing asserts on the counters directly
 *
 * `GameViewModel.allowance` is private and stays private: D-05 forbids surfacing a count in
 * the UI, so exposing one for a test would be a test-only accessor to state the production
 * code deliberately hides — and it would let the spec pass while the *player-visible*
 * behaviour regressed. Every claim about the counters is therefore made through its
 * observable consequence instead:
 *
 *  - **"the counter is exhausted"** is proven by [GameUiState.pendingAdOffer] becoming
 *    non-null on the next assist of that kind;
 *  - **"the counter was refilled"** is proven by the assists that follow being free again,
 *    and by the offer reappearing exactly one full allowance later;
 *  - **"the counter did not move"** is proven by the offer still being raised at the very
 *    next attempt.
 *
 * Every count is [AssistAllowanceLimits.FREE_HINTS] / [AssistAllowanceLimits.FREE_UNDOS].
 * The two numbers are written once in the codebase (D-02), and a literal here would be a
 * second place for them to drift.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ALLOWANCE_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
@Suppress("LargeClass") // one file per behaviour bullet of the economy; kept together
class GameViewModelAllowanceTest {
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

    // ---- Free while the allowance lasts (ADS-02, D-01/D-02) ------------------

    /**
     * **ADS-02.** The free allowance is free in the strongest sense available: not merely
     * "no ad played", but *the gateway was never consulted and no dialog was ever raised*.
     * A spec that only checked the board would pass against a funnel that showed an offer
     * and auto-accepted it.
     */
    @Test
    fun everyFreeUndoSucceedsWithNoDialogAndNoAdRequest() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            val fresh = viewModel.uiState.value.board

            repeat(AssistAllowanceLimits.FREE_UNDOS) {
                playOpeningJump(viewModel)
                runCurrent()
                assertThat(viewModel.uiState.value.canUndo).isTrue()

                viewModel.onUndo()
                runCurrent()

                assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
                assertThat(viewModel.uiState.value.canUndo).isFalse()
                assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
                assertThat(gateway.lastReason).isNull()
            }
        }

    /**
     * **D-09.** The ad is warmed at the moment a counter empties — the earliest point an
     * offer can occur — and never while allowance remains. That "never" is the load-bearing
     * half: it is why most levels issue no ad request at all, and it is asserted on every
     * spend before the last, not just at the end.
     */
    @Test
    fun theLastFreeUndoPreloadsExactlyOnceAndTheEarlierOnesNotAtAll() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()

            repeat(AssistAllowanceLimits.FREE_UNDOS - 1) {
                playOpeningJump(viewModel)
                runCurrent()
                viewModel.onUndo()
                runCurrent()
                assertThat(gateway.preloadedReasons).isEmpty()
            }

            // The spend that empties the counter.
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            assertThat(gateway.preloadedReasons).containsExactly(AdReason.UNDO)
        }

    // ---- The opt-in offer past exhaustion (ADS-01, D-06) ---------------------

    /**
     * **ADS-01.** Past the allowance the assist is *offered*, not taken: the dialog goes up
     * and the game is untouched until the player answers. The move history assertion matters
     * as much as the board one — a funnel that popped the history and then asked would leave
     * an identical-looking board on this fixture.
     */
    @Test
    fun theUndoAfterExhaustionRaisesTheOfferWithoutTouchingTheBoardOrTheHistory() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            viewModel.onUndo()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(state.adOfferLoading).isFalse()
            assertThat(state.board).isEqualTo(afterJump)
            // The history survives intact, which is what makes the offer answerable.
            assertThat(state.canUndo).isTrue()
            // Raising an offer is not requesting an ad (T-09-32).
            assertThat(gateway.lastReason).isNull()
        }

    /**
     * **D-06 / T-09-33.** Declining must leave the game *exactly* as it was — AdMob's
     * rewarded policy requires that saying no "must not impede or interfere with the normal
     * usage of the app", and the counters are part of "as it was": a dismiss that quietly
     * spent a unit would charge for a refusal.
     */
    @Test
    fun dismissingTheOfferChangesNothingAtAll() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)

            viewModel.onDismissAdOffer()
            runCurrent()

            val dismissed = viewModel.uiState.value
            assertThat(dismissed.pendingAdOffer).isNull()
            assertThat(dismissed.adOfferLoading).isFalse()
            assertThat(dismissed.board).isEqualTo(afterJump)
            assertThat(dismissed.canUndo).isTrue()
            assertThat(dismissed.adMessage).isNull()
            assertThat(gateway.lastReason).isNull()

            // Neither counter moved: the very next undo raises the same offer again rather
            // than being silently free (which a refill would look like) or silently taken.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(afterJump)
        }

    // ---- Accepting: refill then spend (ADS-03/ADS-05, D-03/D-07/D-11) --------

    /**
     * **D-03/D-07.** One watched ad buys a whole new allowance, and the assist it was watched
     * for is the first use of it — `refill(kind).spend(kind)`, in that order. That ordering
     * is what makes the dialog's own copy ("watch a short ad for 3 more?") literally true,
     * and it is proven here by counting: exactly `FREE_UNDOS - 1` further free undos, and
     * then the offer returns.
     */
    @Test
    fun acceptingAGrantedOfferPerformsTheUndoAndRefillsToOneShortOfFull() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)
            val fresh = viewModel.uiState.value.board

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            // The assist the ad was watched for landed.
            assertThat(gateway.lastReason).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            assertThat(viewModel.uiState.value.adOfferLoading).isFalse()
            // A Granted refill is silent — the snackbar belongs to the Unavailable path only.
            assertThat(viewModel.uiState.value.adMessage).isNull()

            // ...and the refill left exactly FREE_UNDOS - 1 more free undos behind it.
            repeat(AssistAllowanceLimits.FREE_UNDOS - 1) {
                playOpeningJump(viewModel)
                runCurrent()
                viewModel.onUndo()
                runCurrent()
                assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            }
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
        }

    /**
     * **ADS-03 / D-11.** No ad could be served, so the assist is granted anyway — the player
     * is never dead-ended by an ad that failed to arrive (T-09-37) — and a transient
     * snackbar states the problem *and* its already-applied resolution, which is why it
     * carries no action label. The tick is what a one-shot consumer keys on, so it is
     * asserted to have moved rather than merely to be non-zero.
     */
    @Test
    fun acceptingWhenNoAdCanBeServedStillUndoesAndExplainsItself() =
        allowanceTest {
            val gateway = AllowanceAdGateway(outcome = RewardOutcome.Unavailable)
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)
            val fresh = viewModel.uiState.value.board
            val tickBefore = viewModel.uiState.value.adMessageTick

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.board).isEqualTo(fresh)
            assertThat(state.adMessage).isEqualTo(R.string.ad_unavailable_undos)
            assertThat(state.adMessageTick).isEqualTo(tickBefore + 1)
            assertThat(state.pendingAdOffer).isNull()

            // The refill happened too, so the fail-open grant is a real allowance, not a
            // one-off exception: the next undo is free rather than another offer.
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            assertThat(viewModel.uiState.value.board).isEqualTo(fresh)
        }

    /**
     * **ADS-05 / T-09-34.** An unearned early dismiss arrives as `Declined`, and a decline
     * costs nothing and buys nothing. This is the anti-fraud assertion: if a decline refilled,
     * a player could open and instantly close ads forever, and the advertiser would be billed
     * for rewards nobody earned.
     */
    @Test
    fun acceptingButDecliningTheAdPerformsNothingAndRefillsNothing() =
        allowanceTest {
            val gateway = AllowanceAdGateway(outcome = RewardOutcome.Declined)
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            val afterJump = viewModel.uiState.value.board

            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(gateway.lastReason).isEqualTo(AdReason.UNDO)
            assertThat(state.board).isEqualTo(afterJump)
            assertThat(state.canUndo).isTrue()
            assertThat(state.adMessage).isNull()
            assertThat(state.pendingAdOffer).isNull()

            // Nothing was refilled: the next undo is offered again, not granted free.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(afterJump)
        }

    // ---- Two counters, not one pool (D-01) -----------------------------------

    /**
     * **D-01.** Hints and undos are separate counters. Exhausting your hints must not cost
     * you the ability to take back a misplaced jump — undo is a correction of the player's
     * own mistake, not a shortcut past the puzzle.
     */
    @Test
    fun spendingEveryFreeHintLeavesTheUndoAllowanceUntouched() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeHints(viewModel)

            // Hints are gone: the next one is offered.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.HINT)
            viewModel.onDismissAdOffer()
            runCurrent()

            // Undos are untouched: a full FREE_UNDOS of them are still free.
            repeat(AssistAllowanceLimits.FREE_UNDOS) {
                playOpeningJump(viewModel)
                runCurrent()
                viewModel.onUndo()
                runCurrent()
                assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            }
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
        }

    // ---- Knowably unservable ads are silent (CONS-04, D-12) ------------------

    /**
     * **CONS-04 / D-12.** When ads are knowably unservable — consent refused, SDK never
     * initialized, readiness not yet resolved — the assist is granted for free with no dialog
     * *and no snackbar*, byte-identical to v1.0. Asking "watch an ad?" and then granting
     * instantly because nothing could be served is the absurdity D-12 exists to rule out, and
     * explaining a consent failure to a puzzle player is noise about a system they were never
     * told existed (T-09-38, deliberate).
     */
    @Test
    fun assistsPastTheAllowanceStaySilentlyFreeWhenAdsAreUnservable() =
        allowanceTest {
            val gateway = AllowanceAdGateway(offerable = false)
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)
            exhaustFreeHints(viewModel)
            val fresh = viewModel.uiState.value.board

            // The undo past exhaustion simply happens.
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            val afterUndo = viewModel.uiState.value
            assertThat(afterUndo.board).isEqualTo(fresh)
            assertThat(afterUndo.pendingAdOffer).isNull()
            assertThat(afterUndo.adMessage).isNull()

            // ...and so does the hint.
            viewModel.onHintRequested()
            runCurrent()

            val afterHint = viewModel.uiState.value
            assertThat(afterHint.hintSource).isEqualTo(at(LEFT_PEG_COL))
            assertThat(afterHint.hintTarget).isEqualTo(at(JUMP_TARGET_COL))
            assertThat(afterHint.pendingAdOffer).isNull()
            assertThat(afterHint.adMessage).isNull()

            // No ad was requested and none was even warmed — the gateway said it had nothing.
            assertThat(gateway.lastReason).isNull()
        }

    // ---- Retrying is not a refill (D-04, T-09-36) ----------------------------

    /**
     * **D-04 / T-09-36.** "Restart to farm three more hints" must not be a one-tap loophole.
     * `resetToInitial()` clears every other piece of per-game state *and* the stored
     * snapshot, so the counters can only survive by living in the ViewModel instance —
     * which is exactly why the omission there is deliberate and commented.
     */
    @Test
    fun restartingTheLevelDoesNotRefillTheAllowance() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(winnableStrip(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onConfirmRestart()
            runCurrent()
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            // A fresh board, but not a fresh allowance.
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
        }

    // ---- The counters round-trip through the snapshot (D-04/D-17) ------------

    /**
     * **D-04/D-17.** The counters ride the resume snapshot, so process death is not a free
     * refill — and each counter is resolved independently, which is what a mixed
     * `(1 hint, 0 undos)` blob proves in one test.
     */
    @Test
    fun aRestoredGameResumesEachCounterWhereItWasLeft() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel =
                resumedViewModelFor(
                    gateway,
                    snapshotWith(freeHints = ONE_HINT_LEFT, freeUndos = NO_UNDOS_LEFT),
                )
            runCurrent()
            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            // One hint was left, so this one is free and delivered.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            assertThat(viewModel.uiState.value.hintSource).isNotNull()

            // No undos were left, so the very first undo is offered rather than taken.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.canUndo).isTrue()
        }

    /**
     * **D-04/D-17, fail-open half.** A blob written before the allowance existed carries no
     * counters at all, and `null` means "not written", NOT "zero remaining": an old saved
     * game resumes with a fresh allowance rather than a locked-out one. Proven by counting a
     * full `FREE_UNDOS` of free undos, not merely by the first one being free — a
     * partially-decoded counter would pass the weaker check.
     */
    @Test
    fun aRestoredGameWithNoStoredCountersGetsAFullAllowance() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = resumedViewModelFor(gateway, snapshotWithNoAllowanceFields())
            runCurrent()
            assertThat(viewModel.uiState.value.isLoading).isFalse()
            // The restore genuinely landed: without this the loop below would silently be
            // testing a fresh board, where a full allowance is true for the wrong reason.
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            // The restored history already holds one move, so the first cycle undoes that.
            repeat(AssistAllowanceLimits.FREE_UNDOS) {
                if (!viewModel.uiState.value.canUndo) {
                    playOpeningJump(viewModel)
                    runCurrent()
                }
                viewModel.onUndo()
                runCurrent()
                assertThat(viewModel.uiState.value.pendingAdOffer).isNull()
            }

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
        }

    // ---- The rescue banner reaches the same dialog (ADS-04, D-08) ------------

    /**
     * **ADS-04 / D-08.** The rescue banner's Undo is not a second offer surface: it routes
     * into the same `onUndo` funnel, so at zero it raises the same dialog. That is why the
     * ViewModel needs no banner-specific branch and `NoWinBanner` needed no change at all —
     * one consistent allowance, one dialog.
     */
    @Test
    fun theRescueBannersUndoRaisesTheSameOfferOnceUndosAreExhausted() =
        allowanceTest {
            val gateway = AllowanceAdGateway()
            val viewModel = viewModelFor(unwinnableButMovableBoard(), gateway)
            runCurrent()
            exhaustFreeUndos(viewModel)

            // A move to undo, and then the honest "no winning move" answer that raises the
            // banner. The answer itself is free (ADS-06) — it costs no hint.
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onHintRequested()
            runCurrent()
            val stranded = viewModel.uiState.value
            assertThat(stranded.noWinningMove).isTrue()
            assertThat(stranded.showRescueBanner).isTrue()
            assertThat(stranded.pendingAdOffer).isNull()
            val boardAtBanner = stranded.board

            // The banner's action IS onUndo.
            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)
            assertThat(viewModel.uiState.value.board).isEqualTo(boardAtBanner)
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Runs [body] on the test dispatcher and ALWAYS cancels the ViewModel store afterwards.
     *
     * The `finally` is load-bearing rather than tidy, and the same idiom as
     * `GameViewModelAdClockTest.adClockTest`: every fixture here plays moves, which starts
     * the eager per-second ticker, so a failing assertion that skipped the cancel would leave
     * that ticker live and `runTest`'s end-of-test drain would spin on virtual time forever
     * instead of reporting the failure.
     */
    private fun allowanceTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                viewModelStore.clear()
            }
        }

    /** Spends every free undo, leaving the board exactly where it started. */
    private fun TestScope.exhaustFreeUndos(viewModel: GameViewModel) {
        repeat(AssistAllowanceLimits.FREE_UNDOS) {
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
        }
        check(!viewModel.uiState.value.canUndo) { "the fixture must be back at its initial board" }
        check(viewModel.uiState.value.pendingAdOffer == null) { "a free undo must raise no offer" }
    }

    /** Spends every free hint; no jump is played, so the board is untouched. */
    private fun TestScope.exhaustFreeHints(viewModel: GameViewModel) {
        repeat(AssistAllowanceLimits.FREE_HINTS) {
            viewModel.onHintRequested()
            runCurrent()
        }
        check(viewModel.uiState.value.pendingAdOffer == null) { "a free hint must raise no offer" }
    }

    /** The one jump every fixture shares: col 0 over col 1 into the hole at col 2. */
    private fun playOpeningJump(viewModel: GameViewModel) {
        viewModel.onCellTap(at(LEFT_PEG_COL))
        viewModel.onCellTap(at(JUMP_TARGET_COL))
    }

    private fun viewModelFor(
        board: Board,
        adGateway: AdGateway,
        progressRepository: ProgressRepository = mockk(relaxed = true),
        resume: Boolean = false,
    ): GameViewModel {
        val args =
            mapOf<String, Any?>(
                ALLOWANCE_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33,
                ALLOWANCE_RESUME_ARG to resume,
            )
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = AllowanceLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = AllowanceSettings,
                        // This spec is the economy, not the ladder; a relaxed stand-in
                        // absorbs the advance-on-solve call.
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

    /** A Continue entry restoring [snapshot] — the D-04 process-death harness. */
    private fun resumedViewModelFor(
        adGateway: AdGateway,
        snapshot: ResumeSnapshotDto,
    ): GameViewModel =
        viewModelFor(
            winnableStrip(),
            adGateway,
            progressRepository = StoredSnapshotRepository(snapshot),
            resume = true,
        )

    /**
     * A real snapshot of [winnableStrip] one move in, carrying the given counters — built
     * through the production `SnapshotMapper.toDto`, so the decode side under test is the
     * one the app actually runs rather than a hand-built shortcut.
     */
    private fun snapshotWith(
        freeHints: Int,
        freeUndos: Int,
    ): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = ClassicLevelIds.ENGLISH33,
            initialBoard = winnableStrip(),
            moveHistory = listOf(openingMove()),
            elapsedMs = 0L,
            freeHintsRemaining = freeHints,
            freeUndosRemaining = freeUndos,
        )

    /** The same snapshot as a blob written before the allowance fields existed (both null). */
    private fun snapshotWithNoAllowanceFields(): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = ClassicLevelIds.ENGLISH33,
            initialBoard = winnableStrip(),
            moveHistory = listOf(openingMove()),
            elapsedMs = 0L,
        )

    /** The opening jump as a domain move, for seeding a stored history. */
    private fun openingMove(): Move =
        RulesEngine
            .legalMoves(winnableStrip())
            .first { it.from == at(LEFT_PEG_COL) && it.to == at(JUMP_TARGET_COL) }

    /**
     * A 1x5 strip with pegs at cols 0, 1 and 3. Its only opening jump is 0→2, and the
     * position has a genuine one-peg line (0→2 then 2→4), so the real Solver returns a move
     * here — which is what lets one fixture serve both the hint and the undo halves of the
     * economy without mocking any domain component.
     */
    private fun winnableStrip(): Board =
        Board(spec = horizontalLine(ALLOWANCE_STRIP_LENGTH), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(THIRD_PEG_COL))

    /**
     * Two mutually unreachable 1x3 rows (no shared edge, so no jump can cross between them),
     * each with pegs at cols 0 and 1. Four pegs in two components can only ever reduce to
     * two, so no one-peg line exists and the hint is an honest `null` — while the board stays
     * movable, which is what makes the rescue banner a rescuable state rather than a dead one.
     */
    private fun unwinnableButMovableBoard(): Board {
        val cells =
            (0 until ALLOWANCE_COMPONENT_LENGTH).map { at(it) } +
                (0 until ALLOWANCE_COMPONENT_LENGTH).map { at(it, row = ALLOWANCE_DETACHED_ROW) }
        return Board(spec = BoardSpec(inPlay = cells, target = null), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(LEFT_PEG_COL, row = ALLOWANCE_DETACHED_ROW))
            .withPeg(at(ADJACENT_PEG_COL, row = ALLOWANCE_DETACHED_ROW))
    }

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { at(it) }, target = null)

    private fun at(
        col: Int,
        row: Int = ALLOWANCE_FIXTURE_ROW,
    ): Position = Position(row = row, col = col)
}

/**
 * An [AdGateway] recording everything the funnel is allowed to ask of it.
 *
 * [lastReason] staying `null` is how "no ad was requested" is asserted — a strictly stronger
 * claim than "the board did not change", since a decline also leaves the board untouched.
 * [preloadedReasons] is the D-09 witness, and [offerable] drives the D-12 path.
 */
private class AllowanceAdGateway(
    private val outcome: RewardOutcome = RewardOutcome.Granted,
    offerable: Boolean = true,
) : AdGateway {
    var lastReason: AdReason? = null
        private set

    private val preloaded = mutableListOf<AdReason>()

    val preloadedReasons: List<AdReason> get() = preloaded.toList()

    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(offerable).asStateFlow()

    override fun preloadRewarded(reason: AdReason) {
        preloaded += reason
    }

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        lastReason = reason
        return outcome
    }

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}

/** A [LevelRepository] serving one fixed board through Classic; Challenge is unreachable. */
private class AllowanceLevelRepository(
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
        error("This spec is Classic-only — the allowance is identical in both modes (D-04)")
}

/**
 * A [ProgressRepository] that hands back one stored snapshot, so the restore under test runs
 * the production `SnapshotMapper` decode path rather than a hand-built `RestoredGame`.
 */
private class StoredSnapshotRepository(
    private val stored: ResumeSnapshotDto,
) : ProgressRepository {
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
    ) = Unit

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = stored.takeIf { it.mode == mode }

    override suspend fun clearSnapshot(mode: String) = Unit

    override fun observeResumeModes(): Flow<List<String>> = flowOf(listOf(stored.mode))

    /** Unreachable from here — gameplay never calls cloud save (CLOUD-01/CLOUD-04). */
    override suspend fun exportProgress(ladder: Int): CloudProgress = error("gameplay never calls cloud save")

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("gameplay never calls cloud save")

    override suspend fun resetAllProgress() = error("gameplay never calls cloud save")
}

/** Hints/sound/haptics all on; this spec is about the economy, not the feedback gates. */
private object AllowanceSettings : SettingsRepository {
    override val colorblindPaletteOn: Flow<Boolean> = flowOf(false)

    override suspend fun setColorblindPaletteOn(on: Boolean) = Unit

    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ALLOWANCE_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg keys (the route's property names). */
private const val ALLOWANCE_LEVEL_ID_ARG = "levelId"
private const val ALLOWANCE_RESUME_ARG = "resume"

/** The single row the strip fixtures are laid out on. */
private const val ALLOWANCE_FIXTURE_ROW = 0

/** The 1x5 strip the winnable fixture uses. */
private const val ALLOWANCE_STRIP_LENGTH = 5

/** The 1x3 footprint of each unreachable component in the rescue-banner fixture. */
private const val ALLOWANCE_COMPONENT_LENGTH = 3

/** The second component's row — two rows down, so no vertical jump can bridge them. */
private const val ALLOWANCE_DETACHED_ROW = 2

/** The jumping peg of the shared opening jump. */
private const val LEFT_PEG_COL = 0

/** The peg it jumps over. */
private const val ADJACENT_PEG_COL = 1

/** The empty cell it lands on. */
private const val JUMP_TARGET_COL = 2

/** The extra peg that gives the winnable fixture its one-peg line. */
private const val THIRD_PEG_COL = 3

/** The stored hint counter of the mixed-allowance snapshot: one free hint left. */
private const val ONE_HINT_LEFT = 1

/** The stored undo counter of the mixed-allowance snapshot: exhausted, and preserved as such. */
private const val NO_UNDOS_LEFT = 0
