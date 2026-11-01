package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/*
 * Reading a drawn star off a frame (docs/ux/colour-vision.md §3.4, §12.3; CV-AC05 as amended).
 *
 * A star glyph is a 24-unit viewport scaled to its box, so a point named in units lands on the same
 * part of the star at a 16dp catalogue pip and at a 48dp Results star. (12, 12) lies inside the
 * filled star's body and inside the hollow star's open interior; (12, 4.5) lies on the hollow
 * star's band, below the top tip, where an earned star's outline also runs.
 */

/** The pixel at ([x], [y]) of a star glyph drawn in [box], in the glyph's 24-unit viewport. */
internal fun Bitmap.starPixel(
    box: Rect,
    x: Float,
    y: Float,
): Int =
    getPixel(
        (box.left + box.width * x / STAR_VIEWPORT_UNITS).roundToInt(),
        (box.top + box.height * y / STAR_VIEWPORT_UNITS).roundToInt(),
    )

/**
 * Whether [pixel] lies between the two ends of a gradient's [stops] in every channel, within
 * [tolerance]: what any point of a two-stop gradient fill reads, and what a point of another
 * colour does not.
 */
internal fun isBetweenStops(
    pixel: Int,
    stops: List<Color>,
    tolerance: Int = CHANNEL_TOLERANCE,
): Boolean {
    val ends = stops.map { it.toArgb() }
    return STAR_CHANNEL_SHIFTS.all { shift ->
        val channel = (pixel shr shift) and STAR_BYTE
        val range = ends.map { (it shr shift) and STAR_BYTE }
        channel in (range.min() - tolerance)..(range.max() + tolerance)
    }
}

/** The glyph's viewport edge, in units. */
internal const val STAR_VIEWPORT_UNITS = 24f

/** The glyph's centre, in units: the filled star's body, the hollow star's open interior. */
internal const val STAR_CENTRE_UNITS = 12f

/** Below the top tip, in units: on the hollow star's band and on an earned star's outline. */
internal const val STAR_BAND_UNITS = 4.5f

private const val STAR_BYTE = 0xFF
private val STAR_CHANNEL_SHIFTS = listOf(16, 8, 0)
