package com.app.pegsolitaire.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.HomeProgress
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.platform.games.GamesService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Home mode-select screen and carries the standing first-run tutorial flag
 * (NAV-01, ONB-01) over an immutable [StateFlow]<[HomeUiState]>.
 *
 * It collects [OnboardingRepository.onboardingSeen] into [HomeUiState.tutorialPending]:
 * while the flag is `false` (first launch, or a fail-open corrupt read — D-16) the tutorial
 * is still owed, and `HomeRoute` auto-starts the `Tutorial` destination once (D-02a).
 *
 * **This ViewModel no longer writes that flag** (Phase 15, D-01f/D-02b). The tutorial is a
 * nav destination with its own state holder, and `TutorialViewModel` persists `markSeen()`
 * — awaited, on the application scope — before it emits its exit signal. Home therefore has
 * exactly one reader and no writer, which is the property the Phase-15 guard rests on: the
 * profile-swap question can be *withheld* by a pending tutorial but can never be raised
 * early by one, because nothing here can flip the flag back.
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
 * It also owns the one-time Play Games opt-in prompt (PGS-01, D-15 to D-18). The
 * prompt is sequenced strictly after the onboarding overlay and retired permanently once
 * answered, accepting persists the opt-in and signs in without ever making the player
 * wait, and declining changes nothing but the two persisted flags.
 *
 * Finally it owns the account-switch question (CLOUD-02, D-12 to D-15) — the one dialog
 * this milestone reserved. It is a **third** surface sequenced behind the other two, never
 * stacked with either, and both answers hand straight off to
 * [ProgressSubmitter.onProfileSwapResolved] without this scope ever owning the merge or the
 * wipe that follows.
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
 * @param progressSubmitter the single sync orchestrator. Exactly two of its members are
 *   consumed here — `profileSwap`, the pending account-switch question, and
 *   `onProfileSwapResolved`, its one answer channel. No push and no pull runs while that
 *   question stands (D-15), so this collector is what unblocks the whole cloud pass.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        onboardingRepository: OnboardingRepository,
        challengeProgress: ChallengeProgress,
        progressRepository: ProgressRepository,
        levelRepository: LevelRepository,
        private val optInRepository: PlayGamesOptInRepository,
        private val gamesService: GamesService,
        private val progressSubmitter: ProgressSubmitter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks (Pitfall 10). */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            // First-launch gate (D-14, D-02a): mirror the persisted flag. Once the tutorial's
            // own markSeen() flips it true, nothing is owed; until then the destination is
            // auto-started once and the two questions behind it are withheld.
            viewModelScope.launch {
                onboardingRepository.onboardingSeen.collect { seen ->
                    _uiState.update { it.copy(tutorialPending = !seen) }
                }
            }
            // The one-time Play Games opt-in prompt (D-15). Its visibility is a pure
            // function of two persisted flags, which is what makes it genuinely one-time:
            // it shows only once onboarding is behind the player AND the question is still
            // unanswered, so the two first-run surfaces are sequenced rather than stacked.
            // Once either answer lands, promptAnswered is true forever and this collector
            // can never emit true again — there is no per-session re-ask and no nag
            // (D-17 / T-11-08). The Settings row is the only later way in (D-19).
            //
            // Both inputs are maps over the SAME `app_prefs` DataStore flow, so every store
            // write (either flag, challenge progress, settings, ad cadence) re-emits both,
            // and combine delivers the two re-emissions in no fixed order. Without the
            // per-input dedupe, an unchanged `seen = true` arriving before the new
            // `answered = true` would re-raise the prompt for a frame right after the player
            // answered it (WR-07). Deduping each input means an unchanged flag is not an
            // event at all; the final dedupe keeps a same-valued recompute off the state.
            viewModelScope.launch {
                combine(
                    onboardingRepository.onboardingSeen.distinctUntilChanged(),
                    optInRepository.promptAnswered.distinctUntilChanged(),
                ) { seen, answered -> seen && !answered }
                    .distinctUntilChanged()
                    .collect { shouldShow ->
                        _uiState.update { it.copy(playGamesPromptVisible = shouldShow) }
                    }
            }
            // The account-switch question (D-12/D-15) — a THIRD surface, sequenced behind
            // the other two rather than layered over them. It is asked only once onboarding
            // is behind the player and the opt-in question has been answered, so a
            // brand-new player is never handed a choice between two accounts' progress
            // before they have played a level, and the opt-in prompt — which decides
            // whether Play Games is involved at all — is always answered first.
            //
            // Visibility is a pure function of those three inputs, so nothing here can
            // leave the dialog standing after the data layer has withdrawn the question,
            // and nothing can raise it without one.
            //
            // The two boolean inputs are deduped for the reason the collector above is
            // (WR-07): both are maps over the SAME `app_prefs` DataStore, so every store
            // write re-emits them and combine delivers the re-emissions in no fixed order.
            // Undeduped, an unchanged flag arriving after an answer would flicker the
            // dialog back on for a frame — over the only wipe in the app.
            viewModelScope.launch {
                combine(
                    progressSubmitter.profileSwap,
                    onboardingRepository.onboardingSeen.distinctUntilChanged(),
                    optInRepository.promptAnswered.distinctUntilChanged(),
                ) { swap, seen, answered -> swap.takeIf { seen && answered } }
                    .distinctUntilChanged()
                    .collect { request ->
                        _uiState.update {
                            it.copy(
                                profileSwapVisible = request != null,
                                profileSwapAccountName = request?.displayName,
                            )
                        }
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
            // The progress ribbon's two values (§7 R-1/R-2, D-03c). ONE collector over ONE
            // aggregated row: the Stars and Boards pills read the same emission, so they
            // structurally cannot disagree, and there is no second query for the second pill.
            //
            // A SIXTH INDEPENDENT collector, deliberately — not a `combine` over all inputs.
            // `combine` emits nothing until every input has emitted once, which would make
            // the whole Home state wait on a database round trip and would destroy the
            // placeholder rule structurally rather than merely in practice. The independent
            // collectors over one MutableStateFlow(HomeUiState()) are what make "the ribbon
            // paints '—' and fills in" true by construction (D-03g).
            //
            // The values stay `null` until this emits; a FAILED read arrives as
            // HomeProgress.UNKNOWN and is mapped back to `null` here, so "not yet" and "the
            // read failed" are indistinguishable on screen and neither renders as 0 (D-03h).
            viewModelScope.launch {
                progressRepository.observeHomeProgress().collect { progress ->
                    val known = progress != HomeProgress.UNKNOWN
                    _uiState.update {
                        it.copy(
                            totalStars = progress.totalStars.takeIf { known },
                            classicBoardsCleared = progress.classicBoardsCleared.takeIf { known },
                        )
                    }
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
                    val classicResume =
                        if (ProgressPersistenceKeys.MODE_CLASSIC in modes) {
                            resumableClassicBoard(progressRepository, levelRepository)
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
                            classicResumeLevelId = classicResume?.id,
                            classicResumeBoard = classicResume?.previewBoard,
                            challengeResumeLevel = challengeResumeLevel,
                        )
                    }
                }
            }
        }

        /**
         * The Classic catalogue **descriptor** a Continue may safely resume into, or `null`
         * when the stored snapshot is absent, unreadable, or points at a board the catalog no
         * longer knows (CR-01). A decodable blob with an unknown id is affirmatively stale —
         * the row is purged so the phantom Continue cannot come back on the next emission.
         *
         * It returns the descriptor rather than the bare id because the catalogue lookup that
         * validates the id already has the board in hand, and Phase 15's Continue chip renders
         * that board as its thumbnail (§4.2 band 3). Reaching for the id alone and mapping it
         * back to a board in the UI would be a **second** id-to-board translation, in the
         * layer QUAL-01 keeps id strings out of — and would read the static catalogue twice on
         * the start destination for a value this call already knows (D-03g).
         */
        private suspend fun resumableClassicBoard(
            progressRepository: ProgressRepository,
            levelRepository: LevelRepository,
        ): ClassicLevelDescriptor? {
            val storedLevelId =
                progressRepository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)?.levelId
                    ?: return null
            val descriptor = levelRepository.classicCatalog().firstOrNull { it.id == storedLevelId }
            if (descriptor == null) progressRepository.clearSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)
            return descriptor
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
         * guards, and the guard covers exactly the one expression that may throw. It also
         * catches `CancellationException`, which is rethrown first (WR-04): a cancelled round
         * trip is this scope being cleared, not an SDK error, and must not raise the tick.
         */
        fun onAcceptPlayGamesPrompt() {
            _uiState.update { it.copy(playGamesPromptVisible = false) }
            viewModelScope.launch {
                optInRepository.setOptedIn()
                runCatching { gamesService.signIn() }
                    .onFailure { failure ->
                        if (failure is CancellationException) throw failure
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

        /**
         * "Keep this device's progress" (D-13) — the safe answer, and the one **every**
         * dismissal resolves to.
         *
         * The device's progress is merged into the arriving account's cloud save exactly as
         * a first sign-in would merge it: a monotonic union, nothing lost on either side
         * (CLOUD-02). Back, a tap outside the scrim and any other dismissal arrive here too,
         * but that mapping lives at the screen layer — `ProfileSwapDialog` passes this
         * handler as its `onDismissRequest` — so the safe default is expressed in exactly
         * one place and cannot drift between two that both think they own it.
         *
         * The flag is cleared **synchronously, first**, then the resolution is handed off.
         * [ProgressSubmitter.onProfileSwapResolved] is deliberately not `suspend` and is
         * **not** wrapped in [viewModelScope]: it runs on the application scope, and awaiting
         * a merge-and-push from a screen scope is the anti-pattern its KDoc names — a
         * back-navigation mid-write would cancel the very work the answer authorised
         * (T-13-46).
         */
        fun onKeepDeviceProgress() {
            _uiState.update { it.copy(profileSwapVisible = false) }
            progressSubmitter.onProfileSwapResolved(keepLocalProgress = true)
        }

        /**
         * "Use their cloud progress" (D-14) — the deliberate, destructive answer.
         *
         * The device becomes the arriving account's device outright: the data layer wipes
         * the level results, the play history, the Challenge ladder and **both** resume
         * snapshots, then imports that account's cloud save. It is safe precisely because
         * the departing account's own data already lives in its own cloud save from earlier
         * pushes — but the dialog still names all four categories before this is reachable,
         * because a player must never authorise a wipe they could not have understood.
         *
         * Same shape as [onKeepDeviceProgress] and for a sharper version of the same reason:
         * the flag clears synchronously, and the wipe runs on the application scope. A
         * `viewModelScope` launch cancelled halfway through would leave the device a
         * half-replaced hybrid of two accounts (T-13-46).
         */
        fun onUseCloudProgress() {
            _uiState.update { it.copy(profileSwapVisible = false) }
            progressSubmitter.onProfileSwapResolved(keepLocalProgress = false)
        }
    }
