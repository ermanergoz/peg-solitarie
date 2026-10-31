package com.app.pegsolitaire.ui.backdrop

import android.content.res.Configuration
import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BackdropColors
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.LocalBackdropColors
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/** The backdrop root's test tag (backdrop spec §12). Never announced. */
const val BACKDROP_TEST_TAG = "backdrop"

/** A layer the backdrop draws, for the semantics hook (BD-07). */
enum class BackdropLayer { Sky, Waves, Lights }

/** Semantics: the density the backdrop shows — lets BD-06 assert it per destination. */
val BackdropDensityKey = SemanticsPropertyKey<BackdropDensity>("BackdropDensity")

/** Semantics: the layers drawn — lets BD-07 and BD-23 (a) assert them. */
val BackdropLayersKey = SemanticsPropertyKey<Set<BackdropLayer>>("BackdropLayers")

/** Semantics: the clock's state — lets BD-16 be asserted without counting frames (the tech-lead's E5). */
val BackdropMotionKey = SemanticsPropertyKey<BackdropMotionState>("BackdropMotion")

private var SemanticsPropertyReceiver.backdropDensityValue by BackdropDensityKey
private var SemanticsPropertyReceiver.backdropLayersValue by BackdropLayersKey
private var SemanticsPropertyReceiver.backdropMotionValue by BackdropMotionKey

/**
 * The backdrop behind every screen (backdrop spec §2–§3, direction B): the density's
 * sky, and on Home and the game screen two waves and the lights, drawn from `BackdropColors` and the
 * pure [buildSceneModel]. Stateless with respect to app state: it reads no route and no `UiState`,
 * only [density], the theme, the window and [LocalBackdropClock], whose time it reads in draw.
 *
 * **The sky is continuous across navigation** (B-13, the tech-lead's R1): a new density cross-fades
 * in over the old one, which stays opaque beneath until the new one is fully in, over
 * `ClayAnim.NAV_TRANSITION_MS`. A change during a fade promotes the incoming slot to the one beneath
 * at full alpha. With animations off the swap is instant.
 *
 * It announces nothing and takes no focus: its only semantics are a test tag and three test-only
 * properties (BD-15).
 */
@Composable
fun SceneBackdrop(
    density: BackdropDensity,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val ready = LocalBackdropSceneReady.current.value
    val clock = LocalBackdropClock.current
    var below by remember { mutableStateOf(density) }
    var above by remember { mutableStateOf<BackdropDensity?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(density, animationsEnabled) {
        above?.let { below = it }
        above = null
        if (density == below) return@LaunchedEffect
        if (!animationsEnabled) {
            below = density
            return@LaunchedEffect
        }
        fade.snapTo(0f)
        above = density
        fade.animateTo(1f, tween(ClayAnim.NAV_TRANSITION_MS, easing = LinearEasing))
        below = density
        above = null
    }
    val layers = if (ready && density.moves) SCENE_LAYERS else SKY_ONLY
    val motion = if (density.moves && !clock.isStatic) clock.motion else BackdropMotionState.AtRest
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clearAndSetSemantics {
                    testTag = BACKDROP_TEST_TAG
                    backdropDensityValue = density
                    backdropLayersValue = layers
                    backdropMotionValue = motion
                },
    ) {
        BackdropSlot(density = below, sceneReady = ready, clock = clock)
        val incoming = above
        if (incoming != null) {
            BackdropSlot(
                density = incoming,
                sceneReady = ready,
                clock = clock,
                modifier = Modifier.graphicsLayer { alpha = fade.value },
            )
        }
    }
}

/** One density's whole picture: its sky, and its scene once the gate is open. */
@Composable
private fun BackdropSlot(
    density: BackdropDensity,
    sceneReady: Boolean,
    clock: BackdropClock,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBackdropColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Spacer(
            modifier =
                Modifier.fillMaxSize().drawWithCache {
                    val brush = skyBrush(skyStopsOf(density), colors, size)
                    onDrawBehind { drawRect(brush) }
                },
        )
        if (sceneReady && density.moves) BackdropScene(density = density, clock = clock, colors = colors)
    }
}

/**
 * The waves and lights in one node (the tech-lead's E11): every path and brush is built once per
 * size, theme, density and direction in the cache block, and the draw block only translates, sets
 * an alpha and replays, allocating nothing. The time is read here, in draw, so a frame of motion
 * re-records this node alone.
 */
@Composable
private fun BackdropScene(
    density: BackdropDensity,
    clock: BackdropClock,
    colors: BackdropColors,
) {
    val dimens = rememberBackdropDimens()
    Spacer(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer()
                .drawWithCache {
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    val model = buildSceneModel(density, size.width, size.height, rtl, dimens)
                    val waves = model.waves.map { WaveDrawing.of(it, colors) }
                    val lights = model.lights.map { LightDrawing.of(it, colors) }
                    onDrawBehind {
                        val static = clock.isStatic
                        val t = clock.timeMs
                        for (i in waves.indices) {
                            val wave = waves[i]
                            val ox = if (static) WAVE_REST_OFFSET else waveOffsetX(wave.model, t)
                            val oy = if (static) WAVE_REST_OFFSET else waveOffsetY(wave.model, t)
                            translate(ox, oy) {
                                drawPath(wave.shape, wave.fill, alpha = wave.model.alpha)
                                drawPath(wave.lip, colors.waveLip, alpha = wave.model.alpha)
                                drawPath(wave.sheenPath, wave.sheen, alpha = wave.model.alpha)
                            }
                        }
                        for (i in lights.indices) {
                            val light = lights[i]
                            val dy = if (static) LIGHT_REST_DY else lightDy(model, light.model, t)
                            val alpha =
                                if (static) lightRestAlpha(model, light.model) else lightAlpha(model, light.model, t)
                            if (alpha > 0f) {
                                translate(light.model.centerX, light.model.centerY + dy) {
                                    drawCircle(light.brush, light.radius, center = Offset.Zero, alpha = alpha)
                                }
                            }
                        }
                    }
                },
    )
}

/** A wave's cached drawing at its rest pose: the shape, its lip band, its clipped sheen and their brushes. */
private class WaveDrawing(
    val model: WaveModel,
    val shape: Path,
    val lip: Path,
    val sheenPath: Path,
    val fill: Brush,
    val sheen: Brush,
) {
    companion object {
        fun of(
            model: WaveModel,
            colors: BackdropColors,
        ): WaveDrawing {
            val oval = Rect(model.left, model.crestY, model.right, model.crestY + model.radiusY * 2f)
            val shape = waveShape(oval, model.crestY + model.radiusY, model.bottom)
            val centreLine = model.crestY + model.radiusY
            val lowered = waveShape(oval.translate(0f, model.lipDepth), centreLine + model.lipDepth, model.bottom)
            val lip = Path().apply { op(shape, lowered, PathOperation.Difference) }
            val sheenCentre = Offset(model.crestX, model.sheenCenterY)
            val sheenOval =
                Rect(
                    left = sheenCentre.x - model.sheenRadiusX,
                    top = sheenCentre.y - model.sheenRadiusY,
                    right = sheenCentre.x + model.sheenRadiusX,
                    bottom = sheenCentre.y + model.sheenRadiusY,
                )
            // Clipped to the wave once, here, rather than with a clip in every frame's draw.
            val sheenEllipse = Path().apply { addOval(sheenOval) }
            val sheenPath = Path().apply { op(sheenEllipse, shape, PathOperation.Intersect) }
            val bodyFraction = (model.bodyStopY - model.crestY) / (model.deepStopY - model.crestY)
            val fill =
                Brush.verticalGradient(
                    0f to colors.waveCrest,
                    bodyFraction to colors.waveBody,
                    1f to colors.waveDeep,
                    startY = model.crestY,
                    endY = model.deepStopY,
                )
            val sheen = ellipticalRadialBrush(sheenCentre, model.sheenRadiusX, model.sheenRadiusY, colors.waveSheen)
            return WaveDrawing(model, shape, lip, sheenPath, fill, sheen)
        }

        private fun waveShape(
            oval: Rect,
            centreLine: Float,
            bottom: Float,
        ): Path {
            val ellipse = Path().apply { addOval(oval) }
            val body = Path().apply { addRect(Rect(oval.left, centreLine, oval.right, bottom)) }
            return Path().apply { op(ellipse, body, PathOperation.Union) }
        }
    }
}

/** A light's cached radial brush, centred on its own origin so the draw only translates it. */
private class LightDrawing(
    val model: LightModel,
    val brush: Brush,
) {
    val radius: Float = model.diameter / 2f

    companion object {
        fun of(
            model: LightModel,
            colors: BackdropColors,
        ): LightDrawing {
            val core = colors.lightCore(model.kind)
            val brush =
                Brush.radialGradient(
                    0f to core,
                    BackdropMetrics.LIGHT_CORE_STOP to core,
                    BackdropMetrics.LIGHT_RIM_STOP to colors.lightRim(model.kind),
                    1f to Color.Transparent,
                    center = Offset.Zero,
                    radius = model.diameter / 2f,
                )
            return LightDrawing(model, brush)
        }
    }
}

/**
 * An elliptical radial gradient, [colour] at its centre to transparent at its edge: a circular
 * gradient's shader scaled down its height about [center], the usual way to draw one in Compose.
 */
private fun ellipticalRadialBrush(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    colour: Color,
): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader =
            RadialGradientShader(center, radiusX, listOf(colour, Color.Transparent)).apply {
                setLocalMatrix(Matrix().apply { setScale(1f, radiusY / radiusX, center.x, center.y) })
            }
    }

private fun skyBrush(
    stops: List<SkyStop>,
    colors: BackdropColors,
    size: Size,
): Brush =
    Brush.verticalGradient(
        colorStops = Array(stops.size) { i -> stops[i].fraction to colors.sky(stops[i].role) },
        startY = 0f,
        endY = size.height,
    )

private val SKY_ONLY: Set<BackdropLayer> = setOf(BackdropLayer.Sky)
private val SCENE_LAYERS: Set<BackdropLayer> = setOf(BackdropLayer.Sky, BackdropLayer.Waves, BackdropLayer.Lights)

/** Each density, light and dark, at a fixed pose (BD-25). */
@Preview(name = "Backdrop · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Backdrop · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SceneBackdropLandscapePreview() {
    AppTheme { BackdropPreviewHost(BackdropDensity.Landscape) {} }
}

@Preview(name = "Backdrop quiet · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Backdrop quiet · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SceneBackdropQuietPreview() {
    AppTheme { BackdropPreviewHost(BackdropDensity.Quiet) {} }
}

@Preview(name = "Backdrop sparse · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Backdrop sparse · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SceneBackdropSparsePreview() {
    AppTheme { BackdropPreviewHost(BackdropDensity.Sparse) {} }
}

/** The 6.7-inch baseline window. */
private const val PREVIEW_WIDTH_DP = 412
private const val PREVIEW_HEIGHT_DP = 915
