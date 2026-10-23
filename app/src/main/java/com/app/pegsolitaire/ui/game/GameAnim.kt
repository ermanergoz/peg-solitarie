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

    /**
     * How long the Undecided notice stays up (ux-spec §4.4.2, §6.4): its reading time, before
     * the system's accessibility timeout lengthens it. Functional, not decoration, so it holds
     * with animations off as well.
     */
    const val HINT_NOTICE_DISPLAY_MS = 5_000

    /** The Undecided notice's fade-out, two thirds of `ClayAnim.ENTRANCE_MS` (§6.4). */
    const val HINT_NOTICE_EXIT_MS = 160
}
