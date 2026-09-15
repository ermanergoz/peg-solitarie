package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The persisted "last Play Games player id this device's progress was merged with" —
 * D-12's account-switch gate, and the second of the two pieces of device-local state
 * cloud save introduces.
 *
 * One nullable string, in the shared `app_prefs` store, keyed by
 * [PlayGamesPreferences.MERGED_PLAYER_ID_KEY]. The value is
 * [com.app.pegsolitaire.platform.games.GamesSignInState.SignedIn.playerId].
 *
 * ## Where it is read, and why the order matters
 *
 * The stored id is compared against the signed-in id in `ProgressSubmitter`'s pass
 * **before any pull or push runs**, never after. If the pull ran first and the ids then
 * turned out to disagree, the new account's progress would already have been merged into
 * the old account's local store — unrecoverable, and precisely the data loss the D-12
 * dialog exists to prevent. A mismatch therefore returns from the pass having performed
 * zero store calls, and raises the one-time Home dialog (D-15) instead.
 *
 * ## No `clear()`, deliberately
 *
 * D-12 never un-remembers an id. The only transition is an overwrite: after the
 * profile-swap dialog is answered — either way, "Keep" or "Replace" (D-13/D-14) — the
 * stored id becomes the signed-in one (D-15). A `clear()` would introduce a third state
 * that means "merge silently with whoever signs in next", which is the destructive path
 * arrived at by forgetting rather than by a player's answer.
 *
 * ## Fail-open, in the direction that cannot wipe a device
 *
 * [mergedPlayerId] is `null` when the key is missing (fresh install, first-ever sign-in)
 * **and** when the store cannot be read at all, and `null` means "merge silently per
 * D-02". It never means "show the dialog". That asymmetry is the whole mitigation for
 * T-13-04: the swap dialog owns the only destructive path in the app, so a damaged
 * preferences file must not be able to put a player in front of it. See
 * [PlayGamesPreferences.MERGED_PLAYER_ID_KEY] for why there is no default constant.
 *
 * The id is **never logged** (T-11-04) — it identifies the player, and this app-private
 * store is the only place it is written.
 *
 * The `@Inject internal` constructor takes the shared [DataStore] directly, so Hilt
 * resolves it with no `@Binds`/`@Provides` entry — a `@Singleton` with an `@Inject`
 * constructor is constructible on its own. `open`, and both members `open`, so the
 * submitter and Home tests can subclass it over a `MutableStateFlow` rather than standing
 * up a real DataStore on the JVM.
 */
@Singleton
open class CloudProfileStore
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /**
         * The player id this device's progress was last merged with, or `null` if it has
         * never been merged with anyone — including when the store cannot be read (D-12,
         * T-13-04). `null` is the *silent merge*, never the profile-swap dialog.
         */
        open val mergedPlayerId: Flow<String?> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[PlayGamesPreferences.MERGED_PLAYER_ID_KEY] }

        /**
         * Records the player id this device's progress has now been merged with, degrading a
         * failed write to "not persisted" rather than throwing (the
         * [SubmissionSyncStore.setPending] shape).
         *
         * Callers are the first silent merge of a process and the two answers to the swap
         * dialog, all of them fire-and-forget launches with no handler of their own. A
         * dropped write fails open in the same direction as a dropped read: the device
         * simply does not remember this merge, and the next pass merges silently again —
         * lossless, because the merge is a monotonic union (CLOUD-02).
         *
         * Cancellation is rethrown before classification: it is structured concurrency's
         * signal, never a write failure.
         */
        open suspend fun setMergedPlayerId(playerId: String) {
            runCatching {
                dataStore.edit { it[PlayGamesPreferences.MERGED_PLAYER_ID_KEY] = playerId }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
