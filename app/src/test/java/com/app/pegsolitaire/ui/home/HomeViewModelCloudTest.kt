package com.app.pegsolitaire.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.FakePlayGamesOptInRepository
import com.app.pegsolitaire.data.FakeProgressSubmitter
import com.app.pegsolitaire.data.HomeProgress
import com.app.pegsolitaire.data.ImportOutcome
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.ProfileSwapRequest
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.games.FakeGamesService
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
 * Coverage of [HomeViewModel]'s half of the account-switch question (CLOUD-02, D-12 to
 * D-15) — the one dialog this milestone reserved, and the only route to the only
 * destructive path in the app.
 *
 * Three claims are under test, and each of them is a decision rather than a detail:
 *
 *  1. **Visibility is a pure function of three sequenced inputs (D-15).** The question is
 *     raised only once onboarding is behind the player AND the opt-in prompt has been
 *     answered, so the three surfaces are sequenced and never stacked. A swap dialog
 *     appearing over the first-run overlay would ask a brand-new player to choose between
 *     two accounts' progress before they have played a level.
 *  2. **A missing display name is not a missing question.** `SignedIn.displayName` is
 *     legitimately `null` (Phase 11 D-14), so the flag and the name are peers; a single
 *     nullable field could not tell "visible, name unknown" from "not visible".
 *  3. **Both answers hand off immediately and out of the screen's scope (T-13-46).** The
 *     flag is cleared synchronously and `onProfileSwapResolved` — non-`suspend`,
 *     application-scoped — is called exactly once, never awaited, because a
 *     back-navigation mid-wipe must not be able to cancel it.
 *
 * `Dispatchers.setMain(StandardTestDispatcher())` makes the `viewModelScope` collectors
 * deterministic under `runTest` virtual time, matching `HomeViewModelTest`'s harness.
 */
class HomeViewModelCloudTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- The question is raised (D-12) ---------------------------------------

    @Test
    fun aPendingSwapRaisesTheDialogAndCarriesTheArrivingAccountName() =
        runTest(testDispatcher) {
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(submitter = submitter)
            runCurrent()

            assertThat(viewModel.uiState.value.profileSwapVisible).isTrue()
            assertThat(viewModel.uiState.value.profileSwapAccountName).isEqualTo(ARRIVING_ACCOUNT_NAME)
        }

    @Test
    fun noPendingSwapAsksNothing() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()

            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
            assertThat(viewModel.uiState.value.profileSwapAccountName).isNull()
        }

    @Test
    fun aSwapRaisedWhileThePlayerIsOnHomeReachesTheState() =
        runTest(testDispatcher) {
            // The live sequence: Home is already composed when the session resolves and the
            // pass's step 1 raises the question.
            val submitter = FakeProgressSubmitter()
            val viewModel = viewModelFor(submitter = submitter)

            viewModel.uiState.test {
                assertThat(awaitItem().profileSwapVisible).isFalse()

                submitter.raise(ARRIVING_REQUEST)
                runCurrent()

                assertThat(awaitItem().profileSwapVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun anUnknownDisplayNameStillRaisesTheDialogWithNoName() =
        runTest(testDispatcher) {
            // Phase 11 D-14: a failed name lookup is a degraded NAME, not a failed sign-in.
            // "Visible with an unknown name" and "not visible" must stay distinguishable,
            // which is exactly why the flag and the name are two fields rather than one.
            val submitter = FakeProgressSubmitter(initialRequest = UNNAMED_REQUEST)
            val viewModel = viewModelFor(submitter = submitter)
            runCurrent()

            assertThat(viewModel.uiState.value.profileSwapVisible).isTrue()
            assertThat(viewModel.uiState.value.profileSwapAccountName).isNull()
        }

    // ---- Never stacked with the two first-run surfaces (D-15) -----------------

    @Test
    fun aSwapIsNotAskedWhileTheOnboardingOverlayIsUp() =
        runTest(testDispatcher) {
            // A brand-new player must not be asked to choose between two accounts' progress
            // before they have seen how the game is played.
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel =
                viewModelFor(
                    repository = SwapTestOnboardingRepository(initiallySeen = false),
                    submitter = submitter,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
        }

    @Test
    fun aSwapIsNotAskedWhileTheOptInPromptIsUp() =
        runTest(testDispatcher) {
            // Two dialogs at once is exactly what D-15 forbids, and the opt-in prompt is the
            // one that must be answered first — it is the question that decides whether Play
            // Games is involved at all.
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel =
                viewModelFor(
                    optIn = FakePlayGamesOptInRepository(initiallyAnswered = false),
                    submitter = submitter,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()
            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
        }

    @Test
    fun aSwapHeldBackByAFirstRunSurfaceIsAskedOnceThatSurfaceIsAnswered() =
        runTest(testDispatcher) {
            // Sequenced, not dropped: the question is still pending in the data layer, so it
            // must surface the moment the surface ahead of it is answered.
            val onboarding = SwapTestOnboardingRepository(initiallySeen = false)
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(repository = onboarding, submitter = submitter)
            runCurrent()
            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()

            viewModel.onDismissOnboarding()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
            assertThat(viewModel.uiState.value.profileSwapVisible).isTrue()
        }

    // ---- The two answers (D-13 / D-14) ---------------------------------------

    @Test
    fun keepingThisDevicesProgressHidesTheDialogAndResolvesTheSwapAsAMerge() =
        runTest(testDispatcher) {
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(submitter = submitter)
            runCurrent()
            assertThat(viewModel.uiState.value.profileSwapVisible).isTrue()

            viewModel.onKeepDeviceProgress()

            // Asserted with NO runCurrent(): the flag moves synchronously, and the
            // resolution is already handed off — nothing is awaited from this scope.
            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
            assertThat(submitter.resolvedWith).containsExactly(true)
        }

    @Test
    fun usingTheCloudProgressHidesTheDialogAndResolvesTheSwapAsAReplace() =
        runTest(testDispatcher) {
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(submitter = submitter)
            runCurrent()

            viewModel.onUseCloudProgress()

            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
            assertThat(submitter.resolvedWith).containsExactly(false)
        }

    @Test
    fun anAnsweredSwapIsNeverReRaisedWhenTheRequestClears() =
        runTest(testDispatcher) {
            // The data layer clears profileSwap once it has applied the answer. That
            // transition must be inert up here: a re-raise would put the wipe question back
            // in front of a player who has already answered it.
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(submitter = submitter)
            runCurrent()

            viewModel.onKeepDeviceProgress()
            submitter.raise(null)
            runCurrent()

            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
            assertThat(submitter.resolvedWith).containsExactly(true)
        }

    @Test
    fun anUnrelatedStoreWriteNeverFlickersTheDialogBackOnAfterAnAnswer() =
        runTest(testDispatcher) {
            // WR-07: both gating flags are maps over the SAME app_prefs DataStore, so any
            // unrelated write re-emits them and combine delivers the re-emissions in no fixed
            // order. Without the per-input dedupe an unchanged value would re-raise the
            // dialog for a frame — over the only wipe in the app.
            val onboarding = SwapTestOnboardingRepository(initiallySeen = true)
            val submitter = FakeProgressSubmitter(initialRequest = ARRIVING_REQUEST)
            val viewModel = viewModelFor(repository = onboarding, submitter = submitter)
            runCurrent()

            viewModel.onKeepDeviceProgress()
            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()

            // An unrelated write re-emitting the same, unchanged flag value.
            onboarding.seen.value = true
            runCurrent()

            assertThat(viewModel.uiState.value.profileSwapVisible).isFalse()
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Builds the ViewModel with every pre-Phase-13 input in its quietest form — onboarding
     * seen, the opt-in prompt already answered, nothing to resume — so the only surface any
     * assertion here can be reading is the swap dialog.
     */
    private fun viewModelFor(
        repository: OnboardingRepository = SwapTestOnboardingRepository(initiallySeen = true),
        optIn: FakePlayGamesOptInRepository = FakePlayGamesOptInRepository(initiallyAnswered = true),
        submitter: FakeProgressSubmitter = FakeProgressSubmitter(),
    ): HomeViewModel {
        val challengeProgress = mockk<ChallengeProgress>(relaxed = true)
        every { challengeProgress.currentLevel } returns MutableStateFlow(FIRST_LEVEL)
        return HomeViewModel(
            repository,
            challengeProgress,
            NoResumeProgressRepository,
            CloudCatalogOnlyLevelRepository,
            optIn,
            FakeGamesService(),
            submitter,
        )
    }
}

/** The arriving account's display name — never the departing one (see [ProfileSwapRequest]). */
private const val ARRIVING_ACCOUNT_NAME = "PegChampion"

/** A player id that is plainly not this device's stored one. */
private const val ARRIVING_PLAYER_ID = "g_arriving_player"

/** The account-switch question as the pass's step 1 raises it. */
private val ARRIVING_REQUEST =
    ProfileSwapRequest(playerId = ARRIVING_PLAYER_ID, displayName = ARRIVING_ACCOUNT_NAME)

/** The same question with the display-name lookup having failed (Phase 11 D-14). */
private val UNNAMED_REQUEST = ProfileSwapRequest(playerId = ARRIVING_PLAYER_ID, displayName = null)

/** The fail-open Challenge entry level — irrelevant here, but the tile needs one. */
private const val FIRST_LEVEL = 1

/**
 * A settable in-memory [OnboardingRepository] — the same idiom `HomeViewModelTest` uses,
 * with [seen] settable so an unrelated re-emission of an unchanged flag can be staged.
 */
private class SwapTestOnboardingRepository(
    initiallySeen: Boolean,
) : OnboardingRepository(NoopCloudPreferencesDataStore) {
    val seen = MutableStateFlow(initiallySeen)

    override val onboardingSeen: Flow<Boolean> get() = seen

    override suspend fun markSeen() {
        seen.value = true
    }
}

/** A never-emitting [DataStore] so the base [OnboardingRepository] constructor is safe here. */
private object NoopCloudPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        error("not used by the fake")
}

/**
 * The catalog authority Home validates a stored Classic id against (CR-01). Never consulted
 * here — [NoResumeProgressRepository] lists no resumable mode, so the validation branch is
 * unreachable — but the constructor demands the real type.
 */
private object CloudCatalogOnlyLevelRepository : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level = error("Home never loads a level")

    override fun loadChallenge(levelNumber: Int): Level = error("Home never loads a level")
}

/** Nothing to resume: the resume surface is asserted in `HomeViewModelTest`, not here. */
private object NoResumeProgressRepository : ProgressRepository {
    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("Home never records a result")

    override suspend fun aggregate(): ProgressAggregate = ProgressAggregate()

    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeHomeProgress(): Flow<HomeProgress> = flowOf(HomeProgress.UNKNOWN)

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Home never saves a snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = null

    override suspend fun clearSnapshot(mode: String) = Unit

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())

    /**
     * Unreachable from Home. The wipe and the import are the DATA layer's (D-14): this
     * ViewModel only forwards the player's answer, which is the whole point of
     * `onProfileSwapResolved` being the single entry point (T-13-31).
     */
    override suspend fun exportProgress(ladder: Int): CloudProgress = error("Home never calls cloud save")

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("Home never calls cloud save")

    override suspend fun resetAllProgress() = error("Home never calls cloud save")
}
