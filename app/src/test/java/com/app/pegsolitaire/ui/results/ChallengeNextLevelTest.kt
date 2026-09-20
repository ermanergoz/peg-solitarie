package com.app.pegsolitaire.ui.results

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ChallengeProgressPreferences
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.CloudProfileStore
import com.app.pegsolitaire.data.CloudSyncStore
import com.app.pegsolitaire.data.ImportOutcome
import com.app.pegsolitaire.data.InterstitialCadenceStore
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.data.SubmissionSyncStore
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.save.FakeCloudSaveStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * The D-04a contract for the Challenge "Next level" action: it serves
 * **`max(challenge_current_level, N + 1)`**, where `N` is the level just solved and the
 * ladder is read *after* `GameViewModel.advancePast(N)` has been applied.
 *
 * ## Why this file exists
 *
 * Phase 13's D-04 keeps a stale in-progress Challenge game when a merged cloud save moves the
 * ladder ahead of it: the Continue tile still resumes level 11 while the ladder head is 13.
 * Solving that level used to offer "Next level" → 12 — a level the player had already
 * completed on the other device — because the number was computed as `challengeLevel + 1` in
 * `ResultsActions`, a stateless leaf with no access to the ladder. At a realistic merge gap
 * (D-04's own worked example is a head of 50 under a resumed level 10) that is dozens of
 * consecutive replays of already-solved boards, each ending on a Results screen that can award
 * no new star. The game-designer ruled it a defect against D-04's own text (**D-04a**).
 *
 * Two properties of the rule are load-bearing and each has a test below:
 *
 *  - **The ladder head wins when it is ahead**, so a behind-the-head finish rejoins the head
 *    instead of walking forward through completed levels.
 *  - **`N + 1` is kept inside the `max`**, so a ladder read that has not yet seen
 *    `advancePast(N)` — the documented WR-01 ladder/load-window race — can never re-serve the
 *    level just played. That state is unreachable by construction rather than unlikely.
 *
 * Nothing is told to the player: D-03 (silent restore) stands unamended and the action label
 * carries no number, so no on-screen promise of `N + 1` is ever made to be broken.
 *
 * The ladder is read with the **awaitable** [ChallengeProgress.settledLevel], never
 * `currentLevel.value`, so the read is ordered after the `advancePast` write rather than after
 * whenever the `stateIn` cache happens to re-emit (tech-lead sign-off, FU-3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class ChallengeNextLevelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val storeScope = CoroutineScope(testDispatcher)

    /**
     * The app-lifetime scope every [ChallengeProgress] built here runs on — over its **own**
     * dispatcher, which no test advances. See [ladderAt] for why that is the point.
     */
    private val ladderScope = CoroutineScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        ladderScope.cancel()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun nextLevelIsLadderHeadAfterSolvingBehindTheHead() =
        runTest(testDispatcher) {
            // D-04's shape: the merged ladder head is ahead of the resumed board.
            val progress = ladderAt(MERGED_HEAD_LEVEL)

            val viewModel = viewModelFor(challengeLevel = BEHIND_HEAD_LEVEL, challengeProgress = progress)
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(MERGED_HEAD_LEVEL))
        }

    @Test
    fun nextLevelIsSuccessorWhenSolvedLevelIsTheHead() =
        runTest(testDispatcher) {
            // Normal play, which is every play on a device that never merged anything: the
            // ladder has already advanced past N, so the head IS N + 1 and behaviour is
            // byte-identical to the pre-ruling build.
            val progress = ladderAt(BEHIND_HEAD_LEVEL + 1)

            val viewModel = viewModelFor(challengeLevel = BEHIND_HEAD_LEVEL, challengeProgress = progress)
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(BEHIND_HEAD_LEVEL + 1))
        }

    @Test
    fun nextLevelNeverServesTheLevelJustPlayed() =
        runTest(testDispatcher) {
            // The pre-mortem's worst failure mode, and the reason the rule is a `max` rather
            // than "serve the head": a ladder read that lands BEFORE advancePast(N) commits
            // still reports N, and serving that would loop the player on one board forever.
            val progress = ladderAt(BEHIND_HEAD_LEVEL)

            val viewModel = viewModelFor(challengeLevel = BEHIND_HEAD_LEVEL, challengeProgress = progress)
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            val target = viewModel.uiState.value.pendingNavigation as ResultsNavTarget.NextLevel
            assertThat(target.level).isGreaterThan(BEHIND_HEAD_LEVEL)
            assertThat(target).isEqualTo(ResultsNavTarget.NextLevel(BEHIND_HEAD_LEVEL + 1))
        }

    @Test
    fun nextLevelNeverServesACompletedLevelWhenTheHeadIsAhead() =
        runTest(testDispatcher) {
            // Every level strictly below the head is completed (the ladder is monotonic and
            // Challenge is solved in order), so "not a completed level" is exactly "not below
            // the head". Asserted over the whole gap rather than at one point, because the
            // damage of the old behaviour scaled with the gap: it served N + 1 for every N.
            val head = MERGED_HEAD_LEVEL
            for (solved in BEHIND_HEAD_LEVEL until head) {
                val viewModel = viewModelFor(challengeLevel = solved, challengeProgress = ladderAt(head))
                advanceUntilIdle()

                viewModel.onNextLevelTapped()
                advanceUntilIdle()

                val target = viewModel.uiState.value.pendingNavigation as ResultsNavTarget.NextLevel
                assertThat(target.level).isAtLeast(head)
            }
        }

    @Test
    fun nextLevelReadsTheSettledLadderRatherThanTheCachedValue() =
        runTest(testDispatcher) {
            // The architecture half of the ruling (tech-lead FU-3). `currentLevel` is a
            // `stateIn` cache seeded with the fail-open default and refreshed by a separate
            // coroutine; `settledLevel()` reads what DataStore committed. The cache here is
            // frozen at the default for the whole test (see `ladderAt`), so a read of
            // `currentLevel.value` would serve N + 1 and this assertion would fail — which is
            // exactly the ordering guarantee "read the ladder after advancePast(N)" needs.
            val progress = ladderAt(MERGED_HEAD_LEVEL)

            val viewModel = viewModelFor(challengeLevel = BEHIND_HEAD_LEVEL, challengeProgress = progress)
            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(progress.currentLevel.value)
                .isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(MERGED_HEAD_LEVEL))
        }

    @Test
    fun nextLevelStaysInsideTheLadderBoundsWhenTheLastLevelIsSolved() =
        runTest(testDispatcher) {
            // The ceiling case: `N + 1` at the top of the ladder would name a level the
            // generator refuses. The clamp is the same WARN-03 boundary every other level
            // number on this screen passes through, so the action cannot point past MAX.
            //
            // Note what this test therefore pins, so nobody reads it as a contradiction of
            // `nextLevelNeverServesTheLevelJustPlayed`: at MAX the clamp re-serves MAX, so that
            // invariant holds for `level < MAX` only. Serving the ceiling again is the right
            // answer for a bound — there is no level above it — and the case is unreachable in
            // practice, because MAX is `GeneratorConstants.MAX_CHALLENGE_LEVEL` = 10,000, well
            // above any level a player hand-reaches. If the ceiling is ever lowered to a
            // reachable number, this expectation is the one to revisit first.
            val progress = ladderAt(ChallengeLevelBounds.MAX)

            val viewModel =
                viewModelFor(challengeLevel = ChallengeLevelBounds.MAX, challengeProgress = progress)
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation)
                .isEqualTo(ResultsNavTarget.NextLevel(ChallengeLevelBounds.MAX))
        }

    @Test
    fun aClassicResultNeverPublishesANextLevelTarget() =
        runTest(testDispatcher) {
            // Classic has no ladder and no "Next level" action (the composable never renders
            // one). Reaching the ViewModel entry point anyway must publish nothing rather than
            // invent a Challenge level 1.
            val viewModel =
                viewModelFor(
                    challengeLevel = null,
                    levelId = ClassicLevelIds.ENGLISH33,
                    challengeProgress = ladderAt(MERGED_HEAD_LEVEL),
                )
            advanceUntilIdle()

            viewModel.onNextLevelTapped()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pendingNavigation).isNull()
        }

    // ---- Harness ---------------------------------------------------------------

    /**
     * A [ChallengeProgress] whose committed ladder is [level], over an in-memory store.
     *
     * Two deliberate choices, both about determinism:
     *
     *  - **In-memory, not `PreferenceDataStoreFactory` over a `TemporaryFolder`.** That store
     *    commits on `Dispatchers.IO`, which `advanceUntilIdle()` cannot drive, so a suspending
     *    ladder read would resume at a moment virtual time knows nothing about
     *    (`ProgressSubmitterCloudTest` makes the same choice for the same reason).
     *  - **A scope this test never advances** ([ladderScope], its own dispatcher). That freezes
     *    `currentLevel`'s `stateIn` cache at the fail-open default for the whole test while the
     *    committed value is [level], which is what makes the settled-versus-cached distinction
     *    *observable* — a ViewModel that read `currentLevel.value` would see 1 and serve
     *    `N + 1`. Reads through `settledLevel()` are unaffected: that suspend function runs on
     *    the *caller's* coroutine, which is the `viewModelScope` this test does advance.
     */
    private fun ladderAt(level: Int): ChallengeProgress =
        ChallengeProgress(
            dataStore = inMemoryDataStore(level),
            scope = ladderScope,
        )

    private fun viewModelFor(
        challengeLevel: Int?,
        challengeProgress: ChallengeProgress,
        levelId: String = "",
    ): ResultsViewModel =
        ResultsViewModel(
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put(LEVEL_ID_ARG, levelId)
                        put(STARS_ARG, THREE_STARS)
                        put(PEGS_REMAINING_ARG, SOLVED_PEGS_REMAINING)
                        put(ELAPSED_SECONDS_ARG, SAMPLE_ELAPSED_SECONDS)
                        put(SCORE_ARG, SAMPLE_SCORE)
                        if (challengeLevel != null) put(CHALLENGE_LEVEL_ARG, challengeLevel)
                    },
                ),
            progressRepository = NoBestsProgressRepository(),
            progressSubmitter = inertSubmitter(),
            challengeProgress = challengeProgress,
            cadenceStore = InterstitialCadenceStore(inMemoryDataStore(level = null), storeScope),
            // Holds no ad, so every tap takes the D-10 fast path and no quiescence pause sits
            // between the tap and the published target.
            adGateway = FakeAdGateway(interstitialReadyInitially = false),
        )

    /**
     * A [ProgressSubmitter] whose one hook does nothing: this file is about the navigation
     * arithmetic, and a real pass would drag the whole cloud seam into every case.
     */
    private fun inertSubmitter(): ProgressSubmitter =
        object : ProgressSubmitter(
            NoBestsProgressRepository(),
            FakeGamesService(),
            SubmissionSyncStore(unusedDataStore()),
            CloudSyncStore(unusedDataStore()),
            CloudProfileStore(unusedDataStore()),
            FakeCloudSaveStore(),
            ChallengeProgress(unusedDataStore(), storeScope),
            storeScope,
        ) {
            override fun start() = Unit

            override fun onResultRecorded() = Unit
        }
}

/**
 * An in-memory Preferences [DataStore] that commits on the calling coroutine, optionally
 * pre-seeded with a committed Challenge ladder [level].
 */
private fun inMemoryDataStore(level: Int?): DataStore<Preferences> =
    object : DataStore<Preferences> {
        private val state =
            MutableStateFlow(
                if (level == null) {
                    emptyPreferences()
                } else {
                    preferencesOf(ChallengeProgressPreferences.CURRENT_LEVEL_KEY to level)
                },
            )

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

/** A [DataStore] that exists only to satisfy a constructor; every use throws, because none happens. */
private fun unusedDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("the fake never reads the store") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("the fake never writes the store")
    }

/** Records nothing and improves nothing: the bests merge is specified in `ResultsViewModelTest`. */
private class NoBestsProgressRepository : ProgressRepository {
    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = NewBests(stars = false, time = false, score = false)

    override suspend fun aggregate(): ProgressAggregate = ProgressAggregate()

    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Results never saves a resume snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = error("Results never loads a snapshot")

    override suspend fun clearSnapshot(mode: String) = error("Results never clears a resume snapshot")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())

    override suspend fun exportProgress(ladder: Int): CloudProgress = CloudProgress(ladder = ladder)

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

/** A solved Challenge board leaves one peg, which is every generated footprint's floor. */
private const val SOLVED_PEGS_REMAINING = 1
private const val THREE_STARS = 3
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1234

/** The resumed, behind-the-head level of D-04's scenario (QA observed 11 against a head of 13). */
private const val BEHIND_HEAD_LEVEL = 11

/** The merged ladder head that arrived from the other device — deliberately a gap of ≥ 2. */
private const val MERGED_HEAD_LEVEL = 13
