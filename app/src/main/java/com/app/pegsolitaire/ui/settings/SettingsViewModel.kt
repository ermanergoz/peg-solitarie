package com.app.pegsolitaire.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import com.app.pegsolitaire.platform.games.GamesService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
 * [gamesService] is the fourth source and the same shape of dependency for the same reason:
 * the Play Games session is a platform-owned flow collected into the state, and the
 * interface is what is injected so no Play Games SDK type — and no `Activity` — can reach
 * this class through it. The Play Games section renders exclusively from the collected
 * [SettingsUiState.gamesSignInState]; this ViewModel never guesses at it, and there is no
 * local "signed in" copy to fall out of step (D-11).
 *
 * @param settingsRepository the DataStore-backed settings source of truth.
 * @param soundPlayer the audio seam, used only to answer a sound-enable with one pop (D-12).
 * @param consentGateway the consent seam: publishes whether the privacy-options entry point
 *   is required (CONS-03) and opens the form on request.
 * @param gamesService the Play Games seam: publishes the observed session (D-11) and signs
 *   the player in on request (D-10). Never called at launch — that is `MainActivity`'s
 *   opt-in-gated bootstrap (D-01).
 * @param optInRepository the persisted Play Games opt-in answer (D-17), written by
 *   [onSignInToPlayGames] so a Settings sign-in also opts the player in for good (D-19).
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val soundPlayer: SoundPlayer,
        private val consentGateway: ConsentGateway,
        private val gamesService: GamesService,
        private val optInRepository: PlayGamesOptInRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        /**
         * The sign-in round trip currently in flight, if any — the row's re-entry guard
         * (WR-03).
         *
         * The row stays rendered and enabled while `signIn()` is suspended, because the
         * session is still `SignedOut` until the round trip resolves, so a double tap (or a
         * tap during the squish replay) would otherwise launch two overlapping SDK sign-ins:
         * two `initialize` calls, two account choosers queued, two writes to the session and,
         * on failure, two identical snackbars. Home needs no equivalent — its dialog is hidden
         * synchronously on the first tap.
         */
        private var signInJob: Job? = null

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
            // The Play Games session (D-11), a peer of the three collectors above rather
            // than a one-shot read on screen entry: an opted-in player's silent launch
            // check can still be pending when Settings opens, and the section must appear
            // the moment it resolves instead of staying hidden until the next visit.
            viewModelScope.launch {
                gamesService.session.collect { session ->
                    _uiState.update { it.copy(gamesSignInState = session) }
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

        /**
         * The Settings sign-in row's action (PGS-03, D-10/D-19) — the second and permanent
         * entry point into Play Games, and the only one left for a player who declined the
         * Home prompt or was never asked.
         *
         * [PlayGamesOptInRepository.setOptedIn] is awaited **before** [GamesService.signIn],
         * and unconditionally: the tap is the player's answer to the *app's* question, so it
         * stands whatever then happens in Google's own account chooser. Persisting after the
         * call — or only on success — would let a cancelled chooser silently resurrect the
         * one-time Home prompt this tap was meant to retire, and would leave the next launch
         * on the un-opted-in branch with the SDK untouched (D-19).
         *
         * Only a **throw** raises [SettingsUiState.playGamesSignInErrorTick]. A `false`
         * return is a plain chooser dismissal — the player's own cancel — and takes no action
         * at all: no snackbar, and the row stays exactly where it was for whenever they want
         * it (D-10/D-13). This is deliberately the same shape as
         * `HomeViewModel.onAcceptPlayGamesPrompt`: one seam contract, one reading of it, so
         * the two entry points cannot drift into disagreeing about what a decline means.
         *
         * Nothing here sets [SettingsUiState.gamesSignInState] — a successful sign-in reaches
         * the screen through the collected [GamesService.session] flow like every other state
         * change, so there is no optimistic local copy to be wrong.
         *
         * `runCatching` around the single call it guards rather than a `try`/`catch` over the
         * whole body, so a failure cannot skip the persistence that has already completed. It
         * also catches `CancellationException`, which is rethrown first (WR-04): this
         * ViewModel is cleared when Settings is popped, and a chooser still open at that
         * moment cancels the round trip — a structured-concurrency cancel, not an SDK error,
         * and not a reason to raise the tick.
         *
         * The whole body is fire-and-forget from composition's point of view: the click
         * handler returns immediately and the suspending round trip runs in [viewModelScope],
         * never blocking the frame (T-11-13).
         *
         * A tap while a previous round trip is still in flight is ignored ([signInJob], WR-03):
         * the player's answer has already been taken, and the chooser it opened is the one
         * that will resolve it.
         */
        fun onSignInToPlayGames() {
            if (signInJob?.isActive == true) return
            signInJob =
                viewModelScope.launch {
                    optInRepository.setOptedIn()
                    runCatching { gamesService.signIn() }
                        .onFailure { failure ->
                            if (failure is CancellationException) throw failure
                            _uiState.update { state ->
                                state.copy(
                                    playGamesSignInErrorTick = state.playGamesSignInErrorTick + 1,
                                )
                            }
                        }
                }
        }

        /**
         * The screen's acknowledgement that the [SettingsUiState.playGamesSignInErrorTick]
         * snackbar has been raised — resets the ordinal to 0, so the error is a consumed
         * one-shot event rather than standing state (CR-01).
         *
         * Less exposed than `HomeViewModel`'s counterpart, because this ViewModel is cleared
         * when Settings is popped, but the same replay exists within one visit: a rotation
         * after a failed tap recomposes the screen, and an unconsumed ordinal would announce
         * the failure a second time. Same shape as Home so the two entry points cannot drift.
         */
        fun onPlayGamesSignInErrorShown() {
            _uiState.update { it.copy(playGamesSignInErrorTick = 0) }
        }
    }
