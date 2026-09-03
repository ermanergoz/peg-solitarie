package com.app.pegsolitaire.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.FakePlayGamesOptInRepository
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesService
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Coverage of [HomeViewModel]'s onboarding gate (ONB-01, D-13/D-14/D-15) and the
 * persisted Challenge level it surfaces to the Challenge tile (CHAL-05 / PERS-02).
 *
 * A fake [OnboardingRepository] backed by a settable `MutableStateFlow<Boolean>`
 * stands in for the DataStore so the gate is driven deterministically:
 *  - on first launch (`onboardingSeen == false`) the overlay is visible (D-14);
 *  - once seen it is hidden;
 *  - the help affordance forces it visible again independent of the flag (D-15);
 *  - dismissing persists `markSeen()` and hides it (D-13).
 *
 * A [ChallengeProgress] stand-in drives `currentLevel` the same way, proving the tile's
 * entry level comes from persistence (never a hardcoded 1) and re-emits on an advance.
 *
 * A [FakeResumeProgressRepository] drives the per-mode resume snapshots (PERS-03), proving
 * Home offers a Continue affordance exactly when one exists — for either mode, for both at
 * once (D-03), dropped again when a solve clears it (T-05-12), and silently absent when the
 * stored blob is unreadable (T-05-03).
 *
 * `Dispatchers.setMain(StandardTestDispatcher())` makes the `viewModelScope`
 * collector deterministic under `runTest` virtual time.
 */
class HomeViewModelTest {
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
    fun onboardingIsVisibleOnFirstLaunchWhenNotYetSeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
        }

    @Test
    fun onboardingIsHiddenWhenAlreadySeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
        }

    @Test
    fun showHowToPlayForcesTheOverlayVisibleEvenAfterItHasBeenSeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = viewModelFor(repository)
            runCurrent()
            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()

            viewModel.onShowHowToPlay()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
        }

    @Test
    fun dismissOnboardingPersistsSeenAndHidesTheOverlay() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(repository)
            runCurrent()
            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()

            viewModel.onDismissOnboarding()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
            assertThat(repository.markSeenCount).isEqualTo(1)
            assertThat(repository.seen.value).isTrue()
        }

    // ---- Persisted Challenge level (CHAL-05 / PERS-02) ----------------------

    @Test
    fun challengeCurrentLevelMirrorsThePersistedLevel() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = PERSISTED_LEVEL,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(PERSISTED_LEVEL)
        }

    @Test
    fun challengeCurrentLevelDefaultsToLevelOneBeforeTheStoreEmits() =
        runTest(testDispatcher) {
            // Nothing collected yet (no runCurrent): the tile must already paint a usable
            // level rather than 0/blank — the fail-open default (T-05-02).
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = PERSISTED_LEVEL,
                )

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun advancingTheStoredLevelWhileOnHomeReEmitsTheNewLevel() =
        runTest(testDispatcher) {
            val levels = MutableStateFlow(FIRST_LEVEL)
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), levels = levels)

            viewModel.uiState.test {
                assertThat(awaitItem().challengeCurrentLevel).isEqualTo(FIRST_LEVEL)

                // A solve advanced the persisted level while Home was in the back stack.
                levels.value = PERSISTED_LEVEL
                runCurrent()

                assertThat(awaitItem().challengeCurrentLevel).isEqualTo(PERSISTED_LEVEL)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Helpers -------------------------------------------------------------

    // ---- Resume affordances (PERS-03, D-01/D-03) -----------------------------

    @Test
    fun noContinueAffordanceIsOfferedWhenNothingIsSaved() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true))
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isNull()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun aClassicSnapshotOffersAContinueCarryingItsBoardId() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isTrue()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isEqualTo(RESUMABLE_LEVEL_ID)
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun bothModesCanOfferAContinueAtTheSameTime() =
        runTest(testDispatcher) {
            // D-03: one snapshot PER MODE — leaving a Classic game to play Challenge keeps
            // both, so Home shows two independent Continue affordances.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isTrue()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isTrue()
        }

    @Test
    fun clearingASnapshotWhileOnHomeDropsItsContinueAffordance() =
        runTest(testDispatcher) {
            // A solve clears the mode's snapshot (D-05); Home must stop offering it rather
            // than leaving a stale Continue pointing at a finished board (T-05-12).
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isTrue()

            progress.clear(ProgressPersistenceKeys.MODE_CHALLENGE)
            runCurrent()

            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun anUnreadableClassicSnapshotSimplyOffersNoContinue() =
        runTest(testDispatcher) {
            // T-05-03: the mode is listed but its blob does not decode — the affordance
            // disappears; nothing throws and no error surfaces to the player.
            val progress = FakeResumeProgressRepository()
            progress.putUnreadable(ProgressPersistenceKeys.MODE_CLASSIC)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
        }

    @Test
    fun aChallengeSnapshotOffersAContinueAtItsStampedLevelNotTheColdStartLadder() =
        runTest(testDispatcher) {
            // WR-03: at a cold start the ladder flow still sits at its fail-open initial
            // value while the snapshot's stamp names the real level — the tile must carry
            // the stamp, or the restored level-N board gets played AS level 1.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = FIRST_LEVEL,
                    progress = progress,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(FIRST_LEVEL)
            assertThat(viewModel.uiState.value.challengeResumeLevel).isEqualTo(PERSISTED_LEVEL)
        }

    @Test
    fun anUnreadableChallengeSnapshotOffersNoContinue() =
        runTest(testDispatcher) {
            // WR-02: the Challenge mode lists a row but its blob does not decode — keying
            // the tile on row existence alone would show a phantom "Continue Challenge"
            // that forever starts a fresh board. The tile needs a decodable blob.
            val progress = FakeResumeProgressRepository()
            progress.putUnreadable(ProgressPersistenceKeys.MODE_CHALLENGE)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun aClassicSnapshotOnAnUnknownBoardOffersNoContinueAndPurgesTheRow() =
        runTest(testDispatcher) {
            // CR-01: the blob decodes but its levelId is not a catalog board (tampering,
            // or an app update removed the board). Offering it would navigate into the
            // Gameplay route's fail-fast catalog lookup — an app crash on Continue — so
            // the affordance is withheld and the stale row purged.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, UNKNOWN_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isNull()
            assertThat(progress.clears).containsExactly(ProgressPersistenceKeys.MODE_CLASSIC)
        }

    @Test
    fun aClassicSnapshotOnAKnownBoardIsNeverPurged() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeLevelId).isEqualTo(RESUMABLE_LEVEL_ID)
            assertThat(progress.clears).isEmpty()
        }

    // ---- Play Games opt-in prompt (PGS-01, D-15/D-17/D-18) --------------------

    @Test
    fun theOptInPromptStaysHiddenWhileTheOnboardingOverlayIsStillShowing() =
        runTest(testDispatcher) {
            // D-15: the two first-run surfaces never stack — onboarding goes first.
            val optIn = FakePlayGamesOptInRepository()
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = false), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    @Test
    fun theOptInPromptShowsOnceOnboardingIsSeenAndTheQuestionIsStillUnanswered() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository()
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()
        }

    @Test
    fun dismissingOnboardingHandsOverToTheOptInPrompt() =
        runTest(testDispatcher) {
            // The real first-run sequence: overlay first, then the prompt takes its place.
            val optIn = FakePlayGamesOptInRepository()
            val onboarding = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(onboarding, optIn = optIn)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()

            viewModel.onDismissOnboarding()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()
        }

    @Test
    fun theOptInPromptNeverShowsAgainOnceAnswered() =
        runTest(testDispatcher) {
            // D-17 / T-11-08: "Not now" retires the prompt permanently — no nag on a later
            // launch, which is what keeps this out of dark-pattern territory.
            val optIn = FakePlayGamesOptInRepository(initiallyAnswered = true)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    @Test
    fun theOptInPromptStaysRetiredEvenWhenTheHelpAffordanceReopensOnboarding() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository(initiallyAnswered = true)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn)
            runCurrent()

            viewModel.onShowHowToPlay()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    @Test
    fun acceptingHidesThePromptPersistsTheOptInAndSignsIn() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(optIn.setOptedInCount).isEqualTo(1)
            assertThat(games.signInCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun acceptingHidesThePromptBeforeTheSignInRoundTripEvenBegins() =
        runTest(testDispatcher) {
            // T-11-09: the dialog must never sit on screen waiting for the SDK — no wall,
            // no pause. Asserted with no runCurrent(), so nothing the launch does has run.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(games.signInCallCount).isEqualTo(0)
        }

    @Test
    fun aGenuineSignInErrorRaisesTheErrorTick() =
        runTest(testDispatcher) {
            // D-18: only a throw — an SDK-reported failure — surfaces the Home snackbar.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService().apply { signInBehavior = FakeGamesService.SignInBehavior.Error() }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(1)
            // The opt-in still stands, so the next launch retries silently (D-18).
            assertThat(optIn.optedInState.value).isTrue()
        }

    @Test
    fun aPlainChooserDismissalStaysSilent() =
        runTest(testDispatcher) {
            // D-18 / D-10 parity: signIn() returning false is a player-initiated cancel,
            // not an error — no snackbar, nothing to apologise for.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService().apply { signInBehavior = FakeGamesService.SignInBehavior.Decline }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(games.signInCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun decliningHidesThePromptPersistsNotNowAndNeverTouchesTheSdk() =
        runTest(testDispatcher) {
            // D-16/D-17: declining leaves the game exactly as it was — the only change in
            // the world is the pair of persisted flags.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()

            viewModel.onDeclinePlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(optIn.setNotNowCount).isEqualTo(1)
            assertThat(optIn.setOptedInCount).isEqualTo(0)
            assertThat(games.signInCallCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Builds the ViewModel over the given onboarding fake, a [ChallengeProgress]
     * stand-in whose `currentLevel` is driven by [levels] (defaulting to a fixed
     * [persistedLevel]), a resume store [progress] (empty unless a test fills it), and a
     * catalog that knows exactly the real Classic ids — neither real store is touched.
     *
     * [optIn] defaults to an ALREADY-ANSWERED prompt so the pre-Play-Games tests above
     * (onboarding, ladder level, resume affordances) keep asserting the surface they were
     * written for — an unanswered default would silently raise the opt-in dialog under
     * every one of them.
     */
    private fun viewModelFor(
        repository: OnboardingRepository,
        persistedLevel: Int = FIRST_LEVEL,
        levels: MutableStateFlow<Int> = MutableStateFlow(persistedLevel),
        progress: ProgressRepository = FakeResumeProgressRepository(),
        optIn: PlayGamesOptInRepository = FakePlayGamesOptInRepository(initiallyAnswered = true),
        games: GamesService = FakeGamesService(),
    ): HomeViewModel {
        val challengeProgress = mockk<ChallengeProgress>(relaxed = true)
        every { challengeProgress.currentLevel } returns levels
        return HomeViewModel(
            repository,
            challengeProgress,
            progress,
            CatalogOnlyLevelRepository,
            optIn,
            games,
        )
    }
}

/** The Classic board a saved game sits on — a real catalog id, carried into the resume. */
private val RESUMABLE_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** A levelId no catalog board has ever had — the CR-01 stale/tampered snapshot. */
private const val UNKNOWN_LEVEL_ID = "ghost-board"

/**
 * A [LevelRepository] that answers only the catalog question Home asks (CR-01): the real
 * Classic ids over a placeholder board. The load paths are never taken from Home.
 */
private object CatalogOnlyLevelRepository : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> =
        listOf(
            ClassicLevelIds.ENGLISH33,
            ClassicLevelIds.EUROPEAN37,
            ClassicLevelIds.ASYMMETRIC39,
            ClassicLevelIds.DIAMOND41,
            ClassicLevelIds.WIEGLEB45,
        ).map { id ->
            ClassicLevelDescriptor(
                id = id,
                displayName = id,
                holeCount = Boards.english33Initial.spec.inPlay.size,
                previewBoard = Boards.english33Initial,
            )
        }

    override fun loadClassic(levelId: String): Level = error("Home never loads a level")

    override fun loadChallenge(levelNumber: Int): Level = error("Home never loads a level")
}

/**
 * A [ProgressRepository] holding just enough of the resume surface for Home: which modes
 * have a snapshot, and (for Classic) which board it is on. [putUnreadable] models a stored
 * blob that fails to decode — the repository's own fail-safe, seen from up here as a mode
 * that lists but cannot be loaded.
 */
private class FakeResumeProgressRepository : ProgressRepository {
    private val snapshots = mutableMapOf<String, ResumeSnapshotDto?>()
    private val modes = MutableStateFlow<List<String>>(emptyList())

    /** The modes Home purged as unusable (CR-01) — observable by the tests. */
    val clears = mutableListOf<String>()

    fun put(
        mode: String,
        levelId: String,
    ) {
        snapshots[mode] = snapshotOf(mode = mode, levelId = levelId)
        modes.value = snapshots.keys.toList()
    }

    /** A mode whose row exists but whose payload is unusable (`loadSnapshot` → null). */
    fun putUnreadable(mode: String) {
        snapshots[mode] = null
        modes.value = snapshots.keys.toList()
    }

    fun clear(mode: String) {
        snapshots -= mode
        modes.value = snapshots.keys.toList()
    }

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        nowMs: Long,
    ): NewBests = error("Home never records a result")

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Home never saves a snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = snapshots[mode]

    override suspend fun clearSnapshot(mode: String) {
        clears += mode
        snapshots -= mode
        modes.value = snapshots.keys.toList()
    }

    override fun observeResumeModes(): Flow<List<String>> = modes

    private fun snapshotOf(
        mode: String,
        levelId: String,
    ): ResumeSnapshotDto =
        ResumeSnapshotDto(
            mode = mode,
            levelId = levelId,
            // A Challenge snapshot always carries its provenance stamp (WR-03) — the
            // level the tile must label and resume into.
            challengeLevel =
                levelId.toIntOrNull().takeIf { mode == ProgressPersistenceKeys.MODE_CHALLENGE },
            inPlay = emptyList(),
            initialPegs = 0L,
            moves = emptyList(),
            elapsedMs = 0L,
        )
}

/** A persisted level well past the on-ramp, so "continues at the stored level" is unambiguous. */
private const val PERSISTED_LEVEL = 7

/** The fail-open Challenge entry level (a fresh install, or a corrupt read). */
private const val FIRST_LEVEL = 1

/**
 * A settable in-memory [OnboardingRepository]: overrides the DataStore-backed members
 * so [seen] drives the flag and [markSeen] is observable, with the base store never
 * touched (its constructor takes a no-op, never-emitting [DataStore]).
 */
private class FakeOnboardingRepository(
    initiallySeen: Boolean,
) : OnboardingRepository(NoopPreferencesDataStore) {
    val seen = MutableStateFlow(initiallySeen)
    var markSeenCount = 0
        private set

    override val onboardingSeen: Flow<Boolean> get() = seen

    override suspend fun markSeen() {
        markSeenCount++
        seen.value = true
    }
}

/** A never-emitting [DataStore] so the base [OnboardingRepository] constructor is safe in the fake. */
private object NoopPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        error("not used by the fake")
}
