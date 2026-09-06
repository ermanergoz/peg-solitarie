package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which the game talks to Play Games Services (spec §8.4,
 * PLAT-03).
 *
 * Slice 1 ships only the seam: a [NoOpGamesService] stub reports "not signed in" and
 * drops every submission, so nothing in the app depends on the Play Games SDK. The seam
 * covers an observable [session], the opt-in-gated launch bootstrap
 * ([bootstrapIfOptedIn]), explicit [signIn], leaderboard submission, achievements, and
 * the two stock Play Games UI intents — the whole method set Phases 11-12 need, declared
 * before any Play Games SDK code exists so that the SDK never reaches `ui/`.
 *
 * **The opt-in gate is part of the contract, not an implementation detail.** Every
 * implementation must touch the Play Games SDK only through [bootstrapIfOptedIn]'s
 * opted-in branch or an explicit [signIn] call, so a player who has never opted in is
 * never reached by the SDK at all (D-01/D-17).
 *
 * Phase 11 binds a real implementation in `PlatformModule`, but that is **not** a
 * binding swap alone: the stock UI intents resolve to an `Intent` that something must
 * start, so the real implementation also depends on the foreground-Activity bridge
 * (PLAT-01). What the swap does guarantee is that the dependency stops there — this
 * interface names no Activity type and no Play Games SDK type, so no ViewModel can
 * obtain one through it.
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
     * Submits [score] to the leaderboard identified by [leaderboardId]. Fire-and-forget
     * by design — a failed submission must never block or fail a solved level. A no-op
     * in Slice 1.
     */
    fun submitScore(
        leaderboardId: String,
        score: Long,
    )

    /**
     * Unlocks the one-shot achievement identified by [achievementId]. Fire-and-forget
     * by design — a failed submission must never block or fail a solved level. A no-op
     * in Slice 1.
     */
    fun unlockAchievement(achievementId: String)

    /**
     * Advances the incremental achievement identified by [achievementId] by [steps].
     * Fire-and-forget by design — a failed submission must never block or fail a solved
     * level. A no-op in Slice 1.
     */
    fun incrementAchievement(
        achievementId: String,
        steps: Int,
    )

    /**
     * Opens the stock Play Games leaderboard UI for [leaderboardId]. Returns nothing
     * because Play Games Services v2 hands back no result the app must act on, and is a
     * silent no-op when Play Games is unavailable — the player is never shown an error
     * for a cosmetic surface they cannot reach. `suspend` so the Phase-11 implementation
     * can await a foreground Activity with a timeout. A no-op in Slice 1.
     */
    suspend fun showLeaderboard(leaderboardId: String)

    /**
     * Opens the stock Play Games achievements UI. A silent no-op when Play Games is
     * unavailable, and `suspend` for the same reason as [showLeaderboard]. A no-op in
     * Slice 1.
     */
    suspend fun showAchievements()
}
