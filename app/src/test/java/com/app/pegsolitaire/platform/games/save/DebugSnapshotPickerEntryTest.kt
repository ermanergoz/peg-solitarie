package com.app.pegsolitaire.platform.games.save

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/**
 * Behaviour spec for [DebugSnapshotPickerEntry] — the gate in front of the QA-only snapshot picker.
 *
 * **The case this file exists for is the first one.** `MainActivity` is exported, so any app on the
 * device, and anyone with a shell, can start it with any extras they like. On a release build the
 * extra must be inert: the picker is a second cloud-save surface, and the requirements reserve
 * exactly one (D-03). `BuildConfig.DEBUG` is a compile-time `true` in this unit-test variant, which
 * is why the gate is a constructor argument rather than a read of that constant in the method body
 * — a release configuration would otherwise be untestable here. Since the picker itself moved into
 * `src/debug`, a release build has a second, structural reason to be inert: the class it would
 * launch is not compiled at all, and the release Hilt module provides a no-op `show`. The gate is
 * still tested here, because "inert twice over" is the claim.
 *
 * The last two cases are the [CoroutineScope] contract, and both are about a *debug* surface
 * misbehaving in a QA session rather than about anything a player sees: the wait belongs to the
 * Activity that was launched with the extra (a process-lifetime scope would let the picker surface
 * on a later one), and a failure inside it must not reach a scope with no handler — cancelling the
 * Activity's own scope would take the screen down with it.
 */
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class DebugSnapshotPickerEntryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private var shown = 0

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt() =
        runTest(testDispatcher) {
            val intent = intentWithExtra(true)

            entry(debugBuild = false).onIntent(intent, scope)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
            verify(exactly = 0) { intent.getBooleanExtra(any(), any()) }
        }

    @Test
    fun aDebugBuildStartedWithTheExtraShowsThePickerOnce() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(intentWithExtra(true), scope)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(1)
        }

    @Test
    fun aDebugBuildStartedWithoutTheExtraShowsNothing() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(intentWithExtra(false), scope)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
        }

    @Test
    fun aMissingIntentShowsNothing() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(null, scope)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
        }

    @Test
    fun theWaitBelongsToTheScopeTheCallerSupplied() =
        runTest(testDispatcher) {
            // L-2: the picker waits up to 30 s for sign-in. On a process-lifetime scope that wait
            // outlives the Activity that carried the extra, so the picker can surface over a
            // *later* one — which reads as flakiness in a QA session. Handing in the Activity's
            // own scope makes the wait die with the Activity, which is what this proves: the
            // Activity goes away before the wait resolves, and nothing is ever shown.
            val activityScope = CoroutineScope(testDispatcher)

            entry(debugBuild = true).onIntent(intentWithExtra(true), activityScope)
            activityScope.cancel()
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
        }

    @Test
    fun aFailureInsideTheShowNeverCancelsTheCallersScope() =
        runTest(testDispatcher) {
            // The Activity's `lifecycleScope` has no exception handler, and its job is the
            // screen's: an exception escaping the launch would cancel the scope — i.e. a debug
            // extra could take down the Activity it was sent to. The picker's own `runCatching`
            // covers only the SDK call; the two bounded waits in front of it sit outside it.
            val activityScope = CoroutineScope(testDispatcher)

            DebugSnapshotPickerEntry(debugBuild = true, show = { throw PickerFailure() })
                .onIntent(intentWithExtra(true), activityScope)
            advanceUntilIdle()

            var ranAfterwards = false
            activityScope.launch { ranAfterwards = true }
            advanceUntilIdle()

            assertThat(activityScope.isActive).isTrue()
            assertThat(ranAfterwards).isTrue()

            activityScope.cancel()
        }

    @Test
    fun theExtraKeepsTheNameTheQaCommandUses() {
        // Quoted literally in QA briefs as `--ez <name> true`; a rename makes that command a no-op.
        assertThat(CloudSaveConstants.DEBUG_PICKER_EXTRA)
            .isEqualTo("com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER")
    }

    private fun entry(debugBuild: Boolean) = DebugSnapshotPickerEntry(debugBuild = debugBuild, show = { shown++ })

    private fun intentWithExtra(value: Boolean): Intent =
        mockk {
            every { getBooleanExtra(CloudSaveConstants.DEBUG_PICKER_EXTRA, false) } returns value
        }
}

/** What the failing `show` fixture throws; a named type rather than a bare generic exception. */
private class PickerFailure : RuntimeException("the debug picker exploded")
