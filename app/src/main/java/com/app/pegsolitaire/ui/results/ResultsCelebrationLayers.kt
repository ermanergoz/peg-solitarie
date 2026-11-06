package com.app.pegsolitaire.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.toSize
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.backdrop.BackdropClock
import com.app.pegsolitaire.ui.backdrop.BackdropClockDriver
import com.app.pegsolitaire.ui.backdrop.BackdropMotion
import com.app.pegsolitaire.ui.backdrop.InfiniteBackdropFrames
import com.app.pegsolitaire.ui.backdrop.LiveBackdropClock
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.LocalClayColors
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlin.math.cos
import kotlin.math.sin

/** Test tag of the Results rays layer (ux-spec §4.7). Never announced. */
const val RESULTS_RAYS_TEST_TAG = "results_rays"

/** Test tag of the Results confetti layer, present only while a fall runs (ux-spec §4.7). */
const val RESULTS_CONFETTI_TEST_TAG = "results_confetti"

/** Whether the rays move or are drawn at their rest pose, for the semantics hook. */
enum class ResultsRaysPose {
    /** On the clock: turning and breathing. */
    Live,

    /** The rest pose: animations off or battery saver. */
    Static,
}

/** Semantics, test-only: the rays' pose kind (the `EarnedStarCountKey` idiom). TalkBack does not read it. */
val ResultsRaysPoseKey = SemanticsPropertyKey<ResultsRaysPose>("ResultsRaysPose")

/** Semantics, test-only: the confetti burst's seed while the layer is composed. */
val ResultsConfettiSeedKey = SemanticsPropertyKey<Int>("ResultsConfettiSeed")

private var SemanticsPropertyReceiver.raysPoseValue by ResultsRaysPoseKey
private var SemanticsPropertyReceiver.confettiSeedValue by ResultsConfettiSeedKey

/**
 * Where the rays' centre comes from (ux-spec §4.7, "Centre"): the star row reports its centre and the
 * layer its origin, both in root coordinates, so the burst follows the row as the column scrolls.
 * Snapshot state written only on layout changes; the layer's draw cache reads it.
 */
@Stable
internal class RaysAnchor {
    var rowCentreInRoot by mutableStateOf(Offset.Unspecified)
    var layerOriginInRoot by mutableStateOf(Offset.Unspecified)
}

/**
 * Reports this node's centre in root coordinates to [onCentre] on every layout, for the rays'
 * [RaysAnchor] (ux-spec §4.7, "Centre": the burst follows the star row as the column scrolls).
 * The centre is the node's own, unclipped: `boundsInRoot()` clips to the scrolling column, so a
 * part-scrolled row would report its visible slice's centre and a scrolled-out one `Rect.Zero`'s.
 */
internal fun Modifier.reportCentreInRoot(onCentre: (Offset) -> Unit): Modifier =
    onGloballyPositioned { onCentre(it.positionInRoot() + it.size.toSize().center) }

/**
 * The rays' own clock (D-RC-3): the backdrop's clock type, created by the screen and never the
 * host's, idling after the game screen's [BackdropMotion.BACKDROP_GAME_IDLE_MS] on the Sparse screen
 * where the host's clock rests at once. [BackdropClockDriver] runs it with the backdrop's rules:
 * RESUMED only, the rest pose with animations off or in battery saver.
 */
@Composable
internal fun rememberResultsRaysClock(): BackdropClock {
    val clock = remember { LiveBackdropClock(fixedIdleDelayMs = BackdropMotion.BACKDROP_GAME_IDLE_MS) }
    val alwaysReady = remember { mutableStateOf(true) }
    BackdropClockDriver(clock = clock, sceneReady = alwaysReady, frames = InfiniteBackdropFrames)
    return clock
}

/**
 * Layer 1 of Results (ux-spec D-RC-1 to D-RC-3, §4.7): twelve gold wedges from the star row's centre,
 * filled by one radial brush, turning and breathing on [clock] in the draw phase only, so a frame of
 * motion re-records one draw and recomposes nothing. With animations off or in battery saver it draws
 * the rest pose from its first frame. No description, no role, no pointer input.
 */
@Composable
internal fun ResultsRaysLayer(
    clock: BackdropClock,
    anchor: RaysAnchor,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val entrance = remember { Animatable(if (animationsEnabled) HIDDEN else SHOWN) }
    LaunchedEffect(Unit) {
        entrance.animateTo(SHOWN, tween(ClayAnim.ENTRANCE_MS, easing = ClayAnim.ENTRANCE_EASING))
    }
    val colour = LocalClayColors.current.celebrationRay
    val pose = if (!animationsEnabled || clock.isStatic) ResultsRaysPose.Static else ResultsRaysPose.Live
    Spacer(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { anchor.layerOriginInRoot = it.positionInRoot() }
                .clearAndSetSemantics {
                    testTag = RESULTS_RAYS_TEST_TAG
                    raysPoseValue = pose
                }.drawWithCache {
                    val centre = RaysGeometry.centre(anchor.rowCentreInRoot, anchor.layerOriginInRoot, size)
                    val reach = RaysGeometry.reach(centre, size)
                    val wedges = List(ResultsRays.WEDGES) { wedgePath(it, centre, reach) }
                    val brush =
                        Brush.radialGradient(
                            0f to colour,
                            ResultsRays.CORE_STOP to colour,
                            1f to colour.copy(alpha = 0f),
                            center = centre,
                            radius = CelebrationFx.RAY_FADE_RADIUS_FRACTION * size.height,
                        )
                    onDrawBehind {
                        val isStatic = !animationsEnabled || clock.isStatic
                        val time = if (isStatic) 0L else clock.timeMs
                        val layerAlpha = ResultsRays.layerAlpha(entrance.value, isStatic)
                        rotate(ResultsRays.angle(time, isStatic), pivot = centre) {
                            for (wedge in wedges.indices) {
                                drawPath(
                                    path = wedges[wedge],
                                    brush = brush,
                                    alpha = layerAlpha * ResultsRays.wedgeAlpha(time, wedge, isStatic),
                                )
                            }
                        }
                    }
                },
    )
}

/** Wedge [wedge] at angle 0: a hard-edged triangle from [centre] out to [reach]. */
private fun wedgePath(
    wedge: Int,
    centre: Offset,
    reach: Float,
): Path =
    Path().apply {
        moveTo(centre.x, centre.y)
        lineToward(centre, ResultsRays.startEdgeDegrees(wedge), reach)
        lineToward(centre, ResultsRays.endEdgeDegrees(wedge), reach)
        close()
    }

/** A line to the point [reach] from [centre] at [degrees] clockwise from straight up. */
private fun Path.lineToward(
    centre: Offset,
    degrees: Float,
    reach: Float,
) {
    val radians = Math.toRadians(degrees.toDouble())
    lineTo(centre.x + reach * sin(radians).toFloat(), centre.y - reach * cos(radians).toFloat())
}

/**
 * Layer 2 of Results (ux-spec D-RC-4, D-RC-5, §4.7): at three stars, once per result, the seeded fall,
 * starting as the third star pops ([CelebrationFx.CONFETTI_START_MS] of animator-scaled time after the
 * screen's first frame) and leaving the composition [CelebrationFx.CONFETTI_TOTAL_MS] later.
 *
 * It plays when [ResultsUiState.playCelebration] (D-16's latch) is set and this screen's own saved
 * flag is not — the `ResultsSummaryAnnouncement` idiom — so a rotation, a theme switch, a return onto
 * the entry or a restore never replays it, and a recreation mid-fall ends it. With animations off it
 * is not composed; a fall under way when they switch off ends on that frame and none starts again.
 */
@Composable
internal fun ResultsConfettiLayer(
    state: ResultsUiState,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var played by rememberSaveable { mutableStateOf(false) }
    val eligible =
        animationsEnabled &&
            state.playCelebration &&
            state.stars == CelebrationFx.TOTAL_STARS &&
            state.headlineTier == ResultsHeadlineTier.PERFECT
    val elapsed = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(eligible) {
        if (!eligible) {
            running = false
            return@LaunchedEffect
        }
        if (played) return@LaunchedEffect
        played = true
        running = true
        elapsed.animateTo(CONFETTI_END_MS.toFloat(), tween(CONFETTI_END_MS, easing = LinearEasing))
        running = false
    }
    val falling by remember {
        derivedStateOf {
            running && elapsed.value >= CelebrationFx.CONFETTI_START_MS && elapsed.value < CONFETTI_END_MS
        }
    }
    if (eligible && falling) {
        ConfettiCanvas(
            seed = confettiSeed(state.replayId, state.challengeLevel),
            elapsedMs = { elapsed.value - CelebrationFx.CONFETTI_START_MS },
            modifier = modifier,
        )
    }
}

/**
 * The fall itself: one draw per frame, one progress read, and per piece one transform and one
 * `drawRect` or `drawCircle`. The burst is drawn from the seed once, when the canvas composes.
 */
@Composable
private fun ConfettiCanvas(
    seed: Int,
    elapsedMs: () -> Float,
    modifier: Modifier,
) {
    val sizeMin = dimensionResource(R.dimen.confetti_piece_size_min)
    val sizeMax = dimensionResource(R.dimen.confetti_piece_size_max)
    val swayMin = dimensionResource(R.dimen.confetti_sway_min)
    val swayMax = dimensionResource(R.dimen.confetti_sway_max)
    val span =
        with(LocalDensity.current) {
            ConfettiSpan(sizeMin.toPx(), sizeMax.toPx(), swayMin.toPx(), swayMax.toPx())
        }
    val pieces = remember(seed, span) { ConfettiBurst.create(seed, CelebrationFx.CONFETTI_PIECE_COUNT, span) }
    val palette = LocalClayColors.current.confettiPalette
    Spacer(
        modifier =
            modifier
                .fillMaxSize()
                .clearAndSetSemantics {
                    testTag = RESULTS_CONFETTI_TEST_TAG
                    confettiSeedValue = seed
                }.drawBehind {
                    val time = elapsedMs()
                    for (index in pieces.indices) {
                        val piece = pieces[index]
                        if (piece.isFallingAt(time)) drawPiece(piece, time, palette[piece.colourIndex])
                    }
                },
    )
}

/** One piece at [time]: translated to its centre, tumbled, flipped, then its shape. */
private fun DrawScope.drawPiece(
    piece: ConfettiPiece,
    time: Float,
    colour: Color,
) {
    val edge = piece.sizePx
    val alpha = piece.alphaAt(time)
    withTransform({
        translate(piece.xAt(time, size.width), piece.yAt(time, size.height))
        rotate(piece.rotationAt(time), pivot = Offset.Zero)
        scale(piece.scaleXAt(time), 1f, pivot = Offset.Zero)
    }) {
        when (piece.shape) {
            ConfettiShape.Strip -> {
                val short = edge * CelebrationFx.CONFETTI_STRIP_ASPECT
                drawRect(colour, topLeft = Offset(-edge / 2, -short / 2), size = Size(edge, short), alpha = alpha)
            }
            ConfettiShape.Square -> {
                val side = edge * CelebrationFx.CONFETTI_SQUARE_SCALE
                drawRect(colour, topLeft = Offset(-side / 2, -side / 2), size = Size(side, side), alpha = alpha)
            }
            ConfettiShape.Disc ->
                drawCircle(
                    colour,
                    radius = edge * CelebrationFx.CONFETTI_DISC_SCALE / 2,
                    center = Offset.Zero,
                    alpha = alpha,
                )
        }
    }
}

/** When the confetti layer leaves the composition, counted from the screen's first frame. */
private const val CONFETTI_END_MS = CelebrationFx.CONFETTI_START_MS + CelebrationFx.CONFETTI_TOTAL_MS

/** The rays' entrance endpoints. */
private const val HIDDEN = 0f
private const val SHOWN = 1f
