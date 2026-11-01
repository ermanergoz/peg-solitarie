package com.app.pegsolitaire.ui.components

import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI

/**
 * A stroke that draws [dashes] equal dashes at a 50 % duty cycle around a ring of [radius] (the
 * stroke's centreline), [widthPx] wide. `Stroke`'s default cap is butt, which keeps the gaps open
 * (docs/ux/colour-vision.md §3.4: round caps would close them). The pattern is fixed: it has no
 * phase and never moves (§3.6). The colour-blind hint rings use it with eight dashes; it is the
 * wrong-tap mark's formula with the dash count as a parameter.
 */
internal fun dashedRingStroke(
    radius: Float,
    widthPx: Float,
    dashes: Int,
): Stroke {
    val interval = (FULL_TURN * radius) / (dashes * DASH_AND_GAP)
    return Stroke(width = widthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval)))
}

/** A full turn in radians: a ring's circumference is this times its radius. */
private const val FULL_TURN = 2 * PI.toFloat()

/** Each dash is followed by a gap of the same length. */
private const val DASH_AND_GAP = 2
