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
 * Behaviour spec for the DataStore-backed [ChallengeProgress] holder (PERS-02 / CHAL-05).
 *
 * The current Challenge level now lives in the shared `app_prefs` Preferences store, so a
 * level survives process death. The public surface is unchanged (`currentLevel` is still a
 * `StateFlow<Int>` with a synchronously readable `.value`, via `stateIn`), which is what
 * "restart" is simulated with here: a *second* [ChallengeProgress] constructed over the
 * same store must observe the level the first one wrote.
 *
 * Runs on the JVM against a real temp-file Preferences store (the same style as
 * [SettingsRepositoryTest]); a deliberately-throwing store proves the fail-open default.
 */
class ChallengeProgressTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun challengeProgressStartsAtLevelOne() =
        runTest {
            val progress = ChallengeProgress(realDataStore(), backgroundScope)

            // D-04: a fresh install starts Challenge at level 1, readable synchronously.
            assertThat(progress.currentLevel.value).isEqualTo(FIRST_LEVEL)
            assertThat(progress.currentLevel.first()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun advancePastPersistsSoAFreshInstanceResumesAtTheNextLevel() =
        runTest {
            val store = realDataStore()
            val progress = ChallengeProgress(store, backgroundScope)

            progress.advancePast(FIRST_LEVEL)
            assertThat(progress.currentLevel.first { it == SECOND_LEVEL }).isEqualTo(SECOND_LEVEL)

            // "Restart": a brand-new holder over the same store must see the persisted level.
            val restarted = ChallengeProgress(store, backgroundScope)
            assertThat(restarted.currentLevel.first { it == SECOND_LEVEL }).isEqualTo(SECOND_LEVEL)
        }

    @Test
    fun advancingPastConsecutiveSolvedLevelsReachesLevelThree() =
        runTest {
            val progress = ChallengeProgress(realDataStore(), backgroundScope)

            progress.advancePast(FIRST_LEVEL)
            assertThat(progress.currentLevel.first { it == SECOND_LEVEL }).isEqualTo(SECOND_LEVEL)

            progress.advancePast(SECOND_LEVEL)
            assertThat(progress.currentLevel.first { it == THIRD_LEVEL }).isEqualTo(THIRD_LEVEL)
        }

    @Test
    fun advancingPastTheSameSolvedLevelTwiceAdvancesOnlyOnce() =
        runTest {
            // CR-02: re-solving the same board (back from Results → undo → re-jump) fires
            // a second advance for the SAME level — the ladder must not skip a level.
            val progress = ChallengeProgress(realDataStore(), backgroundScope)

            progress.advancePast(FIRST_LEVEL)
            progress.advancePast(FIRST_LEVEL)

            assertThat(progress.currentLevel.first { it == SECOND_LEVEL }).isEqualTo(SECOND_LEVEL)
        }

    @Test
    fun advancingPastAnOlderLevelNeverMovesTheLadderBackwards() =
        runTest {
            // Solving a level below the ladder (a stale resumed board) keeps the maximum.
            val progress = ChallengeProgress(realDataStore(), backgroundScope)
            progress.advancePast(FIRST_LEVEL)
            progress.advancePast(SECOND_LEVEL)
            progress.currentLevel.first { it == THIRD_LEVEL }

            progress.advancePast(FIRST_LEVEL)

            assertThat(progress.currentLevel.first { it == THIRD_LEVEL }).isEqualTo(THIRD_LEVEL)
        }

    @Test
    fun resetPersistsAReturnToLevelOne() =
        runTest {
            val store = realDataStore()
            val progress = ChallengeProgress(store, backgroundScope)
            progress.advancePast(FIRST_LEVEL)
            progress.advancePast(SECOND_LEVEL)
            progress.currentLevel.first { it == THIRD_LEVEL }

            progress.reset()
            assertThat(progress.currentLevel.first { it == FIRST_LEVEL }).isEqualTo(FIRST_LEVEL)

            val restarted = ChallengeProgress(store, backgroundScope)
            assertThat(restarted.currentLevel.first()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun currentLevelFailsOpenToLevelOneWhenTheReadErrors() =
        runTest {
            val progress = ChallengeProgress(throwingDataStore(), backgroundScope)

            // A corrupt/IO read must degrade to level 1, never throw.
            assertThat(progress.currentLevel.first()).isEqualTo(FIRST_LEVEL)
            assertThat(progress.currentLevel.value).isEqualTo(FIRST_LEVEL)
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
        const val TEST_STORE_FILE = "challenge_progress_test.preferences_pb"
        const val FIRST_LEVEL = 1
        const val SECOND_LEVEL = 2
        const val THIRD_LEVEL = 3
    }
}
