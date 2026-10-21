package com.app.pegsolitaire.ui.game

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

/**
 * Animation timing tokens (milliseconds + easings) for the jump/removal/selection
 * feedback.
 *
 * Durations are unitless `Int` ms (not dp/sp), so they live in this scoped object
 * rather than `dimens.xml`. Both the jump slide (D-07) and the jumped-peg removal
 * (D-08) launch together keyed on the move id; the removal ([REMOVAL_MS]) finishes
 * just as the jumper lands ([JUMP_SLIDE_MS]).
 */
object GameAnim {
    /** Jumper straight-line slide source → destination (D-07). */
    const val JUMP_SLIDE_MS = 200

    /** Jumped peg alpha 1→0 + scale 1→`REMOVAL_SCALE_TARGET` (D-08). */
    const val REMOVAL_MS = 180

    /** Selection ring / destination dots fade-in. */
    const val SELECT_FADE_MS = 120

    /** Easing for the jumper slide (D-07). */
    val JUMP_SLIDE_EASING: Easing = FastOutSlowInEasing

    /** Easing for the jumped-peg fade + shrink (D-08). */
    val REMOVAL_EASING: Easing = FastOutLinearInEasing

    /** Easing for the selection/destination fade-in. */
    val SELECT_FADE_EASING: Easing = LinearEasing

    /** One half-cycle of the selected peg's breathing pulse (reverse-repeats). */
    const val PULSE_MS = 900

    /** Legal-destination dots start scale before their spring-overshoot pop-in. */
    const val DOT_POP_START = 0f

    /** Legal-destination dots settled scale. */
    const val DOT_POP_END = 1f

    // ---- The wrong tap (D-06, docs/ux/invalid-tap-feedback.md §3.1–§3.2, §5) ----

    /** The head-shake of the tapped cell, inside the 150–300 ms micro-interaction band. */
    const val WRONG_TAP_SHAKE_MS = 270

    /**
     * The shake's keyframes, a decaying wobble of about two and a half swings: each `_AT` is a
     * fraction of [WRONG_TAP_SHAKE_MS] (≈ 46 / 105 / 165 / 219 / 270 ms), each value a multiple
     * of the amplitude `BoardMetrics.WRONG_TAP_SHAKE_RATIO × cellSize`, rightward positive and
     * the same in LTR and RTL.
     */
    const val WRONG_TAP_SHAKE_KEY_1_AT = 0.17f
    const val WRONG_TAP_SHAKE_KEY_1 = 1.0f
    const val WRONG_TAP_SHAKE_KEY_2_AT = 0.39f
    const val WRONG_TAP_SHAKE_KEY_2 = -0.75f
    const val WRONG_TAP_SHAKE_KEY_3_AT = 0.61f
    const val WRONG_TAP_SHAKE_KEY_3 = 0.45f
    const val WRONG_TAP_SHAKE_KEY_4_AT = 0.81f
    const val WRONG_TAP_SHAKE_KEY_4 = -0.2f
    const val WRONG_TAP_SHAKE_KEY_5_AT = 1.0f
    const val WRONG_TAP_SHAKE_KEY_5 = 0f

    /** Every segment of the shake and of the ring nudge eases on the jump slide's own curve. */
    val WRONG_TAP_EASING: Easing = JUMP_SLIDE_EASING

    /** The held peg's ring grows to its nudge peak over this long, while a peg is held (W2, W3). */
    const val WRONG_TAP_RING_NUDGE_OUT_MS = 110

    /** …and returns to its resting radius over this long. */
    const val WRONG_TAP_RING_NUDGE_BACK_MS = 150

    /**
     * How long the reduced-motion mark holds on the tapped cell. **Functional, not decorative**: it
     * applies precisely when animations are off (D-06a), where a fade would never show.
     */
    const val WRONG_TAP_MARK_HOLD_MS = 450
}
