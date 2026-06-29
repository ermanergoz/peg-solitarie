package com.app.pegsolitaire.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The single app-wide Preferences store (D-16: only persistence pulled into Phase 2). */
private val Context.onboardingDataStore by preferencesDataStore(name = OnboardingPreferences.STORE_NAME)

/** Scoped constants for the onboarding Preferences store (QUAL-03 — no inline magic strings). */
internal object OnboardingPreferences {
    /** DataStore file name; folds into the broader Phase-5 preferences store. */
    const val STORE_NAME = "app_prefs"

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
 * The primary `internal` constructor takes the [DataStore] directly so unit tests can
 * inject a real temp-file store or a throwing one; the Hilt `@Inject` constructor
 * delegates to it over the app's real Preferences file. As a constructor-injectable
 * `@Singleton`, no Hilt module entry is needed.
 */
@Singleton
open class OnboardingRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.onboardingDataStore)

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
