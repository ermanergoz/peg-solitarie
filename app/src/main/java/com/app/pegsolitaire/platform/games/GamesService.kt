package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which the game talks to Play Games Services (spec §8.4,
 * PLAT-03).
 *
 * The seam covers an observable [session], the opt-in-gated launch bootstrap
 * ([bootstrapIfOptedIn]), explicit [signIn], leaderboard and achievement submission, and
 * the two stock Play Games UI launches — the whole method set Phases 11-12 need. It was
 * declared before any Play Games SDK code existed, precisely so that the SDK could never
 * reach `ui/`; `PlayGamesService` now implements all of it, and [NoOpGamesService]
 * remains the stub that declines everything.
 *
 * **The opt-in gate is part of the contract, not an implementation detail.** Every
 * implementation must touch the Play Games SDK only through [bootstrapIfOptedIn]'s
 * opted-in branch or an explicit [signIn] call, so a player who has never opted in is
 * never reached by the SDK at all (D-01/D-17).
 *
 * **Failure is a return value, not an exception (PGSL-04).** The three submission methods
 * report `false` rather than throwing, so a caller can *observe* a failed submission and
 * act on it. That observability is the whole reason the signatures are shaped this way:
 * the fire-and-forget SDK forms (`submitScore`, `unlock`, `setSteps`) return `void` and
 * report nothing, which would leave the Phase-12 pending-submissions flag (D-10) with no
 * event to be defined in terms of. A `Unit`-returning submission signature is therefore
 * not a simplification of this contract — it is the removal of its point.
 *
 * **`Boolean` means "the SDK accepted it"**, i.e. the round trip completed without
 * throwing. It is explicitly **not** the payload of the SDK's own
 * `AchievementsClient.setStepsImmediate`, whose `Task<Boolean>` reports *"the achievement
 * is now unlocked"* — the single most likely misreading of [setAchievementSteps]'s
 * signature. An implementation discards that payload and reports acceptance.
 *
 * **`false` is a normal, expected outcome** — offline, backgrounded with no foreground
 * Activity, not signed in, or a misconfigured Play Console project — and is **never** an
 * error the player is shown. The caller's answer to `false` is to set the
 * pending-submissions flag and resubmit later (D-10); it is never a snackbar, and never a
 * blocked or failed level.
 *
 * **No popup method appears on this interface, and none can (D-16).** Play Games Services
 * v2 retired the v1 client that owned popup placement and exposes no replacement, so the
 * stock "achievement unlocked" toast is positioned and rendered entirely by the SDK, in its
 * default position, outside the app's control. That is the intended outcome, not a gap to
 * be filled: there is no method an implementation could honour even if this seam declared
 * one. A grep gate enforces the absence, so this paragraph names no such API.
 *
 * This interface names no Activity type and no Play Games SDK type in any signature, so no
 * ViewModel can obtain one through it. That is load-bearing for the two stock-UI methods in
 * particular: their `Intent`s are both *obtained* and *started* inside `platform/games`, so
 * nothing framework-shaped crosses into `ui/` (PLAT-03/PLAT-04). The real implementation
 * consequently depends on the foreground-Activity bridge (PLAT-01) — and the dependency
 * stops there.
 *
 * The method set is deliberately minimal: only what a leaderboard-submitting puzzle
 * game needs. Cloud save (Saved Games) is explicitly out of scope here — it arrives
 * as a second data source behind `ProgressRepository`, not as a method on this seam.
 */
interface GamesService {
    /**
     * The player's current sign-in state, [GamesSignInState.Unknown] until the first
     * check completes. Observable so a surface reacts to sign-in resolving
     * asynchronously rather than holding a stale answer from a single [signIn] call.
     */
    val session: StateFlow<GamesSignInState>

    /**
     * The launch-time, **opt-in-gated** bootstrap (D-01) — the single place the Play
     * Games SDK may first be touched, and the one gate every implementation of this
     * interface must respect.
     *
     * The implementation reads the persisted opt-in flag
     * ([com.app.pegsolitaire.data.PlayGamesOptInRepository], D-17) and:
     *
     * - **if and only if the player previously opted in** — via the Home opt-in prompt
     *   (D-15/D-18) or the Settings sign-in row (D-19) — runs `PlayGamesSdk.initialize`
     *   plus the silent `isAuthenticated` check, then resolves [session] to
     *   [GamesSignInState.SignedIn] or [GamesSignInState.SignedOut] (implemented in
     *   `PlayGamesService`);
     * - **otherwise, does nothing at all**: no `PlayGamesSdk.initialize`, no
     *   `isAuthenticated`, no SDK UI. [session] resolves straight to
     *   [GamesSignInState.SignedOut] **without the SDK ever being touched**, so a
     *   player who has never opted in is also never [GamesSignInState.Unknown].
     *
     * Deliberately **not** `suspend` and fire-and-forget: called from
     * `MainActivity.onCreate` immediately after `consentGateway.bootstrap()`, it must
     * never block or delay the first frame or the first tap. It returns before any
     * sign-in work has finished — callers observe the outcome through [session], never
     * through this method.
     *
     * Runs once per process (D-02); afterwards [session] changes only through accepting
     * the opt-in prompt (D-18) or the manual [signIn] path from Settings (D-10). There
     * is no re-check on foreground return and no connectivity listener.
     *
     * Logs nothing about the player: neither the opt-in flag nor any identifier reaches
     * logcat, here or in the real implementation.
     */
    fun bootstrapIfOptedIn()

    /**
     * Signs the player in explicitly, returning whether they are signed in afterwards.
     * Called from the Home opt-in prompt's positive action (D-18) and from the Settings
     * sign-in row (D-10/D-19) — never at launch, which goes through
     * [bootstrapIfOptedIn]. It is `suspend` because the Phase-11 implementation awaits a
     * real sign-in round trip.
     *
     * ## Return-vs-throw: a decline is not an error (D-10)
     *
     * The two ways this call can fail to produce a signed-in player are **not** the same
     * outcome, and are deliberately not conflated into one `Boolean`:
     *
     * - **Returns `false`** — a plain, player-initiated decline: they dismissed the
     *   account chooser, pressed back, or otherwise cancelled. This is **silent at every
     *   call site**: no snackbar, no error copy, no retry prompt. The Settings row simply
     *   remains (D-13), and the player may tap it again whenever they like.
     * - **Throws** — a genuine SDK-reported error (misconfigured Play Console project,
     *   network failure, Play services unavailable). This is what raises the D-10/D-18
     *   snackbar ("Couldn't sign in to Play Games") at the caller, via the existing
     *   `ClaySnackbarHost`. The row remains in this case too.
     *
     * Classifying an SDK outcome into those two branches happens in exactly one place —
     * `PlayGamesService` (plan 11-02) — so no ViewModel ever inspects an SDK exception
     * type. Callers only need to distinguish "returned `false`" from "threw".
     *
     * **[UNVERIFIED against a live Play Console project — D-04]** The exact
     * `Task<AuthenticationResult>` shape that Play Games v2 reports for a plain chooser
     * dismissal, versus a genuine error, is undocumented by Google (RESEARCH assumption
     * A2 / Open Question 2), and no Play Console project exists yet against which to
     * observe it. `PlayGamesService` therefore implements the conservative
     * classification, and confirming it on a real signed-in device is a pre-release
     * checklist item (D-04). If the observed taxonomy differs, only that one
     * classification changes — this contract, and every caller written against it, stay
     * as they are. Naming the open uncertainty inline follows this codebase's existing
     * convention (see `AppForegroundTracker`'s A1 precedent).
     *
     * The Slice-1 [NoOpGamesService] stub always returns `false` and never throws: a
     * stub has no SDK outcome to classify, so it reports the silent-decline branch and
     * callers correctly keep every dependent affordance hidden.
     *
     * There is deliberately no counterpart to this method: Play Games Services v2 signs
     * the player in automatically and exposes no app-facing sign-out, so the v1 pattern
     * of a sign-in wall the player can leave does not port.
     *
     * @return `true` when the player is signed in afterwards; `false` on a plain,
     *   silent decline.
     * @throws Exception when the SDK reports a genuine sign-in error the caller must
     *   surface to the player (D-10). Never thrown by [NoOpGamesService].
     */
    suspend fun signIn(): Boolean

    /**
     * Submits [score] to the leaderboard identified by [leaderboardId], reporting whether
     * the SDK accepted it.
     *
     * Awaiting this never blocks or fails a solved level: the caller launches it on an
     * application-scoped coroutine, so the Results screen neither waits for the round trip
     * nor cancels it on back-navigation (PGSL-04, D-13).
     *
     * @return `true` when the round trip completed without throwing; `false` for every
     *   "nothing was submitted" cause — signed out, backgrounded, offline, SDK failure.
     *   `false` is not an error: the caller records the pending-submissions flag (D-10)
     *   and resubmits later. This method does not throw for a submission failure.
     */
    suspend fun submitScore(
        leaderboardId: String,
        score: Long,
    ): Boolean

    /**
     * Unlocks the one-shot achievement identified by [achievementId], reporting whether
     * the SDK accepted it. Safe to resend: unlocking an already-unlocked achievement is a
     * no-op on Google's side.
     *
     * @return `true` on acceptance, `false` on any failure — same contract, and same
     *   caller response, as [submitScore].
     */
    suspend fun unlockAchievement(achievementId: String): Boolean

    /**
     * Sets the incremental achievement identified by [achievementId] to [steps] —
     * **absolute** steps, not a delta (D-12).
     *
     * Absoluteness is what makes resubmission safe and idempotent, and is why this
     * supersedes the increment-by-delta shape: a retried delta double-counts, whereas a
     * retried absolute value converges on the truth the app already holds. The SDK no-ops
     * when the player's recorded steps already exceed [steps], so a late or duplicated
     * submission can never move progress backwards either.
     *
     * Play Games documents that `numSteps` must be **greater than 0**. Callers are
     * responsible for omitting zero values; this seam adds no defensive clamp, because it
     * does not invent behaviour the SDK owns and a silently-swallowed zero would hide a
     * caller bug rather than fix it.
     *
     * @return `true` when the round trip completed without throwing; `false` otherwise.
     *   Note this is *acceptance*, not the SDK's own `Task<Boolean>` payload — that
     *   payload means "the achievement is now unlocked" and is deliberately discarded.
     */
    suspend fun setAchievementSteps(
        achievementId: String,
        steps: Int,
    ): Boolean

    /**
     * Opens the stock Play Games **all-leaderboards picker**.
     *
     * Takes no argument because the SDK's own `getAllLeaderboardsIntent()` takes none
     * (D-14): the player is shown every leaderboard the game publishes and chooses among
     * them, so there is no id a caller could meaningfully supply.
     *
     * Returns nothing because Play Games Services v2 hands back no result the app must act
     * on, and is a silent no-op when Play Games is unavailable, the player is signed out,
     * or no foreground Activity arrives in time — the player is never shown an error for a
     * cosmetic surface they cannot reach. `suspend` so the implementation can await a
     * foreground Activity with a timeout.
     */
    suspend fun showLeaderboards()

    /**
     * Opens the stock Play Games achievements UI. Best-effort and a silent no-op under the
     * same conditions, and `suspend` for the same reason, as [showLeaderboards].
     */
    suspend fun showAchievements()
}
