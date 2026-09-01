package com.app.pegsolitaire.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Settings screen (SET-01 sound, SET-02 haptics; D-14) over an immutable
 * [StateFlow]<[SettingsUiState]>.
 *
 * The repository is the single authority: both flags are collected into the state, and
 * a toggle writes the new value through [SettingsRepository] rather than flipping a
 * local copy — the state changes when the persisted write lands (unidirectional data
 * flow, mirroring `GameViewModel.onToggleHints`). Until the store's first emission the
 * state holds the fail-open ON defaults (D-17), so a corrupt store degrades to
 * "everything on" and can never leave the screen blank or crashing (T-05-02).
 *
 * [soundPlayer] does not weaken that: it is a feedback sink, never a state source. Nothing read
 * from it reaches [SettingsUiState], and the switches still render exclusively from the collected
 * repository flows. Its single use is D-12's confirmation pop on the sound *enable* event — see
 * [onToggleSound].
 *
 * [consentGateway] is the third source, and the only authority on whether the CONS-03
 * "Manage privacy choices" row exists (D-14). It is injected as the **interface**: this
 * class lives outside `..platform..`, so naming the concrete UMP implementation — or
 * accepting a framework Activity to hand the form — would fail `PlatformBoundaryRules`'
 * bytecode gates (`onlyPlatformDependsOnPlaySdks` / `uiAndDomainNeverTouchAnActivity`).
 * The Activity the UMP form needs is resolved inside the platform layer, which is why
 * [ConsentGateway.showPrivacyOptions] takes no parameters.
 *
 * @param settingsRepository the DataStore-backed settings source of truth.
 * @param soundPlayer the audio seam, used only to answer a sound-enable with one pop (D-12).
 * @param consentGateway the consent seam: publishes whether the privacy-options entry point
 *   is required (CONS-03) and opens the form on request.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val soundPlayer: SoundPlayer,
        private val consentGateway: ConsentGateway,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.soundOn.collect { on ->
                    _uiState.update { it.copy(soundOn = on) }
                }
            }
            viewModelScope.launch {
                settingsRepository.hapticsOn.collect { on ->
                    _uiState.update { it.copy(hapticsOn = on) }
                }
            }
            viewModelScope.launch {
                consentGateway.privacyOptionsRequired.collect { required ->
                    _uiState.update { it.copy(privacyOptionsRequired = required) }
                }
            }
        }

        /**
         * Persists the player's sound choice (SET-01); the collected flow re-renders the switch.
         *
         * Switching sound ON also answers with a single confirmation pop (D-12) — instant audible
         * proof that audio works, which is the one Settings change the player cannot otherwise
         * perceive. Switching it OFF stays silent: the silence is the confirmation.
         *
         * Two things about this one line are load-bearing.
         *
         * **It belongs here, not in the `init` block's `soundOn` collector.** That collector fires
         * on its initial emission every time the Settings screen is opened, which is exactly what
         * D-12 forbids. Hanging the pop off the user event makes "only on a user-initiated enable"
         * true by construction rather than by a flag that has to be reset correctly.
         *
         * **It calls [SoundPlayer.playSoundEnabledConfirmation], not `play(...)`.** The write below
         * is asynchronous, so at the instant the pop is due the value has not round-tripped through
         * DataStore and back out of the collected flow, the player's own gate is still closed, and
         * an ordinary gated call would be dropped without a sound. That method is the single
         * documented bypass and exists for precisely this.
         */
        fun onToggleSound(on: Boolean) {
            if (on) soundPlayer.playSoundEnabledConfirmation()
            viewModelScope.launch { settingsRepository.setSoundOn(on) }
        }

        /** Persists the player's haptics choice (SET-02); the collected flow re-renders the switch. */
        fun onToggleHaptics(on: Boolean) {
            viewModelScope.launch { settingsRepository.setHapticsOn(on) }
        }

        /**
         * Opens the UMP privacy-options form so the player can change their ad-consent
         * choices (CONS-03).
         *
         * Nothing is read back and no state is set: the form is UMP-rendered and owns its own
         * result, and any consequence of a changed choice arrives through the collected
         * [ConsentGateway.privacyOptionsRequired] flow like every other state change here. The
         * call is `suspend` because awaiting the foreground Activity and the form's dismissal
         * are both genuinely asynchronous — that Activity is resolved inside the platform
         * layer, never held by this ViewModel.
         */
        fun onManagePrivacyChoices() {
            viewModelScope.launch { consentGateway.showPrivacyOptions() }
        }
    }
