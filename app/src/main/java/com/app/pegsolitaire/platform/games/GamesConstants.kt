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
 * name and no leaderboard/achievement ID belongs in this file either: the 27 leaderboard
 * and achievement IDs are resolved at build time into `BuildConfig` and read through
 * [GamesIds], which follows the [com.app.pegsolitaire.platform.ads.AdIds] precedent. That
 * reservation has been honoured — the IDs live there, not here.
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

    /**
     * The `requestCode` passed to `Activity.startActivityForResult` when launching the stock
     * all-leaderboards picker.
     *
     * **A request code is not optional here, and `startActivity` is not an equivalent.** The
     * 22.0.0 javadoc for every Intent getter (`getAllLeaderboardsIntent`,
     * `getLeaderboardIntent`, `getAchievementsIntent`) states the returned Intent *"must be
     * invoked with `Activity.startActivityForResult(Intent, int)`, so that the identity of the
     * calling package can be established"* — launched the plain way, the stock UI comes up
     * broken rather than failing loudly, which is the kind of bug that survives review.
     *
     * **Nothing consumes the result, deliberately.** `MainActivity` overrides no
     * `onActivityResult`: Play Games v2 hands back no outcome the app must act on, and the
     * code exists purely so the SDK can attribute the launch. It is a `const val` rather than
     * an inline literal because detekt's `MagicNumber` rule is active over `src/main` and
     * The project forbids magic numbers regardless.
     *
     * Value matches Google's own Play Games samples (`RC_LEADERBOARD_UI`).
     */
    const val LEADERBOARDS_REQUEST_CODE = 9004

    /**
     * The `requestCode` passed to `Activity.startActivityForResult` when launching the stock
     * achievements UI — same contract, same reasoning and the same "nothing reads the result"
     * note as [LEADERBOARDS_REQUEST_CODE].
     *
     * Value matches Google's own Play Games samples (`RC_ACHIEVEMENT_UI`). The two codes are
     * distinct only because `startActivityForResult` requires a code per launch site; no
     * dispatch is ever performed on them.
     */
    const val ACHIEVEMENTS_REQUEST_CODE = 9003
}
