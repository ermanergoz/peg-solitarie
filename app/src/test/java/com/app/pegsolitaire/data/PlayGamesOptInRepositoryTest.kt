package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/**
 * Unit coverage of [PlayGamesOptInRepository]'s two D-17 flags and their fail-open
 * contract — the single source of truth for D-01's launch-time branch.
 *
 * The two flags are deliberately *not* one tri-state enum, and the difference matters:
 *  - `promptAnswered = false` → the player has never been asked; Home may show the
 *    one-time opt-in prompt (D-15);
 *  - `promptAnswered = true, optedIn = false` → "Not now"; the prompt is **permanently**
 *    retired and the Settings row is the only way back in (D-17);
 *  - `promptAnswered = true, optedIn = true` → the SDK may be bootstrapped silently on
 *    every later launch (D-01), whether the player opted in from the prompt (D-18) or
 *    from the Settings row (D-19).
 *
 * Exercised against real and fake [DataStore]s (pure JVM, no Android), mirroring
 * [OnboardingRepositoryTest]: a real Preferences store over a temp file proves the
 * defaults and both writes, and a deliberately-throwing store proves the reads **fail
 * open** to `false`. Failing open is the safe direction here — a damaged store degrades
 * to "never asked, not opted in", which leaves the SDK untouched and at worst re-asks a
 * harmless question. It can never fabricate an opt-in the player did not give.
 */
class PlayGamesOptInRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun bothFlagsDefaultToFalseOnAFreshStore() =
        runTest {
            val repository = PlayGamesOptInRepository(realDataStore())

            assertThat(repository.promptAnswered.first()).isFalse()
            assertThat(repository.optedIn.first()).isFalse()
        }

    @Test
    fun setNotNowRetiresThePromptWithoutOptingIn() =
        runTest {
            val repository = PlayGamesOptInRepository(realDataStore())

            repository.setNotNow()

            // D-17: answered, so the prompt never appears again; not opted in, so D-01's
            // launch branch leaves the Play Games SDK untouched.
            assertThat(repository.promptAnswered.first()).isTrue()
            assertThat(repository.optedIn.first()).isFalse()
        }

    @Test
    fun setOptedInPersistsBothFlags() =
        runTest {
            val repository = PlayGamesOptInRepository(realDataStore())

            repository.setOptedIn()

            assertThat(repository.promptAnswered.first()).isTrue()
            assertThat(repository.optedIn.first()).isTrue()
        }

    @Test
    fun setOptedInAfterADeclineOptsThePlayerBackIn() =
        runTest {
            // D-19: the Settings row is the declined player's only way back in, and taking
            // it must fully opt them in — not leave them stuck on the earlier "Not now".
            val repository = PlayGamesOptInRepository(realDataStore())
            repository.setNotNow()

            repository.setOptedIn()

            assertThat(repository.promptAnswered.first()).isTrue()
            assertThat(repository.optedIn.first()).isTrue()
        }

    @Test
    fun bothFlagsFailOpenToFalseWhenTheReadErrors() =
        runTest {
            val repository = PlayGamesOptInRepository(throwingDataStore())

            // A corrupt/IO read must degrade to "never asked, not opted in", never throw —
            // and never to an opt-in the player did not give.
            assertThat(repository.promptAnswered.first()).isFalse()
            assertThat(repository.optedIn.first()).isFalse()
        }

    private fun realDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
        )

    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                error("not used in the read path")
        }

    private companion object {
        const val TEST_STORE_FILE = "play_games_opt_in_test.preferences_pb"
    }
}
