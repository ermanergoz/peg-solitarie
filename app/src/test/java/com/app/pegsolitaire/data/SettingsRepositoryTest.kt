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
 * Unit coverage of [SettingsRepositoryImpl]'s DataStore-backed `hintsEnabled` flag and
 * its fail-open contract (pure JVM, no Android).
 *
 *  - a real Preferences store over a temp file proves the default is `true` (fresh
 *    install → hints on) and that [SettingsRepositoryImpl.setHintsEnabled] round-trips
 *    `false` back to `true`;
 *  - a deliberately-throwing store proves the read **fails open** — a corrupt/IO error
 *    degrades to "hints on" (`true`) rather than crashing, mirroring [OnboardingRepository].
 */
class SettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun hintsEnabledDefaultsToTrueOnAFreshStore() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            assertThat(repository.hintsEnabled.first()).isTrue()
        }

    @Test
    fun setHintsEnabledFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            repository.setHintsEnabled(false)
            assertThat(repository.hintsEnabled.first()).isFalse()

            repository.setHintsEnabled(true)
            assertThat(repository.hintsEnabled.first()).isTrue()
        }

    @Test
    fun hintsEnabledFailsOpenToTrueWhenTheReadErrors() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingDataStore())

            // A corrupt/IO read must degrade to "hints on" (never hide beginner help), never throw.
            assertThat(repository.hintsEnabled.first()).isTrue()
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
        const val TEST_STORE_FILE = "settings_test.preferences_pb"
    }
}
