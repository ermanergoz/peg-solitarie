package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
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
 * The persistence half of the interstitial cadence (INT-02, D-13) — see the implementation
 * commit for the full contract.
 */
@Singleton
class InterstitialCadenceStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        val state: StateFlow<InterstitialCadenceState>
            get() = TODO("10-02 task 1: read the persisted scalars + the in-memory session count")

        val lastForegroundAtMs: StateFlow<Long>
            get() = TODO("10-02 task 1: read the persisted last-foreground stamp")

        fun recordWin(): Unit = TODO("10-02 task 1")

        fun recordInterstitialShown(nowMs: Long): Unit = TODO("10-02 task 1: $nowMs")

        fun recordRewardedShown(nowMs: Long): Unit = TODO("10-02 task 1: $nowMs")

        fun recordForegroundLeft(nowMs: Long): Unit = TODO("10-02 task 1: $nowMs")

        fun startSessionIfGapElapsed(nowMs: Long): Unit = TODO("10-02 task 1: $nowMs")
    }
