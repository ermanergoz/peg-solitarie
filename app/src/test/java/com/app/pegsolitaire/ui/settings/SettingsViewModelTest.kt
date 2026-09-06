package com.app.pegsolitaire.ui.settings

import app.cash.turbine.test
import com.app.pegsolitaire.audio.NoOpSoundPlayer
import com.app.pegsolitaire.data.FakePlayGamesOptInRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Coverage of [SettingsViewModel]'s read/write contract over [SettingsRepository]
 * (SET-01 sound, SET-02 haptics; D-14/D-17).
 *
 * A fake repository backed by settable `MutableStateFlow`s stands in for the DataStore
 * so the screen state is driven deterministically:
 *  - both toggles default ON before the store has emitted anything (D-17 fail-open);
 *  - persisted values are mirrored into [SettingsUiState];
 *  - a later store emission re-renders the state (the repository is the authority —
 *    the ViewModel never flips a local copy optimistically);
 *  - `onToggle*` writes the new value through the repository setters;
 *  - the two flags are independent (toggling one never rewrites the other).
 *
 * `Dispatchers.setMain(StandardTestDispatcher())` makes the `viewModelScope`
 * collectors deterministic under `runTest` virtual time (the [HomeViewModelTest] style).
 */
class SettingsViewModelTest {
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
    fun bothTogglesDefaultOnBeforeTheStoreEmits() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(NeverEmittingSettingsRepository)
            runCurrent()

            assertThat(viewModel.uiState.value.soundOn).isTrue()
            assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        }

    @Test
    fun persistedFlagsAreMirroredIntoTheUiState() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(soundOn = false, hapticsOn = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.soundOn).isFalse()
            assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        }

    @Test
    fun aLaterStoreEmissionUpdatesTheUiState() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModelFor(repository)

            viewModel.uiState.test {
                assertThat(awaitItem().soundOn).isTrue()

                repository.emitSound(false)

                assertThat(awaitItem().soundOn).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onToggleSoundPersistsTheNewValueAndTheStateFollows() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(soundOn = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            viewModel.onToggleSound(false)
            runCurrent()

            assertThat(repository.lastSoundWritten).isFalse()
            assertThat(viewModel.uiState.value.soundOn).isFalse()
        }

    @Test
    fun onToggleHapticsPersistsTheNewValueAndTheStateFollows() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(hapticsOn = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            viewModel.onToggleHaptics(false)
            runCurrent()

            assertThat(repository.lastHapticsWritten).isFalse()
            assertThat(viewModel.uiState.value.hapticsOn).isFalse()
        }

    // ---- Play Games session + manual sign-in (PGS-02/PGS-03, D-10/D-11/D-19) ----

    @Test
    fun theSessionIsUnknownBeforeTheSeamAnswers() =
        runTest(testDispatcher) {
            // D-11's fail-hidden default: until the seam speaks there is no section at all.
            val viewModel = viewModelFor(FakeSettingsRepository(), games = NeverEmittingGamesService)
            runCurrent()

            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(GamesSignInState.Unknown)
        }

    @Test
    fun aSignedOutSessionIsMirroredIntoTheUiState() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(FakeSettingsRepository(), games = FakeGamesService(GamesSignInState.SignedOut))
            runCurrent()

            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun aSignedInSessionIsMirroredIntoTheUiStateWithItsDisplayName() =
        runTest(testDispatcher) {
            val signedIn = GamesSignInState.SignedIn(playerId = PLAYER_ID, displayName = DISPLAY_NAME)
            val viewModel = viewModelFor(FakeSettingsRepository(), games = FakeGamesService(signedIn))
            runCurrent()

            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(signedIn)
        }

    @Test
    fun aLaterSessionChangeUpdatesTheUiState() =
        runTest(testDispatcher) {
            // The live sequence for an opted-in player: Unknown while the silent check runs,
            // then whatever it resolves to. The section appears only at that second emission.
            val games = FakeGamesService(GamesSignInState.Unknown)
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()
            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(GamesSignInState.Unknown)

            games.emitSession(GamesSignInState.SignedOut)
            runCurrent()

            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun onSignInToPlayGamesPersistsTheOptInBeforeReachingTheSeam() =
        runTest(testDispatcher) {
            // D-19: the Settings row is a full opt-in, not a one-off sign-in — both flags
            // must land so later launches bootstrap silently and the prompt never returns.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService(GamesSignInState.SignedOut)
            val viewModel = viewModelFor(FakeSettingsRepository(), optIn = optIn, games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(optIn.setOptedInCount).isEqualTo(1)
            assertThat(optIn.answered.value).isTrue()
            assertThat(optIn.optedInState.value).isTrue()
            assertThat(games.signInCallCount).isEqualTo(1)
        }

    @Test
    fun aSuccessfulManualSignInResolvesTheSessionThroughTheSeam() =
        runTest(testDispatcher) {
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Success(PLAYER_ID, DISPLAY_NAME)
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(viewModel.uiState.value.gamesSignInState)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun aThrownSignInRaisesTheErrorTick() =
        runTest(testDispatcher) {
            // D-10: only a genuine SDK error is worth a snackbar.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Error()
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(1)
        }

    @Test
    fun theScreenConsumesAShownSignInErrorSoARotationCannotReplayIt() =
        runTest(testDispatcher) {
            // CR-01: once the snackbar is raised the ordinal is handed back and reset, so a
            // recomposition under this same ViewModel finds nothing to announce.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Error()
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()
            viewModel.onSignInToPlayGames()
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(1)

            viewModel.onPlayGamesSignInErrorShown()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun aDeclinedSignInIsSilentAndLeavesTheRowInPlace() =
        runTest(testDispatcher) {
            // D-10/D-13: a plain chooser dismissal is the player's own cancel — no snackbar,
            // no state change, and the SignedOut session keeps the row exactly where it was.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Decline
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
            assertThat(viewModel.uiState.value.gamesSignInState).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun aSecondFailedSignInRaisesTheTickAgain() =
        runTest(testDispatcher) {
            // The ordinal must keep moving: two identical failures are two messages, which is
            // the whole reason the tick is a counter rather than a nullable message.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Error()
            val viewModel = viewModelFor(FakeSettingsRepository(), games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()
            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(2)
        }

    @Test
    fun aDeclinedSignInStillPersistsTheOptIn() =
        runTest(testDispatcher) {
            // The opt-in is the player's answer to the *app's* question and stands regardless
            // of what they then do in Google's chooser — otherwise a cancel would silently
            // resurrect the Home prompt the tap was meant to retire (D-19).
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService(GamesSignInState.SignedOut)
            games.signInBehavior = FakeGamesService.SignInBehavior.Decline
            val viewModel = viewModelFor(FakeSettingsRepository(), optIn = optIn, games = games)
            runCurrent()

            viewModel.onSignInToPlayGames()
            runCurrent()

            assertThat(optIn.optedInState.value).isTrue()
        }

    /** Builds the subject with the Play Games collaborators defaulted to their quiet doubles. */
    private fun viewModelFor(
        repository: SettingsRepository,
        optIn: FakePlayGamesOptInRepository = FakePlayGamesOptInRepository(),
        games: GamesService = FakeGamesService(),
    ) = SettingsViewModel(repository, NoOpSoundPlayer(), FakeConsentGateway(), games, optIn)

    @Test
    fun togglingSoundLeavesHapticsUntouched() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(soundOn = true, hapticsOn = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            viewModel.onToggleSound(false)
            runCurrent()

            assertThat(repository.lastHapticsWritten).isNull()
            assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        }
}

/**
 * A settable in-memory [SettingsRepository]: the two Phase-5 flags are backed by
 * `MutableStateFlow`s so a test can both observe writes ([lastSoundWritten] /
 * [lastHapticsWritten]) and push a store emission ([emitSound] / [emitHaptics]).
 */
private class FakeSettingsRepository(
    soundOn: Boolean = true,
    hapticsOn: Boolean = true,
) : SettingsRepository {
    private val soundState = MutableStateFlow(soundOn)
    private val hapticsState = MutableStateFlow(hapticsOn)

    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = soundState.asStateFlow()

    override val hapticsOn: Flow<Boolean> = hapticsState.asStateFlow()

    var lastSoundWritten: Boolean? = null
        private set

    var lastHapticsWritten: Boolean? = null
        private set

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) {
        lastSoundWritten = on
        soundState.value = on
    }

    override suspend fun setHapticsOn(on: Boolean) {
        lastHapticsWritten = on
        hapticsState.value = on
    }

    /** Simulates the persisted sound flag changing underneath the ViewModel. */
    fun emitSound(on: Boolean) {
        soundState.value = on
    }

    /** Simulates the persisted haptics flag changing underneath the ViewModel. */
    fun emitHaptics(on: Boolean) {
        hapticsState.value = on
    }
}

/** A repository whose flows never emit — proves the pre-emission defaults are ON (D-17). */
private object NeverEmittingSettingsRepository : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = emptyFlow()

    override val soundOn: Flow<Boolean> = emptyFlow()

    override val hapticsOn: Flow<Boolean> = emptyFlow()

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** A [GamesService] whose session never emits — proves the D-11 fail-hidden Unknown default. */
private object NeverEmittingGamesService : GamesService {
    override val session: StateFlow<GamesSignInState> = MutableStateFlow(GamesSignInState.Unknown)

    override fun bootstrapIfOptedIn() = Unit

    override suspend fun signIn(): Boolean = false

    override fun submitScore(
        leaderboardId: String,
        score: Long,
    ) = Unit

    override fun unlockAchievement(achievementId: String) = Unit

    override fun incrementAchievement(
        achievementId: String,
        steps: Int,
    ) = Unit

    override suspend fun showLeaderboard(leaderboardId: String) = Unit

    override suspend fun showAchievements() = Unit
}

/** The Play Games identity a successful fake sign-in reports. */
private const val PLAYER_ID = "player-1"
private const val DISPLAY_NAME = "Erman"
