package com.app.pegsolitaire.data

import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Scoped constants for the two Play Games opt-in Preferences keys (QUAL-03 — no inline
 * magic strings), mirroring [OnboardingPreferences].
 *
 * Both live in the shared `app_prefs` store (see [AppPreferences]) rather than a store of
 * their own — the D-17 answer is one more launch-time preference alongside
 * `onboarding_seen`, not a new persistence concern.
 */
internal object PlayGamesPreferences {
    /** Whether the one-time opt-in prompt has been answered either way (D-15/D-17). */
    val PROMPT_ANSWERED_KEY = booleanPreferencesKey("play_games_prompt_answered")

    /** Fail-open default (D-17): a missing/corrupt read is treated as "never asked". */
    const val DEFAULT_PROMPT_ANSWERED = false

    /** Whether the player opted in to Play Games — D-01's launch-time gate. */
    val OPTED_IN_KEY = booleanPreferencesKey("play_games_opted_in")

    /** Fail-open default (D-17): a missing/corrupt read never fabricates an opt-in. */
    const val DEFAULT_OPTED_IN = false

    /**
     * Whether a Play Games submission is outstanding — D-10's "submissions pending"
     * marker, read and written by [SubmissionSyncStore] and nothing else.
     *
     * Set whenever a solve is recorded while
     * [com.app.pegsolitaire.platform.games.GamesService.session] is not
     * `SignedIn` — which includes `Unknown`, the state before the first check answers —
     * and whenever ANY submission in a pass reports `false` or throws. Cleared only when
     * a pass that actually issued calls saw every one of them succeed.
     *
     * The key string lives here and nowhere else (QUAL-03, CLAUDE.md's no-magic-strings
     * rule), beside the two opt-in flags, so every `play_games_*` name in the shared
     * `app_prefs` store is declared in one scoped constants object.
     */
    val SUBMISSIONS_PENDING_KEY = booleanPreferencesKey("play_games_submissions_pending")

    /**
     * Fail-open default: a missing key (fresh install) or a corrupt/IO read means
     * "nothing pending".
     *
     * **The tradeoff is real and the direction is deliberate.** `true` would be *more*
     * recoverable — every submission is idempotent (absolute steps, D-12) and an empty
     * database issues no SDK calls at all, so a spurious pass costs nothing — but it
     * contradicts D-10's "clean launches make no calls", which is the property that keeps
     * a player who has never signed in away from the Play Games code path entirely. `false`
     * is taken instead, and its worst case is bounded: at most one solve's delay before the
     * next write re-sets the flag and the backfill resumes.
     */
    const val DEFAULT_SUBMISSIONS_PENDING = false
}
