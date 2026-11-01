package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
 * The single source of truth for the player's Play Games opt-in answer (PGS-01, D-17) —
 * and therefore for D-01's launch-time branch, which decides whether the Play Games SDK
 * is touched at all on a given launch.
 *
 * Backed by two DataStore-Preferences booleans in the shared `app_prefs` store
 * ([PlayGamesPreferences]), which together encode three states:
 *
 * | `promptAnswered` | `optedIn` | Meaning |
 * |------------------|-----------|---------|
 * | `false` | `false` | Never asked — Home may show the one-time opt-in prompt (D-15). |
 * | `true` | `false` | "Not now" — the prompt is **permanently** retired (D-17). |
 * | `true` | `true` | Opted in — later launches bootstrap the SDK silently (D-01). |
 *
 * Two booleans rather than one tri-state enum because each answers a different question
 * for a different caller: Home reads [promptAnswered] to decide whether to ask, and the
 * launch bootstrap reads [optedIn] to decide whether to touch the SDK. Neither caller
 * needs the other's answer.
 *
 * ## Why both writes are single, atomic edits
 *
 * [setNotNow] and [setOptedIn] each write **both** keys inside one `dataStore.edit { }`
 * block, never two sequential edits. Split writes would publish an intermediate state to
 * every active collector — most damagingly `promptAnswered = true` with `optedIn` not yet
 * written, which is indistinguishable from a deliberate "Not now" and could bootstrap the
 * wrong branch on a launch racing the write. The two flags must never observably
 * disagree, so they move together or not at all.
 *
 * ## Fail-open, in the safe direction
 *
 * Both reads **fail open** to `false`: a missing key (fresh install) or a corrupt/IO read
 * degrade to "never asked, not opted in" (the D-16 pattern [OnboardingRepository] already
 * uses). That direction is deliberate — a damaged store leaves the SDK untouched and at
 * worst re-asks a dismissible question. It can never fabricate an opt-in the player did
 * not give, nor crash a launch.
 *
 * Both **writes** fail open as well (WR-05, see [editFailOpen]): a write the store cannot
 * complete is degraded to "not persisted" rather than thrown at a tap handler that has no
 * handler of its own. The no-crash contract above is only true if it covers both halves.
 *
 * The `@Inject internal` constructor takes the shared [DataStore] directly: Hilt provides
 * the single `app_prefs` store (see `DataModule`), so as a constructor-injectable
 * `@Singleton` this needs no `@Binds`/`@Provides` entry of its own. `open` (like
 * [OnboardingRepository]) so ViewModel tests can subclass it with a `MutableStateFlow`
 * stand-in rather than a real store.
 */
@Singleton
open class PlayGamesOptInRepository
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /**
         * Whether the one-time opt-in prompt has been answered — either way. `true` after
         * [setNotNow] or [setOptedIn]; the prompt is never shown again once it is `true`
         * (D-17), leaving the Settings row (D-08/D-19) as the only later way in.
         */
        open val promptAnswered: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] ?: PlayGamesPreferences.DEFAULT_PROMPT_ANSWERED }

        /**
         * Whether the player opted in to Play Games — the gate
         * [com.app.pegsolitaire.platform.games.GamesService.bootstrapIfOptedIn] reads at
         * launch. While `false`, no `PlayGamesSdk.initialize` and no `isAuthenticated`
         * check may run (D-01).
         */
        open val optedIn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[PlayGamesPreferences.OPTED_IN_KEY] ?: PlayGamesPreferences.DEFAULT_OPTED_IN }

        /**
         * Records a "Not now" — the prompt's negative action, a tap outside, or back
         * (D-16). Answered, but not opted in: the prompt is retired permanently and the
         * SDK stays untouched on every later launch (D-17).
         */
        open suspend fun setNotNow() {
            editFailOpen {
                it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
                it[PlayGamesPreferences.OPTED_IN_KEY] = false
            }
        }

        /**
         * Records an opt-in, from either of its two entry points: the prompt's "Sign in"
         * action (D-18) or the Settings sign-in row (D-19). Both set `promptAnswered` too,
         * so a player who signs in from Settings is never asked again — and a player who
         * previously declined is fully opted back in, not left on the old "Not now".
         */
        open suspend fun setOptedIn() {
            editFailOpen {
                it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
                it[PlayGamesPreferences.OPTED_IN_KEY] = true
            }
        }

        /**
         * One atomic edit whose failure degrades to "not persisted" (WR-05).
         *
         * `DataStore.edit` throws `IOException` when the write fails — a full disk, a corrupt
         * file, an I/O error — and every caller here is a fire-and-forget
         * `viewModelScope.launch` from a tap ("Not now", "Sign in", the Settings row) with no
         * handler of its own, so an unguarded write would take the process down with it. On
         * the accept path it would do so with the prompt already hidden and *before*
         * `signIn()`, and the next launch would re-ask and crash on the same tap while the
         * disk condition persists. Applied here rather than at the call sites so every caller
         * inherits the policy: the tap still does what the player can see, the flags simply
         * do not survive the launch, and the worst case is the harmless question again.
         *
         * Cancellation is rethrown, not swallowed: it is the caller's structured-concurrency
         * signal, never a write failure.
         */
        private suspend fun editFailOpen(transform: suspend (MutablePreferences) -> Unit) {
            runCatching { dataStore.edit(transform) }
                .onFailure { if (it is CancellationException) throw it }
        }
    }
