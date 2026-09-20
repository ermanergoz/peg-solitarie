package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.app.pegsolitaire.platform.games.save.CloudPullProbe
import com.app.pegsolitaire.platform.games.save.CloudSaveOutcome
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
 * ## The diagnostic keys beside it
 *
 * [recordPushOutcome] also writes the stage and the status code of the last attempted push,
 * and [recordPullProbe] writes what the last pull's opened snapshot said about itself. All of
 * them are **write-only from the app's point of view**: nothing here exposes them as a
 * `Flow`, deliberately, so no behaviour and no UI can come to depend on them (CLOUD-05,
 * D-03). They are for a developer reading a debuggable device.
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

        /**
         * Records an **attempted** push: the marker moves to "not committed", and the stage
         * and status code that explain it are written in the **same edit**.
         *
         * One edit and not three, because the three keys describe one event. Written
         * separately, a process death between them would leave a marker from this push
         * beside a stage from the last one, which is worse than no diagnostic at all: it
         * would send whoever reads it after the wrong refusal. The status key is *removed*
         * when the outcome carries no code, for the reason given on
         * [PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY].
         *
         * The marker semantics are exactly [setPushPending]'s — pending means not committed —
         * and so is the failure handling: a failed write is dropped, cancellation is
         * rethrown. [setPushPending] remains for the callers that set the marker without
         * having attempted a push, and so have no outcome to record.
         */
        open suspend fun recordPushOutcome(outcome: CloudSaveOutcome) {
            runCatching {
                dataStore.edit { prefs ->
                    prefs[PlayGamesPreferences.CLOUD_PUSH_PENDING_KEY] = !outcome.committed
                    prefs[PlayGamesPreferences.CLOUD_LAST_PUSH_STAGE_KEY] = outcome.stage.name
                    val statusCode = outcome.statusCode
                    if (statusCode == null) {
                        prefs.remove(PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY)
                    } else {
                        prefs[PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY] = statusCode
                    }
                }
            }.onFailure { if (it is CancellationException) throw it }
        }

        /**
         * Records what one **pull** observed, in a single edit; `null` is a pull that never
         * opened a snapshot.
         *
         * One edit for the reason [recordPushOutcome] gives: the keys describe one event, and a
         * process death between separate writes would leave half of this pull beside half of
         * the last one. A `null` [probe] writes `opened = false` and **removes** the other
         * three, so values from an earlier pull can never be read as describing a failed one.
         *
         * The push marker and the push diagnostic are not touched: a pull is not a push, and
         * nothing about a probe — not even an upload still outstanding — changes what is owed.
         * Failure handling is [setPushPending]'s: a failed write is dropped, cancellation is
         * rethrown.
         */
        open suspend fun recordPullProbe(probe: CloudPullProbe?) {
            runCatching {
                dataStore.edit { prefs ->
                    prefs[PlayGamesPreferences.CLOUD_LAST_PULL_OPENED_KEY] = probe != null
                    if (probe == null) {
                        prefs.remove(PlayGamesPreferences.CLOUD_LAST_PULL_CHANGE_PENDING_KEY)
                        prefs.remove(PlayGamesPreferences.CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY)
                        prefs.remove(PlayGamesPreferences.CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY)
                    } else {
                        prefs[PlayGamesPreferences.CLOUD_LAST_PULL_CHANGE_PENDING_KEY] = probe.changePending
                        prefs[PlayGamesPreferences.CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY] = probe.conflictsResolved
                        prefs[PlayGamesPreferences.CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY] = probe.lastModifiedAt
                    }
                }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
