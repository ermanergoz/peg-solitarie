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
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.Mid),
            SkyStop(fraction = 0f, role = SkyRole.Low),
            SkyStop(fraction = 0f, role = SkyRole.Deep),
        )

    /** The game screen's sky: Home's values, kept as a constant of its own (one owner per constant). */
    val QUIET_SKY_STOPS: List<SkyStop> =
        listOf(
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.Mid),
            SkyStop(fraction = 0f, role = SkyRole.Low),
            SkyStop(fraction = 0f, role = SkyRole.Deep),
        )

    /** The Sparse wash: the background held to 0.12 H, then a blend to `SparseLow` (B-23). */
    val SPARSE_SKY_STOPS: List<SkyStop> =
        listOf(
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.Top),
            SkyStop(fraction = 0f, role = SkyRole.SparseLow),
        )

    /** Where Home's calm band ends: above it the sky alone, at every pose (B-20). */
    const val LANDSCAPE_CALM_END = 0f

    /** Where the game screen's calm band ends (B-20). */
    const val QUIET_CALM_END = 0f

    /** The lowest a wrapped Home title can reach; only lane lights rise above it, and no wave (B-21). */
    const val TITLE_REACH = 0f

    /** The band where the Challenge loading spinner sits; no game-screen light ever enters it (B-21). */
    val QUIET_SPINNER_BAND: ClosedFloatingPointRange<Float> = 0f..0.59f

    /** A wave ellipse's semi-axis across, as a share of W. */
    const val WAVE_RX = 0f

    /** A wave ellipse's semi-axis down, as a share of H. */
    const val WAVE_RY = 0f

    /** How far below the crest a wave's fill reaches its deep colour, as a share of H. */
    const val WAVE_DEPTH = 0f

    /** Where between the crest and [WAVE_DEPTH] the fill takes its body colour. */
    const val WAVE_BODY_STOP = 0f

    /** The back wave's per-draw alpha: each of its fill, lip and sheen, with no offscreen layer. */
    const val WAVE_BACK_ALPHA = 0f

    /** Half the width of a wave's sheen, as a share of W. */
    const val WAVE_SHEEN_HALF_WIDTH = 0f

    /** Home's back wave, then front wave. */
    val LANDSCAPE_WAVE_BACK = WavePlacement(cx = 0f, crest = 0f)
    val LANDSCAPE_WAVE_FRONT = WavePlacement(cx = 0f, crest = 0f)

    /** The game screen's back wave, then front wave: below the slab and behind the dock. */
    val QUIET_WAVE_BACK = WavePlacement(cx = 0f, crest = 0f)
    val QUIET_WAVE_FRONT = WavePlacement(cx = 0f, crest = 0f)

    /** A light's radial gradient: its core colour to this share of the radius. */
    const val LIGHT_CORE_STOP = 0f

    /** A light's radial gradient: its rim colour here, the light's extreme point; transparent at the edge. */
    const val LIGHT_RIM_STOP = 0f

    /** A light fades in over the first share of its phase... */
    const val LIGHT_FADE_IN = 0f

    /** ...and out from this share on, so it wraps from top to bottom only while invisible. */
    const val LIGHT_FADE_OUT = 0f

    /** Home's nine lights, H1 to H9 (candy-backdrop.md §3.3); periods in [BackdropMotion]. */
    val LANDSCAPE_LIGHTS: List<LightSpec> = emptyList()

    /** The game screen's five lights, G1 to G5 (candy-backdrop.md §3.3); periods in [BackdropMotion]. */
    val QUIET_LIGHTS: List<LightSpec> = emptyList()

    /** Where a glossed container's white gloss has faded to transparent, as a share of its height. */
    const val FINISH_GLOSS_FADE = 0f

    /** The gel sheen band's width, as a share of the tile's width. */
    const val GEL_SHEEN_WIDTH = 0f

    /** The gel sheen band's lean, its top toward the end edge, in degrees. */
    const val GEL_SHEEN_SKEW_DEG = 0f

    /** The share of its 10 s loop after which the gel sheen passes. */
    const val GEL_SHEEN_START = 0f

    /** The backdrop host is drawn behind the `NavHost` it is composed after (the tech-lead's R1). */
    const val BACKDROP_Z_INDEX = -0f
}
