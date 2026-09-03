package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.app.pegsolitaire.domain.ads.InterstitialCadenceParams
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
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
 * Behaviour spec for [InterstitialCadenceStore] — the persistence half of the interstitial
 * cadence (INT-02 "survives being killed", D-13's persisted/in-memory split).
 *
 * "Killed and relaunched" is simulated the way [ChallengeProgressTest] simulates it: a SECOND
 * holder constructed over the SAME `DataStore`. That is the only observation that proves the
 * frequency cap survives process death, and it is also what proves the D-13 split, because the
 * per-session count must come back as zero on that second holder while everything else comes
 * back intact.
 *
 * Runs on the JVM against a real temp-file Preferences store; two hostile doubles (every read
 * throws / every write throws) prove the store fails open to "no interstitial" and never
 * propagates a storage failure onto the app-lifetime scope (T-10-01 / T-10-10).
 *
 * Where a test asserts that a call did NOT change anything, it first issues a LATER write and
 * awaits that one. `DataStore` serialises `updateData`, so observing the later write is proof
 * the earlier call has already been applied — without it the assertion could pass simply by
 * running before the write it is meant to rule out.
 */
class InterstitialCadenceStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun aFreshStoreYieldsTheDefaultStateAndShowsNoInterstitial() =
        runTest {
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)

            // Fail-safe direction: the default state is the one that authorises nothing.
            assertThat(store.state.value).isEqualTo(InterstitialCadenceState())
            assertThat(store.state.first().mayShow(NOW_MS)).isFalse()
        }

    @Test
    fun recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack() =
        runTest {
            // INT-02: the frequency cap must survive the app being killed.
            val dataStore = realDataStore()
            val store = InterstitialCadenceStore(dataStore, backgroundScope)

            store.recordWin()
            store.state.first { it.winsSinceLastInterstitial == ONE }

            val restarted = InterstitialCadenceStore(dataStore, backgroundScope)
            val persisted = restarted.state.first { it.winsSinceLastInterstitial == ONE }
            assertThat(persisted.lifetimeCompletedLevels).isEqualTo(ONE)
        }

    @Test
    fun recordInterstitialShownRestartsTheWinsStampsTheAdAndRaisesTheSessionCount() =
        runTest {
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)
            store.recordWin()
            store.state.first { it.winsSinceLastInterstitial == ONE }

            store.recordInterstitialShown(NOW_MS)

            val after = store.state.first { it.lastFullscreenAdAtMs == NOW_MS }
            assertThat(after.winsSinceLastInterstitial).isEqualTo(NONE)
            assertThat(after.interstitialsThisSession).isEqualTo(ONE)
        }

    @Test
    fun recordRewardedShownStampsTheSharedClockAndLeavesTheWinCounterAlone() =
        runTest {
            // D-15: a rewarded ad opens the same 90s cooldown but is not charged to the
            // interstitial budget — the player opted into it.
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)
            store.recordWin()
            store.state.first { it.winsSinceLastInterstitial == ONE }

            store.recordRewardedShown(NOW_MS)

            val after = store.state.first { it.lastFullscreenAdAtMs == NOW_MS }
            assertThat(after.winsSinceLastInterstitial).isEqualTo(ONE)
            assertThat(after.interstitialsThisSession).isEqualTo(NONE)
        }

    @Test
    fun theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero() =
        runTest {
            // D-13: everything else survives a relaunch; this one deliberately does not.
            val dataStore = realDataStore()
            val store = InterstitialCadenceStore(dataStore, backgroundScope)

            store.recordInterstitialShown(NOW_MS)
            store.state.first { it.interstitialsThisSession == ONE }

            val restarted = InterstitialCadenceStore(dataStore, backgroundScope)
            val afterRestart = restarted.state.first { it.lastFullscreenAdAtMs == NOW_MS }
            assertThat(afterRestart.interstitialsThisSession).isEqualTo(NONE)
        }

    @Test
    fun aStoreWhoseReadsAlwaysThrowYieldsTheDefaultStateAndNoAd() =
        runTest {
            val store = InterstitialCadenceStore(throwingDataStore(), backgroundScope)

            // T-10-01: a corrupt store costs revenue, never a crash and never an unauthorised ad.
            assertThat(store.state.first()).isEqualTo(InterstitialCadenceState())
            assertThat(store.state.value.mayShow(NOW_MS)).isFalse()
        }

    @Test
    fun recordWinDropsTheWriteInsteadOfCrashingWhenTheStoreThrows() =
        runTest {
            val store = InterstitialCadenceStore(throwingWriteDataStore(), backgroundScope)

            // T-10-10: the write runs detached on the app-lifetime scope, whose SupervisorJob
            // isolates a failed child but does NOT swallow its exception.
            store.recordWin()
            advanceUntilIdle()

            assertThat(store.state.value).isEqualTo(InterstitialCadenceState())
        }

    @Test
    fun recordForegroundLeftDropsTheWriteInsteadOfCrashingWhenTheStoreThrows() =
        runTest {
            val store = InterstitialCadenceStore(throwingWriteDataStore(), backgroundScope)

            store.recordForegroundLeft(NOW_MS)
            store.startSessionIfGapElapsed(NOW_MS)
            advanceUntilIdle()

            assertThat(store.lastForegroundAtMs.value).isEqualTo(InterstitialCadenceState.NEVER_FOREGROUNDED)
        }

    @Test
    fun forgedOutOfRangePersistedValuesAreNormalisedOnTheWayOut() =
        runTest {
            // T-10-01: the read path is the only validation point, so assert the READ result.
            val dataStore = realDataStore()
            dataStore.edit {
                it[InterstitialCadencePreferences.WINS_SINCE_LAST_KEY] = FORGED_NEGATIVE_WINS
                it[InterstitialCadencePreferences.LAST_FULLSCREEN_AD_AT_KEY] = Long.MAX_VALUE
                it[InterstitialCadencePreferences.LIFETIME_COMPLETED_KEY] = FORGED_HUGE_LIFETIME
                it[InterstitialCadencePreferences.FIRST_SESSION_DONE_KEY] = true
            }

            val store = InterstitialCadenceStore(dataStore, backgroundScope)

            val read = store.state.first { it.firstSessionComplete }
            assertThat(read.winsSinceLastInterstitial).isEqualTo(NONE)
            assertThat(read.lastFullscreenAdAtMs).isEqualTo(InterstitialCadenceState.NEVER_SHOWN)
            assertThat(read.lifetimeCompletedLevels)
                .isEqualTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS)
        }

    @Test
    fun recordForegroundLeftPersistsTheStampAcrossARestart() =
        runTest {
            val dataStore = realDataStore()
            val store = InterstitialCadenceStore(dataStore, backgroundScope)

            store.recordForegroundLeft(NOW_MS)

            assertThat(store.lastForegroundAtMs.first { it == NOW_MS }).isEqualTo(NOW_MS)
            val restarted = InterstitialCadenceStore(dataStore, backgroundScope)
            assertThat(restarted.lastForegroundAtMs.first { it == NOW_MS }).isEqualTo(NOW_MS)
        }

    @Test
    fun startSessionIfGapElapsedOpensANewSessionAfterTheFullBackgroundGap() =
        runTest {
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)
            store.recordInterstitialShown(NOW_MS)
            store.state.first { it.interstitialsThisSession == ONE }
            val leftAt = NOW_MS - InterstitialCadenceParams.SESSION_GAP_MS
            store.recordForegroundLeft(leftAt)
            store.lastForegroundAtMs.first { it == leftAt }

            store.startSessionIfGapElapsed(NOW_MS)

            // D-03's first-session suppression ends here, and D-04's cap restarts.
            val after = store.state.first { it.firstSessionComplete }
            assertThat(after.interstitialsThisSession).isEqualTo(NONE)
        }

    @Test
    fun startSessionIfGapElapsedInsideTheGapContinuesTheSameSession() =
        runTest {
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)
            store.recordInterstitialShown(NOW_MS)
            store.state.first { it.interstitialsThisSession == ONE }
            val leftAt = NOW_MS - InterstitialCadenceParams.SESSION_GAP_MS + ONE_MS
            store.recordForegroundLeft(leftAt)
            store.lastForegroundAtMs.first { it == leftAt }

            store.startSessionIfGapElapsed(NOW_MS)
            store.recordWin()

            val after = store.state.first { it.winsSinceLastInterstitial == ONE }
            assertThat(after.firstSessionComplete).isFalse()
            assertThat(after.interstitialsThisSession).isEqualTo(ONE)
        }

    @Test
    fun startSessionIfGapElapsedOnAFirstEverLaunchLeavesTheSuppressionIntact() =
        runTest {
            // Without the NEVER_FOREGROUNDED guard this would end D-03's suppression on
            // install day — the one window the phase most needs to hold.
            val store = InterstitialCadenceStore(realDataStore(), backgroundScope)

            store.startSessionIfGapElapsed(NOW_MS)
            store.recordWin()

            val after = store.state.first { it.winsSinceLastInterstitial == ONE }
            assertThat(after.firstSessionComplete).isFalse()
        }

    private fun realDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
        )

    /** Every read fails like a corrupt store (T-10-01). */
    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                error("not used in the read path")
        }

    /** Reads work (and are empty); every write fails like a full disk (T-10-10). */
    private fun throwingWriteDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }

    private companion object {
        const val TEST_STORE_FILE = "interstitial_cadence_test.preferences_pb"

        /** An arbitrary wall-clock instant; the policy's thresholds are read from the params. */
        const val NOW_MS = 1_700_000_000_000L
        const val NONE = 0
        const val ONE = 1
        const val ONE_MS = 1L

        /** Tampered stored values — arbitrary by construction, hence bare literals. */
        const val FORGED_NEGATIVE_WINS = -5
        const val FORGED_HUGE_LIFETIME = 9_999
    }
}
