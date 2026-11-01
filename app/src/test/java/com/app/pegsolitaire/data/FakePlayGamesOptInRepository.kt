package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A settable in-memory [PlayGamesOptInRepository] (D-17): overrides the two DataStore-backed
 * flows so the tri-state opt-in answer is driven deterministically, and counts each writer so
 * "declining never opts in" and "signing in persists the opt-in first" are observable.
 *
 * It sits here, beside the repository it fakes, rather than beside one consumer — for the same
 * reason [com.app.pegsolitaire.platform.games.FakeGamesService] sits in `platform.games`: two
 * unrelated packages need it. `ui.home` drives the one-time opt-in prompt against it
 * (D-15/D-18) and `ui.settings` drives the manual sign-in row (D-10/D-19). A file-private copy
 * in each would be two implementations of the same three-state contract free to drift apart —
 * and the D-19 claim under test is precisely that both entry points write the *same* pair of
 * flags. A shared double makes that shared-ness structural rather than coincidental.
 *
 * The base store is a no-op that never emits, mirroring the `FakeOnboardingRepository` idiom:
 * every read is overridden here, so the real DataStore is never reached.
 *
 * @param initiallyAnswered the starting `promptAnswered` flag — `true` retires the prompt.
 * @param initiallyOptedIn the starting `optedIn` flag — the launch bootstrap's D-01 gate.
 */
class FakePlayGamesOptInRepository(
    initiallyAnswered: Boolean = false,
    initiallyOptedIn: Boolean = false,
) : PlayGamesOptInRepository(NoopPreferencesDataStore) {
    val answered = MutableStateFlow(initiallyAnswered)
    val optedInState = MutableStateFlow(initiallyOptedIn)

    var setNotNowCount = 0
        private set

    var setOptedInCount = 0
        private set

    override val promptAnswered: Flow<Boolean> get() = answered

    override val optedIn: Flow<Boolean> get() = optedInState

    override suspend fun setNotNow() {
        setNotNowCount++
        // Both flags move together, exactly as the real atomic edit does.
        answered.value = true
        optedInState.value = false
    }

    override suspend fun setOptedIn() {
        setOptedInCount++
        answered.value = true
        optedInState.value = true
    }
}

/** A never-emitting [DataStore] so the base [PlayGamesOptInRepository] constructor is safe here. */
private object NoopPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        error("not used by the fake")
}
