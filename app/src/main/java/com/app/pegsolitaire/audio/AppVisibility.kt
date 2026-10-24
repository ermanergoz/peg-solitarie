package com.app.pegsolitaire.audio

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the app's process is in the foreground, for P8's background gate in [SoundPoolPlayer]
 * (`docs/tech/audio-silent-mode.md` §2.1): one-shots still ringing when the player leaves are
 * stopped, and new cues are refused until the process is back.
 */
interface AppVisibility {
    /** `true` while any Activity of the app is visible; de-duplicated. Never completes. */
    val inForeground: Flow<Boolean>
}

/**
 * [AppVisibility] read from the **process** lifecycle, `ProcessLifecycleOwner`.
 *
 * The process, not an Activity: D-16's reasoning holds unchanged, a same-process interstitial is
 * not a background. And `ProcessLifecycleOwner` read here directly, **not** through
 * `platform`'s `AppForegroundTracker`: that class is the interstitial session boundary, has no
 * interface and drives the ad cap, and making audio depend on it would widen `platform`'s role
 * and couple audio to the ads. Both read the same lifecycle, so they agree on what "background"
 * means.
 *
 * At process start the lifecycle is CREATED until `MainActivity` starts, so the first value is
 * `false`; the player's resulting `silenceNow()` on an empty pool is harmless, and no cue exists
 * before the Activity does. `ProcessLifecycleOwner` dispatches `ON_STOP` about 700 ms after the
 * last Activity stops, so a tail can ring that long after leaving — accepted in the design.
 */
@Singleton
class ProcessAppVisibility internal constructor(
    lifecycle: Lifecycle,
) : AppVisibility {
    @Inject
    constructor() : this(ProcessLifecycleOwner.get().lifecycle)

    override val inForeground: Flow<Boolean> =
        lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .distinctUntilChanged()
}
