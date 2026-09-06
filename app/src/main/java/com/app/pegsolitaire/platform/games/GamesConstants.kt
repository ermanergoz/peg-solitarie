package com.app.pegsolitaire.platform.games

import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Play Games timing and status tokens: the bounded waits the games seam's implementations
 * must observe, and the SDK status codes they classify.
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

    /**
     * The status code Google's sign-in flows report when the player backs out of the account
     * chooser: `GoogleSignInStatusCodes.SIGN_IN_CANCELLED`.
     *
     * Declared here, with its source, because that constant lives in `play-services-auth`,
     * which is not on this project's dependency graph — pulling a second Play SDK in for one
     * integer is the worse trade. Source:
     * https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInStatusCodes#SIGN_IN_CANCELLED
     */
    const val SIGN_IN_CANCELLED_STATUS_CODE = 12501

    /**
     * The status codes a failed `Task<AuthenticationResult>` reports for a player-initiated
     * cancel (D-10) — the family `PlayGamesService.signIn` swallows as a silent decline rather
     * than raising as an error (WR-08). The generic [CommonStatusCodes.CANCELED] covers a
     * cancelled PendingIntent; [SIGN_IN_CANCELLED_STATUS_CODE] is what a dismissed account
     * chooser most commonly reports, and treating it as an error would show the "Couldn't
     * sign in" snackbar to a player who simply changed their mind — the misclassification D-10
     * names as the worse one.
     *
     * **[UNVERIFIED against a live Play Console project — D-04]**: both codes are on the
     * pre-release emulator checklist (`11-HUMAN-UAT.md`).
     */
    val CANCEL_STATUS_CODES: Set<Int> = setOf(CommonStatusCodes.CANCELED, SIGN_IN_CANCELLED_STATUS_CODE)
}
