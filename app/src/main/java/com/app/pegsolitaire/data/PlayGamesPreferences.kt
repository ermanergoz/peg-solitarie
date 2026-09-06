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
}
