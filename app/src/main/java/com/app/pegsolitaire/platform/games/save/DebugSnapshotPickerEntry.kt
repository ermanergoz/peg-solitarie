package com.app.pegsolitaire.platform.games.save

import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The gate in front of the QA-only snapshot picker: a launch-intent extra that a **debuggable** build
 * honours and every other build ignores.
 *
 *     adb shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity \
 *         --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true
 *
 * ## Why an intent extra, and why it is safe on an exported Activity
 *
 * `MainActivity` is exported (it is the launcher), so any app could send this extra. On a debug build
 * it opens a read-only Google screen showing the signed-in tester their own save; on a release build
 * it does nothing, for **two independent reasons**:
 *
 *  1. [debugBuild] is `BuildConfig.DEBUG`, a compile-time `false` in release, so [onIntent] returns
 *     on its first line — before the extra is even read.
 *  2. `PlayGamesSnapshotPicker` lives in the `debug` source set and is not compiled into release at
 *     all; the release Hilt module builds this gate with a **no-op** [show] (tech-lead L-3).
 *
 * This class itself stays in `src/main` on purpose. The release build's *ignoring* of the extra is
 * then a behaviour with a test (`DebugSnapshotPickerEntryTest`), rather than an absence nothing
 * asserts.
 *
 * ## Why the gate is a constructor argument
 *
 * `BuildConfig.DEBUG` is `true` in the unit-test variant, so a gate read inside the method could
 * never be tested closed. Taking it as a constructor argument is what lets
 * `DebugSnapshotPickerEntryTest` prove a release configuration ignores the extra.
 *
 * @param debugBuild whether this build honours the extra at all.
 * @param show shows the picker. A function rather than the class so the gate is testable without the
 *   SDK — and so the release variant can bind an empty one without the picker existing.
 */
class DebugSnapshotPickerEntry(
    private val debugBuild: Boolean,
    private val show: suspend () -> Unit,
) {
    /**
     * Shows the snapshot picker if, and only if, this is a debug build and [intent] carries
     * [CloudSaveConstants.DEBUG_PICKER_EXTRA] set to `true`.
     *
     * **[scope] is the caller's, and it must be the launched Activity's** (`lifecycleScope`), never
     * the process-lifetime `@ApplicationScope` this used to hold (convergence L-2). [show] waits up
     * to 30 seconds for sign-in, and on a process-lifetime scope that wait outlives the Activity
     * that carried the extra — so the picker could surface over a *later* one, which reads as
     * flakiness in a QA session rather than as the bug it is. Bound to the Activity's scope, the
     * wait dies with the Activity.
     *
     * **The launch body cannot throw into [scope].** An Activity's `lifecycleScope` has no exception
     * handler and its job is the screen's, so an escaping failure would cancel the scope — a debug
     * extra taking down the Activity it was sent to. `runCatching` therefore wraps the whole of
     * [show], including the two bounded waits that sit outside the picker's own guard, and
     * cancellation alone is rethrown so the scope's own teardown is not swallowed.
     */
    fun onIntent(
        intent: Intent?,
        scope: CoroutineScope,
    ) {
        if (!debugBuild) return
        if (intent?.getBooleanExtra(CloudSaveConstants.DEBUG_PICKER_EXTRA, false) != true) return
        scope.launch {
            runCatching { show() }.onFailure { if (it is CancellationException) throw it }
        }
    }
}
