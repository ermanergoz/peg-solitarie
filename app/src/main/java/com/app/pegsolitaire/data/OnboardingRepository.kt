package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped constants for the onboarding Preferences key (QUAL-03 — no inline magic strings). */
internal object OnboardingPreferences {
    /** The single boolean key tracking whether the first-run how-to-play has been seen. */
    val SEEN_KEY = booleanPreferencesKey("onboarding_seen")

    /** Fail-open default (D-16): a missing/corrupt read is treated as "not seen" → teach. */
    const val DEFAULT_SEEN = false
}

/**
 * The single source of truth for the first-run onboarding flag (ONB-01, D-16).
 *
 * Backed by a single DataStore-Preferences boolean (`onboarding_seen`). The read
 * **fails open**: a missing key (fresh install) or a corrupt/IO read both degrade to
 * `false` ("not seen" → show onboarding), so a damaged store can never crash Home —
 * it only re-teaches the harmless tutorial (T-02-08-DS). `HomeViewModel` collects
 * [onboardingSeen] to gate the overlay and calls [markSeen] on dismiss.
 *
 * The `@Inject internal` constructor takes the shared [DataStore] directly: Hilt
 * provides the single `app_prefs` store (see `DataModule`), and unit tests construct
 * the repository with a real temp-file store or a throwing one over the same seam. As
 * a constructor-injectable `@Singleton`, no Hilt `@Binds` entry is needed.
 */
@Singleton
open class OnboardingRepository
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /**
         * Emits `false` until [markSeen] has run (fresh install), then `true`. A read error
         * is caught and degraded to empty preferences (→ `false`), the D-16 fail-open path.
         */
        open val onboardingSeen: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[OnboardingPreferences.SEEN_KEY] ?: OnboardingPreferences.DEFAULT_SEEN }

        /** Persists that the onboarding has been seen (D-13 — called when the overlay is dismissed). */
        open suspend fun markSeen() {
            dataStore.edit { it[OnboardingPreferences.SEEN_KEY] = true }
        }
    }
