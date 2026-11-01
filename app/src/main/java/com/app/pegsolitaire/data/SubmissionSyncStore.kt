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
 * The persisted "a Play Games submission is outstanding" marker — the whole of D-10's
 * dirty-flag half, and the only state [ProgressSubmitter] keeps between processes.
 *
 * One boolean, in the shared `app_prefs` store, keyed by
 * [PlayGamesPreferences.SUBMISSIONS_PENDING_KEY]. Both halves fail open to
 * [PlayGamesPreferences.DEFAULT_SUBMISSIONS_PENDING] — see that key's KDoc for why the
 * direction is `false` even though `true` would be more recoverable.
 *
 * ## Why this is its own class and not two more members on [PlayGamesOptInRepository]
 *
 * The obvious home for this flag is beside the opt-in booleans, and the KEY *is* there —
 * every `play_games_*` name is still declared in one scoped constants object. The
 * ACCESSOR is not, because [PlayGamesOptInRepository]'s class KDoc states a contract this
 * flag would falsify. That class is documented as "the single source of truth for the
 * player's opt-in answer"; its two booleans "must never observably disagree, so they move
 * together or not at all"; and its fail-open direction is justified by the claim that it
 * "can never fabricate an opt-in the player did not give". This flag is a third boolean
 * with **no** atomicity relationship to that pair and a **different** fail-open argument,
 * and it is written from the submission path rather than from a launch-time tap. Folding
 * it in would make three true paragraphs false at once.
 *
 * ## What the flag deliberately does not store
 *
 * Only *that* something is pending — never *what*. CONTEXT's Claude's-Discretion bullet
 * offers both, and the boolean is not merely the simpler option: every pass recomputes
 * the entire submission set from SQL from scratch, so a stored record of what was pending
 * could only ever go stale against the database it would then be checked against. A
 * second, weaker source of truth for a set the first source already recomputes is not an
 * optimisation, it is a disagreement waiting to happen.
 *
 * The `@Inject internal` constructor takes the shared [DataStore] directly, so Hilt
 * resolves it with no `@Binds`/`@Provides` entry — the [ChallengeProgress] /
 * [PlayGamesOptInRepository] precedent. `open`, and both members `open`, for the reason
 * [PlayGamesOptInRepository] is: `ProgressSubmitterTest` subclasses it over a
 * `MutableStateFlow` rather than standing up a real DataStore on the JVM.
 */
@Singleton
open class SubmissionSyncStore
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /**
         * Whether a submission is outstanding. `false` on a fresh install and on any read
         * the store cannot complete (D-10 — a clean launch makes no calls).
         */
        open val pending: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[PlayGamesPreferences.SUBMISSIONS_PENDING_KEY]
                        ?: PlayGamesPreferences.DEFAULT_SUBMISSIONS_PENDING
                }

        /**
         * Records whether a submission is outstanding, degrading a failed write to "not
         * persisted" rather than throwing (the [PlayGamesOptInRepository] `editFailOpen`
         * shape).
         *
         * `DataStore.edit` throws `IOException` on a full disk or a corrupt file, and every
         * caller here is inside [ProgressSubmitter]'s application-scoped, fire-and-forget
         * pass — a coroutine with no handler of its own, running long after the player left
         * the Results screen. An unguarded write would therefore take the process down at a
         * moment the player cannot connect to anything they did. The flag simply does not
         * survive the launch instead, and the next solve re-sets it.
         *
         * Cancellation is rethrown before classification: it is structured concurrency's
         * signal, never a write failure.
         */
        open suspend fun setPending(pending: Boolean) {
            runCatching {
                dataStore.edit { it[PlayGamesPreferences.SUBMISSIONS_PENDING_KEY] = pending }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
