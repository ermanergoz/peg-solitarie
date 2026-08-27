package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which the game talks to Play Games Services (spec §8.4,
 * PLAT-03).
 *
 * Slice 1 ships only the seam: a [NoOpGamesService] stub reports "not signed in" and
 * drops every submission, so nothing in the app depends on the Play Games SDK. The seam
 * covers an observable [session], leaderboard submission, achievements, and the two
 * stock Play Games UI intents — the whole method set Phases 11-12 need, declared before
 * any Play Games SDK code exists so that the SDK never reaches `ui/`.
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
     * Signs the player in, returning whether they are signed in afterwards. It is
     * `suspend` because the Phase-11 implementation awaits a real sign-in round trip.
     * The Slice-1 stub returns `false`: callers must treat a `false` return as
     * "Play Games unavailable" and hide the dependent affordance rather than assume
     * a connected player.
     *
     * There is deliberately no counterpart to this method: Play Games Services v2 signs
     * the player in automatically and exposes no app-facing sign-out, so the v1 pattern
     * of a sign-in wall the player can leave does not port.
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
