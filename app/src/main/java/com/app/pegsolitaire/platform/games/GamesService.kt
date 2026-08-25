package com.app.pegsolitaire.platform.games

/**
 * The platform seam through which the game talks to Play Games Services (spec §8.4).
 *
 * Slice 1 ships only the seam: a [NoOpGamesService] stub reports "not signed in" and
 * drops submissions, so nothing in the app depends on the Play Games SDK. Slice 2
 * swaps a single Hilt binding in `PlatformModule` for a real
 * `play-services-games-v2`-backed implementation and leaderboards/achievements light
 * up with no gameplay or UI change — the same swap-point contract
 * [com.app.pegsolitaire.platform.ads.AdGateway] provides for ads.
 *
 * The method set is deliberately minimal: only what a leaderboard-submitting puzzle
 * game needs. Cloud save (Saved Games) is explicitly out of scope here — it arrives
 * as a second data source behind `ProgressRepository`, not as a method on this seam.
 */
interface GamesService {
    /**
     * Signs the player in, returning whether they are signed in afterwards. It is
     * `suspend` because the Slice-2 implementation awaits a real sign-in round trip.
     * The Slice-1 stub returns `false`: callers must treat a `false` return as
     * "Play Games unavailable" and hide the dependent affordance rather than assume
     * a connected player.
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
}
