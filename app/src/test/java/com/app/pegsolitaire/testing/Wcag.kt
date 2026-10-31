package com.app.pegsolitaire.testing

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * WCAG 2.x arithmetic over `Color.kt` tokens, for the JVM tests that prove a spec's ratios from the
 * shipped values rather than trusting the document (backdrop spec §5, colour-vision.md §4).
 *
 * Translucent layers composite in gamma-encoded sRGB, channel by channel, as Compose's default
 * alpha draw does (backdrop spec §5's stated method), so [over] and [lerpSrgb] work on the
 * encoded components and only [wcagLuminance] linearises.
 */

/** WCAG relative luminance of this colour's RGB, ignoring its alpha. */
internal fun Color.wcagLuminance(): Double =
    LUMA_RED * linear(red.toDouble()) + LUMA_GREEN * linear(green.toDouble()) + LUMA_BLUE * linear(blue.toDouble())

/** The WCAG contrast ratio between two relative luminances. */
internal fun contrast(
    a: Double,
    b: Double,
): Double = (maxOf(a, b) + FLARE) / (minOf(a, b) + FLARE)

/** The WCAG contrast ratio between two colours' RGB. */
internal fun contrast(
    a: Color,
    b: Color,
): Double = contrast(a.wcagLuminance(), b.wcagLuminance())

/** This colour drawn at its own alpha over [dst], source-over in gamma sRGB; opaque when [dst] is. */
internal fun Color.over(dst: Color): Color {
    val a = alpha
    return Color(
        red = red * a + dst.red * (1f - a),
        green = green * a + dst.green * (1f - a),
        blue = blue * a + dst.blue * (1f - a),
        alpha = a + dst.alpha * (1f - a),
    )
}

/** This colour with its alpha multiplied by [factor] (a layer's or a light's opacity). */
internal fun Color.fade(factor: Float): Color = copy(alpha = alpha * factor)

/** The gamma-sRGB blend of [a] and [b] at [t], as a linear gradient between two stops draws it. */
internal fun lerpSrgb(
    a: Color,
    b: Color,
    t: Float,
): Color =
    Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t,
    )

/** The HSL hue of this colour's RGB in degrees, or `null` for an achromatic grey or white. */
internal fun Color.hueDegrees(): Double? {
    val r = red.toDouble()
    val g = green.toDouble()
    val b = blue.toDouble()
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val chroma = max - min
    if (chroma < ACHROMATIC) return null
    val sector =
        when (max) {
            r -> ((g - b) / chroma).mod(HUE_SECTORS)
            g -> (b - r) / chroma + 2.0
            else -> (r - g) / chroma + 4.0
        }
    return sector * DEGREES_PER_SECTOR
}

/**
 * The lowest edge contrast a container keeps against ANY adjacent colour, when its edge is the
 * stronger of fill-versus-neighbour and rim-versus-neighbour (backdrop spec §5.2):
 * `√((R + 0.05) / (F + 0.05))`, with the rim the brighter of the two.
 */
internal fun edgeFloor(
    fill: Double,
    rim: Double,
): Double = sqrt((maxOf(fill, rim) + FLARE) / (minOf(fill, rim) + FLARE))

/** True when two colours' RGB agree within [tolerance] per channel (0..1 scale). */
internal fun Color.rgbClose(
    other: Color,
    tolerance: Float,
): Boolean =
    abs(red - other.red) <= tolerance &&
        abs(green - other.green) <= tolerance &&
        abs(blue - other.blue) <= tolerance

private fun linear(channel: Double): Double =
    if (channel <= LINEAR_KNEE) channel / LINEAR_SLOPE else ((channel + LINEAR_OFFSET) / LINEAR_SCALE).pow(LINEAR_GAMMA)

private const val LUMA_RED = 0.2126
private const val LUMA_GREEN = 0.7152
private const val LUMA_BLUE = 0.0722
private const val FLARE = 0.05
private const val LINEAR_KNEE = 0.04045
private const val LINEAR_SLOPE = 12.92
private const val LINEAR_OFFSET = 0.055
private const val LINEAR_SCALE = 1.055
private const val LINEAR_GAMMA = 2.4
private const val ACHROMATIC = 1e-6
private const val HUE_SECTORS = 6.0
private const val DEGREES_PER_SECTOR = 60.0
