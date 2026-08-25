package com.app.pegsolitaire.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.OnboardingRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

    /**
     * Builds the ViewModel over the given onboarding fake and a [ChallengeProgress]
     * stand-in whose `currentLevel` is driven by [levels] (defaulting to a fixed
     * [persistedLevel]) — the store is never touched.
     */
    private fun viewModelFor(
        repository: OnboardingRepository,
        persistedLevel: Int = FIRST_LEVEL,
        levels: MutableStateFlow<Int> = MutableStateFlow(persistedLevel),
    ): HomeViewModel {
        val progress = mockk<ChallengeProgress>(relaxed = true)
        every { progress.currentLevel } returns levels
        return HomeViewModel(repository, progress)
    }
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
