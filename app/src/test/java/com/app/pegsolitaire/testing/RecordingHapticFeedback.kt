package com.app.pegsolitaire.testing

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * A [HapticFeedback] that records every fire instead of buzzing, so a screen's haptic gates are
 * asserted JVM-side. Installed with
 * `CompositionLocalProvider(LocalHapticFeedback provides RecordingHapticFeedback())`.
 *
 * One shared double for every screen test that reads haptics — the gameplay flow, the tutorial
 * and the wrong tap — rather than a private copy in each. The physical *feel* of a type is
 * device-only and stays with the qa-engineer.
 */
class RecordingHapticFeedback : HapticFeedback {
    /** Every haptic type performed, in call order. */
    val events = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        events += hapticFeedbackType
    }
}
