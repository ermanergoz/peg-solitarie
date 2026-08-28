package com.app.pegsolitaire.ui.settings

import app.cash.turbine.test
import com.app.pegsolitaire.audio.NoOpSoundPlayer
import com.app.pegsolitaire.data.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            val viewModel = SettingsViewModel(NeverEmittingSettingsRepository, NoOpSoundPlayer())
            runCurrent()

            assertThat(viewModel.uiState.value.soundOn).isTrue()
            assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        }

    @Test
    fun persistedFlagsAreMirroredIntoTheUiState() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(soundOn = false, hapticsOn = true)
            val viewModel = SettingsViewModel(repository, NoOpSoundPlayer())
            runCurrent()

            assertThat(viewModel.uiState.value.soundOn).isFalse()
            assertThat(viewModel.uiState.value.hapticsOn).isTrue()
        }

    @Test
    fun aLaterStoreEmissionUpdatesTheUiState() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = SettingsViewModel(repository, NoOpSoundPlayer())

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
            val viewModel = SettingsViewModel(repository, NoOpSoundPlayer())
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
            val viewModel = SettingsViewModel(repository, NoOpSoundPlayer())
            runCurrent()

            viewModel.onToggleHaptics(false)
            runCurrent()

            assertThat(repository.lastHapticsWritten).isFalse()
            assertThat(viewModel.uiState.value.hapticsOn).isFalse()
        }

    @Test
    fun togglingSoundLeavesHapticsUntouched() =
        runTest(testDispatcher) {
            val repository = FakeSettingsRepository(soundOn = true, hapticsOn = true)
            val viewModel = SettingsViewModel(repository, NoOpSoundPlayer())
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
