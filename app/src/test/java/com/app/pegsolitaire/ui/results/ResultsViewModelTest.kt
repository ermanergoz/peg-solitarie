package com.app.pegsolitaire.ui.results

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.CloudProfileStore
import com.app.pegsolitaire.data.CloudSyncStore
import com.app.pegsolitaire.data.ImportOutcome
import com.app.pegsolitaire.data.InterstitialCadenceStore
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.data.SubmissionSyncStore
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.ads.InterstitialCadenceParams
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.ads.AdConstants
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.platform.games.save.CloudSaveStore
import com.app.pegsolitaire.platform.games.save.FakeCloudSaveStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Truth coverage of `ResultsViewModel`'s documented behavior: it builds an immutable
 * [ResultsUiState] from the typed `Results` route args, maps stars to the headline tier
 * (D-08), exposes the played board as `replayId` — without re-deriving any of the
 * passed-through score values — and records the solved play's per-level best through
 * [ProgressRepository], surfacing the returned [NewBests] as the "New best!" cue (D-09).
 *
 * The record path is exercised against a [FakeProgressRepository] that reproduces the
 * repository's documented per-field merge contract (stars/score keep the max, time the
 * min), so a first solve reports all-true flags and a strictly worse replay all-false.
 *
 * Robolectric backs `SavedStateHandle.toRoute<Results>()` (the same harness
 * `GameViewModelTest` uses for `toRoute<Gameplay>()`); `Dispatchers.setMain` with a
 * `StandardTestDispatcher` makes the `viewModelScope` record deterministic.
 *
 * Since Phase 10 the ViewModel also owns the interstitial trigger (D-06..D-15), so the
 * lower half of this file pins that: eligibility, the D-12 preload, the D-10 fast paths,
 * the D-08 quiescence boundary, D-09's ad-then-navigate ordering and D-11's record-only-on-
 * a-real-presentation rule. Those cases run against a **real** `InterstitialCadenceStore`
 * over a temp-file Preferences store (the `ChallengeProgressTest` harness) and a gated
 * [FakeAdGateway], so no cadence arithmetic is ever restated here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
// The single behavior spec for ResultsViewModel (args + tier + record + celebration +
// interstitial trigger); kept together so one file exhaustively documents the VM's
// contract, following GameViewModelTest's identical exemption and its reasoning.
@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent/advanceTimeBy virtual-time control
class ResultsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * The app-lifetime scope every [InterstitialCadenceStore] built here runs on.
     *
     * It shares [testDispatcher]'s scheduler, so a `runTest` body drives the store's
     * fire-and-forget writes; outside `runTest` nothing is dispatched, which leaves a
     * default (fully suppressed) store and is exactly what the non-ad cases want.
     */
    private val storeScope = CoroutineScope(testDispatcher)

    /** Keeps every temp store file distinct; `TemporaryFolder.newFile` refuses a repeat name. */
    private var storeFileCount = 0

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun replayIdIsThePlayedBoard() {
        val viewModel = viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = THREE_STARS)

        assertThat(viewModel.uiState.value.replayId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    @Test
    fun headlineTierMapsThreeTwoAndOneStarsToPerfectSolvedAndStuck() {
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = THREE_STARS).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.PERFECT)
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = TWO_STARS).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.SOLVED)
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = ONE_STAR).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.STUCK)
    }

    @Test
    fun theFourScoreArgValuesPassThroughUnchanged() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.EUROPEAN37,
                stars = TWO_STARS,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
                score = SAMPLE_SCORE,
            )

        val state = viewModel.uiState.value
        assertThat(state.stars).isEqualTo(TWO_STARS)
        assertThat(state.pegsRemaining).isEqualTo(SAMPLE_PEGS_REMAINING)
        assertThat(state.elapsedSeconds).isEqualTo(SAMPLE_ELAPSED_SECONDS)
        assertThat(state.score).isEqualTo(SAMPLE_SCORE)
    }

    @Test
    fun solvedChallengeLevelOffersNextLevel() {
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        val state = viewModel.uiState.value
        assertThat(state.challengeLevel).isEqualTo(SAMPLE_CHALLENGE_LEVEL)
        assertThat(state.showNextLevel).isTrue()
    }

    @Test
    fun stuckChallengeLevelDoesNotOfferNextLevel() {
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = ONE_STAR,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        assertThat(viewModel.uiState.value.showNextLevel).isFalse()
    }

    @Test
    fun stuckTwoStarChallengeShowsStuckHeadlineNotSolved() {
        // A Challenge board stranded at 2-3 pegs earns 2 stars but is NOT solved: its
        // headline must read STUCK ("Out of moves"), consistent with the Retry action —
        // never the contradictory "Solved!" (WR-03).
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = TWO_STARS,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        val state = viewModel.uiState.value
        assertThat(state.headlineTier).isEqualTo(ResultsHeadlineTier.STUCK)
        assertThat(state.showNextLevel).isFalse()
    }

    @Test
    fun solvedChallengeStillShowsACelebratoryHeadline() {
        // A genuinely solved Challenge board (one peg) keeps its star-derived headline.
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.PERFECT)
    }

    @Test
    fun classicResultsNeverOffersNextLevelAndKeepsReplaySemantics() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = null,
            )

        val state = viewModel.uiState.value
        assertThat(state.challengeLevel).isNull()
        assertThat(state.showNextLevel).isFalse()
        assertThat(state.replayId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    @Test
    fun aFirstSolveRecordsTheClassicResultOnceAndReportsEveryNewBest() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    repository = repository,
                )
            runCurrent()

            assertThat(repository.recordCalls).hasSize(1)
            val call = repository.recordCalls.single()
            assertThat(call.mode).isEqualTo(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(call.levelId).isEqualTo(ClassicLevelIds.ENGLISH33)
            assertThat(call.result.stars).isEqualTo(THREE_STARS)
            assertThat(call.result.score).isEqualTo(SAMPLE_SCORE)
            assertThat(call.result.elapsedSeconds).isEqualTo(SAMPLE_ELAPSED_SECONDS)

            val newBests = viewModel.uiState.value.newBests
            assertThat(newBests.stars).isTrue()
            assertThat(newBests.time).isTrue()
            assertThat(newBests.score).isTrue()
            assertThat(newBests.any).isTrue()
        }

    @Test
    fun aStrictlyWorseReplayReportsNoNewBestAtAll() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            repository.seed(
                mode = ProgressPersistenceKeys.MODE_CLASSIC,
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                bestTimeMs = FAST_ELAPSED_SECONDS * MILLIS_IN_SECOND,
                bestScore = HIGH_SCORE,
            )

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = ONE_STAR,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
                    score = SAMPLE_SCORE,
                    repository = repository,
                )
            runCurrent()

            assertThat(repository.recordCalls).hasSize(1)
            val newBests = viewModel.uiState.value.newBests
            assertThat(newBests.stars).isFalse()
            assertThat(newBests.time).isFalse()
            assertThat(newBests.score).isFalse()
            assertThat(newBests.any).isFalse()
        }

    @Test
    fun aChallengeResultIsKeyedByTheChallengeModeAndItsLevelNumber() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()

            viewModelFor(
                levelId = "",
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                repository = repository,
            )
            runCurrent()

            val call = repository.recordCalls.single()
            assertThat(call.mode).isEqualTo(ProgressPersistenceKeys.MODE_CHALLENGE)
            assertThat(call.levelId).isEqualTo(SAMPLE_CHALLENGE_LEVEL.toString())
        }

    // ---- WARN-03: `Results` is an independently reachable route -----------------

    @Test
    fun aForgedHugeChallengeLevelArgIsRecordedUnderTheClampedLevelKey() =
        runTest(testDispatcher) {
            // `Results` is a typed route in `AppNavHost`, reachable WITHOUT going through
            // `GameRoute` — so it is its own trust boundary, not merely downstream of the
            // GameViewModel fix. A forged arg must not reach the database key.
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    challengeLevel = Int.MAX_VALUE,
                    repository = repository,
                )
            runCurrent()

            val call = repository.recordCalls.single()
            assertThat(call.mode).isEqualTo(ProgressPersistenceKeys.MODE_CHALLENGE)
            assertThat(call.levelId).isEqualTo(GeneratorConstants.MAX_CHALLENGE_LEVEL.toString())
            // `onNextLevelTapped` resolves the next level off this value (D-04a), so clamping
            // it is what stops "Next level" from exceeding the ceiling.
            assertThat(viewModel.uiState.value.challengeLevel)
                .isEqualTo(GeneratorConstants.MAX_CHALLENGE_LEVEL)
        }

    @Test
    fun aChallengeLevelArgBelowOneIsRecordedUnderTheLowestValidLevelKey() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    challengeLevel = BELOW_MIN_CHALLENGE_LEVEL,
                    repository = repository,
                )
            runCurrent()

            assertThat(repository.recordCalls.single().levelId)
                .isEqualTo(ChallengeLevelBounds.MIN.toString())
            assertThat(viewModel.uiState.value.challengeLevel).isEqualTo(ChallengeLevelBounds.MIN)
        }

    @Test
    fun aClassicResultStaysClassicWhenTheChallengeArgIsAbsent() =
        runTest(testDispatcher) {
            // The clamp must not manufacture a Challenge level out of `null` — a Classic
            // play would then be recorded under the Challenge mode key.
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    challengeLevel = null,
                    repository = repository,
                )
            runCurrent()

            val call = repository.recordCalls.single()
            assertThat(call.mode).isEqualTo(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(call.levelId).isEqualTo(ClassicLevelIds.ENGLISH33)
            assertThat(viewModel.uiState.value.challengeLevel).isNull()
        }

    @Test
    fun anUnsolvedPlayRecordsNothing() =
        runTest(testDispatcher) {
            // D-08: only a solved play earns a stored best. A stuck board reaching Results
            // (the Retry state, D-10) must never write a row for a level never solved.
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = ONE_STAR,
                    pegsRemaining = SAMPLE_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                    repository = repository,
                )
            runCurrent()

            assertThat(repository.recordCalls).isEmpty()
            assertThat(viewModel.uiState.value.newBests.any).isFalse()
        }

    @Test
    fun aPerfectDiamond41FinishAtItsTwoPegFloorIsRecordedAndSubmittedLikeAnyOtherSolve() =
        runTest(testDispatcher) {
            // CR-01, the regression this whole per-board floor exists for. Diamond-41's
            // proven floor is TWO pegs (Phase 8, GF(4) colour invariant), so a flawless
            // Diamond play arrives at Results with pegsRemaining = 2 and THREE stars. The
            // record gate used to be `pegsRemaining != 1`, which silently dropped it: no
            // level_result row, no play_session row, no submission — and therefore no
            // CLEARED_DIAMOND41, no GRAND_TOUR, no PERFECTIONIST and an empty Diamond-41
            // leaderboard, for the one player who played it perfectly.
            val log = mutableListOf<String>()
            val repository = FakeProgressRepository(log = log)
            val submitter = recordingSubmitter(log = log)

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.DIAMOND41,
                    stars = THREE_STARS,
                    pegsRemaining = DIAMOND_41_PERFECT_PEGS_REMAINING,
                    minimumPegs = DIAMOND_41_PERFECT_PEGS_REMAINING,
                    repository = repository,
                    progressSubmitter = submitter,
                )
            advanceUntilIdle()

            val call = repository.recordCalls.single()
            assertThat(call.mode).isEqualTo(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(call.levelId).isEqualTo(ClassicLevelIds.DIAMOND41)
            assertThat(call.result.stars).isEqualTo(THREE_STARS)
            assertThat(call.result.pegsRemaining).isEqualTo(DIAMOND_41_PERFECT_PEGS_REMAINING)
            // Recorded THEN submitted, exactly once — the same ordering every other solve gets.
            assertThat(log).containsExactly(RECORD_EVENT, SUBMIT_EVENT).inOrder()
            assertThat(submitter.callCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.isSolved).isTrue()
            assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.PERFECT)
        }

    @Test
    fun aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded() =
        runTest(testDispatcher) {
            // The other half of CR-01: widening the gate to a per-board floor must NOT
            // widen it for the boards whose floor really is one peg. A Challenge board left
            // with two pegs is stuck, banks nothing and submits nothing — exactly as before.
            val repository = FakeProgressRepository()
            val submitter = recordingSubmitter()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = TWO_STARS,
                    pegsRemaining = DIAMOND_41_PERFECT_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                    minimumPegs = DEFAULT_MINIMUM_PEGS,
                    repository = repository,
                    progressSubmitter = submitter,
                )
            advanceUntilIdle()

            assertThat(repository.recordCalls).isEmpty()
            assertThat(submitter.callCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.isSolved).isFalse()
            assertThat(viewModel.uiState.value.showNextLevel).isFalse()
            assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.STUCK)
        }

    @Test
    fun aForgedHugeMinimumPegsArgCannotDeclareABarelyPlayedBoardSolved() =
        runTest(testDispatcher) {
            // `Results` is independently reachable, so the floor is untrusted in its own
            // right. It is clamped into the shipped range at the nav boundary — NEVER
            // against the play's own pegsRemaining, which would make every forged value
            // satisfy the solved test and hand out a stored best for an unplayed board.
            val repository = FakeProgressRepository()
            val submitter = recordingSubmitter()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SAMPLE_PEGS_REMAINING,
                    minimumPegs = FORGED_HUGE_MINIMUM_PEGS,
                    repository = repository,
                    progressSubmitter = submitter,
                )
            advanceUntilIdle()

            assertThat(repository.recordCalls).isEmpty()
            assertThat(submitter.callCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.isSolved).isFalse()
        }

    @Test
    fun aForgedNonPositiveMinimumPegsArgStillLetsAGenuineSolveBank() =
        runTest(testDispatcher) {
            // The clamp's lower bound: a forged zero or negative floor would otherwise make
            // `pegsRemaining <= minimumPegs` unsatisfiable and destroy an honest player's
            // recorded best — the same fail-safe posture the Challenge-level clamp takes.
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    minimumPegs = FORGED_NON_POSITIVE_MINIMUM_PEGS,
                    repository = repository,
                )
            advanceUntilIdle()

            assertThat(repository.recordCalls).hasSize(1)
            assertThat(viewModel.uiState.value.isSolved).isTrue()
        }

    @Test
    fun recreatingTheViewModelOverTheSameSavedStateRecordsOnlyOnce() =
        runTest(testDispatcher) {
            // A process-death restore rebuilds the ViewModel over the SAME SavedStateHandle;
            // the record guard rides in that handle so the play is never counted twice.
            val repository = FakeProgressRepository()
            val handle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                )

            viewModelOver(handle, repository)
            runCurrent()
            viewModelOver(handle, repository)
            runCurrent()

            assertThat(repository.recordCalls).hasSize(1)
        }

    // ---- D-16: the celebration sounds once per result ---------------------------

    @Test
    fun aFirstEntryIntoResultsClaimsTheCelebration() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
            )

        assertThat(viewModel.uiState.value.playCelebration).isTrue()
    }

    @Test
    fun recreatingTheViewModelOverTheSameSavedStateCelebratesOnlyOnce() {
        // The two D-16 re-entry paths — process-death restore and back navigation — both
        // rebuild the ViewModel over the SAME SavedStateHandle. Deliberately no
        // `runCurrent()` between the two constructions: the latch is written synchronously
        // in `init`, so the second instance cannot race past an unclaimed flag.
        val repository = FakeProgressRepository()
        val handle =
            savedStateHandleFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
            )

        val first = viewModelOver(handle, repository)
        val second = viewModelOver(handle, repository)

        assertThat(first.uiState.value.playCelebration).isTrue()
        assertThat(second.uiState.value.playCelebration).isFalse()
    }

    @Test
    fun aStuckOneStarResultStillCelebratesOnFirstEntry() =
        runTest(testDispatcher) {
            // The latch is about re-entry, not about star count: a stuck board records no
            // best (D-08) and still earns its single rising pop.
            val repository = FakeProgressRepository()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = ONE_STAR,
                    pegsRemaining = SAMPLE_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                    repository = repository,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.playCelebration).isTrue()
            assertThat(viewModel.uiState.value.stars).isEqualTo(ONE_STAR)
            assertThat(repository.recordCalls).isEmpty()
        }

    @Test
    fun theCelebrationLatchIsIndependentOfTheRecordLatch() =
        runTest(testDispatcher) {
            // An unsolved play never sets the record guard, so the two keys cannot be the
            // same one: the second entry must still be silent even though nothing was
            // recorded on the first.
            val repository = FakeProgressRepository()
            val handle =
                savedStateHandleFor(
                    levelId = "",
                    stars = ONE_STAR,
                    pegsRemaining = SAMPLE_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                )

            val first = viewModelOver(handle, repository)
            runCurrent()
            val second = viewModelOver(handle, repository)
            runCurrent()

            assertThat(first.uiState.value.playCelebration).isTrue()
            assertThat(second.uiState.value.playCelebration).isFalse()
            assertThat(repository.recordCalls).isEmpty()
        }

    @Test
    fun aSolvedReEntryIsSilentAndStillRecordsExactlyOnce() =
        runTest(testDispatcher) {
            // Both latches ride in the same handle without disturbing each other.
            val repository = FakeProgressRepository()
            val handle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                )

            val first = viewModelOver(handle, repository)
            runCurrent()
            val second = viewModelOver(handle, repository)
            runCurrent()

            assertThat(first.uiState.value.playCelebration).isTrue()
            assertThat(second.uiState.value.playCelebration).isFalse()
            assertThat(repository.recordCalls).hasSize(1)
        }

    // ---- INT-01: nothing is shown, and nothing is decided, at construction ------

    @Test
    fun constructingTheViewModelNeverShowsAnInterstitial() =
        runTest(testDispatcher) {
            // INT-01's hardest half: the ad may only follow a continue TAP, so no
            // combination of eligibility, readiness and a passing cadence may produce one
            // from construction alone. All three fixtures at once, because the claim is
            // about the constructor rather than about any one of them.
            val eligibleGateway = FakeAdGateway()
            val stuckGateway = FakeAdGateway()
            val suppressedGateway = FakeAdGateway()

            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                cadenceStore = eligibleCadenceStore(backgroundScope),
                adGateway = eligibleGateway,
            )
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = ONE_STAR,
                cadenceStore = eligibleCadenceStore(backgroundScope),
                adGateway = stuckGateway,
            )
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                cadenceStore = freshCadenceStore(),
                adGateway = suppressedGateway,
            )
            advanceUntilIdle()

            assertThat(eligibleGateway.showCount).isEqualTo(0)
            assertThat(stuckGateway.showCount).isEqualTo(0)
            assertThat(suppressedGateway.showCount).isEqualTo(0)
        }

    // ---- D-12: warm an ad exactly when the next tap could show one -------------

    @Test
    fun anEligibleFinishWhoseProjectedCadencePassesEveryGatePreloadsExactlyOnce() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()

            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                cadenceStore = eligibleCadenceStore(backgroundScope),
                adGateway = gateway,
            )
            advanceUntilIdle()

            assertThat(gateway.preloadCount).isEqualTo(1)
        }

    @Test
    fun anEligibleFinishUnderFirstSessionSuppressionPreloadsNothing() =
        runTest(testDispatcher) {
            // D-12 / Pitfall 11: issuing the request IS the exposure, so a win that cannot
            // possibly be followed by an ad must not warm one. A fresh store has
            // firstSessionComplete = false, which alone makes mayShow false at every instant.
            val gateway = FakeAdGateway()

            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                cadenceStore = freshCadenceStore(),
                adGateway = gateway,
            )
            advanceUntilIdle()

            assertThat(gateway.preloadCount).isEqualTo(0)
        }

    @Test
    fun aNonEligibleFinishBanksNoWinAndPreloadsNothing() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()
            val store = freshCadenceStore()

            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = ONE_STAR,
                cadenceStore = store,
                adGateway = gateway,
            )
            advanceUntilIdle()

            assertThat(settledState(store).lifetimeCompletedLevels).isEqualTo(0)
            assertThat(gateway.preloadCount).isEqualTo(0)
        }

    @Test
    fun aSecondViewModelOverTheSameHandleBanksNoSecondWinAndPreloadsNoSecondTime() =
        runTest(testDispatcher) {
            // The WIN_COUNTED_KEY latch, reached by both D-16 re-entry paths: a
            // process-death restore and a back navigation rebuild the ViewModel over the
            // SAME SavedStateHandle. A win counted twice inflates the counter that decides
            // how often ads appear.
            val handle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                )
            val freshStore = freshCadenceStore()
            val gateway = FakeAdGateway()

            viewModelOver(handle, FakeProgressRepository(), cadenceStore = freshStore, adGateway = gateway)
            viewModelOver(handle, FakeProgressRepository(), cadenceStore = freshStore, adGateway = gateway)
            advanceUntilIdle()

            assertThat(settledState(freshStore).lifetimeCompletedLevels).isEqualTo(1)

            // The preload half needs a store that can actually pass the gates, since a
            // suppressed one would report zero either way.
            val eligibleHandle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                )
            val eligibleStore = eligibleCadenceStore(backgroundScope)
            val eligibleGateway = FakeAdGateway()

            viewModelOver(
                eligibleHandle,
                FakeProgressRepository(),
                cadenceStore = eligibleStore,
                adGateway = eligibleGateway,
            )
            viewModelOver(
                eligibleHandle,
                FakeProgressRepository(),
                cadenceStore = eligibleStore,
                adGateway = eligibleGateway,
            )
            advanceUntilIdle()

            assertThat(eligibleGateway.preloadCount).isEqualTo(1)
        }

    // ---- D-14 / INT-03: which finishes an interstitial may follow ---------------

    @Test
    fun aTwoStarClassicSolveIsAdEligibleSoTheCatalogTapShowsAnAd() =
        runTest(testDispatcher) {
            // D-14: an interstitial-eligible win is ANY solved finish, two stars included.
            // The strict-win test (stars == THREE_STARS) would deny this one.
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = TWO_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()

            assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.SOLVED)
            assertThat(gateway.showCount).isEqualTo(1)
        }

    @Test
    fun aOneStarClassicFinishIsNotAdEligibleAndNavigatesImmediately() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = ONE_STAR,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation).isEqualTo(ResultsNavTarget.Catalog)
            assertThat(gateway.showCount).isEqualTo(0)
        }

    @Test
    fun aStuckChallengeFinishIsNotAdEligibleAtAnyStarCount() =
        runTest(testDispatcher) {
            // INT-03: three stars but the board did not fold — tierFor calls that STUCK, and
            // the eligibility gate inherits that branch rather than restating it.
            val gateway = FakeAdGateway()
            val eligibleStore = eligibleCadenceStore(backgroundScope)

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = THREE_STARS,
                    pegsRemaining = SAMPLE_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                    cadenceStore = eligibleStore,
                    adGateway = gateway,
                )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.STUCK)
            assertThat(gateway.preloadCount).isEqualTo(0)

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(gateway.showCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(SAMPLE_CHALLENGE_LEVEL + 1))

            // The same finish banks no win either — a stuck board is not a win at all.
            val freshStore = freshCadenceStore()
            viewModelFor(
                levelId = "",
                stars = THREE_STARS,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                cadenceStore = freshStore,
                adGateway = FakeAdGateway(),
            )
            advanceUntilIdle()

            assertThat(settledState(freshStore).lifetimeCompletedLevels).isEqualTo(0)
        }

    @Test
    fun aPerfectDiamond41FinishWithTwoPegsRemainingIsAdEligible() =
        runTest(testDispatcher) {
            // RESEARCH Pitfall 1, the regression guard this whole gate is shaped around.
            // Diamond-41's proven floor is TWO pegs (Phase 8, GF(4) colour invariant), so a
            // perfect Diamond finish leaves 2. An eligibility gate written as
            // `pegsRemaining == 1` — the constant sitting a few lines away in the ViewModel —
            // would misclassify the flawless play as a loss and permanently deny it an ad.
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.DIAMOND41,
                    stars = THREE_STARS,
                    pegsRemaining = DIAMOND_41_PERFECT_PEGS_REMAINING,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.headlineTier).isEqualTo(ResultsHeadlineTier.PERFECT)
            assertThat(gateway.preloadCount).isEqualTo(1)

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()

            assertThat(gateway.showCount).isEqualTo(1)
        }

    // ---- D-10: the fast path costs nothing -------------------------------------

    @Test
    fun withNoInterstitialHeldTheTapNavigatesOnTheSameTurnWithNoScrim() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway(interstitialReadyInitially = false)

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()

            // Deliberately asserted with NO dispatch in between: D-10 means the navigation
            // is published on the same turn as the tap, not one coroutine hop later.
            assertThat(viewModel.uiState.value.pendingNavigation).isEqualTo(ResultsNavTarget.Catalog)
            assertThat(viewModel.uiState.value.adScrimVisible).isFalse()

            advanceUntilIdle()
            assertThat(gateway.showCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.adScrimVisible).isFalse()
        }

    @Test
    fun withTheCadenceSayingNoTheTapNavigatesOnTheSameTurnWithNoScrim() =
        runTest(testDispatcher) {
            // Same fast path, reached by the other clause: an ad IS held, but a fresh store
            // is still inside D-03's first-session suppression.
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = freshCadenceStore(),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()

            assertThat(viewModel.uiState.value.pendingNavigation).isEqualTo(ResultsNavTarget.Catalog)
            assertThat(viewModel.uiState.value.adScrimVisible).isFalse()

            advanceUntilIdle()
            assertThat(gateway.showCount).isEqualTo(0)
        }

    // ---- D-08 / D-09 / INT-04: the presentation -------------------------------

    @Test
    fun theScrimGoesUpAtOnceAndTheAdShowsOnlyAfterTheFullQuiescencePause() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            runCurrent()

            assertThat(viewModel.uiState.value.adScrimVisible).isTrue()

            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS - 1)
            runCurrent()
            assertThat(gateway.showCount).isEqualTo(0)

            advanceTimeBy(1)
            runCurrent()
            assertThat(gateway.showCount).isEqualTo(1)
        }

    @Test
    fun navigationWaitsForTheAdAndTheScrimIsAlreadyDownWhenItArrives() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()

            // D-09: the ad is on screen and navigation has NOT happened.
            assertThat(gateway.showCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.pendingNavigation).isNull()
            assertThat(viewModel.uiState.value.adScrimVisible).isTrue()

            gateway.releaseShow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation).isEqualTo(ResultsNavTarget.Catalog)
            assertThat(viewModel.uiState.value.adScrimVisible).isFalse()

            viewModel.onNavigationHandled()
            assertThat(viewModel.uiState.value.pendingNavigation).isNull()
        }

    // ---- D-11 / INT-02: counters advance only on a real presentation -----------

    @Test
    fun aShowThatNeverPresentedNavigatesAnywayAndRecordsNothing() =
        runTest(testDispatcher) {
            // D-11: a held ad that fails at show() is indistinguishable from a no-fill and
            // costs the player nothing — the counters keep running, so the very next
            // eligible win can show one. Recording here would silently spend the quota on
            // an ad nobody saw.
            val gateway = FakeAdGateway(showResult = false)
            val store = eligibleCadenceStore(backgroundScope)

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = store,
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()
            gateway.releaseShow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation).isEqualTo(ResultsNavTarget.Catalog)

            val settled = settledState(store)
            assertThat(settled.lastFullscreenAdAtMs).isEqualTo(InterstitialCadenceState.NEVER_SHOWN)
            assertThat(settled.winsSinceLastInterstitial)
                .isEqualTo(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS)
        }

    @Test
    fun aShowThatPresentedRecordsTheInterstitial() =
        runTest(testDispatcher) {
            val gateway = FakeAdGateway()
            val store = eligibleCadenceStore(backgroundScope)

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    cadenceStore = store,
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()
            gateway.releaseShow()
            advanceUntilIdle()

            val recorded = store.state.first { it.winsSinceLastInterstitial == 0 }
            assertThat(recorded.lastFullscreenAdAtMs)
                .isNotEqualTo(InterstitialCadenceState.NEVER_SHOWN)
        }

    @Test
    fun aSecondContinueTapWhileTheAdIsInFlightIsANoOp() =
        runTest(testDispatcher) {
            // D-09: the tap's intent is honoured EXACTLY once. The scrim swallows input for
            // the whole presentation, so this guard covers the instant before it renders —
            // and a second navigation across an ad is a Challenge ladder that jumps two.
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = "",
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    challengeLevel = SAMPLE_CHALLENGE_LEVEL,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(gateway.showCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.pendingNavigation).isNull()

            gateway.releaseShow()
            advanceUntilIdle()

            assertThat(gateway.showCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(SAMPLE_CHALLENGE_LEVEL + 1))
        }

    // ---- ADSF-01 ---------------------------------------------------------------

    @Test
    fun theElapsedTimeIsUnchangedAcrossTheWholePresentation() =
        runTest(testDispatcher) {
            // The time carried INTO Results is the time carried OUT of it: no clock runs or
            // restarts across an ad, so a player cannot be charged decay for watching one.
            val gateway = FakeAdGateway()

            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
                    cadenceStore = eligibleCadenceStore(backgroundScope),
                    adGateway = gateway,
                )
            advanceUntilIdle()

            val beforeTap = viewModel.uiState.value.elapsedSeconds

            viewModel.onCatalogTapped()
            advanceTimeBy(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
            runCurrent()

            assertThat(viewModel.uiState.value.adScrimVisible).isTrue()
            val duringAd = viewModel.uiState.value.elapsedSeconds

            gateway.releaseShow()
            advanceUntilIdle()

            val onNavigation = viewModel.uiState.value.elapsedSeconds

            assertThat(beforeTap).isEqualTo(SAMPLE_ELAPSED_SECONDS)
            assertThat(duringAd).isEqualTo(beforeTap)
            assertThat(onNavigation).isEqualTo(beforeTap)
            assertThat(viewModel.uiState.value.pendingNavigation).isNotNull()
        }

    // --- solve telemetry (D-08/D-09) --------------------------------------------

    @Test
    fun aResultsRouteWithoutTheTelemetryArgsReadsAsNoTelemetry() {
        // The three route defaults are load-bearing, not cosmetic: they are what makes a
        // Results destination reached without telemetry read as NONE rather than crash.
        val viewModel = viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = THREE_STARS)

        assertThat(viewModel.solveTelemetry).isEqualTo(SolveTelemetry.NONE)
    }

    @Test
    fun theTelemetryArgsResolveIntoExactlyThatTelemetry() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                hintsUsed = SAMPLE_HINTS_USED,
                undosUsed = SAMPLE_UNDOS_USED,
                lastPegOnTarget = true,
            )

        assertThat(viewModel.solveTelemetry)
            .isEqualTo(
                SolveTelemetry(
                    hintsUsed = SAMPLE_HINTS_USED,
                    undosUsed = SAMPLE_UNDOS_USED,
                    lastPegOnTarget = true,
                ),
            )
    }

    @Test
    fun aNegativeCounterArgIsResolvedAgainstPurityRatherThanRefusedOrZeroed() {
        // T-12-20 / WR-03: `Results` is registered in `AppNavHost` independently of
        // `GameRoute`, so its args are untrusted in their own right. The posture is the
        // codebase's existing one — resolve, never refuse — because refusing would convert
        // a one-byte edit into a crashed Results screen and a lost recorded best for an
        // honest player. But the resolution must not be ZERO: zero is precisely what the
        // Hint-free / Undo-free predicates reward, so it would pay the forger.
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                hintsUsed = FORGED_NEGATIVE_COUNTER,
                undosUsed = FORGED_NEGATIVE_COUNTER,
            )

        assertThat(viewModel.solveTelemetry)
            .isEqualTo(
                SolveTelemetry(
                    hintsUsed = SolveTelemetry.UNACCOUNTED_ASSIST,
                    undosUsed = SolveTelemetry.UNACCOUNTED_ASSIST,
                    lastPegOnTarget = false,
                ),
            )
        assertThat(viewModel.solveTelemetry).isNotEqualTo(SolveTelemetry.NONE)
    }

    @Test
    fun aLargeForgedCounterArgPassesThroughUnchanged() {
        // Nothing is capped at the top: only the negative case is resolved, and a
        // forged-high count only denies the tamperer their own badge.
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                hintsUsed = FORGED_LARGE_COUNTER,
            )

        assertThat(viewModel.solveTelemetry.hintsUsed).isEqualTo(FORGED_LARGE_COUNTER)
    }

    @Test
    fun theTelemetryArgsAreInertForEverythingButSolveTelemetry() =
        runTest(testDispatcher) {
            // The whole screen and the whole write path must be byte-identical with and
            // without telemetry — including a FORGED one. This plan adds a value; it
            // changes no behaviour, and `recordResult` keeps its current signature (12-06).
            val plainRepository = FakeProgressRepository()
            val plain =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    repository = plainRepository,
                )
            advanceUntilIdle()

            val forgedRepository = FakeProgressRepository()
            val forged =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    repository = forgedRepository,
                    hintsUsed = FORGED_NEGATIVE_COUNTER,
                    undosUsed = FORGED_LARGE_COUNTER,
                    lastPegOnTarget = true,
                )
            advanceUntilIdle()

            // 12-06 gave recordResult a telemetry parameter, so the two write paths are
            // deliberately no longer byte-identical: everything SCORING-related still is,
            // and the telemetry is the one field that legitimately differs.
            assertThat(forgedRepository.recordCalls.map { it.result })
                .isEqualTo(plainRepository.recordCalls.map { it.result })
            assertThat(forgedRepository.recordCalls.map { it.mode to it.levelId })
                .isEqualTo(plainRepository.recordCalls.map { it.mode to it.levelId })
            assertThat(plainRepository.recordCalls.single().telemetry).isEqualTo(SolveTelemetry.NONE)
            assertThat(forged.uiState.value).isEqualTo(plain.uiState.value)
            // The one and only difference: the resolved telemetry — and it is exactly what
            // reached the repository. The forged NEGATIVE hint count resolves to one
            // unaccounted assist, not to zero (WR-03), while the forged large undo count
            // passes through verbatim.
            assertThat(forgedRepository.recordCalls.single().telemetry).isEqualTo(forged.solveTelemetry)
            assertThat(forged.solveTelemetry)
                .isEqualTo(
                    SolveTelemetry(
                        hintsUsed = SolveTelemetry.UNACCOUNTED_ASSIST,
                        undosUsed = FORGED_LARGE_COUNTER,
                        lastPegOnTarget = true,
                    ),
                )
        }

    @Test
    fun aSolvedResultPassesTheResolvedTelemetryToTheRepositoryVerbatim() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            val viewModel =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    hintsUsed = SAMPLE_HINTS_USED,
                    undosUsed = SAMPLE_UNDOS_USED,
                    lastPegOnTarget = true,
                    repository = repository,
                )
            advanceUntilIdle()

            // D-08: the value 12-05 already coerced at the nav boundary is handed straight
            // through — this ViewModel neither re-coerces it nor invents its own.
            assertThat(repository.recordCalls).hasSize(1)
            assertThat(repository.recordCalls.single().telemetry).isEqualTo(viewModel.solveTelemetry)
            assertThat(repository.recordCalls.single().telemetry)
                .isEqualTo(
                    SolveTelemetry(
                        hintsUsed = SAMPLE_HINTS_USED,
                        undosUsed = SAMPLE_UNDOS_USED,
                        lastPegOnTarget = true,
                    ),
                )
        }

    @Test
    fun aSecondViewModelOverTheSameHandleWritesNoSecondTelemetryRow() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            val handle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    hintsUsed = SAMPLE_HINTS_USED,
                    undosUsed = SAMPLE_UNDOS_USED,
                    lastPegOnTarget = true,
                )
            viewModelOver(handle = handle, repository = repository)
            advanceUntilIdle()
            viewModelOver(handle = handle, repository = repository)
            advanceUntilIdle()

            // The existing exactly-once guard is INHERITED, not duplicated (Pattern 2).
            assertThat(repository.recordCalls).hasSize(1)
        }

    @Test
    fun aStuckFinishWritesNoTelemetryAtAll() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = ONE_STAR,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                hintsUsed = SAMPLE_HINTS_USED,
                undosUsed = SAMPLE_UNDOS_USED,
                lastPegOnTarget = true,
                repository = repository,
            )
            advanceUntilIdle()

            // Only solved plays are recorded (D-08), so a stuck finish leaves neither a
            // best row nor a telemetry row.
            assertThat(repository.recordCalls).isEmpty()
        }

    // --- the Play Games submission hook (PGSL-04, D-13) -------------------------

    @Test
    fun aSolvedResultSubmitsExactlyOnceAndOnlyAfterTheRecordReturns() =
        runTest(testDispatcher) {
            val log = mutableListOf<String>()
            val submitter = recordingSubmitter(log = log)
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                repository = FakeProgressRepository(log = log),
                progressSubmitter = submitter,
            )
            advanceUntilIdle()

            // ONE shared list, so this is an ordering claim rather than two counters that
            // happen to both read 1: the screen's own state is committed before anything
            // leaves for the network.
            assertThat(log).containsExactly(RECORD_EVENT, SUBMIT_EVENT).inOrder()
            assertThat(submitter.callCount).isEqualTo(1)
        }

    @Test
    fun aStuckFinishSubmitsNothingAtAll() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            val submitter = recordingSubmitter()
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = ONE_STAR,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                repository = repository,
                progressSubmitter = submitter,
            )
            advanceUntilIdle()

            // The hook sits INSIDE the existing D-08 solved-play guard, so a board that
            // never folded submits nothing for the same reason it banks nothing.
            assertThat(repository.recordCalls).isEmpty()
            assertThat(submitter.callCount).isEqualTo(0)
        }

    @Test
    fun aProcessDeathRestoreSubmitsNothingFurther() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository()
            val submitter = recordingSubmitter()
            val handle =
                savedStateHandleFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                )
            viewModelOver(handle = handle, repository = repository, progressSubmitter = submitter)
            advanceUntilIdle()
            viewModelOver(handle = handle, repository = repository, progressSubmitter = submitter)
            advanceUntilIdle()

            // The submission INHERITS the RECORDED_KEY latch rather than adding a fourth
            // guard of its own — which is the whole reason the hook lives here and not in
            // GameViewModel, whose finished latch is reset by every undo.
            assertThat(repository.recordCalls).hasSize(1)
            assertThat(submitter.callCount).isEqualTo(1)
        }

    @Test
    fun aSubmitterWhoseEverySdkCallThrowsLeavesTheBankedResultAndTheWholeScreenUntouched() =
        runTest(testDispatcher) {
            // PGSL-04, and the load-bearing case of this plan.
            //
            // The failing submitter here is the REAL `ProgressSubmitter`, wired to a seam
            // that throws on every call — not a fake that throws out of
            // `onResultRecorded()` itself. That distinction is the whole point rather than
            // a convenience. Production's `onResultRecorded()` is non-suspend and its entire
            // body is a launch on the application scope, so it *cannot* throw at this
            // coroutine; a fake that did would be asserting the ViewModel defends against a
            // violated contract, and the only way to pass would be the defensive
            // `runCatching` at the call site that this plan's grep gate forbids — which
            // would then hide any real regression in the fire-and-forget shape. The shape
            // itself is pinned by `ProgressSubmitterTest` and by those greps; what is
            // asserted here is the reachable failure: an SDK that explodes mid-pass.
            //
            // The two ViewModels differ in exactly one thing — whether every submission
            // throws — and the claim is that nothing the player can observe differs.
            val healthyRepository = FakeProgressRepository()
            val healthy =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    repository = healthyRepository,
                    progressSubmitter = recordingSubmitter(),
                )
            advanceUntilIdle()

            val explodingRepository = FakeProgressRepository()
            val exploding =
                viewModelFor(
                    levelId = ClassicLevelIds.ENGLISH33,
                    stars = THREE_STARS,
                    pegsRemaining = SOLVED_PEGS_REMAINING,
                    repository = explodingRepository,
                    progressSubmitter = submitterOverAThrowingSeam(),
                )
            advanceUntilIdle()

            // The `runTest` body reaching here at all is half the claim: nothing escaped
            // into the record coroutine or onto the application scope.
            assertThat(exploding.uiState.value).isEqualTo(healthy.uiState.value)
            assertThat(exploding.uiState.value.newBests).isEqualTo(NewBests(stars = true, time = true, score = true))
            assertThat(explodingRepository.recordCalls).isEqualTo(healthyRepository.recordCalls)
        }

    // ---- Harness ---------------------------------------------------------------

    private fun viewModelFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
        challengeLevel: Int? = null,
        minimumPegs: Int = DEFAULT_MINIMUM_PEGS,
        hintsUsed: Int = NO_TELEMETRY_COUNT,
        undosUsed: Int = NO_TELEMETRY_COUNT,
        lastPegOnTarget: Boolean = false,
        repository: ProgressRepository = FakeProgressRepository(),
        progressSubmitter: ProgressSubmitter = recordingSubmitter(),
        challengeProgress: ChallengeProgress = freshLadder(),
        cadenceStore: InterstitialCadenceStore = freshCadenceStore(),
        adGateway: AdGateway = FakeAdGateway(interstitialReadyInitially = false),
    ): ResultsViewModel =
        ResultsViewModel(
            savedStateHandle =
                savedStateHandleFor(
                    levelId = levelId,
                    stars = stars,
                    pegsRemaining = pegsRemaining,
                    elapsedSeconds = elapsedSeconds,
                    score = score,
                    challengeLevel = challengeLevel,
                    minimumPegs = minimumPegs,
                    hintsUsed = hintsUsed,
                    undosUsed = undosUsed,
                    lastPegOnTarget = lastPegOnTarget,
                ),
            progressRepository = repository,
            progressSubmitter = progressSubmitter,
            challengeProgress = challengeProgress,
            cadenceStore = cadenceStore,
            adGateway = adGateway,
        )

    /**
     * A ViewModel over an already-built [handle] — the shape the process-death and
     * back-navigation cases need.
     *
     * The ad defaults are deliberately the inert pair: a [freshCadenceStore] is inside D-03's
     * first-session suppression and a [FakeAdGateway] holding no ad takes the D-10 fast
     * path, so every case that predates Phase 10 keeps exactly the meaning it had — none of
     * them can accidentally acquire an ad.
     */
    private fun viewModelOver(
        handle: SavedStateHandle,
        repository: ProgressRepository,
        progressSubmitter: ProgressSubmitter = recordingSubmitter(),
        challengeProgress: ChallengeProgress = freshLadder(),
        cadenceStore: InterstitialCadenceStore = freshCadenceStore(),
        adGateway: AdGateway = FakeAdGateway(interstitialReadyInitially = false),
    ): ResultsViewModel =
        ResultsViewModel(
            savedStateHandle = handle,
            progressRepository = repository,
            progressSubmitter = progressSubmitter,
            challengeProgress = challengeProgress,
            cadenceStore = cadenceStore,
            adGateway = adGateway,
        )

    /**
     * A fresh player's Challenge ladder (level 1), over an in-memory store.
     *
     * Every case in this file is about the args, the record, the celebration or the ad — none is
     * about the ladder, so the default is the identity: with the ladder at 1,
     * `max(head, N + 1)` is always `N + 1` and the pre-D-04a expectations still hold verbatim.
     * D-04a's own behaviour is specified in `ChallengeNextLevelTest`.
     */
    private fun freshLadder(): ChallengeProgress = ChallengeProgress(inMemoryDataStore(), storeScope)

    /**
     * A [RecordingProgressSubmitter] with every constructor argument the real
     * `ProgressSubmitter` demands, in one place.
     *
     * Threading a repository, a games seam, four stores and a scope through forty call sites
     * would be noise, and widening production visibility to avoid it would be worse — the
     * arguments are real types precisely so this fake cannot drift from the constructor it
     * stands in for. None of them is ever touched: the subclass overrides both public
     * methods, and the base class does nothing in its own constructor.
     */
    private fun recordingSubmitter(log: MutableList<String> = mutableListOf()): RecordingProgressSubmitter =
        RecordingProgressSubmitter(
            log = log,
            progressRepository = FakeProgressRepository(),
            gamesService = FakeGamesService(),
            syncStore = SubmissionSyncStore(unusedDataStore()),
            scope = storeScope,
            cloudSyncStore = CloudSyncStore(unusedDataStore()),
            cloudProfileStore = CloudProfileStore(unusedDataStore()),
            cloudSaveStore = FakeCloudSaveStore(),
            challengeProgress = ChallengeProgress(unusedDataStore(), storeScope),
        )

    /**
     * A **real** [ProgressSubmitter] whose every SDK call throws — the PGSL-04 fixture.
     *
     * The aggregate is deliberately non-empty (one solved level, three stars), because a
     * fresh-player aggregate issues zero calls and would give a throwing seam nothing to
     * throw from. The sync store is the real one over a store that also throws, so even the
     * dirty-flag write on the failure path is a failing write — and none of it reaches the
     * Results screen.
     */
    private fun submitterOverAThrowingSeam(): ProgressSubmitter =
        ProgressSubmitter(
            progressRepository =
                FakeProgressRepository(
                    aggregate = ProgressAggregate(solvedLevelCount = 1, totalStars = THREE_STARS),
                ),
            gamesService = ThrowingGamesService(),
            syncStore = SubmissionSyncStore(unusedDataStore()),
            // Phase 13's cloud collaborators, each over the SAME throwing store, so the
            // cloud half of the pass is as hostile as the submission half and still reaches
            // the Results screen with nothing (CLOUD-05). The saved-games seam is the empty
            // fake: `null` from a load is "stay local", which is the quiet path.
            cloudSyncStore = CloudSyncStore(unusedDataStore()),
            cloudProfileStore = CloudProfileStore(unusedDataStore()),
            cloudSaveStore = FakeCloudSaveStore(),
            challengeProgress = ChallengeProgress(unusedDataStore(), storeScope),
            scope = storeScope,
        )

    /** A brand-new install's cadence: `firstSessionComplete` false, so `mayShow` is never true. */
    private fun freshCadenceStore(): InterstitialCadenceStore = InterstitialCadenceStore(realDataStore(), storeScope)

    /**
     * A cadence store seeded into a fully ad-eligible state, settled before it is returned.
     *
     * Seeded through the store's own PUBLIC transitions rather than by writing preference
     * keys: `recordWin()` saturates both lifetime counters at their thresholds, and the
     * `recordForegroundLeft` / `startSessionIfGapElapsed` pair closes the install's first
     * session exactly the way `AppForegroundTracker` does at runtime. A fixture built from
     * real transitions cannot drift away from the policy it is meant to satisfy, whereas one
     * that pokes keys directly would keep passing after the policy changed underneath it.
     *
     * The trailing `first { it.mayShow(now) }` is the barrier: it returns only once the
     * seeding writes have landed, so a ViewModel constructed after it reads a settled value
     * rather than racing the disk.
     */
    private suspend fun eligibleCadenceStore(scope: CoroutineScope): InterstitialCadenceStore {
        val store = InterstitialCadenceStore(realDataStore(), scope)
        val now = System.currentTimeMillis()
        repeat(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS) { store.recordWin() }
        store.recordForegroundLeft(now - InterstitialCadenceParams.SESSION_GAP_MS - 1)
        store.startSessionIfGapElapsed(now)
        store.state.first { it.mayShow(now) }
        return store
    }

    /**
     * [store]'s cadence state with every queued write applied — the 10-02 sequencing-barrier
     * pattern, which is what makes a "this recorded nothing" assertion mean anything.
     *
     * A negative assertion can otherwise pass simply by running before the write it is meant
     * to rule out. DataStore serialises `updateData`, so issuing a LATER write and awaiting
     * that one proves anything launched earlier has already been applied. The barrier writes
     * the foreground stamp specifically because no cadence gate reads it — it settles the
     * queue without disturbing a single field under assertion.
     */
    private suspend fun TestScope.settledState(store: InterstitialCadenceStore): InterstitialCadenceState {
        store.recordForegroundLeft(WRITE_BARRIER_STAMP_MS)
        store.lastForegroundAtMs.first { it == WRITE_BARRIER_STAMP_MS }
        advanceUntilIdle()
        return store.state.value
    }

    /** A real temp-file Preferences store, one per call (the `ChallengeProgressTest` harness). */
    private fun realDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("cadence_${storeFileCount++}.preferences_pb") },
        )

    private fun savedStateHandleFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
        challengeLevel: Int? = null,
        minimumPegs: Int = DEFAULT_MINIMUM_PEGS,
        hintsUsed: Int = NO_TELEMETRY_COUNT,
        undosUsed: Int = NO_TELEMETRY_COUNT,
        lastPegOnTarget: Boolean = false,
    ): SavedStateHandle =
        SavedStateHandle(
            buildMap {
                put(LEVEL_ID_ARG, levelId)
                put(STARS_ARG, stars)
                put(PEGS_REMAINING_ARG, pegsRemaining)
                put(ELAPSED_SECONDS_ARG, elapsedSeconds)
                put(SCORE_ARG, score)
                if (challengeLevel != null) put(CHALLENGE_LEVEL_ARG, challengeLevel)
                // Same discipline as the telemetry args below: the per-board floor is put
                // into the map ONLY when it differs from the route default, so the cases
                // that predate CR-01 genuinely exercise `Results`' own one-peg default.
                if (minimumPegs != DEFAULT_MINIMUM_PEGS) put(MINIMUM_PEGS_ARG, minimumPegs)
                // Each telemetry arg is put into the map ONLY when it differs from the
                // route default, so the "absent args" case genuinely exercises the
                // `Results` defaults rather than an explicit zero that happens to match.
                if (hintsUsed != NO_TELEMETRY_COUNT) put(HINTS_USED_ARG, hintsUsed)
                if (undosUsed != NO_TELEMETRY_COUNT) put(UNDOS_USED_ARG, undosUsed)
                if (lastPegOnTarget) put(LAST_PEG_ON_TARGET_ARG, true)
            },
        )
}

/** One observed [ProgressRepository.recordResult] invocation. */
private data class RecordCall(
    val mode: String,
    val levelId: String,
    val result: ScoreResult,
    val telemetry: SolveTelemetry,
)

/**
 * An in-memory [ProgressRepository] reproducing the real per-field best merge (D-07):
 * stars and score keep the maximum, time the minimum, and [NewBests] reports which of
 * the three the incoming play actually improved. Every record call is captured so the
 * "exactly once" and mode-keying contracts can be asserted. The resume members are
 * unused by the Results screen.
 */
private class FakeProgressRepository(
    private val log: MutableList<String> = mutableListOf(),
    private val aggregate: ProgressAggregate = ProgressAggregate(),
) : ProgressRepository {
    val recordCalls = mutableListOf<RecordCall>()

    private val stored = mutableMapOf<Pair<String, String>, LevelResult>()

    /** Pre-stores an existing best so a replay can be measured against it. */
    fun seed(
        mode: String,
        levelId: String,
        stars: Int,
        bestTimeMs: Long,
        bestScore: Int,
    ) {
        stored[mode to levelId] =
            LevelResult(
                mode = mode,
                levelId = levelId,
                stars = stars,
                bestTimeMs = bestTimeMs,
                bestScore = bestScore,
                completedAt = 0L,
            )
    }

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests {
        recordCalls += RecordCall(mode = mode, levelId = levelId, result = result, telemetry = telemetry)
        log += RECORD_EVENT
        val key = mode to levelId
        val previous = stored[key]
        val timeMs = result.elapsedSeconds * MILLIS_IN_SECOND
        val newBests =
            NewBests(
                stars = previous == null || result.stars > previous.stars,
                time = previous == null || timeMs < previous.bestTimeMs,
                score = previous == null || result.score > previous.bestScore,
            )
        stored[key] =
            LevelResult(
                mode = mode,
                levelId = levelId,
                stars = maxOf(result.stars, previous?.stars ?: 0),
                bestTimeMs = minOf(timeMs, previous?.bestTimeMs ?: Long.MAX_VALUE),
                bestScore = maxOf(result.score, previous?.bestScore ?: 0),
                completedAt = if (newBests.any) nowMs else previous?.completedAt ?: nowMs,
            )
        return newBests
    }

    /**
     * Unread by the Results screen itself — only by a [ProgressSubmitter] built over this
     * repository, which is why it is settable: an empty aggregate issues no submissions.
     */
    override suspend fun aggregate(): ProgressAggregate = aggregate

    /** Unused by this screen — no per-board leaderboard read happens here. */
    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeResults(mode: String): Flow<List<LevelResult>> =
        flowOf(stored.values.filter { it.mode == mode })

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Results never saves a resume snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = error("Results never loads a resume snapshot")

    override suspend fun clearSnapshot(mode: String) = error("Results never clears a resume snapshot")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())

    /**
     * The fresh-player payload a push would write.
     *
     * Results still never *calls* cloud save — but the solve it records triggers
     * `ProgressSubmitter`'s pass, and Phase 13 gave that pass a push (D-01). This returns the
     * empty shape rather than throwing, because a throw here would land in the
     * application-scoped coroutine driving the pass and take the scope with it, which is
     * exactly the failure mode CLOUD-05 says must not exist.
     */
    override suspend fun exportProgress(ladder: Int): CloudProgress = CloudProgress(ladder = ladder)

    /** Unreachable from here — the fake saved-games seam holds no payload to import. */
    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("Results never imports a cloud payload")

    override suspend fun resetAllProgress() = error("Results never answers the profile-swap dialog (D-14)")
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The `Results(...)` route arg keys (the route's property names). */
private const val LEVEL_ID_ARG = "levelId"
private const val STARS_ARG = "stars"
private const val PEGS_REMAINING_ARG = "pegsRemaining"
private const val ELAPSED_SECONDS_ARG = "elapsedSeconds"
private const val SCORE_ARG = "score"
private const val CHALLENGE_LEVEL_ARG = "challengeLevel"
private const val MINIMUM_PEGS_ARG = "minimumPegs"
private const val HINTS_USED_ARG = "hintsUsed"
private const val UNDOS_USED_ARG = "undosUsed"
private const val LAST_PEG_ON_TARGET_ARG = "lastPegOnTarget"

/**
 * The `Results` route's own default perfect-finish floor: one peg, which is every shipped
 * board's floor except Diamond-41's proven two (CR-01).
 */
private const val DEFAULT_MINIMUM_PEGS = 1

/** The `Results` route's own default for both telemetry counters (D-08). */
private const val NO_TELEMETRY_COUNT = 0

/** A play that took two hints — the non-zero pass-through fixture. */
private const val SAMPLE_HINTS_USED = 2

/** A play that took three undos — the non-zero pass-through fixture. */
private const val SAMPLE_UNDOS_USED = 3

/** T-12-20 forgery: a negative counter arg, resolved against purity rather than refused. */
private const val FORGED_NEGATIVE_COUNTER = -4

/** T-12-20 forgery: a large in-range counter arg, preserved verbatim. */
private const val FORGED_LARGE_COUNTER = 9_999

/** CR-01 forgery: a floor above every shipped board's, clamped down rather than obeyed. */
private const val FORGED_HUGE_MINIMUM_PEGS = 41

/** CR-01 forgery: a floor below one, clamped up so an honest solve still banks. */
private const val FORGED_NON_POSITIVE_MINIMUM_PEGS = 0

/** Earned-star counts for the three D-08 headline tiers. */
private const val THREE_STARS = 3
private const val TWO_STARS = 2
private const val ONE_STAR = 1

/** Arbitrary pass-through metric values asserted to survive verbatim. */
private const val SAMPLE_PEGS_REMAINING = 3
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1234

/** A solved board leaves exactly one peg — the sole state that unlocks the next level (D-01). */
private const val SOLVED_PEGS_REMAINING = 1

/**
 * What a PERFECT Diamond-41 finish leaves on the board.
 *
 * Two, not one: Diamond-41's proven floor is two pegs (Phase 8, GF(4) colour invariant), so
 * this is a flawless play and not a stuck one. It is the fixture RESEARCH Pitfall 1 exists
 * for — an interstitial gate written as `pegsRemaining == 1` denies exactly this player.
 */
private const val DIAMOND_41_PERFECT_PEGS_REMAINING = 2

/**
 * An arbitrary, recognisable foreground stamp used only to settle the DataStore write queue.
 *
 * A real epoch value (well inside `fromStored`'s trusted `0 until Long.MAX_VALUE` range, so
 * it is never read back as the sentinel) and far enough in the past that it can never
 * collide with a stamp a fixture derives from `System.currentTimeMillis()`.
 */
private const val WRITE_BARRIER_STAMP_MS = 1_700_000_000_000L

/** An arbitrary in-progress Challenge level number. */
private const val SAMPLE_CHALLENGE_LEVEL = 5

/** A forged sub-1 `Results` Challenge arg that must clamp to the lowest valid level (WARN-03). */
private const val BELOW_MIN_CHALLENGE_LEVEL = 0

/** A previously stored best that the sample play cannot beat (faster time, higher score). */
private const val FAST_ELAPSED_SECONDS = 10L
private const val HIGH_SCORE = 9999

/** `ScoreResult` reports whole seconds; stored best times are milliseconds. */
private const val MILLIS_IN_SECOND = 1_000L

/**
 * A [ProgressSubmitter] that records the hook instead of talking to Play Games — and, on
 * demand, throws from it.
 *
 * [log] is deliberately the SAME list [FakeProgressRepository] appends to, so
 * "recorded before submitted" is a single ordered comparison rather than two independent
 * counters that could both be satisfied in the wrong order.
 *
 * It never throws. The PGSL-04 failure case is driven through the real submitter over a
 * [ThrowingGamesService] instead, because production's `onResultRecorded()` cannot throw at
 * its caller — its whole body is a launch on the application scope — so a fake that did
 * would only be asserting the ViewModel defends against a violated contract.
 *
 * [start] is a no-op: it belongs to `MainActivity`, which no JVM test constructs.
 */
@Suppress("LongParameterList") // mirrors the superclass's, which is suppressed for the same reason
private class RecordingProgressSubmitter(
    private val log: MutableList<String>,
    progressRepository: ProgressRepository,
    gamesService: FakeGamesService,
    syncStore: SubmissionSyncStore,
    scope: CoroutineScope,
    cloudSyncStore: CloudSyncStore,
    cloudProfileStore: CloudProfileStore,
    cloudSaveStore: CloudSaveStore,
    challengeProgress: ChallengeProgress,
) : ProgressSubmitter(
        progressRepository,
        gamesService,
        syncStore,
        cloudSyncStore,
        cloudProfileStore,
        cloudSaveStore,
        challengeProgress,
        scope,
    ) {
    var callCount = 0
        private set

    override fun start() = Unit

    override fun onResultRecorded() {
        callCount++
        log += SUBMIT_EVENT
    }
}

/**
 * A signed-in [GamesService] whose three submission methods throw instead of reporting —
 * the SDK failure the real [ProgressSubmitter] must absorb entirely.
 *
 * Delegation supplies the rest of the seam unchanged, so this file names only the three
 * methods the failure is actually about.
 */
private class ThrowingGamesService(
    private val delegate: FakeGamesService = FakeGamesService(GamesSignInState.SignedIn(SUBMIT_TEST_PLAYER_ID)),
) : GamesService by delegate {
    override suspend fun submitScore(
        leaderboardId: String,
        score: Long,
    ): Boolean = throw SubmitterFailure()

    override suspend fun unlockAchievement(achievementId: String): Boolean = throw SubmitterFailure()

    override suspend fun setAchievementSteps(
        achievementId: String,
        steps: Int,
    ): Boolean = throw SubmitterFailure()
}

/** What [ThrowingGamesService] throws; a named type rather than a bare generic exception. */
private class SubmitterFailure : RuntimeException("the seam exploded")

/** The signed-in player id the PGSL-04 fixture carries; no assertion depends on it. */
private const val SUBMIT_TEST_PLAYER_ID = "results-test-player"

/**
 * A Preferences [DataStore] that lives entirely in memory and commits on the calling coroutine.
 *
 * Deliberately not `PreferenceDataStoreFactory` over a `TemporaryFolder` (the cadence fixtures
 * above): that store commits on `Dispatchers.IO`, which `advanceUntilIdle()` cannot drive, so a
 * suspending ladder read would resume at a moment virtual time knows nothing about.
 */
private fun inMemoryDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

/**
 * A [DataStore] that exists only to satisfy the real `SubmissionSyncStore` constructor the
 * submitter fake must call. Every use throws, because none happens.
 */
private fun unusedDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("the fake never reads the store") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("the fake never writes the store")
    }

/** The two entries of the shared ordered log — a bank, then the submission it triggers. */
private const val RECORD_EVENT = "record"
private const val SUBMIT_EVENT = "submit"
