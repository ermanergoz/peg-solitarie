package com.app.pegsolitaire.ui.backdrop

import androidx.compose.runtime.Immutable

/** Which `BackdropColors` sky token a sky stop takes (candy-backdrop.md §3.3). */
enum class SkyRole { Top, Mid, Low, Deep, SparseLow }

/** One stop of a density's vertical sky gradient: a fraction of the window height and its colour role. */
@Immutable
data class SkyStop(
    val fraction: Float,
    val role: SkyRole,
)

/** The three kinds of light, each a core and a rim colour (candy-backdrop.md §3.2). */
enum class LightKind { Pearl, Pink, Violet }

/**
 * Which set a light belongs to (candy-backdrop.md §3.3): Home's [Lane] lights may reach above the
 * title's reach and never overlap; its [Low] lights never rise above it; the game screen's [Upper]
 * lights stay above the spinner's band and its [Lower] lights below it.
 */
enum class LightSet { Lane, Low, Upper, Lower }

/** A light's diameter, one of the six `backdrop_light_*` size tokens (candy-backdrop.md §4.2). */
enum class LightSize { Xs, Sm, Md, Lg, Xl, Xxl }

/**
 * One light of a density's table: its rest centre as window fractions from the start edge, its
 * size token, kind, phase at clock time 0, opacity and set. Its period is in [BackdropMotion].
 */
@Immutable
data class LightSpec(
    val x: Float,
    val y: Float,
    val size: LightSize,
    val kind: LightKind,
    val phase0: Float,
    val opacity: Float,
    val set: LightSet,
)

/** A wave's centre across (`cx`, a fraction of W) and its crest's height (a fraction of H). */
@Immutable
data class WavePlacement(
    val cx: Float,
    val crest: Float,
)

/**
 * The backdrop's unitless ratios, anchors and lists (candy-backdrop.md §4.3), beside
 * `BoardMetrics` and `HomeStageMetrics`. Every position the scene draws is one of these or a
 * `dimens.xml` token; the builder invents none. Durations live in [BackdropMotion].
 */
object BackdropMetrics {
    /** Home's sky: the background held to 0.12 H, then violet-pink to the bottom edge. */
    val LANDSCAPE_SKY_STOPS: List<SkyStop> =
        listOf(
            SkyStop(fraction = 0.00f, role = SkyRole.Top),
            SkyStop(fraction = 0.12f, role = SkyRole.Top),
            SkyStop(fraction = 0.34f, role = SkyRole.Mid),
            SkyStop(fraction = 0.66f, role = SkyRole.Low),
            SkyStop(fraction = 1.00f, role = SkyRole.Deep),
        )

    /** The game screen's sky: Home's values, kept as a constant of its own (one owner per constant). */
    val QUIET_SKY_STOPS: List<SkyStop> =
        listOf(
            SkyStop(fraction = 0.00f, role = SkyRole.Top),
            SkyStop(fraction = 0.12f, role = SkyRole.Top),
            SkyStop(fraction = 0.34f, role = SkyRole.Mid),
            SkyStop(fraction = 0.66f, role = SkyRole.Low),
            SkyStop(fraction = 1.00f, role = SkyRole.Deep),
        )

    /** The Sparse wash: the background held to 0.12 H, then a blend to `SparseLow` (B-23). */
    val SPARSE_SKY_STOPS: List<SkyStop> =
        listOf(
            SkyStop(fraction = 0.00f, role = SkyRole.Top),
            SkyStop(fraction = 0.12f, role = SkyRole.Top),
            SkyStop(fraction = 1.00f, role = SkyRole.SparseLow),
        )

    /** Where Home's calm band ends: above it the sky alone, at every pose (B-20). */
    const val LANDSCAPE_CALM_END = 0.14f

    /** Where the game screen's calm band ends (B-20). */
    const val QUIET_CALM_END = 0.14f

    /** The lowest a wrapped Home title can reach; only lane lights rise above it, and no wave (B-21). */
    const val TITLE_REACH = 0.47f

    /** The band where the Challenge loading spinner sits; no game-screen light ever enters it (B-21). */
    val QUIET_SPINNER_BAND: ClosedFloatingPointRange<Float> = 0.33f..0.59f

    /** A wave ellipse's semi-axis across, as a share of W. */
    const val WAVE_RX = 0.75f

    /** A wave ellipse's semi-axis down, as a share of H. */
    const val WAVE_RY = 0.112f

    /** How far below the crest a wave's fill reaches its deep colour, as a share of H. */
    const val WAVE_DEPTH = 0.70f

    /** Where between the crest and [WAVE_DEPTH] the fill takes its body colour. */
    const val WAVE_BODY_STOP = 0.18f

    /** The back wave's per-draw alpha: each of its fill, lip and sheen, with no offscreen layer. */
    const val WAVE_BACK_ALPHA = 0.60f

    /** Half the width of a wave's sheen, as a share of W. */
    const val WAVE_SHEEN_HALF_WIDTH = 0.42f

    /** Home's back wave, then front wave. */
    val LANDSCAPE_WAVE_BACK = WavePlacement(cx = 0.50f, crest = 0.49f)
    val LANDSCAPE_WAVE_FRONT = WavePlacement(cx = 0.35f, crest = 0.62f)

    /** The game screen's back wave, then front wave: below the slab and behind the dock. */
    val QUIET_WAVE_BACK = WavePlacement(cx = 0.50f, crest = 0.72f)
    val QUIET_WAVE_FRONT = WavePlacement(cx = 0.35f, crest = 0.80f)

    /** A light's radial gradient: its core colour to this share of the radius. */
    const val LIGHT_CORE_STOP = 0.64f

    /** A light's radial gradient: its rim colour here, the light's extreme point; transparent at the edge. */
    const val LIGHT_RIM_STOP = 0.84f

    /** A light fades in over the first share of its phase... */
    const val LIGHT_FADE_IN = 0.15f

    /** ...and out from this share on, so it wraps from top to bottom only while invisible. */
    const val LIGHT_FADE_OUT = 0.85f

    /** Home's nine lights, H1 to H9 (candy-backdrop.md §3.3); periods in [BackdropMotion]. */
    val LANDSCAPE_LIGHTS: List<LightSpec> =
        listOf(
            LightSpec(0.08f, 0.27f, LightSize.Md, LightKind.Pearl, 0.13f, 0.90f, LightSet.Lane),
            LightSpec(0.22f, 0.40f, LightSize.Xs, LightKind.Pink, 0.67f, 0.85f, LightSet.Lane),
            LightSpec(0.50f, 0.33f, LightSize.Xl, LightKind.Violet, 0.30f, 0.70f, LightSet.Lane),
            LightSpec(0.76f, 0.22f, LightSize.Sm, LightKind.Pink, 0.42f, 0.85f, LightSet.Lane),
            LightSpec(0.92f, 0.37f, LightSize.Lg, LightKind.Violet, 0.06f, 0.75f, LightSet.Lane),
            LightSpec(0.39f, 0.91f, LightSize.Xxl, LightKind.Violet, 0.20f, 0.55f, LightSet.Low),
            LightSpec(0.92f, 0.90f, LightSize.Sm, LightKind.Pink, 0.50f, 0.80f, LightSet.Low),
            LightSpec(0.16f, 0.67f, LightSize.Md, LightKind.Pearl, 0.19f, 0.60f, LightSet.Low),
            LightSpec(0.50f, 0.94f, LightSize.Xs, LightKind.Pearl, 0.41f, 0.80f, LightSet.Low),
        )

    /** The game screen's five lights, G1 to G5 (candy-backdrop.md §3.3); periods in [BackdropMotion]. */
    val QUIET_LIGHTS: List<LightSpec> =
        listOf(
            LightSpec(0.13f, 0.19f, LightSize.Sm, LightKind.Pearl, 0.14f, 0.80f, LightSet.Upper),
            LightSpec(0.81f, 0.23f, LightSize.Lg, LightKind.Violet, 0.43f, 0.70f, LightSet.Upper),
            LightSpec(0.46f, 0.25f, LightSize.Xs, LightKind.Pink, 0.33f, 0.80f, LightSet.Upper),
            LightSpec(0.24f, 0.77f, LightSize.Lg, LightKind.Violet, 0.27f, 0.60f, LightSet.Lower),
            LightSpec(0.82f, 0.79f, LightSize.Md, LightKind.Pink, 0.63f, 0.80f, LightSet.Lower),
        )

    /** Where a glossed container's white gloss has faded to transparent, as a share of its height. */
    const val FINISH_GLOSS_FADE = 0.46f

    /** The gel sheen band's width, as a share of the tile's width. */
    const val GEL_SHEEN_WIDTH = 0.30f

    /** The gel sheen band's lean, its top toward the end edge, in degrees. */
    const val GEL_SHEEN_SKEW_DEG = 14f

    /** The share of its 10 s loop after which the gel sheen passes. */
    const val GEL_SHEEN_START = 0.75f

    /** The backdrop host is drawn behind the `NavHost` it is composed after (the tech-lead's R1). */
    const val BACKDROP_Z_INDEX = -1f
}
