package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.testing.InMemoryPreferencesDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
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
 * Exercised against working and hostile [DataStore]s (pure JVM, no Android), mirroring
 * [OnboardingRepositoryTest]: an [InMemoryPreferencesDataStore] proves the
 * defaults and both writes, and a deliberately-throwing store proves the reads **fail
 * open** to `false`. Failing open is the safe direction here — a damaged store degrades
 * to "never asked, not opted in", which leaves the SDK untouched and at worst re-asks a
 * harmless question. It can never fabricate an opt-in the player did not give.
 */
class PlayGamesOptInRepositoryTest {
    @Test
    fun bothFlagsDefaultToFalseOnAFreshStore() =
        runTest {
            val repository = PlayGamesOptInRepository(InMemoryPreferencesDataStore())

            assertThat(repository.promptAnswered.first()).isFalse()
            assertThat(repository.optedIn.first()).isFalse()
        }

    @Test
    fun setNotNowRetiresThePromptWithoutOptingIn() =
        runTest {
            val repository = PlayGamesOptInRepository(InMemoryPreferencesDataStore())

            repository.setNotNow()

            // D-17: answered, so the prompt never appears again; not opted in, so D-01's
            // launch branch leaves the Play Games SDK untouched.
            assertThat(repository.promptAnswered.first()).isTrue()
            assertThat(repository.optedIn.first()).isFalse()
        }

    @Test
    fun setOptedInPersistsBothFlags() =
        runTest {
            val repository = PlayGamesOptInRepository(InMemoryPreferencesDataStore())

            repository.setOptedIn()

            assertThat(repository.promptAnswered.first()).isTrue()
            assertThat(repository.optedIn.first()).isTrue()
        }

    @Test
    fun setOptedInAfterADeclineOptsThePlayerBackIn() =
        runTest {
            // D-19: the Settings row is the declined player's only way back in, and taking
            // it must fully opt them in — not leave them stuck on the earlier "Not now".
            val repository = PlayGamesOptInRepository(InMemoryPreferencesDataStore())
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

    @Test
    fun bothWritesFailOpenWhenTheStoreCannotBeWritten() =
        runTest {
            // WR-05: a full disk or a corrupt file must never crash "Not now", "Sign in" or
            // the Settings row — each is a fire-and-forget tap with no handler of its own.
            // The flags simply do not survive the launch, which at worst re-asks the question.
            val repository = PlayGamesOptInRepository(throwingDataStore())

            repository.setNotNow()
            repository.setOptedIn()

            // Nothing was persisted, and — the claim under test — nothing was thrown.
            assertThat(repository.promptAnswered.first()).isFalse()
            assertThat(repository.optedIn.first()).isFalse()
        }

    @Test
    fun aCancelledWriteStillPropagatesAsCancellation() =
        runTest {
            // The fail-open guard must not swallow the caller's own cancellation signal.
            val repository = PlayGamesOptInRepository(cancellingDataStore())

            val thrown = runCatching { repository.setOptedIn() }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    /** A store that fails every read AND every write with the I/O error DataStore reports. */
    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }

    /** A store whose write is cancelled underneath the caller. */
    private fun cancellingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw CancellationException("cancelled underneath the write")
        }
}
