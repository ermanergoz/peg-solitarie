package com.app.pegsolitaire.ui.settings

import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
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
 * D-12: switching sound ON answers with one confirmation pop; switching it OFF, and merely
 * opening Settings, stay silent.
 *
 * The asymmetry is the point. Enabling audio is the one action whose result the player cannot
 * otherwise perceive — nothing else in Settings changes on screen — so it gets instant audible
 * proof. Disabling needs no confirmation, because the silence *is* the confirmation.
 *
 * The forbidden case is the third one, and it is why the blip hangs off the user event rather
 * than off the `soundOn` collector: that collector fires on its initial emission every single
 * time the Settings screen is opened, so a blip placed there would greet the player on arrival.
 * Putting it in `onToggleSound` makes "only on user-initiated enable" true by construction, with
 * no flag to keep in sync — and the initial-emission case below is what pins that.
 *
 * Same shell as [SettingsViewModelTest]: `Dispatchers.setMain(StandardTestDispatcher())` plus
 * `runCurrent()` so the `viewModelScope` collectors are deterministic under virtual time.
 */
class SettingsViewModelSoundTest {
    private val testDispatcher = StandardTestDispatcher()
    private val sounds = FakeSoundPlayer()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchingSoundOnPlaysExactlyOneConfirmationPop() =
        runTest(testDispatcher) {
            val viewModel = SettingsViewModel(SettableSettingsRepository(soundOn = false), sounds, FakeConsentGateway())
            runCurrent()

            viewModel.onToggleSound(true)
            runCurrent()

            assertThat(sounds.cues).containsExactly(SoundCue.SoundEnabled)
        }

    @Test
    fun theConfirmationPopBypassesTheMuteGate() =
        runTest(testDispatcher) {
            // The DataStore write is asynchronous, so at the instant the pop is due the
            // player's own gate is still closed. A gated play() would be silently dropped —
            // this is the single documented bypass, and the fake records it as such.
            val viewModel = SettingsViewModel(SettableSettingsRepository(soundOn = false), sounds, FakeConsentGateway())
            runCurrent()

            viewModel.onToggleSound(true)
            runCurrent()

            assertThat(sounds.events.single().gateBypassed).isTrue()
        }

    @Test
    fun switchingSoundOffPlaysNothingAtAll() =
        runTest(testDispatcher) {
            val viewModel = SettingsViewModel(SettableSettingsRepository(soundOn = true), sounds, FakeConsentGateway())
            runCurrent()

            viewModel.onToggleSound(false)
            runCurrent()

            assertThat(sounds.events).isEmpty()
        }

    @Test
    fun openingSettingsPlaysNothingWhateverTheStoredValueIs() =
        runTest(testDispatcher) {
            // The case D-12 explicitly forbids: the blip must never fire on the state load.
            SettingsViewModel(SettableSettingsRepository(soundOn = false), sounds, FakeConsentGateway())
            runCurrent()

            assertThat(sounds.events).isEmpty()
        }

    @Test
    fun aStoreAlreadyEmittingSoundOnStillPlaysNothingUntilThePlayerTogglesIt() =
        runTest(testDispatcher) {
            // The trap the collector placement would fall into: the initial emission is
            // `true`, which is indistinguishable from an enable if you watch the flow.
            val viewModel = SettingsViewModel(SettableSettingsRepository(soundOn = true), sounds, FakeConsentGateway())
            runCurrent()
            assertThat(sounds.events).isEmpty()

            viewModel.onToggleSound(true)
            runCurrent()

            assertThat(sounds.cues).containsExactly(SoundCue.SoundEnabled)
        }

    @Test
    fun theEnableStillWritesThroughToTheRepository() =
        runTest(testDispatcher) {
            // The pop is feedback, never a state source: the persisted write is unchanged.
            val repository = SettableSettingsRepository(soundOn = false)
            val viewModel = SettingsViewModel(repository, sounds, FakeConsentGateway())
            runCurrent()

            viewModel.onToggleSound(true)
            runCurrent()

            assertThat(repository.lastSoundWritten).isTrue()
            assertThat(viewModel.uiState.value.soundOn).isTrue()
        }

    @Test
    fun togglingHapticsPlaysNothingInEitherDirection() =
        runTest(testDispatcher) {
            val viewModel = SettingsViewModel(SettableSettingsRepository(), sounds, FakeConsentGateway())
            runCurrent()

            viewModel.onToggleHaptics(true)
            viewModel.onToggleHaptics(false)
            runCurrent()

            assertThat(sounds.events).isEmpty()
        }

    @Test
    fun aStoreThatNeverEmitsStillPlaysNothingOnConstruction() =
        runTest(testDispatcher) {
            // Belt-and-braces on the D-17 fail-open defaults: the state comes up with sound
            // ON before any emission, and that default must not be heard as an enable either.
            SettingsViewModel(SilentSettingsRepository, sounds, FakeConsentGateway())
            runCurrent()

            assertThat(sounds.events).isEmpty()
        }
}

/**
 * A settable in-memory [SettingsRepository] for the sound spec — the [SettingsViewModelTest]
 * fake's shape, redeclared here because that one is file-private to its own spec.
 */
private class SettableSettingsRepository(
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

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) {
        lastSoundWritten = on
        soundState.value = on
    }

    override suspend fun setHapticsOn(on: Boolean) {
        hapticsState.value = on
    }
}

/** A repository whose flows never emit — the pre-emission fail-open case (D-17). */
private object SilentSettingsRepository : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = emptyFlow()

    override val soundOn: Flow<Boolean> = emptyFlow()

    override val hapticsOn: Flow<Boolean> = emptyFlow()

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}
