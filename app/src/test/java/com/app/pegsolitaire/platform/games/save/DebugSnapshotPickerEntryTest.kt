package com.app.pegsolitaire.platform.games.save

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
 * — a release configuration would otherwise be untestable here.
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

            entry(debugBuild = false).onIntent(intent)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
            verify(exactly = 0) { intent.getBooleanExtra(any(), any()) }
        }

    @Test
    fun aDebugBuildStartedWithTheExtraShowsThePickerOnce() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(intentWithExtra(true))
            advanceUntilIdle()

            assertThat(shown).isEqualTo(1)
        }

    @Test
    fun aDebugBuildStartedWithoutTheExtraShowsNothing() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(intentWithExtra(false))
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
        }

    @Test
    fun aMissingIntentShowsNothing() =
        runTest(testDispatcher) {
            entry(debugBuild = true).onIntent(null)
            advanceUntilIdle()

            assertThat(shown).isEqualTo(0)
        }

    @Test
    fun theExtraKeepsTheNameTheQaCommandUses() {
        // Quoted literally in QA briefs as `--ez <name> true`; a rename makes that command a no-op.
        assertThat(CloudSaveConstants.DEBUG_PICKER_EXTRA)
            .isEqualTo("com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER")
    }

    private fun entry(debugBuild: Boolean) =
        DebugSnapshotPickerEntry(debugBuild = debugBuild, scope = scope, show = { shown++ })

    private fun intentWithExtra(value: Boolean): Intent =
        mockk {
            every { getBooleanExtra(CloudSaveConstants.DEBUG_PICKER_EXTRA, false) } returns value
        }
}
