package com.app.pegsolitaire.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The SINGLE app-wide `DataStore<Preferences>` delegate.
 *
 * DataStore forbids more than one active store for the same file: two
 * `preferencesDataStore(name = "app_prefs")` delegates on the same [Context] crash at
 * runtime ("There are multiple DataStores active for the same file"). Every
 * preferences-backed repository ([OnboardingRepository], [SettingsRepositoryImpl], …)
 * therefore shares this one delegate — created once here and Hilt-provided as a
 * `@Singleton` so no repository re-creates it.
 */
internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = AppPreferences.STORE_NAME)

/** Scoped constants for the shared app Preferences store (QUAL-03 — no inline magic strings). */
internal object AppPreferences {
    /** The single DataStore-Preferences file name shared by every preferences repository. */
    const val STORE_NAME = "app_prefs"
}
