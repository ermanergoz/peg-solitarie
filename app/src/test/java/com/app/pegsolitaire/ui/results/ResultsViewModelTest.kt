package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class ResultsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
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
            // `ResultsActions` computes `onNextLevel(challengeLevel + 1)` off this value,
            // so clamping it is what stops "Next level" from exceeding the ceiling + 1.
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

            ResultsViewModel(handle, repository)
            runCurrent()
            ResultsViewModel(handle, repository)
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

        val first = ResultsViewModel(handle, repository)
        val second = ResultsViewModel(handle, repository)

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

            val first = ResultsViewModel(handle, repository)
            runCurrent()
            val second = ResultsViewModel(handle, repository)
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

            val first = ResultsViewModel(handle, repository)
            runCurrent()
            val second = ResultsViewModel(handle, repository)
            runCurrent()

            assertThat(first.uiState.value.playCelebration).isTrue()
            assertThat(second.uiState.value.playCelebration).isFalse()
            assertThat(repository.recordCalls).hasSize(1)
        }

    private fun viewModelFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
        challengeLevel: Int? = null,
        repository: ProgressRepository = FakeProgressRepository(),
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
                ),
            progressRepository = repository,
        )

    private fun savedStateHandleFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
        challengeLevel: Int? = null,
    ): SavedStateHandle =
        SavedStateHandle(
            buildMap {
                put(LEVEL_ID_ARG, levelId)
                put(STARS_ARG, stars)
                put(PEGS_REMAINING_ARG, pegsRemaining)
                put(ELAPSED_SECONDS_ARG, elapsedSeconds)
                put(SCORE_ARG, score)
                if (challengeLevel != null) put(CHALLENGE_LEVEL_ARG, challengeLevel)
            },
        )
}

/** One observed [ProgressRepository.recordResult] invocation. */
private data class RecordCall(
    val mode: String,
    val levelId: String,
    val result: ScoreResult,
)

/**
 * An in-memory [ProgressRepository] reproducing the real per-field best merge (D-07):
 * stars and score keep the maximum, time the minimum, and [NewBests] reports which of
 * the three the incoming play actually improved. Every record call is captured so the
 * "exactly once" and mode-keying contracts can be asserted. The resume members are
 * unused by the Results screen.
 */
private class FakeProgressRepository : ProgressRepository {
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
        nowMs: Long,
    ): NewBests {
        recordCalls += RecordCall(mode = mode, levelId = levelId, result = result)
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

    override fun observeResults(mode: String): Flow<List<LevelResult>> =
        flowOf(stored.values.filter { it.mode == mode })

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Results never saves a resume snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = error("Results never loads a resume snapshot")

    override suspend fun clearSnapshot(mode: String) = error("Results never clears a resume snapshot")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())
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

/** An arbitrary in-progress Challenge level number. */
private const val SAMPLE_CHALLENGE_LEVEL = 5

/** A forged sub-1 `Results` Challenge arg that must clamp to the lowest valid level (WARN-03). */
private const val BELOW_MIN_CHALLENGE_LEVEL = 0

/** A previously stored best that the sample play cannot beat (faster time, higher score). */
private const val FAST_ELAPSED_SECONDS = 10L
private const val HIGH_SCORE = 9999

/** `ScoreResult` reports whole seconds; stored best times are milliseconds. */
private const val MILLIS_IN_SECOND = 1_000L
