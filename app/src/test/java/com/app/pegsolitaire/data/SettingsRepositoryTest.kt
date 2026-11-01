package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.app.pegsolitaire.testing.InMemoryPreferencesDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Unit coverage of [SettingsRepositoryImpl]'s DataStore-backed flags — `hintsEnabled`,
 * `soundOn` (SET-01) and `hapticsOn` (SET-02) — and their fail-open contract (pure JVM,
 * no Android).
 *
 *  - an [InMemoryPreferencesDataStore] proves each flag defaults to `true` (fresh
 *    install → every helper on) and that the setters round-trip `false` back to `true`;
 *  - a deliberately-throwing store proves each read **fails open** — a corrupt/IO error
 *    degrades to `true` rather than crashing, mirroring [OnboardingRepository] (D-17);
 *  - a store that throws on write proves each setter is **best-effort** (T-05-13) — a
 *    disk-full/IO failure drops the write instead of crashing the Settings screen.
 */
class SettingsRepositoryTest {
    @Test
    fun hintsEnabledDefaultsToTrueOnAFreshStore() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            assertThat(repository.hintsEnabled.first()).isTrue()
        }

    @Test
    fun setHintsEnabledFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

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
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            // D-17: fresh install ships both audio/haptic helpers ON.
            assertThat(repository.soundOn.first()).isTrue()
        }

    @Test
    fun setSoundOnFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

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
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            assertThat(repository.hapticsOn.first()).isTrue()
        }

    @Test
    fun setHapticsOnFalseThenTrueRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

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
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            repository.setSoundOn(false)

            // Turning sound off must not disturb the sibling flags.
            assertThat(repository.soundOn.first()).isFalse()
            assertThat(repository.hapticsOn.first()).isTrue()
            assertThat(repository.hintsEnabled.first()).isTrue()
        }

    @Test
    fun aFailingWriteIsDroppedInsteadOfCrashing() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingWriteDataStore())

            // T-05-13: a disk-full/IO failure while persisting a toggle must not crash.
            repository.setHintsEnabled(false)
            repository.setSoundOn(false)
            repository.setHapticsOn(false)

            // The writes were dropped; the flows still report what actually persisted.
            assertThat(repository.hintsEnabled.first()).isTrue()
            assertThat(repository.soundOn.first()).isTrue()
            assertThat(repository.hapticsOn.first()).isTrue()
        }

    /** CV-04: the colour-blind palette is off on a fresh install — the founder-approved look. */
    @Test
    fun theColorblindPaletteIsOffOnAFreshStore() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            assertThat(repository.colorblindPaletteOn.first()).isFalse()
        }

    /**
     * CV-04: the one flag that does **not** fail open. A corrupt store resets every setting at
     * once; showing the alternative look to every player whose store failed would misstate the
     * default, so a failed read means off (docs/ux/colour-vision.md §3.2).
     */
    @Test
    fun aFailedReadLeavesTheColorblindPaletteOff() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingDataStore())

            assertThat(repository.colorblindPaletteOn.first()).isFalse()
        }

    @Test
    fun setColorblindPaletteOnRoundTrips() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())

            repository.setColorblindPaletteOn(true)
            assertThat(repository.colorblindPaletteOn.first()).isTrue()

            repository.setColorblindPaletteOn(false)
            assertThat(repository.colorblindPaletteOn.first()).isFalse()
        }

    /** CV-AC02: the choice lives under `colorblind_palette_on` in the shared store, beside the other flags. */
    @Test
    fun theColorblindChoiceIsStoredUnderItsOwnKeyAndTouchesNoOtherFlag() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = SettingsRepositoryImpl(store)

            repository.setColorblindPaletteOn(true)

            assertThat(store.data.first()[booleanPreferencesKey("colorblind_palette_on")]).isTrue()
            assertThat(repository.hintsEnabled.first()).isTrue()
            assertThat(repository.soundOn.first()).isTrue()
            assertThat(repository.hapticsOn.first()).isTrue()
        }

    @Test
    fun aFailedWriteDropsTheColorblindChoiceWithoutThrowing() =
        runTest {
            val repository = SettingsRepositoryImpl(throwingWriteDataStore())

            repository.setColorblindPaletteOn(true)

            assertThat(repository.colorblindPaletteOn.first()).isFalse()
        }

    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                error("not used in the read path")
        }

    /** Reads work (and are empty); every write fails like a full disk. */
    private fun throwingWriteDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }
}
