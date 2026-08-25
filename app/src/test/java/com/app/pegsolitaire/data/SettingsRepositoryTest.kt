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
 * Unit coverage of [SettingsRepositoryImpl]'s DataStore-backed flags — `hintsEnabled`,
 * `soundOn` (SET-01) and `hapticsOn` (SET-02) — and their fail-open contract (pure JVM,
 * no Android).
 *
 *  - a real Preferences store over a temp file proves each flag defaults to `true` (fresh
 *    install → every helper on) and that the setters round-trip `false` back to `true`;
 *  - a deliberately-throwing store proves each read **fails open** — a corrupt/IO error
 *    degrades to `true` rather than crashing, mirroring [OnboardingRepository] (D-17).
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

    @Test
    fun soundOnDefaultsToTrueOnAFreshStore() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            // D-17: fresh install ships both audio/haptic helpers ON.
            assertThat(repository.soundOn.first()).isTrue()
        }

    @Test
    fun setSoundOnFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            repository.setSoundOn(false)
            assertThat(repository.soundOn.first()).isFalse()

            repository.setSoundOn(true)
            assertThat(repository.soundOn.first()).isTrue()
        }

    @Test
    fun soundOnFailsOpenToTrueWhenTheReadErrors() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingDataStore())

            // A corrupt/IO read must degrade to the ON default, never throw.
            assertThat(repository.soundOn.first()).isTrue()
        }

    @Test
    fun hapticsOnDefaultsToTrueOnAFreshStore() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            assertThat(repository.hapticsOn.first()).isTrue()
        }

    @Test
    fun setHapticsOnFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            repository.setHapticsOn(false)
            assertThat(repository.hapticsOn.first()).isFalse()

            repository.setHapticsOn(true)
            assertThat(repository.hapticsOn.first()).isTrue()
        }

    @Test
    fun hapticsOnFailsOpenToTrueWhenTheReadErrors() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingDataStore())

            assertThat(repository.hapticsOn.first()).isTrue()
        }

    @Test
    fun theThreeFlagsArePersistedIndependently() =
        runTest {
            val repository = SettingsRepositoryImpl(realDataStore())

            repository.setSoundOn(false)

            // Turning sound off must not disturb the sibling flags.
            assertThat(repository.soundOn.first()).isFalse()
            assertThat(repository.hapticsOn.first()).isTrue()
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
