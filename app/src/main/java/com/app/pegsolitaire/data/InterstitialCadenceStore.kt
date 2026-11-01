package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.domain.ads.InterstitialCadenceParams
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped constants for the interstitial-cadence Preferences keys (QUAL-03 — no inline literals). */
internal object InterstitialCadencePreferences {
    /** Wins banked since the last interstitial (D-01). */
    val WINS_SINCE_LAST_KEY = intPreferencesKey("interstitial_wins_since_last")

    /**
     * Wall-clock stamp of the last **fullscreen** ad — interstitial OR rewarded (D-15).
     *
     * The name says `fullscreen`, not `interstitial`, and that is the whole mechanism: a rewarded
     * ad writes this key too, so the D-02 cooldown it opens forbids an interstitial from following
     * it back-to-back. Anything that stamps only interstitials here reopens that adjacency.
     */
    val LAST_FULLSCREEN_AD_AT_KEY = longPreferencesKey("interstitial_last_fullscreen_ad_at")

    /** Levels completed since install, for D-03's first-levels suppression. */
    val LIFETIME_COMPLETED_KEY = intPreferencesKey("interstitial_lifetime_completed")

    /** Whether the install's first session has ended (the other half of D-03). */
    val FIRST_SESSION_DONE_KEY = booleanPreferencesKey("interstitial_first_session_done")

    /**
     * Wall-clock stamp of the last time the app left the foreground (D-04's session boundary).
     *
     * Not prefixed `interstitial_` because it describes the app, not the ad: it is written by
     * `AppForegroundTracker` from a process-lifecycle event and is meaningful to anything that
     * ever needs a session boundary.
     */
    val LAST_FOREGROUND_AT_KEY = longPreferencesKey("app_last_foreground_at")
}

/**
 * The memory of the interstitial cadence (INT-02): [InterstitialCadenceState] decides, and this
 * class is the only thing that remembers.
 *
 * Backed by the **shared** `app_prefs` DataStore (never a second `preferencesDataStore` delegate —
 * that crashes at runtime; see [AppPreferences]), so the frequency cap survives the app being
 * killed. The shape is [ChallengeProgress]'s, deliberately and in full: a `stateIn(…, Eagerly, …)`
 * read with a synchronously readable `.value`, plain (non-`suspend`) writes that fire and forget on
 * the injected app-lifetime scope, and `storageFailSafe` around every one of them.
 *
 * ## The persisted / in-memory split (D-13)
 *
 * Four scalars live on disk; [InterstitialCadenceState.interstitialsThisSession] lives in
 * [sessionCount], in memory, and starts at zero on every process start. See
 * [startSessionIfGapElapsed] for the residual that choice accepts and why it is the right trade.
 *
 * ## Reads fail toward silence
 *
 * `catch { emit(emptyPreferences()) }` turns a corrupt or unreadable store into the default state,
 * and the default state has `firstSessionComplete == false`, which alone makes
 * [InterstitialCadenceState.mayShow] false at every instant. A damaged store therefore costs
 * revenue, never a crash and never an unauthorised ad (T-10-01). This is the deliberate inverse of
 * [SettingsRepositoryImpl]'s fail-open, whose worst case is a lost preference rather than an ad.
 *
 * ## Why `@Singleton` is on the CLASS
 *
 * The same reason [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider]
 * documents: this object holds state that only makes sense once per process. A second, unscoped
 * instance would carry a second [sessionCount] starting at zero, so the D-04 per-session cap would
 * bind against a counter nothing ever increments — a silent failure with no exception to notice.
 * There is no interface here and none is needed; callers inject the concrete type, which is exactly
 * why the annotation must sit on the class rather than on a binding.
 *
 * Nothing in this file logs: the cadence describes ad behaviour, and ad-adjacent state is the
 * ad path's standing no-logging prohibition (ASVS V7).
 */
@Singleton
class InterstitialCadenceStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /** The D-13 in-memory half: interstitials shown since this process's current session began. */
        private val sessionCount = MutableStateFlow(0)

        /**
         * The full cadence state — the four persisted scalars recombined with the in-memory
         * session count.
         *
         * `combine` is what makes the split invisible to callers: [state] is one value with all
         * five fields, so nothing downstream needs to know which half of it came from disk.
         * `stateIn(…, Eagerly, InterstitialCadenceState())` gives the synchronous `.value` the
         * trigger reads at the moment a level is won, seeded with the default that authorises
         * nothing until the first disk read lands.
         */
        val state: StateFlow<InterstitialCadenceState> =
            combine(
                dataStore.data.catch { emit(emptyPreferences()) },
                sessionCount,
            ) { preferences, session ->
                preferences.readCadence().copy(interstitialsThisSession = session)
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = InterstitialCadenceState(),
            )

        /**
         * When the app last left the foreground, or [InterstitialCadenceState.NEVER_FOREGROUNDED].
         *
         * A separate flow rather than a sixth field on [state] because the cadence decision never
         * reads it — only the session-boundary transition does. Keeping it out of the state value
         * keeps [InterstitialCadenceState.mayShow]'s inputs to exactly what it consumes.
         */
        val lastForegroundAtMs: StateFlow<Long> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[InterstitialCadencePreferences.LAST_FOREGROUND_AT_KEY]
                        ?: InterstitialCadenceState.NEVER_FOREGROUNDED
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = InterstitialCadenceState.NEVER_FOREGROUNDED,
                )

        /** Banks a completed level in either mode (D-01/D-03/D-07). Best-effort like every write here. */
        fun recordWin() {
            scope.launch {
                storageFailSafe(fallback = Unit) {
                    dataStore.edit { preferences -> preferences.applyTransition { it.recordWin() } }
                }
            }
        }

        /**
         * Records that an interstitial **actually presented** at [nowMs] (D-11 — never on a no-fill).
         *
         * The in-memory half is raised synchronously, before the write is even launched, because the
         * ad has already been on screen: a failed or slow disk write must not let the D-04 cap
         * under-count what the player just saw.
         */
        fun recordInterstitialShown(nowMs: Long) {
            sessionCount.update { (it + 1).coerceAtMost(InterstitialCadenceParams.MAX_PER_SESSION) }
            scope.launch {
                storageFailSafe(fallback = Unit) {
                    dataStore.edit { preferences ->
                        preferences.applyTransition { it.recordInterstitialShown(nowMs) }
                    }
                }
            }
        }

        /**
         * Records that a REWARDED ad presented at [nowMs] — it moves the shared fullscreen stamp and
         * nothing else (D-15), so an interstitial can never immediately follow one.
         */
        fun recordRewardedShown(nowMs: Long) {
            scope.launch {
                storageFailSafe(fallback = Unit) {
                    dataStore.edit { preferences ->
                        preferences.applyTransition { it.recordRewardedShown(nowMs) }
                    }
                }
            }
        }

        /** Stamps the moment the app left the foreground — the input to the next [startSessionIfGapElapsed]. */
        fun recordForegroundLeft(nowMs: Long) {
            scope.launch {
                storageFailSafe(fallback = Unit) {
                    dataStore.edit {
                        it[InterstitialCadencePreferences.LAST_FOREGROUND_AT_KEY] = nowMs
                    }
                }
            }
        }

        /**
         * Opens a new session if the app has been backgrounded for the whole
         * [InterstitialCadenceParams.SESSION_GAP_MS] window (D-04, D-16) — otherwise does nothing.
         *
         * The boundary is read INSIDE the transaction, from the transaction's own preferences, so
         * the decision and the write it authorises cannot be separated by a concurrent edit. Setting
         * [sessionCount] to zero from inside the transform is safe under DataStore's retry semantics
         * because it is idempotent: the transform may run more than once, and every run assigns the
         * same value.
         *
         * ## The D-13 kill-window residual, stated rather than hidden
         *
         * [sessionCount] is in memory **by decision**, so a force-quit and relaunch inside 30 minutes
         * reports zero interstitials for what is still the same session. D-01's three wins and D-02's
         * 90-second gap both still bind, so the worst case is one extra ad per force-quit — and
         * force-quitting the app in order to be shown MORE ads is not a threat model. Do not
         * "fix" this by persisting the count: that contradicts a locked decision.
         */
        fun startSessionIfGapElapsed(nowMs: Long) {
            scope.launch {
                storageFailSafe(fallback = Unit) {
                    dataStore.edit { preferences ->
                        val lastForegroundAt =
                            preferences[InterstitialCadencePreferences.LAST_FOREGROUND_AT_KEY]
                                ?: InterstitialCadenceState.NEVER_FOREGROUNDED
                        if (InterstitialCadenceState.isNewSession(lastForegroundAt, nowMs)) {
                            preferences.applyTransition { it.startNewSession() }
                            sessionCount.value = 0
                        }
                    }
                }
            }
        }

        /**
         * The cadence state these stored preferences denote, validated on read by the domain.
         *
         * Every read in this class goes through here, so clamping a tampered counter and rejecting
         * a forged stamp happens in exactly one place — the domain's `fromStored` — and the store
         * itself has no opinion to get wrong.
         */
        private fun Preferences.readCadence(): InterstitialCadenceState =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = this[InterstitialCadencePreferences.WINS_SINCE_LAST_KEY],
                lastFullscreenAdAtMs = this[InterstitialCadencePreferences.LAST_FULLSCREEN_AD_AT_KEY],
                lifetimeCompletedLevels = this[InterstitialCadencePreferences.LIFETIME_COMPLETED_KEY],
                firstSessionComplete = this[InterstitialCadencePreferences.FIRST_SESSION_DONE_KEY],
            )

        /**
         * Applies a domain [transition] to the state these preferences currently hold, and writes
         * the four persisted fields back.
         *
         * **The store performs no arithmetic of its own**, and reads the current state from the
         * transaction's own preferences rather than from the cached [state] value. Both halves of
         * that sentence are load-bearing. Delegating the arithmetic means the counters saturate and
         * the sentinels are honoured in one place — the tested domain policy — instead of being
         * restated here where the two copies could drift. Merging inside the transaction is what
         * makes concurrent writes incapable of a lost update (T-10-12), exactly as
         * [ChallengeProgress.advancePast] does it. Do not "simplify" either one.
         *
         * [InterstitialCadenceState.interstitialsThisSession] is intentionally NOT written: the
         * transition may move it, but D-13 keeps that half in [sessionCount].
         */
        private fun MutablePreferences.applyTransition(
            transition: (InterstitialCadenceState) -> InterstitialCadenceState,
        ) {
            val next = transition(readCadence())
            this[InterstitialCadencePreferences.WINS_SINCE_LAST_KEY] = next.winsSinceLastInterstitial
            this[InterstitialCadencePreferences.LAST_FULLSCREEN_AD_AT_KEY] = next.lastFullscreenAdAtMs
            this[InterstitialCadencePreferences.LIFETIME_COMPLETED_KEY] = next.lifetimeCompletedLevels
            this[InterstitialCadencePreferences.FIRST_SESSION_DONE_KEY] = next.firstSessionComplete
        }
    }
