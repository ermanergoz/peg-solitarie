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
 * Unit coverage of [OnboardingRepository]'s DataStore-backed `onboardingSeen` flag
 * and its D-16 fail-open contract.
 *
 * Two paths are exercised against real and fake [DataStore]s (pure JVM, no Android):
 *  - a real Preferences store over a temp file proves the default is `false` (fresh
 *    install) and that [OnboardingRepository.markSeen] flips it to `true`;
 *  - a deliberately-throwing store proves the read **fails open** — a corrupt/IO
 *    error degrades to "not seen" (`false`) rather than crashing (T-02-08-DS).
 *
 * The repository's `internal` `DataStore`-taking constructor is the test seam; the
 * Hilt `@Inject` constructor delegates to it over the app's real Preferences file.
 */
class OnboardingRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun onboardingSeenDefaultsToFalseOnAFreshStore() =
        runTest {
            val repository = OnboardingRepository(realDataStore())

            assertThat(repository.onboardingSeen.first()).isFalse()
        }

    @Test
    fun markSeenPersistsTrue() =
        runTest {
            val repository = OnboardingRepository(realDataStore())

            repository.markSeen()

            assertThat(repository.onboardingSeen.first()).isTrue()
        }

    @Test
    fun onboardingSeenFailsOpenToFalseWhenTheReadErrors() =
        runTest {
            val repository = OnboardingRepository(throwingDataStore())

            // A corrupt/IO read must degrade to "not seen" (teach again), never throw.
            assertThat(repository.onboardingSeen.first()).isFalse()
        }

    @Test
    fun markSeenFailsOpenWhenTheWriteErrors() =
        runTest {
            // WR-05: the dismiss tap is a fire-and-forget launch with no handler of its own;
            // a full disk must degrade to "teach again next launch", never crash the tap.
            val repository = OnboardingRepository(throwingDataStore())

            repository.markSeen()

            assertThat(repository.onboardingSeen.first()).isFalse()
        }

    private fun realDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
        )

    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }

    private companion object {
        const val TEST_STORE_FILE = "onboarding_test.preferences_pb"
    }
}
