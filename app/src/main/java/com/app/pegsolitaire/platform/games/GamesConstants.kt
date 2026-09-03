package com.app.pegsolitaire.platform.games

/**
 * Play Games timing tokens: the bounded waits the games seam's implementations must
 * observe.
 *
 * Scoped, not global — one object per platform concern, as
 * [com.app.pegsolitaire.platform.ads.AdConstants] is for ads and
 * [com.app.pegsolitaire.audio.SoundParams] is for audio. The value below happens to equal
 * `AdConstants.ACTIVITY_WAIT_MS` today, and this object deliberately does **not** reference
 * it: the two answer the same *question* for different subsystems, and importing one
 * package's constant into another would make an ad-timing change silently retune Play
 * Games sign-in. Duplication across a package boundary is the cheaper mistake here.
 *
 * **What must never be written here:** anything identifying. The Play Games services
 * project ID is a per-build-type value resolved through `resValue`/`@string` and read by
 * the SDK from the manifest — never a Kotlin constant. No player identifier, no display
 * name and no leaderboard/achievement ID belongs in this file either; Phase 12's IDs get
 * their own build-time resolution, following the [com.app.pegsolitaire.platform.ads.AdIds]
 * precedent.
 */
object GamesConstants {
    /**
     * The cap callers MUST apply around
     * [com.app.pegsolitaire.platform.activity.ForegroundActivityProvider.awaitActivity].
     *
     * That interface's own KDoc makes the timeout the caller's responsibility: a
     * backgrounded app may never resume, and an unbounded await inside a process-lifetime
     * singleton hangs it for the life of the process (T-11-06). Giving up here is not a
     * failure mode to be avoided — it resolves
     * [com.app.pegsolitaire.platform.games.GamesSignInState.SignedOut], which is exactly
     * what D-12 asks for: one state for every "not signed in" cause.
     */
    const val ACTIVITY_WAIT_MS = 10_000L
}
