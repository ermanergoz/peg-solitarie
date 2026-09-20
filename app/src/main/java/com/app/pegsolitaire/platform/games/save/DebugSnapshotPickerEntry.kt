package com.app.pegsolitaire.platform.games.save

import android.content.Intent
import com.app.pegsolitaire.BuildConfig
import com.app.pegsolitaire.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The gate in front of [PlayGamesSnapshotPicker]: a launch-intent extra that a **debuggable** build
 * honours and every other build ignores.
 *
 *     adb shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity \
 *         --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true
 *
 * ## Why an intent extra, and why it is safe on an exported Activity
 *
 * There is no `debug` source set in this module, so "debug-only" cannot be "not compiled into
 * release"; it is `BuildConfig.DEBUG`, a compile-time `false` in release that leaves [onIntent]
 * returning on its first line — before the extra is even read. `MainActivity` is exported (it is
 * the launcher), so any app could send this extra; on a release build that does nothing, and on a
 * debug build it opens a read-only Google screen showing the signed-in tester their own save.
 *
 * ## Why the gate is a constructor argument
 *
 * `BuildConfig.DEBUG` is `true` in the unit-test variant, so a gate read inside the method could
 * never be tested closed. The `@Inject` constructor pins it to the build; the primary one lets
 * `DebugSnapshotPickerEntryTest` prove a release configuration ignores the extra.
 *
 * @param debugBuild whether this build honours the extra at all.
 * @param scope where the picker is shown from: the call site is `Activity.onCreate`, which must
 *   not be blocked on a sign-in that may take seconds.
 * @param show shows the picker. A function rather than the class so the gate is testable without
 *   the SDK.
 */
@Singleton
class DebugSnapshotPickerEntry internal constructor(
    private val debugBuild: Boolean,
    private val scope: CoroutineScope,
    private val show: suspend () -> Unit,
) {
    @Inject
    constructor(
        @ApplicationScope scope: CoroutineScope,
        picker: PlayGamesSnapshotPicker,
    ) : this(debugBuild = BuildConfig.DEBUG, scope = scope, show = picker::show)

    /**
     * Shows the snapshot picker if, and only if, this is a debug build and [intent] carries
     * [CloudSaveConstants.DEBUG_PICKER_EXTRA] set to `true`.
     */
    fun onIntent(intent: Intent?) {
        if (!debugBuild) return
        if (intent?.getBooleanExtra(CloudSaveConstants.DEBUG_PICKER_EXTRA, false) != true) return
        scope.launch { show() }
    }
}
