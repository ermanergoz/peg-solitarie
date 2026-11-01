// The design contract names this file Clay.kt: the whole clay toolkit (palette holder,
// brushes, squish, shadow, entrance, motion gate) lives here as one cohesive unit.
@file:Suppress("MatchingDeclarationName")

package com.app.pegsolitaire.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R

/**
 * The Candy-Clay render toolkit (DESIGN-SYSTEM.md): per-theme gradient palettes
 * ([ClayColors] via [LocalClayColors]), gradient [Brush] providers, the shared
 * [claySquish] press interaction, the tinted soft-shadow helper ([clayShadow])
 * and the OS animator-scale gate ([rememberAnimationsEnabled]).
 *
 * Everything here is render-layer sugar: no logic, no state ownership, and no
 * hex — all colors flow from `ui/theme/Color.kt` through [ClayColors].
 */
@Immutable
data class ClayColors(
    /** `CtaGradient` stops (vertical) for primary buttons/tiles. */
    val ctaGradient: List<Color>,
    /** `PegGradient` stops (radial highlight → body → rim) for candy pegs. */
    val pegGradient: List<Color>,
    /** `GoldGradient` stops for stars/celebration accents. */
    val goldGradient: List<Color>,
    /** Small specular highlight dot on a candy peg sphere. */
    val pegHighlight: Color,
    /** Inner fill of the two-tone inset hole dimple. */
    val dimpleFill: Color,
    /** Darker top ring of the two-tone inset hole dimple. */
    val dimpleShadow: Color,
    /** Violet-tinted spot/ambient shadow color (never harsh black). */
    val shadowTint: Color,
)

internal val LightClayColors =
    ClayColors(
        ctaGradient = LightCtaGradientColors,
        pegGradient = LightPegGradientColors,
        goldGradient = LightGoldGradientColors,
        pegHighlight = LightPegHighlight,
        dimpleFill = LightDimpleFill,
        dimpleShadow = LightDimpleShadow,
        shadowTint = LightClayShadowTint,
    )

internal val DarkClayColors =
    ClayColors(
        ctaGradient = DarkCtaGradientColors,
        pegGradient = DarkPegGradientColors,
        goldGradient = DarkGoldGradientColors,
        pegHighlight = DarkPegHighlight,
        dimpleFill = DarkDimpleFill,
        dimpleShadow = DarkDimpleShadow,
        shadowTint = DarkClayShadowTint,
    )

/** The active clay palette, provided by `AppTheme` alongside the color scheme. */
val LocalClayColors = staticCompositionLocalOf { LightClayColors }

/**
 * The vertical `CtaGradient` brush. The dark stop is weighted to the midpoint
 * ([CTA_DARK_STOP]) so button labels sit on the high-contrast half (≥4.5:1 for
 * the on-gradient label) while the light lip still reads as clay depth.
 */
@Composable
fun ctaGradientBrush(): Brush {
    val stops = LocalClayColors.current.ctaGradient
    return Brush.verticalGradient(
        CTA_LIGHT_STOP to stops.first(),
        CTA_DARK_STOP to stops.last(),
    )
}

/** The linear `GoldGradient` brush for stars and celebration accents. */
@Composable
fun goldGradientBrush(): Brush = Brush.linearGradient(LocalClayColors.current.goldGradient)

/**
 * Recolors the wrapped content (e.g. a star [androidx.compose.material3.Icon])
 * with the `GoldGradient`, keeping the content's own alpha as the mask. Renders
 * offscreen so the blend never bleeds onto the background.
 */
@Composable
fun Modifier.goldGradientFill(): Modifier = gradientFill(goldGradientBrush())

/**
 * Recolors the wrapped content with [brush], the content's own alpha as the mask; offscreen so
 * nothing bleeds onto the background. [goldGradientFill]'s blend with the brush as a parameter, so
 * a star or pip in another palette's fill (docs/ux/colour-vision.md §3.4) needs no copy of it.
 */
internal fun Modifier.gradientFill(brush: Brush): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            }
        }

/**
 * The clay double-shadow feel: a normal Compose [shadow] whose spot + ambient
 * colors are tinted to the surface hue ([ClayColors.shadowTint]) instead of
 * harsh black (DESIGN-SYSTEM.md §Elevation).
 */
@Composable
fun Modifier.clayShadow(
    elevation: Dp,
    shape: Shape,
): Modifier {
    val tint = LocalClayColors.current.shadowTint
    return shadow(elevation = elevation, shape = shape, ambientColor = tint, spotColor = tint)
}

/**
 * The shared squish press: while pressed the surface scales to
 * [ClayAnim.PRESSED_SCALE]; on release it springs back with a MediumBouncy
 * spring (DESIGN-SYSTEM.md §Motion).
 *
 * It only *observes* pointer events (`awaitFirstDown(requireUnconsumed =
 * false)` + no consumption), so the host's own `clickable`/`onClick` semantics,
 * ripples and test `performClick` behavior are untouched. When the OS animator
 * scale is 0 the modifier is a no-op (decorative motion is skipped).
 */
fun Modifier.claySquish(): Modifier =
    composed {
        if (!rememberAnimationsEnabled()) {
            this
        } else {
            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (pressed) ClayAnim.PRESSED_SCALE else ClayAnim.IDLE_SCALE,
                animationSpec = if (pressed) tween(ClayAnim.PRESS_IN_MS) else ClayAnim.releaseSpring(),
                label = "claySquish",
            )
            graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
    }

/**
 * A staggered fade + slide-up entrance for the [order]-th item of a list/grid
 * ([ClayAnim.ENTRANCE_STAGGER_MS] per item). The delay rides the animation
 * spec's `delayMillis` (frame-clock driven, so tests stay deterministic). When
 * animations are off the content simply appears settled.
 */
@Composable
fun Modifier.clayEntrance(order: Int): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val distancePx = with(LocalDensity.current) { dimensionResource(R.dimen.entrance_slide_distance).toPx() }
    val progress = remember { Animatable(if (animationsEnabled) ENTRANCE_START else ENTRANCE_END) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = ENTRANCE_END,
            animationSpec =
                tween(
                    durationMillis = ClayAnim.ENTRANCE_MS,
                    delayMillis = order * ClayAnim.ENTRANCE_STAGGER_MS,
                    easing = ClayAnim.ENTRANCE_EASING,
                ),
        )
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (ENTRANCE_END - progress.value) * distancePx
    }
}

/**
 * Whether decorative animations should run: `false` while the animation scale
 * is 0, as the system reports it with "Remove animations" on — the contract's
 * reduced-motion gate. Functional transitions still occur instantly.
 *
 * The scale is the [MotionDurationScale] of the composition's own coroutine
 * context, the one every `Animatable` in it runs on, so the gate and the
 * animation clock are one reader of one value and cannot disagree. On a device
 * it is Compose's window scale, which follows
 * `Settings.Global.ANIMATOR_DURATION_SCALE` through Compose's own observer. It is
 * snapshot state, so a toggle with the app resident recomposes every reader and
 * the next wrong tap, star or entrance takes the path the setting says now. A
 * composition with no scale in its context (a preview, a test that provides
 * none) runs its animations at [DEFAULT_ANIMATOR_SCALE], and the gate is on.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val motionScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    return (motionScale?.scaleFactor ?: DEFAULT_ANIMATOR_SCALE) != DISABLED_ANIMATOR_SCALE
}

/** The gradient fraction where the CTA light lip ends. */
private const val CTA_LIGHT_STOP = 0f

/** The gradient fraction from which the CTA dark body runs (label sits here). */
private const val CTA_DARK_STOP = 0.5f

/** Entrance progress end-points (0 = hidden/offset, 1 = settled). */
private const val ENTRANCE_START = 0f
private const val ENTRANCE_END = 1f

/** The scale animations run at when a composition carries none: Compose's default, and the OS default. */
private const val DEFAULT_ANIMATOR_SCALE = 1f

/** Animator scale value meaning "animations disabled". */
private const val DISABLED_ANIMATOR_SCALE = 0f
