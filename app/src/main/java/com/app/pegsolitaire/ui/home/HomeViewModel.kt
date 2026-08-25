package com.app.pegsolitaire.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Home mode-select screen and gates the first-run onboarding overlay
 * (NAV-01, ONB-01) over an immutable [StateFlow]<[HomeUiState]>.
 *
 * It collects [OnboardingRepository.onboardingSeen]: while the flag is `false` (first
 * launch, or a fail-open corrupt read — D-16) the overlay is visible (D-14). The
 * permanent How-to-play affordance ([onShowHowToPlay]) forces it visible again at any
 * time independent of the flag (D-15); dismissing it ([onDismissOnboarding]) persists
 * `markSeen()` and hides it (D-13).
 *
 * It also mirrors [ChallengeProgress.currentLevel] into
 * [HomeUiState.challengeCurrentLevel] (CHAL-05 / PERS-02): the Challenge tile is labelled
 * with — and enters at — the player's persisted level, so closing the app and returning
 * continues the ladder instead of restarting it at level 1.
 *
 * Finally it mirrors which modes hold a saved in-progress game (PERS-03, D-01/D-03) into
 * [HomeUiState.classicResumeLevelId] / [HomeUiState.challengeResumeAvailable], so Home can
 * offer up to two independent "Continue" affordances. Only the Classic snapshot is opened
 * — purely to learn which board to resume into; the Challenge one is answered by its mode
 * key alone, and an unreadable Classic blob degrades to "no resume" (T-05-03).
 *
 * @param onboardingRepository the DataStore-backed onboarding flag (single source of truth).
 * @param challengeProgress the persisted Challenge ladder position (single source of truth
 *   for the tile's entry level; fail-open to level 1 on a corrupt read).
 * @param progressRepository the per-mode resume snapshot store (single source of truth for
 *   which Continue affordances exist).
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        challengeProgress: ChallengeProgress,
        progressRepository: ProgressRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks (Pitfall 10). */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            // First-launch gate (D-14): mirror the persisted flag into visibility. Once
            // markSeen() flips it true, the overlay hides; until then it shows.
            viewModelScope.launch {
                onboardingRepository.onboardingSeen.collect { seen ->
                    _uiState.update { it.copy(onboardingVisible = !seen) }
                }
            }
            // The persisted ladder position is the single source of truth for the tile's
            // entry level (CHAL-05): collecting it means a solve that advances the level
            // is reflected the moment Home is shown again, with no hardcoded fallback.
            viewModelScope.launch {
                challengeProgress.currentLevel.collect { level ->
                    _uiState.update { it.copy(challengeCurrentLevel = level) }
                }
            }
            // Which modes can be continued (PERS-03, D-01/D-03). Collecting the mode list —
            // rather than polling on every Home render — means a snapshot saved while
            // backgrounding, or cleared by a solve, is reflected the next time Home is shown.
            // The Classic blob is opened only to learn WHICH board to resume; a null (absent
            // or corrupt) simply hides the affordance rather than surfacing an error.
            viewModelScope.launch {
                progressRepository.observeResumeModes().collect { modes ->
                    val classicLevelId =
                        if (ProgressPersistenceKeys.MODE_CLASSIC in modes) {
                            progressRepository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)?.levelId
                        } else {
                            null
                        }
                    _uiState.update {
                        it.copy(
                            classicResumeLevelId = classicLevelId,
                            challengeResumeAvailable = ProgressPersistenceKeys.MODE_CHALLENGE in modes,
                        )
                    }
                }
            }
        }

        /** Re-opens the how-to-play overlay any time, independent of the seen flag (D-15). */
        fun onShowHowToPlay() {
            _uiState.update { it.copy(onboardingVisible = true) }
        }

        /** Dismisses the overlay: persists that it has been seen and hides it (D-13). */
        fun onDismissOnboarding() {
            viewModelScope.launch { onboardingRepository.markSeen() }
            _uiState.update { it.copy(onboardingVisible = false) }
        }
    }
