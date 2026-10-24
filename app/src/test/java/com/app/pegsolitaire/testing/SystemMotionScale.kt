package com.app.pegsolitaire.testing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale

/**
 * The system's animator scale as a test can turn it while a screen stays composed: what
 * "Remove animations" does on a phone with the app resident.
 *
 * Pass it as a compose rule's `effectContext`. Every effect's `Animatable` and the reduced-motion
 * gate (`rememberAnimationsEnabled()`) then read it, as both read the window's own scale on a
 * phone. It is snapshot state, like that window scale (`MotionDurationScaleImpl` in `ui-android`
 * 1.11.3 backs its factor with `mutableFloatStateOf` and updates it from a `ContentObserver` on
 * `Settings.Global.ANIMATOR_DURATION_SCALE`), so a composition that read it recomposes when the
 * test turns it.
 *
 * Compose's test rule does not install the window scale: it composes on its own recomposer, whose
 * context carries no scale unless the test provides one here.
 */
internal class SystemMotionScale(
    initial: Float = ON,
) : MotionDurationScale {
    override var scaleFactor by mutableFloatStateOf(initial)
        private set

    /** "Remove animations" on: every animation ends in its first frame. */
    fun turnAnimationsOff() {
        scaleFactor = OFF
    }

    /** "Remove animations" off again: animations run at their own speed. */
    fun turnAnimationsOn() {
        scaleFactor = ON
    }

    companion object {
        /** The system default: animations at their own speed. */
        const val ON = 1f

        /** The animator scale a phone reports with "Remove animations" on. */
        const val OFF = 0f
    }
}
