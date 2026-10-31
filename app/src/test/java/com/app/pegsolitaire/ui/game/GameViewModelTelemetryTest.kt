package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
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
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Move
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
import kotlinx.coroutines.flow.first
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
 * The per-solve assist telemetry (PGSA-02, D-08/D-09): how many hints the player actually
 * received, how many undos actually rewound the board, and whether the final peg came to
 * rest on the board's own target hole.
 *
 * ## Why every claim is made through the finish event
 *
 * `GameViewModel.hintsUsed` / `undosUsed` are private and stay private — they are absent
 * from `GameUiState` for the same D-05 reason the allowance is, and a test-only accessor
 * would let this spec pass while the value that actually reaches Results regressed. The
 * counters do, however, now have a legitimate observable that the allowance never had:
 * the `GameFinish` event, whose `telemetry` is precisely what `GameRoute` forwards. Every
 * assertion below therefore reads `viewModel.finished.first().telemetry` — or, for the
 * cases that cannot reach a Won board, the counters as written into the resume snapshot
 * by `onLeaveScreen`, which is the other place they legitimately escape.
 *
 * ## What counts (D-09)
 *
 * The two counter writes live at the APPLICATION points — `performUndo` and
 * `revealPendingHint` — never at the request and never at the funnel's `true`. That
 * placement is what makes "a declined or failed offer costs nothing" structurally true
 * rather than a rule someone has to remember, and it is why the dismissed-offer, declined-
 * reward, empty-history, no-winning-move and still-loading cases each get a test of their
 * own: they are the paths that return before a counter is touched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TELEMETRY_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
@Suppress("LargeClass") // one test per behaviour of the telemetry contract; kept in one file
class GameViewModelTelemetryTest {
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

    // ---- Counting the assists the player really received (D-09) --------------

    @Test
    fun aFreeUndoThatRewindsTheBoardIsCountedExactlyOnce() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip())
            runCurrent()

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            val telemetry = solveTheStrip(viewModel).telemetry

            assertThat(telemetry.undosUsed).isEqualTo(ONE_ASSIST)
            assertThat(telemetry.hintsUsed).isEqualTo(NO_ASSISTS)
        }

    @Test
    fun aFreeHintThatIsRevealedIsCountedExactlyOnce() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip())
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            // The hint really reached the screen — this is the state the counter mirrors.
            assertThat(viewModel.uiState.value.hintSource).isNotNull()

            val telemetry = solveTheStrip(viewModel).telemetry

            assertThat(telemetry.hintsUsed).isEqualTo(ONE_ASSIST)
            assertThat(telemetry.undosUsed).isEqualTo(NO_ASSISTS)
        }

    // ---- The paths that must cost nothing (T-12-23) --------------------------

    @Test
    fun anUndoOnAnEmptyHistoryCountsNothing() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip())
            runCurrent()

            // T-09-24: this returns before the funnel, so there is no assist to count.
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            val telemetry = solveTheStrip(viewModel).telemetry

            assertThat(telemetry.undosUsed).isEqualTo(NO_ASSISTS)
        }

    @Test
    fun anOfferRaisedPastExhaustionAndThenDismissedCountsNothing() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip())
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.UNDO)

            viewModel.onDismissAdOffer()
            runCurrent()

            // The board is still one jump in, so only the winning jump is left to play.
            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            // Exactly the free spends that preceded the offer — the dismiss added nothing.
            assertThat(telemetry.undosUsed).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
        }

    @Test
    fun anAcceptedOfferThatComesBackDeclinedCountsNothing() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip(), TelemetryAdGateway(RewardOutcome.Declined))
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            // ADS-05: an unearned reward performs no undo, so it counts no undo.
            assertThat(viewModel.uiState.value.canUndo).isTrue()

            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            assertThat(telemetry.undosUsed).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
        }

    @Test
    fun anAcceptedOfferThatIsGrantedCountsExactlyOneMore() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip(), TelemetryAdGateway(RewardOutcome.Granted))
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            // The rewarded undo really rewound the board, so it counts like any other.
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            val telemetry = solveTheStrip(viewModel).telemetry

            assertThat(telemetry.undosUsed).isEqualTo(AssistAllowanceLimits.FREE_UNDOS + ONE_ASSIST)
        }

    @Test
    fun anOfferResolvedAsUnavailableStillCountsBecauseTheAssistWasDelivered() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip(), TelemetryAdGateway(RewardOutcome.Unavailable))
            runCurrent()
            exhaustFreeUndos(viewModel)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()

            // D-11: the ad never arrived but the undo did — and the counter follows the
            // ASSIST, not the ad. A silently-free assist is still an assist taken.
            assertThat(viewModel.uiState.value.canUndo).isFalse()

            val telemetry = solveTheStrip(viewModel).telemetry

            assertThat(telemetry.undosUsed).isEqualTo(AssistAllowanceLimits.FREE_UNDOS + ONE_ASSIST)
        }

    @Test
    fun aHintSearchThatFindsNoWinningMoveCountsNothing() =
        telemetryTest {
            // A board with no one-peg line can never reach Won, so the finish event is not
            // available here — the counters are read from the snapshot instead, which is
            // the other place they legitimately escape the ViewModel.
            val repository = TelemetryProgressRepository()
            val viewModel = viewModelFor(unwinnableButMovableBoard(), progressRepository = repository)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.noWinningMove).isTrue()

            viewModel.onLeaveScreen()
            runCurrent()

            // ADS-06 charges nothing for "no winning move"; D-09 counts nothing either.
            assertThat(repository.snapshots[ProgressPersistenceKeys.MODE_CLASSIC]?.hintsUsed)
                .isEqualTo(NO_ASSISTS)
        }

    /**
     * AC-14 (GD-HT-5; D-09 unchanged): a hint that came back Undecided revealed nothing, so
     * it is not a hint used. The allowance is read through the next hints' offer behaviour,
     * as the allowance specs read it — every free hint is still there afterwards, and only
     * the one past them raises the offer — and the hint-free claim through the finish event.
     */
    @Test
    fun anUndecidedHintDoesNotCountAsAHintUsed() =
        telemetryTest {
            val undecidedThenFound = listOf(HintOutcome.Undecided, HintOutcome.Found(openingMove()))
            val thenFound = mockk<GetHintUseCase>()
            every { thenFound.invoke(any(), any(), any(), any(), any()) } returnsMany undecidedThenFound
            val allowanceKept = viewModelFor(winnableStrip(), getHintUseCase = thenFound)
            runCurrent()
            allowanceKept.onHintRequested()
            runCurrent()
            check(allowanceKept.uiState.value.hintUndecided) { "the first hint must come back Undecided" }

            repeat(AssistAllowanceLimits.FREE_HINTS) {
                allowanceKept.onHintRequested()
                runCurrent()
                assertThat(allowanceKept.uiState.value.pendingAdOffer).isNull()
                assertThat(allowanceKept.uiState.value.hintSource).isEqualTo(at(LEFT_PEG_COL))
            }
            allowanceKept.onHintRequested()
            runCurrent()
            assertThat(allowanceKept.uiState.value.pendingAdOffer).isEqualTo(AdReason.HINT)

            val undecided = mockk<GetHintUseCase>()
            every { undecided.invoke(any(), any(), any(), any(), any()) } returns HintOutcome.Undecided
            val hintFree = viewModelFor(winnableStrip(), getHintUseCase = undecided)
            runCurrent()
            hintFree.onHintRequested()
            runCurrent()
            check(hintFree.uiState.value.hintUndecided) { "the hint must come back Undecided" }

            assertThat(solveTheStrip(hintFree).telemetry.hintsUsed).isEqualTo(NO_ASSISTS)
        }

    @Test
    fun aHintDiscardedBecauseTheBoardMovedOnCountsNothing() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip(), TelemetryAdGateway(RewardOutcome.Granted))
            runCurrent()
            exhaustFreeHints(viewModel)

            // The offer stashes the found move; the jump below invalidates it, and the
            // grant that follows therefore has nothing left to reveal. Both the stash-drop
            // and `revealPendingHint`'s own staleness re-check return before the counter.
            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.pendingAdOffer).isEqualTo(AdReason.HINT)

            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onAcceptAdOffer()
            runCurrent()
            assertThat(viewModel.uiState.value.hintSource).isNull()

            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            assertThat(telemetry.hintsUsed).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        }

    @Test
    fun aHintRequestedWhileTheBoardIsStillLoadingCountsNothing() =
        telemetryTest {
            // WR-01 refuses the request outright while the restore is in flight. The later
            // real hint is what proves the refusal was skipped rather than the counter
            // being broken outright.
            val viewModel = resumedViewModelFor(snapshotOneMoveIn())

            viewModel.onHintRequested()
            assertThat(viewModel.uiState.value.isLoading).isTrue()
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.hintSource).isNotNull()

            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            assertThat(telemetry.hintsUsed).isEqualTo(ONE_ASSIST)
        }

    // ---- Reset on restart, and on a genuinely new level (D-09 vs D-04) -------

    @Test
    fun confirmingARestartReturnsBothCountersToZero() =
        telemetryTest {
            val viewModel = viewModelFor(winnableStrip())
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            viewModel.onConfirmRestart()
            runCurrent()

            val telemetry = solveTheStrip(viewModel).telemetry

            // DELIBERATELY the opposite of `allowance`, which a restart must NOT refill:
            // a retry may not hand back free assists, but neither may it let a player bank
            // a hint-free badge on the back of a restarted attempt (D-04 vs D-09).
            assertThat(telemetry).isEqualTo(SolveTelemetry.NONE)
        }

    // ---- Surviving process death (D-09 anti-laundering) ----------------------

    @Test
    fun leavingTheScreenWritesTheCountersIntoTheSavedSnapshot() =
        telemetryTest {
            val repository = TelemetryProgressRepository()
            val viewModel = viewModelFor(winnableStrip(), progressRepository = repository)
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            playOpeningJump(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            viewModel.onLeaveScreen()
            runCurrent()

            val saved = repository.snapshots[ProgressPersistenceKeys.MODE_CLASSIC]
            assertThat(saved).isNotNull()
            assertThat(saved!!.hintsUsed).isEqualTo(ONE_ASSIST)
            assertThat(saved.undosUsed).isEqualTo(ONE_ASSIST)
        }

    @Test
    fun aContinueEntryResumesWithTheSpentCountersIntact() =
        telemetryTest {
            val viewModel =
                resumedViewModelFor(
                    snapshotOneMoveIn(hintsUsed = RESTORED_HINTS_USED, undosUsed = RESTORED_UNDOS_USED),
                )
            runCurrent()

            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            // Killing the process mid-level must not launder an assist away.
            assertThat(telemetry.hintsUsed).isEqualTo(RESTORED_HINTS_USED)
            assertThat(telemetry.undosUsed).isEqualTo(RESTORED_UNDOS_USED)
        }

    @Test
    fun aResumeOverABlobWithNoCounterFieldsStartsAtZeroAndKeepsCounting() =
        telemetryTest {
            val viewModel = resumedViewModelFor(snapshotOneMoveIn())
            runCurrent()

            viewModel.onHintRequested()
            runCurrent()
            assertThat(viewModel.uiState.value.hintSource).isNotNull()

            val telemetry = playWinningJumpAndFinish(viewModel).telemetry

            assertThat(telemetry.hintsUsed).isEqualTo(ONE_ASSIST)
            assertThat(telemetry.undosUsed).isEqualTo(NO_ASSISTS)
        }

    // ---- lastPegOnTarget, read from the finishing board's own spec (D-01) ----

    @Test
    fun aSinglePegFinishingOnTheBoardsTargetIsATargetFinish() =
        telemetryTest {
            val viewModel = viewModelFor(targetedStrip(target = at(TARGET_HIT_COL)))
            runCurrent()

            val telemetry = solveTheTargetedStrip(viewModel).telemetry

            assertThat(telemetry.lastPegOnTarget).isTrue()
        }

    @Test
    fun aSinglePegFinishingAwayFromTheBoardsTargetIsNot() =
        telemetryTest {
            // Same footprint, same jump, target moved to the other end of the strip: the
            // flag has to follow the board's spec rather than "the game was won".
            val viewModel = viewModelFor(targetedStrip(target = at(TARGET_MISS_COL)))
            runCurrent()

            val telemetry = solveTheTargetedStrip(viewModel).telemetry

            assertThat(telemetry.lastPegOnTarget).isFalse()
        }

    @Test
    fun aBoardWithNoTargetIsNeverATargetFinish() =
        telemetryTest {
            // Every freeform Challenge footprint is built with `target = null`.
            val viewModel = viewModelFor(untargetedStrip())
            runCurrent()

            val telemetry = solveTheTargetedStrip(viewModel).telemetry

            assertThat(telemetry.lastPegOnTarget).isFalse()
        }

    @Test
    fun aMultiPegWinIsNeverATargetFinish() =
        telemetryTest {
            // Diamond-41's proven floor is TWO pegs, so this is a flawless win that is
            // nonetheless not a single-peg finish — `lastPegOnTarget` must stay false.
            val viewModel = viewModelFor(diamondOneJumpFromItsFloor(), levelId = ClassicLevelIds.DIAMOND41)
            runCurrent()

            viewModel.onCellTap(Position(row = DIAMOND_JUMP_ROW, col = DIAMOND_FROM_COL))
            viewModel.onCellTap(Position(row = DIAMOND_JUMP_ROW, col = DIAMOND_TO_COL))
            runCurrent()
            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)

            val finish = viewModel.finished.first()

            assertThat(finish.result.pegsRemaining).isEqualTo(DIAMOND_FLOOR_PEGS)
            assertThat(finish.telemetry.lastPegOnTarget).isFalse()
        }

    // ---- The ScoreResult half is untouched (D-08) ---------------------------

    @Test
    fun theFinishEventStillFiresExactlyOnceAndCarriesTheSameScoreResult() =
        telemetryTest {
            val board = untargetedStrip()
            val viewModel = viewModelFor(board)
            runCurrent()

            viewModel.finished.test {
                expectNoEvents()

                playTargetedStripJump(viewModel)
                runCurrent()

                val finish = awaitItem()
                assertThat(finish.result.pegsRemaining).isEqualTo(SOLVED_PEGS_REMAINING)
                assertThat(finish.result.pegsCleared).isEqualTo(SOLVED_PEGS_CLEARED)
                // ScoreResult gained no field and is computed exactly as before: the
                // telemetry rides ALONGSIDE it rather than inside it (D-08).
                assertThat(finish.result.stars)
                    .isEqualTo(
                        ScoreCalculator.stars(
                            pegsRemaining = SOLVED_PEGS_REMAINING,
                            minimumPegs = board.spec.minimumPegs,
                        ),
                    )
                assertThat(finish.result.score)
                    .isEqualTo(
                        ScoreCalculator.score(
                            pegsCleared = SOLVED_PEGS_CLEARED,
                            elapsedSeconds = finish.result.elapsedSeconds,
                        ),
                    )

                viewModel.onAnimationFinished()
                runCurrent()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Runs [body] on the test dispatcher and ALWAYS clears the ViewModel store afterwards.
     *
     * The `finally` is load-bearing rather than tidy (the `GameViewModelAllowanceTest`
     * idiom): every fixture plays moves, which starts the eager per-second ticker, so a
     * failing assertion that skipped the clear would leave that ticker live and `runTest`'s
     * end-of-test drain would spin on virtual time instead of reporting the failure.
     */
    private fun telemetryTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                viewModelStore.clear()
            }
        }

    /** Plays the winnable strip out to its one-peg finish and returns the finish event. */
    private suspend fun TestScope.solveTheStrip(viewModel: GameViewModel): GameFinish {
        playOpeningJump(viewModel)
        runCurrent()
        return playWinningJumpAndFinish(viewModel)
    }

    /** The second half of the strip's line, for fixtures already one jump in. */
    private suspend fun TestScope.playWinningJumpAndFinish(viewModel: GameViewModel): GameFinish {
        viewModel.onCellTap(at(JUMP_TARGET_COL))
        viewModel.onCellTap(at(WINNING_TARGET_COL))
        runCurrent()
        check(viewModel.uiState.value.status == GameStatus.Won) { "the fixture must reach a Won board" }
        return viewModel.finished.first()
    }

    /** Plays the 1x3 fixture's single jump and returns the finish event. */
    private suspend fun TestScope.solveTheTargetedStrip(viewModel: GameViewModel): GameFinish {
        playTargetedStripJump(viewModel)
        runCurrent()
        check(viewModel.uiState.value.status == GameStatus.Won) { "the fixture must reach a Won board" }
        return viewModel.finished.first()
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

    /** The 1x5 strip's opening jump: col 0 over col 1 into the hole at col 2. */
    private fun playOpeningJump(viewModel: GameViewModel) {
        viewModel.onCellTap(at(LEFT_PEG_COL))
        viewModel.onCellTap(at(JUMP_TARGET_COL))
    }

    /** The 1x3 fixtures' only jump: col 0 over col 1 into the hole at col 2. */
    private fun playTargetedStripJump(viewModel: GameViewModel) {
        viewModel.onCellTap(at(LEFT_PEG_COL))
        viewModel.onCellTap(at(JUMP_TARGET_COL))
    }

    private fun viewModelFor(
        board: Board,
        adGateway: AdGateway = TelemetryAdGateway(),
        progressRepository: ProgressRepository = mockk(relaxed = true),
        resume: Boolean = false,
        levelId: String = ClassicLevelIds.ENGLISH33,
        getHintUseCase: GetHintUseCase = GetHintUseCase(),
    ): GameViewModel {
        val args =
            mapOf<String, Any?>(
                TELEMETRY_LEVEL_ID_ARG to levelId,
                TELEMETRY_RESUME_ARG to resume,
            )
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = TelemetryLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = TelemetrySettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = progressRepository,
                        timeSource = TestTimeSource(),
                        defaultDispatcher = testDispatcher,
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

    /** A Continue entry restoring [snapshot] — the D-09 process-death harness. */
    private fun resumedViewModelFor(snapshot: ResumeSnapshotDto): GameViewModel =
        viewModelFor(
            winnableStrip(),
            progressRepository = TelemetryProgressRepository(stored = snapshot),
            resume = true,
        )

    /**
     * A real snapshot of [winnableStrip] one jump in, carrying the given assist counters —
     * built through the production `SnapshotMapper.toDto`, so the decode side under test is
     * the one the app actually runs. Omitting both arguments produces the legacy shape: a
     * blob carrying neither key.
     */
    private fun snapshotOneMoveIn(
        hintsUsed: Int? = null,
        undosUsed: Int? = null,
    ): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = ClassicLevelIds.ENGLISH33,
            initialBoard = winnableStrip(),
            moveHistory = listOf(openingMove()),
            elapsedMs = 0L,
            hintsUsed = hintsUsed,
            undosUsed = undosUsed,
        )

    /** The opening jump as a domain move, for seeding a stored history. */
    private fun openingMove(): Move =
        RulesEngine
            .legalMoves(winnableStrip())
            .first { it.from == at(LEFT_PEG_COL) && it.to == at(JUMP_TARGET_COL) }

    /**
     * A 1x5 strip with pegs at cols 0, 1 and 3 and no target. Its only opening jump is
     * 0→2, and the position has a genuine one-peg line (0→2 then 2→4), so the real Solver
     * returns a move here — which lets one fixture serve both the hint and the undo halves.
     */
    private fun winnableStrip(): Board =
        Board(spec = horizontalLine(TELEMETRY_STRIP_LENGTH, target = null), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(THIRD_PEG_COL))

    /** A 1x3 strip (pegs at cols 0, 1) whose [target] the single jump either hits or misses. */
    private fun targetedStrip(target: Position): Board =
        Board(spec = horizontalLine(TELEMETRY_SHORT_STRIP_LENGTH, target = target), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))

    /** The same 1x3 fixture with no target at all — the freeform Challenge footprint case. */
    private fun untargetedStrip(): Board =
        Board(spec = horizontalLine(TELEMETRY_SHORT_STRIP_LENGTH, target = null), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))

    /**
     * Two mutually unreachable 1x3 rows (no shared edge, so no jump can cross between them),
     * each with pegs at cols 0 and 1. Four pegs in two components can only ever reduce to
     * two, so no one-peg line exists and the hint is an honest `null` — while the board
     * stays movable, so the game is never terminal.
     */
    private fun unwinnableButMovableBoard(): Board {
        val cells =
            (0 until TELEMETRY_COMPONENT_LENGTH).map { at(it) } +
                (0 until TELEMETRY_COMPONENT_LENGTH).map { at(it, row = TELEMETRY_DETACHED_ROW) }
        return Board(spec = BoardSpec(inPlay = cells, target = null), pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(LEFT_PEG_COL, row = TELEMETRY_DETACHED_ROW))
            .withPeg(at(ADJACENT_PEG_COL, row = TELEMETRY_DETACHED_ROW))
    }

    /** Diamond-41 one jump from its proven two-peg floor (the `GameViewModelTest` fixture). */
    private fun diamondOneJumpFromItsFloor(): Board =
        Board(spec = Boards.Diamond41, pegs = 0L)
            .withPeg(Position(row = DIAMOND_JUMP_ROW, col = DIAMOND_FROM_COL))
            .withPeg(Position(row = DIAMOND_JUMP_ROW, col = DIAMOND_OVER_COL))
            .withPeg(Position(row = DIAMOND_LONE_PEG_ROW, col = DIAMOND_LONE_PEG_COL))

    private fun horizontalLine(
        length: Int,
        target: Position?,
    ): BoardSpec = BoardSpec(inPlay = (0 until length).map { at(it) }, target = target)

    private fun at(
        col: Int,
        row: Int = TELEMETRY_FIXTURE_ROW,
    ): Position = Position(row = row, col = col)
}

/**
 * An [AdGateway] whose rewarded outcome the fixture chooses, so the three-way
 * [RewardOutcome] can be driven without a real SDK. Always offerable: the D-12
 * unservable path is `GameViewModelAllowanceTest`'s subject, not this spec's.
 */
private class TelemetryAdGateway(
    private val outcome: RewardOutcome = RewardOutcome.Granted,
) : AdGateway {
    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = outcome

    override val interstitialReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun preloadInterstitial() = Unit

    override suspend fun showInterstitial(): Boolean = false
}

/** A [LevelRepository] serving one fixed board through Classic; Challenge is unreachable. */
private class TelemetryLevelRepository(
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
        error("This spec is Classic-only — the telemetry is identical in both modes (D-09)")
}

/**
 * A [ProgressRepository] that both serves an optional seeded snapshot and records every
 * save, so the resume round trip runs the production `SnapshotMapper` path in both
 * directions.
 */
private class TelemetryProgressRepository(
    stored: ResumeSnapshotDto? = null,
) : ProgressRepository {
    val snapshots = mutableMapOf<String, ResumeSnapshotDto>()
    val saves = mutableListOf<ResumeSnapshotDto>()

    init {
        if (stored != null) snapshots[stored.mode] = stored
    }

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
        saves += dto
        snapshots[dto.mode] = dto
    }

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = snapshots[mode]

    override suspend fun clearSnapshot(mode: String) {
        snapshots.remove(mode)
    }

    override fun observeResumeModes(): Flow<List<String>> = flowOf(snapshots.keys.toList())

    /** Unreachable from here — gameplay never calls cloud save (CLOUD-01/CLOUD-04). */
    override suspend fun exportProgress(ladder: Int): CloudProgress = error("gameplay never calls cloud save")

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("gameplay never calls cloud save")

    override suspend fun resetAllProgress() = error("gameplay never calls cloud save")
}

/** Hints/sound/haptics all on; this spec is about the counters, not the feedback gates. */
private object TelemetrySettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TELEMETRY_ROBOLECTRIC_SDK = 34

/** The `Gameplay` route arg keys (the route's property names). */
private const val TELEMETRY_LEVEL_ID_ARG = "levelId"
private const val TELEMETRY_RESUME_ARG = "resume"

/** The single row the strip fixtures are laid out on. */
private const val TELEMETRY_FIXTURE_ROW = 0

/** The 1x5 strip the winnable fixture uses. */
private const val TELEMETRY_STRIP_LENGTH = 5

/** The 1x3 strip the `lastPegOnTarget` fixtures use. */
private const val TELEMETRY_SHORT_STRIP_LENGTH = 3

/** The 1x3 footprint of each unreachable component in the no-winning-move fixture. */
private const val TELEMETRY_COMPONENT_LENGTH = 3

/** The second component's row — two rows down, so no vertical jump can bridge them. */
private const val TELEMETRY_DETACHED_ROW = 2

/** The jumping peg of the shared opening jump. */
private const val LEFT_PEG_COL = 0

/** The peg it jumps over. */
private const val ADJACENT_PEG_COL = 1

/** The empty cell it lands on. */
private const val JUMP_TARGET_COL = 2

/** The extra peg that gives the 1x5 fixture its one-peg line. */
private const val THIRD_PEG_COL = 3

/** Where the 1x5 strip's final peg comes to rest. */
private const val WINNING_TARGET_COL = 4

/** The target the 1x3 fixture's jump lands on — the true case. */
private const val TARGET_HIT_COL = 2

/** An in-play target the same jump can never reach — the false case. */
private const val TARGET_MISS_COL = 0

/** No assists taken. */
private const val NO_ASSISTS = 0

/** Exactly one assist taken. */
private const val ONE_ASSIST = 1

/** The hint count carried by the restored snapshot fixture. */
private const val RESTORED_HINTS_USED = 2

/** The undo count carried by the restored snapshot fixture. */
private const val RESTORED_UNDOS_USED = 1

/** Row of Diamond-41's floor-reaching jump. */
private const val DIAMOND_JUMP_ROW = 4

/** The jumping peg's column in the Diamond fixture. */
private const val DIAMOND_FROM_COL = 0

/** The jumped-over peg's column in the Diamond fixture. */
private const val DIAMOND_OVER_COL = 1

/** The landing hole's column in the Diamond fixture. */
private const val DIAMOND_TO_COL = 2

/** Row of the Diamond fixture's isolated survivor peg. */
private const val DIAMOND_LONE_PEG_ROW = 0

/** Column of the Diamond fixture's isolated survivor peg. */
private const val DIAMOND_LONE_PEG_COL = 4

/** Diamond-41's proven perfect finish: two pegs, not one. */
private const val DIAMOND_FLOOR_PEGS = 2

/** A solved 1x3 strip leaves exactly one peg, having cleared exactly one. */
private const val SOLVED_PEGS_REMAINING = 1
private const val SOLVED_PEGS_CLEARED = 1
