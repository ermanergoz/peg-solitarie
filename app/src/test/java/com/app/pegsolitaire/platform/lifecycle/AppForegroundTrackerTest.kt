package com.app.pegsolitaire.platform.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.app.pegsolitaire.data.InterstitialCadenceStore
import com.app.pegsolitaire.domain.ads.InterstitialCadenceParams
import com.app.pegsolitaire.testing.InMemoryPreferencesDataStore
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Behaviour spec for [AppForegroundTracker] — the D-16 session-boundary source.
 *
 * What is under test is the tracker's **translation** of a lifecycle event into a store
 * transition, so the observer is driven directly with a stub [LifecycleOwner] rather than by
 * standing up a real `ProcessLifecycleOwner`. `ProcessLifecycleOwner`'s own dispatch semantics
 * — in particular that a same-process fullscreen ad Activity does not register as a background
 * — are the framework's contract plus RESEARCH assumption A1, confirmed by device UAT in plan
 * 10-06, and are not something a JVM unit test can honestly assert.
 *
 * Two halves, deliberately: mocked-store cases pin exactly WHICH call each event makes (and
 * that it makes no other), and in-memory-store cases pin what the resulting session state is.
 */
class AppForegroundTrackerTest {
    @Test
    fun onStopStampsTheForegroundExitOnceWithWallClockAndStartsNoSession() {
        val store = mockk<InterstitialCadenceStore>(relaxed = true)
        val stamp = slot<Long>()

        val before = System.currentTimeMillis()
        AppForegroundTracker(store).onStop(StubOwner)
        val after = System.currentTimeMillis()

        verify(exactly = 1) { store.recordForegroundLeft(capture(stamp)) }
        // Wall-clock, not elapsedRealtime (RESEARCH pitfall 10): the stamp is persisted and
        // compared after process death, which no monotonic timebase survives. The window
        // assertion is what distinguishes the two — an uptime value would be far below it.
        assertThat(stamp.captured).isAtLeast(before)
        assertThat(stamp.captured).isAtMost(after)
        // Leaving the foreground must never itself open a session.
        verify(exactly = 0) { store.startSessionIfGapElapsed(any()) }
    }

    @Test
    fun onStartAsksForASessionBoundaryOnceWithWallClockAndStampsNoExit() {
        val store = mockk<InterstitialCadenceStore>(relaxed = true)
        val now = slot<Long>()

        val before = System.currentTimeMillis()
        AppForegroundTracker(store).onStart(StubOwner)
        val after = System.currentTimeMillis()

        verify(exactly = 1) { store.startSessionIfGapElapsed(capture(now)) }
        assertThat(now.captured).isAtLeast(before)
        assertThat(now.captured).isAtMost(after)
        verify(exactly = 0) { store.recordForegroundLeft(any()) }
    }

    @Test
    fun onStopPersistsTheStampSoASecondHolderReadsItBack() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val store = InterstitialCadenceStore(dataStore, backgroundScope)

            val before = System.currentTimeMillis()
            AppForegroundTracker(store).onStop(StubOwner)

            val persisted = store.lastForegroundAtMs.first { it >= before }
            val restarted = InterstitialCadenceStore(dataStore, backgroundScope)
            assertThat(restarted.lastForegroundAtMs.first { it == persisted }).isEqualTo(persisted)
        }

    @Test
    fun onStartAfterTheFullBackgroundGapOpensANewSession() =
        runTest {
            val store = InterstitialCadenceStore(InMemoryPreferencesDataStore(), backgroundScope)
            val tracker = AppForegroundTracker(store)
            store.recordInterstitialShown(System.currentTimeMillis())
            store.state.first { it.interstitialsThisSession == ONE }
            val leftAt = System.currentTimeMillis() - InterstitialCadenceParams.SESSION_GAP_MS - SETTLE_MARGIN_MS
            store.recordForegroundLeft(leftAt)
            store.lastForegroundAtMs.first { it == leftAt }

            tracker.onStart(StubOwner)

            // D-04's cap restarts and D-03's first-session suppression ends together.
            val after = store.state.first { it.firstSessionComplete }
            assertThat(after.interstitialsThisSession).isEqualTo(NONE)
        }

    @Test
    fun onStartInsideTheGapContinuesTheSameSession() =
        runTest {
            val store = InterstitialCadenceStore(InMemoryPreferencesDataStore(), backgroundScope)
            val tracker = AppForegroundTracker(store)
            store.recordInterstitialShown(System.currentTimeMillis())
            store.state.first { it.interstitialsThisSession == ONE }
            val leftAt = System.currentTimeMillis()
            store.recordForegroundLeft(leftAt)
            store.lastForegroundAtMs.first { it == leftAt }

            tracker.onStart(StubOwner)
            // DataStore serialises writes, so observing this later one proves the onStart
            // write (if any) already ran — without it the assertion could pass by racing.
            store.recordWin()

            val after = store.state.first { it.winsSinceLastInterstitial == ONE }
            assertThat(after.firstSessionComplete).isFalse()
            assertThat(after.interstitialsThisSession).isEqualTo(ONE)
        }

    @Test
    fun onStartOnAFirstEverLaunchLeavesTheNewInstallSuppressionIntact() =
        runTest {
            // Launch one has no prior foreground stamp, so it closes no session — otherwise
            // D-03's suppression would be deleted on install day.
            val store = InterstitialCadenceStore(InMemoryPreferencesDataStore(), backgroundScope)

            AppForegroundTracker(store).onStart(StubOwner)
            store.recordWin()

            val after = store.state.first { it.winsSinceLastInterstitial == ONE }
            assertThat(after.firstSessionComplete).isFalse()
        }

    /** The tracker never reads its owner — the event itself is the whole input. */
    private object StubOwner : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = error("AppForegroundTracker must not read the LifecycleOwner")
    }

    private companion object {
        const val NONE = 0
        const val ONE = 1

        /** Slack so the seeded stamp is unambiguously outside the gap despite real-clock drift. */
        const val SETTLE_MARGIN_MS = 1_000L
    }
}
