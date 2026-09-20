package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.app.pegsolitaire.domain.progress.ProgressMergeLimits
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
 * Phase 13 added the **awaitable** half of the same ladder: `mergeLadder` / `settledLevel`
 * / `resetAwaiting`. Those are specified here rather than in a cloud test file because
 * they are the same one `max` write the fire-and-forget members always performed — the
 * only difference is that they return once the write has committed, which is what lets a
 * cloud push read back the ladder a cloud pull just imported (D-08).
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

    @Test
    fun advancePastDropsTheWriteInsteadOfCrashingWhenTheStoreThrows() =
        runTest {
            val progress = ChallengeProgress(throwingWriteDataStore(), backgroundScope)

            // T-05-13: the write runs detached on the app-lifetime scope, where an
            // uncaught IO failure would crash the whole app — it must be dropped.
            progress.advancePast(FIRST_LEVEL)
            advanceUntilIdle()

            assertThat(progress.currentLevel.value).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun resetDropsTheWriteInsteadOfCrashingWhenTheStoreThrows() =
        runTest {
            val progress = ChallengeProgress(throwingWriteDataStore(), backgroundScope)

            progress.reset()
            advanceUntilIdle()

            assertThat(progress.currentLevel.value).isEqualTo(FIRST_LEVEL)
        }

    // ---- The awaitable ladder merge the cloud pull needs (CLOUD-01 / D-08) -------

    @Test
    fun mergeLadderSettlesAHigherLevelAndTheFlowCatchesUp() =
        runTest {
            val progress = ChallengeProgress(realDataStore(), backgroundScope)
            progress.mergeLadder(TENTH_LEVEL)

            progress.mergeLadder(FIFTIETH_LEVEL)

            assertThat(progress.settledLevel()).isEqualTo(FIFTIETH_LEVEL)
            assertThat(progress.currentLevel.first { it == FIFTIETH_LEVEL }).isEqualTo(FIFTIETH_LEVEL)
        }

    @Test
    fun mergeLadderNeverRegressesTheStoredLevel() =
        runTest {
            // CLOUD-02: an arriving cloud ladder BELOW this device's is a no-op, the same
            // way re-solving an older board is. The merge is `max`, computed in the write.
            val progress = ChallengeProgress(realDataStore(), backgroundScope)
            progress.mergeLadder(TENTH_LEVEL)

            progress.mergeLadder(FIFTH_LEVEL)

            assertThat(progress.settledLevel()).isEqualTo(TENTH_LEVEL)
        }

    @Test
    fun mergeLadderIsCommittedBeforeItReturns() =
        runTest {
            // The whole point of the suspend variant (D-08): the push that follows a pull
            // in the same ProgressSubmitter pass must read a SETTLED ladder, never the
            // StateFlow's pre-merge cache. A brand-new holder reads only what is on disk.
            val store = realDataStore()
            val progress = ChallengeProgress(store, backgroundScope)

            progress.mergeLadder(FIFTIETH_LEVEL)

            assertThat(ChallengeProgress(store, backgroundScope).settledLevel()).isEqualTo(FIFTIETH_LEVEL)
        }

    @Test
    fun settledLevelOnAFreshStoreIsTheFirstLevel() =
        runTest {
            val progress = ChallengeProgress(realDataStore(), backgroundScope)

            assertThat(progress.settledLevel()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun advancePastStillSettlesTheLadderAtTheNextLevel() =
        runTest {
            // The `+ 1` moved to the call boundary when advancePast was reduced to
            // mergeLadder; the ladder it produces must be byte-for-byte what it always was.
            val progress = ChallengeProgress(realDataStore(), backgroundScope)

            progress.advancePast(NINTH_LEVEL)

            assertThat(progress.currentLevel.first { it == TENTH_LEVEL }).isEqualTo(TENTH_LEVEL)
        }

    @Test
    fun resetAwaitingSettlesTheLadderBackAtTheFirstLevel() =
        runTest {
            // D-14's replace path resets the ladder awaitably, beside resetAllProgress().
            val progress = ChallengeProgress(realDataStore(), backgroundScope)
            progress.mergeLadder(FIFTIETH_LEVEL)

            progress.resetAwaiting()

            assertThat(progress.settledLevel()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun mergeLadderDropsTheWriteInsteadOfThrowingWhenTheStoreThrows() =
        runTest {
            val progress = ChallengeProgress(throwingWriteDataStore(), backgroundScope)

            // T-05-13: unlike advancePast this runs on the CALLER's coroutine, so an
            // unguarded failure would propagate into the sync pass rather than be dropped.
            progress.mergeLadder(FIFTIETH_LEVEL)

            assertThat(progress.settledLevel()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun resetAwaitingDropsTheWriteInsteadOfThrowingWhenTheStoreThrows() =
        runTest {
            val progress = ChallengeProgress(throwingWriteDataStore(), backgroundScope)

            progress.resetAwaiting()

            assertThat(progress.settledLevel()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun settledLevelFailsOpenToTheFirstLevelWhenTheReadThrows() =
        runTest {
            val progress = ChallengeProgress(throwingDataStore(), backgroundScope)

            assertThat(progress.settledLevel()).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun theDomainLadderIdentityEqualsThisModulesDefaultLevel() {
        // The app-module half of the obligation ProgressMergeTest could only assert
        // against ChallengeLevelBounds.MIN: :domain cannot reference an app constant, so
        // the two halves of "a fresh player's ladder is 1" are pinned on both sides.
        assertThat(ProgressMergeLimits.DEFAULT_LADDER)
            .isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
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

    /** Reads work (and are empty); every write fails like a full disk (T-05-13). */
    private fun throwingWriteDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }

    private companion object {
        const val TEST_STORE_FILE = "challenge_progress_test.preferences_pb"
        const val FIRST_LEVEL = 1
        const val SECOND_LEVEL = 2
        const val THIRD_LEVEL = 3
        const val FIFTH_LEVEL = 5
        const val NINTH_LEVEL = 9
        const val TENTH_LEVEL = 10

        /** A ladder far above anything the local device reached — an arriving cloud value. */
        const val FIFTIETH_LEVEL = 50
    }
}
