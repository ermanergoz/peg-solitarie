package com.app.pegsolitaire.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.platform.games.GamesService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
 * It also mirrors which modes hold a saved in-progress game (PERS-03, D-01/D-03) into
 * [HomeUiState.classicResumeLevelId] / [HomeUiState.challengeResumeLevel], so Home can
 * offer up to two independent "Continue" affordances. Both snapshots are opened before a
 * tile is offered — the Classic one to learn which board to resume into, the Challenge
 * one to read its stamped level (WR-03) and prove the blob actually decodes (WR-02; the
 * load also lets the repository purge a corrupt row) — and an unreadable blob degrades
 * to "no resume" (T-05-03). A Classic
 * blob whose `levelId` is no longer a catalog board (DB tampering, or an app update that
 * removed a board while an old snapshot survived) is equally unusable: the affordance is
 * withheld and the stale row purged, so a bogus id can never reach the Gameplay route's
 * fail-fast catalog lookup (CR-01).
 *
 * Finally it owns the one-time Play Games opt-in prompt (PGS-01, D-15 to D-18). The
 * prompt is sequenced strictly after the onboarding overlay and retired permanently once
 * answered, accepting persists the opt-in and signs in without ever making the player
 * wait, and declining changes nothing but the two persisted flags.
 *
 * @param onboardingRepository the DataStore-backed onboarding flag (single source of truth).
 * @param challengeProgress the persisted Challenge ladder position (single source of truth
 *   for the tile's entry level; fail-open to level 1 on a corrupt read).
 * @param progressRepository the per-mode resume snapshot store (single source of truth for
 *   which Continue affordances exist).
 * @param levelRepository the Classic catalog authority a stored snapshot's `levelId` is
 *   validated against before it may become a Continue affordance (CR-01).
 * @param optInRepository the persisted Play Games opt-in answer (D-17) — the single source
 *   of truth for whether the prompt has been answered, and the store both handlers write.
 * @param gamesService the Play Games seam. Reached only from [onAcceptPlayGamesPrompt]:
 *   this ViewModel never calls it at launch (that is `MainActivity`'s opt-in-gated
 *   bootstrap, D-01) and never on the decline path.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        challengeProgress: ChallengeProgress,
        progressRepository: ProgressRepository,
        levelRepository: LevelRepository,
        private val optInRepository: PlayGamesOptInRepository,
        private val gamesService: GamesService,
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
            // The one-time Play Games opt-in prompt (D-15). Its visibility is a pure
            // function of two persisted flags, which is what makes it genuinely one-time:
            // it shows only once onboarding is behind the player AND the question is still
            // unanswered, so the two first-run surfaces are sequenced rather than stacked.
            // Once either answer lands, promptAnswered is true forever and this collector
            // can never emit true again — there is no per-session re-ask and no nag
            // (D-17 / T-11-08). The Settings row is the only later way in (D-19).
            viewModelScope.launch {
                combine(
                    onboardingRepository.onboardingSeen,
                    optInRepository.promptAnswered,
                ) { seen, answered -> seen && !answered }
                    .collect { shouldShow ->
                        _uiState.update { it.copy(playGamesPromptVisible = shouldShow) }
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
            // or corrupt) simply hides the affordance rather than surfacing an error. The id
            // it carries is untrusted stored state, so it must still exist in the catalog
            // before it may be offered (CR-01) — a stale id is purged, never navigated.
            viewModelScope.launch {
                progressRepository.observeResumeModes().collect { modes ->
                    val classicLevelId =
                        if (ProgressPersistenceKeys.MODE_CLASSIC in modes) {
                            resumableClassicLevelId(progressRepository, levelRepository)
                        } else {
                            null
                        }
                    // The Challenge tile is keyed on the blob DECODING, not on the row
                    // existing (WR-02): loading it lets the repository purge a corrupt row,
                    // so a phantom "Continue Challenge" that could only ever start a fresh
                    // board never survives a visit to Home. The level offered is the
                    // snapshot's own provenance stamp (WR-03), never the ladder flow —
                    // which at a cold start may still be its fail-open initial value.
                    val challengeResumeLevel =
                        if (ProgressPersistenceKeys.MODE_CHALLENGE in modes) {
                            progressRepository
                                .loadSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE)
                                ?.challengeLevel
                        } else {
                            null
                        }
                    _uiState.update {
                        it.copy(
                            classicResumeLevelId = classicLevelId,
                            challengeResumeLevel = challengeResumeLevel,
                        )
                    }
                }
            }
        }

        /**
         * The Classic board id a Continue may safely resume into, or `null` when the stored
         * snapshot is absent, unreadable, or points at a board the catalog no longer knows
         * (CR-01). A decodable blob with an unknown id is affirmatively stale — the row is
         * purged so the phantom Continue cannot come back on the next emission.
         */
        private suspend fun resumableClassicLevelId(
            progressRepository: ProgressRepository,
            levelRepository: LevelRepository,
        ): String? {
            val storedLevelId =
                progressRepository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)?.levelId
                    ?: return null
            val known = levelRepository.classicCatalog().any { it.id == storedLevelId }
            if (!known) progressRepository.clearSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)
            return storedLevelId.takeIf { known }
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

        /**
         * The prompt's "Sign in" action (D-18): persist the opt-in, then sign in.
         *
         * The dialog is hidden **synchronously, first** — before the coroutine that does
         * the persisting and the sign-in is even launched. That ordering is the whole
         * point (T-11-09): `signIn()` awaits a real SDK round trip that may put Google's
         * own account chooser on screen, and a player who has already answered must never
         * be left looking at their own answered dialog while it happens. PGS-01's "no
         * wall, no pause" is enforced here, not hoped for.
         *
         * Only a **throw** raises [HomeUiState.playGamesSignInErrorTick]. A `false`
         * return is a plain chooser dismissal — the player's own cancel — and takes no
         * action at all: no snackbar, nothing to apologise for (D-18 / D-10 parity). The
         * opt-in stands either way, so the next launch retries silently through
         * `bootstrapIfOptedIn()` and the Settings row is available meanwhile.
         *
         * `runCatching` rather than a `try`/`catch` around the whole body so a failure
         * cannot skip anything: `setOptedIn()` has already completed before the call it
         * guards, and the guard covers exactly the one expression that may throw.
         */
        fun onAcceptPlayGamesPrompt() {
            _uiState.update { it.copy(playGamesPromptVisible = false) }
            viewModelScope.launch {
                optInRepository.setOptedIn()
                runCatching { gamesService.signIn() }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(playGamesSignInErrorTick = state.playGamesSignInErrorTick + 1)
                        }
                    }
            }
        }

        /**
         * The screen's acknowledgement that the [HomeUiState.playGamesSignInErrorTick]
         * snackbar has been raised — resets the ordinal to 0, so the error is a consumed
         * one-shot event rather than standing state (CR-01).
         *
         * Home is the `NavHost` start destination, so this ViewModel — and the tick — lives
         * for the whole process, while `HomeScreen` leaves composition on every forward
         * navigation and re-enters on every pop and every configuration change. Left
         * standing, one genuine failure would be re-announced on every later visit to Home
         * for the rest of the session. The screen calls this the moment it starts showing
         * the message, so a re-entry during or after the show finds nothing to replay.
         */
        fun onPlayGamesSignInErrorShown() {
            _uiState.update { it.copy(playGamesSignInErrorTick = 0) }
        }

        /**
         * The prompt's "Not now" action, a tap outside it, or system back (D-16): persist
         * the answer and hide it, permanently (D-17).
         *
         * [gamesService] is deliberately untouched on this path — not merely "not signed
         * in", but never reached. Declining leaves the game exactly as it was, and the
         * only change in the world is the pair of persisted flags.
         */
        fun onDeclinePlayGamesPrompt() {
            _uiState.update { it.copy(playGamesPromptVisible = false) }
            viewModelScope.launch { optInRepository.setNotNow() }
        }
    }
