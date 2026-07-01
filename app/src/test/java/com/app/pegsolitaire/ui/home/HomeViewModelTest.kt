package com.app.pegsolitaire.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.OnboardingRepository
import com.google.common.truth.Truth.assertThat
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
 * Coverage of [HomeViewModel]'s onboarding-gating behavior (ONB-01, D-13/D-14/D-15).
 *
 * A fake [OnboardingRepository] backed by a settable `MutableStateFlow<Boolean>`
 * stands in for the DataStore so the gate is driven deterministically:
 *  - on first launch (`onboardingSeen == false`) the overlay is visible (D-14);
 *  - once seen it is hidden;
 *  - the help affordance forces it visible again independent of the flag (D-15);
 *  - dismissing persists `markSeen()` and hides it (D-13).
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
            val viewModel = HomeViewModel(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()
        }

    @Test
    fun onboardingIsHiddenWhenAlreadySeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = HomeViewModel(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
        }

    @Test
    fun showHowToPlayForcesTheOverlayVisibleEvenAfterItHasBeenSeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = HomeViewModel(repository)
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
            val viewModel = HomeViewModel(repository)
            runCurrent()
            assertThat(viewModel.uiState.value.onboardingVisible).isTrue()

            viewModel.onDismissOnboarding()
            runCurrent()

            assertThat(viewModel.uiState.value.onboardingVisible).isFalse()
            assertThat(repository.markSeenCount).isEqualTo(1)
            assertThat(repository.seen.value).isTrue()
        }
}

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
