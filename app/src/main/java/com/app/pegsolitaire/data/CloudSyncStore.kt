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
 * The persisted "a cloud-save push is outstanding" marker — D-01's retry half, and one of
 * the only two pieces of device-local state cloud save introduces.
 *
 * One boolean, in the shared `app_prefs` store, keyed by
 * [PlayGamesPreferences.CLOUD_PUSH_PENDING_KEY]. Both halves fail open to
 * [PlayGamesPreferences.DEFAULT_CLOUD_PUSH_PENDING] — see that key's KDoc for why the
 * direction is `false` even though `true` would be more recoverable.
 *
 * ## Why this is a sibling of [SubmissionSyncStore] and not a fifth key on it
 *
 * The obvious home for this flag is beside the submissions marker, and the KEY *is* there
 * — every `play_games_*` name is still declared in one scoped constants object. The
 * ACCESSOR is not, for the reason [SubmissionSyncStore]'s own KDoc gives for existing
 * separately: a flag with **no atomicity relationship** to the ones already in a class
 * deserves its own accessor.
 *
 * The cloud marker has none with `SUBMISSIONS_PENDING`. A single submitter pass talks to
 * two different backends — the leaderboard/achievement submissions and the Saved Games
 * snapshot — and they fail independently: the leaderboards can all land while the
 * snapshot write is rejected, and vice versa. Those two outcomes must be independently
 * recoverable, so they cannot share one boolean. Clearing a shared flag because the
 * leaderboards succeeded would silently lose the snapshot retry; retaining it because the
 * snapshot failed would re-issue submissions that already landed. Keys together,
 * accessors apart.
 *
 * ## What the flag deliberately does not store
 *
 * Only *that* a push is outstanding — never *what* to push. Every push recomputes the
 * whole progress payload from SQL from scratch (the merge is a monotonic union,
 * CLOUD-02), so a stored record of what was pending could only ever go stale against the
 * database it would then be checked against.
 *
 * The `@Inject internal` constructor takes the shared [DataStore] directly, so Hilt
 * resolves it with no `@Binds`/`@Provides` entry — a `@Singleton` with an `@Inject`
 * constructor is constructible on its own (the [PlayGamesOptInRepository] /
 * [SubmissionSyncStore] precedent). `open`, and both members `open`, for the same reason
 * they are there: `ProgressSubmitter`'s cloud tests subclass this over a
 * `MutableStateFlow` rather than standing up a real DataStore on the JVM.
 */
@Singleton
open class CloudSyncStore
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /**
         * Whether a cloud-save push is outstanding. `false` on a fresh install and on any
         * read the store cannot complete (D-01 inherits D-10's "a clean launch makes no
         * calls").
         */
        open val pushPending: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[PlayGamesPreferences.CLOUD_PUSH_PENDING_KEY]
                        ?: PlayGamesPreferences.DEFAULT_CLOUD_PUSH_PENDING
                }

        /**
         * Records whether a cloud push is outstanding, degrading a failed write to "not
         * persisted" rather than throwing (the [SubmissionSyncStore.setPending] shape).
         *
         * `DataStore.edit` throws `IOException` on a full disk or a corrupt file, and every
         * caller here is inside `ProgressSubmitter`'s application-scoped, fire-and-forget
         * pass — a coroutine with no handler of its own, running long after the player left
         * the Results screen. An unguarded write would take the process down at a moment the
         * player cannot connect to anything they did (T-13-06). The marker simply does not
         * survive the launch instead, and the next recorded solve re-sets it.
         *
         * Cancellation is rethrown before classification: it is structured concurrency's
         * signal, never a write failure.
         */
        open suspend fun setPushPending(pending: Boolean) {
            runCatching {
                dataStore.edit { it[PlayGamesPreferences.CLOUD_PUSH_PENDING_KEY] = pending }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
