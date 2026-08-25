package com.app.pegsolitaire.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.SettingsRepository
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
 * @param settingsRepository the DataStore-backed settings source of truth.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
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
        }

        /** Persists the player's sound choice (SET-01); the collected flow re-renders the switch. */
        fun onToggleSound(on: Boolean) {
            viewModelScope.launch { settingsRepository.setSoundOn(on) }
        }

        /** Persists the player's haptics choice (SET-02); the collected flow re-renders the switch. */
        fun onToggleHaptics(on: Boolean) {
            viewModelScope.launch { settingsRepository.setHapticsOn(on) }
        }
    }
