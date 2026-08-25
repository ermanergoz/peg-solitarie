package com.app.pegsolitaire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.toSize
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.GameAnim
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.game.MoveTransition
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.LocalClayColors
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Semantics tag the screen + tests use to locate and drive the board surface. */
const val BOARD_CANVAS_TEST_TAG = "board_canvas"

/** Removal animation runs full → gone (1 → 0); a peg at full size/opacity is [REMOVAL_START]. */
private const val REMOVAL_START = 1f

/**
 * Stateless board leaf: renders [state] on a Compose `Canvas` and emits taps
 * upward via [onCellTap]. It computes **no** legality — the ViewModel owns all
 * rules (RESEARCH Anti-Pattern 1); the only judgement here is the geometric
 * bounds-check in [BoardGeometry.positionAt] (T-04-01/T-04-02).
 *
 * Candy-Clay rendering (DESIGN-SYSTEM.md Gameplay): pegs are radial-gradient
 * candy spheres with a small specular highlight; empty holes are soft two-tone
 * inset dimples; the selected peg breathes (1.0→1.06, ~900ms reverse-repeat);
 * legal-destination dots pop in with a spring overshoot; a jump slides straight
 * from source to destination (D-07) with a small landing squash while the jumped
 * peg fades + shrinks (D-08), all keyed on the move id so a new move restarts the
 * animation cleanly; [onAnimationFinished] fires when the slide lands (the move
 * is already committed to domain state). Colors reach the draw phase only
 * through the [BoardPalette] captured from `MaterialTheme.colorScheme` +
 * `LocalClayColors` — no hex here. When OS animations are off, decorative motion
 * (pulse, pops, squash) is skipped and the jump/removal resolve instantly.
 *
 * In read-only mode ([interactive] `= false`, the catalog mini-preview D-11) the leaf
 * renders only the resting pegs and the recessed start-empty hole: it suppresses tap
 * emission (no `pointerInput`), the selection ring, the legal-destination dots and all
 * animation. The default ([interactive] `= true`) keeps the full gameplay behavior.
 *
 * @param state the immutable board state to render (single source of truth).
 * @param onCellTap emits the bounds-checked tapped [Position] up to the ViewModel
 *   (never called when [interactive] is `false`).
 * @param onAnimationFinished invoked once the jump slide completes.
 * @param modifier layout modifier for the (square) canvas.
 * @param interactive whether the board accepts taps and draws the selection/destination/
 *   animation overlays; `false` for the static catalog preview.
 */
@Suppress("LongMethod") // One cohesive render unit: state capture + palette + the animation set.
@Composable
fun BoardCanvas(
    state: GameUiState,
    onCellTap: (Position) -> Unit,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val spec = state.board.spec
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val geometry = remember(spec, canvasSize) { BoardGeometry.forCanvas(spec, canvasSize) }

    // Colors + stroke widths are resolved in composition; DrawScope cannot read
    // MaterialTheme or resources, so they are captured into a palette and passed in.
    // Stroke dimens are converted dp → px here (DrawScope draws in pixels).
    val density = LocalDensity.current
    val clay = LocalClayColors.current
    val palette =
        BoardPalette(
            pegGradient = clay.pegGradient,
            pegHighlight = clay.pegHighlight,
            dimpleFill = clay.dimpleFill,
            dimpleShadow = clay.dimpleShadow,
            accent = MaterialTheme.colorScheme.primary,
            hint = MaterialTheme.colorScheme.tertiary,
            ringWidthPx = with(density) { dimensionResource(R.dimen.peg_selected_ring_width).toPx() },
        )

    val animationsEnabled = rememberAnimationsEnabled()
    val transition = state.animating
    val jumpOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val removalProgress = remember { Animatable(REMOVAL_START) }
    val landingSquash = remember { Animatable(SETTLED_SCALE) }
    var landing by remember { mutableStateOf<Position?>(null) }

    LaunchedEffect(transition?.id, geometry, interactive) {
        if (interactive && transition != null && geometry.isReady) {
            runJumpAnimation(
                transition = transition,
                geometry = geometry,
                animationsEnabled = animationsEnabled,
                jumpOffset = jumpOffset,
                removalProgress = removalProgress,
                landingSquash = landingSquash,
                setLanding = { landing = it },
                onAnimationFinished = onAnimationFinished,
            )
        }
    }

    val dotPop = remember { Animatable(GameAnim.DOT_POP_END) }
    LaunchedEffect(state.selected, interactive) {
        if (interactive && state.selected != null) {
            if (animationsEnabled) {
                dotPop.snapTo(GameAnim.DOT_POP_START)
                dotPop.animateTo(GameAnim.DOT_POP_END, ClayAnim.overshootSpring())
            } else {
                dotPop.snapTo(GameAnim.DOT_POP_END)
            }
        }
    }

    val pulseScale = rememberSelectionPulse(active = interactive && animationsEnabled && state.selected != null)
    val animations =
        BoardAnimationValues(
            jumpOffset = { jumpOffset.value },
            removalProgress = { removalProgress.value },
            dotPop = { dotPop.value },
            pulse = { pulseScale?.value ?: SETTLED_SCALE },
            landingScale = { landingSquash.value },
            landing = landing,
        )

    Canvas(
        modifier =
            modifier
                .aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO)
                .testTag(BOARD_CANVAS_TEST_TAG)
                .onSizeChanged { canvasSize = it.toSize() }
                .then(
                    // Read-only previews never attach a tap handler, so they can
                    // never emit onCellTap (the parent Card owns the tap).
                    if (interactive) {
                        Modifier.pointerInput(geometry) {
                            detectTapGestures { offset -> geometry.positionAt(offset)?.let(onCellTap) }
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        drawBoard(
            state = state,
            geometry = geometry,
            palette = palette,
            interactive = interactive,
            animations = animations,
        )
    }
}

/**
 * Runs the jump slide + removal fade for one committed move, then the landing
 * squash. With animations disabled everything resolves instantly (functional
 * transition, no decorative motion) and [onAnimationFinished] fires right away.
 */
private suspend fun runJumpAnimation(
    transition: MoveTransition,
    geometry: BoardGeometry,
    animationsEnabled: Boolean,
    jumpOffset: Animatable<Offset, *>,
    removalProgress: Animatable<Float, *>,
    landingSquash: Animatable<Float, *>,
    setLanding: (Position?) -> Unit,
    onAnimationFinished: () -> Unit,
) {
    if (!animationsEnabled) {
        jumpOffset.snapTo(geometry.center(transition.to))
        removalProgress.snapTo(REMOVAL_END)
        onAnimationFinished()
        return
    }
    coroutineScope {
        jumpOffset.snapTo(geometry.center(transition.from))
        removalProgress.snapTo(REMOVAL_START)
        launch {
            removalProgress.animateTo(
                targetValue = REMOVAL_END,
                animationSpec = tween(GameAnim.REMOVAL_MS, easing = GameAnim.REMOVAL_EASING),
            )
        }
        jumpOffset.animateTo(
            targetValue = geometry.center(transition.to),
            animationSpec = tween(GameAnim.JUMP_SLIDE_MS, easing = GameAnim.JUMP_SLIDE_EASING),
        )
        // The move is committed the moment the slide lands; the squash is pure
        // decoration on the settled peg, so the screen is notified first.
        setLanding(transition.to)
        onAnimationFinished()
        landingSquash.snapTo(BoardMetrics.LANDING_SQUASH_SCALE)
        landingSquash.animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())
        setLanding(null)
    }
}

/**
 * The selected peg's breathing pulse (scale 1.0→[BoardMetrics.PULSE_MAX_SCALE],
 * ~900ms, reverse-repeat), composed only while [active] so the infinite
 * transition never ticks without a selection.
 */
@Composable
private fun rememberSelectionPulse(active: Boolean): State<Float>? {
    if (!active) return null
    val pulse = rememberInfiniteTransition(label = "selectedPegPulse")
    return pulse.animateFloat(
        initialValue = SETTLED_SCALE,
        targetValue = BoardMetrics.PULSE_MAX_SCALE,
        animationSpec =
            infiniteRepeatable(
                animation = tween(GameAnim.PULSE_MS, easing = GameAnim.JUMP_SLIDE_EASING),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "selectedPegPulseScale",
    )
}

/** Resolved theme + clay colors and pixel stroke widths handed to the draw phase. */
private class BoardPalette(
    val pegGradient: List<Color>,
    val pegHighlight: Color,
    val dimpleFill: Color,
    val dimpleShadow: Color,
    val accent: Color,
    val hint: Color,
    val ringWidthPx: Float,
)

/**
 * Animated values read through lambdas so only the draw phase re-runs while an
 * animation plays (RESEARCH Pitfall 7/8), plus the cell currently landing.
 */
private class BoardAnimationValues(
    val jumpOffset: () -> Offset,
    val removalProgress: () -> Float,
    val dotPop: () -> Float,
    val pulse: () -> Float,
    val landingScale: () -> Float,
    val landing: Position?,
)

/** Draws the whole board for one frame. */
private fun DrawScope.drawBoard(
    state: GameUiState,
    geometry: BoardGeometry,
    palette: BoardPalette,
    interactive: Boolean,
    animations: BoardAnimationValues,
) {
    if (!geometry.isReady) return
    val board = state.board
    // Read-only previews never play an animation nor highlight a selection/targets,
    // so the in-flight jumper, the selection ring and the destination dots are all
    // suppressed regardless of the incoming state.
    val transition = if (interactive) state.animating else null
    val jumperLanding = transition?.to

    for (position in board.spec.inPlay) {
        drawCell(
            state = state,
            geometry = geometry,
            palette = palette,
            interactive = interactive,
            animations = animations,
            position = position,
            jumperLanding = jumperLanding,
        )
    }

    // Winning-move hint overlay (D-01): render-only source glow + target ring + faint A→B
    // arrow, in the `tertiary` accent. Suppressed in read-only previews (like selection/dots)
    // and it never performs the jump — the player still makes the move.
    val hintSource = state.hintSource
    val hintTarget = state.hintTarget
    if (interactive && hintSource != null && hintTarget != null) {
        drawHintHighlight(geometry, palette, hintSource, hintTarget)
    }

    if (transition != null) {
        transition.removed.let { removed ->
            val progress = animations.removalProgress()
            val scaleFactor =
                BoardMetrics.REMOVAL_SCALE_TARGET +
                    (REMOVAL_START - BoardMetrics.REMOVAL_SCALE_TARGET) * progress
            drawCandyPeg(
                palette = palette,
                center = geometry.center(removed),
                radius = geometry.pegRadius * scaleFactor,
                alpha = progress,
            )
        }
        drawCandyPeg(palette = palette, center = animations.jumpOffset(), radius = geometry.pegRadius)
    }
}

/**
 * Draws a single in-play cell: an empty dimple (plus the popped legal-destination dot
 * when [interactive] and this cell is a target), or a candy peg (the recessed landing
 * dimple under the in-flight jumper, the pulsing selected peg, the squashing
 * just-landed peg, or a resting peg). All interaction overlays are suppressed when
 * [interactive] is `false` (read-only preview).
 */
private fun DrawScope.drawCell(
    state: GameUiState,
    geometry: BoardGeometry,
    palette: BoardPalette,
    interactive: Boolean,
    animations: BoardAnimationValues,
    position: Position,
    jumperLanding: Position?,
) {
    val center = geometry.center(position)
    when (state.board.cellAt(position)) {
        Cell.Empty -> {
            drawDimple(center, geometry, palette)
            if (interactive && state.showHints && position in state.legalTargets) {
                drawCircle(
                    color = palette.accent.copy(alpha = BoardMetrics.DESTINATION_DOT_ALPHA),
                    radius = geometry.destinationDotRadius * animations.dotPop(),
                    center = center,
                )
            }
        }

        Cell.Peg ->
            when {
                // The in-flight jumper lands here; draw the dimple, the moving peg is an overlay.
                position == jumperLanding -> drawDimple(center, geometry, palette)
                interactive && position == state.selected ->
                    drawSelectedPeg(center, geometry, palette, animations.pulse())
                interactive && position == animations.landing ->
                    drawCandyPeg(
                        palette = palette,
                        center = center,
                        radius = geometry.pegRadius * animations.landingScale(),
                    )
                else -> drawCandyPeg(palette = palette, center = center, radius = geometry.pegRadius)
            }

        Cell.Blocked -> Unit
    }
}

/**
 * A candy-sphere peg: a radial PegGradient (highlight toward the top-left, rim at
 * the edge) plus a small specular highlight dot (DESIGN-SYSTEM.md Gameplay).
 */
private fun DrawScope.drawCandyPeg(
    palette: BoardPalette,
    center: Offset,
    radius: Float,
    alpha: Float = FULL_ALPHA,
) {
    val gradientCenter =
        center +
            Offset(
                -radius * BoardMetrics.PEG_GRADIENT_OFFSET_RATIO,
                -radius * BoardMetrics.PEG_GRADIENT_OFFSET_RATIO,
            )
    val brush =
        Brush.radialGradient(
            GRADIENT_START to palette.pegGradient[HIGHLIGHT_INDEX],
            BoardMetrics.PEG_GRADIENT_MID_STOP to palette.pegGradient[BODY_INDEX],
            GRADIENT_END to palette.pegGradient[RIM_INDEX],
            center = gradientCenter,
            radius = radius * BoardMetrics.PEG_GRADIENT_RADIUS_RATIO,
        )
    drawCircle(brush = brush, radius = radius, center = center, alpha = alpha)
    val specularOffset = radius * BoardMetrics.SPECULAR_OFFSET_RATIO
    drawCircle(
        color = palette.pegHighlight,
        radius = radius * BoardMetrics.SPECULAR_RADIUS_RATIO,
        center = center + Offset(-specularOffset, -specularOffset),
        alpha = alpha,
    )
}

/** A soft two-tone inset dimple: darker base ring + lighter fill nudged downward. */
private fun DrawScope.drawDimple(
    center: Offset,
    geometry: BoardGeometry,
    palette: BoardPalette,
) {
    drawCircle(color = palette.dimpleShadow, radius = geometry.holeRadius, center = center)
    drawCircle(
        color = palette.dimpleFill,
        radius = geometry.holeRadius * BoardMetrics.DIMPLE_INNER_RATIO,
        center = center + Offset(NO_OFFSET, geometry.holeRadius * BoardMetrics.DIMPLE_OFFSET_RATIO),
    )
}

/** The selected peg: enlarged candy sphere breathing at [pulse], plus the accent ring. */
private fun DrawScope.drawSelectedPeg(
    center: Offset,
    geometry: BoardGeometry,
    palette: BoardPalette,
    pulse: Float,
) {
    drawCandyPeg(
        palette = palette,
        center = center,
        radius = geometry.pegRadius * BoardMetrics.SELECTED_PEG_SCALE * pulse,
    )
    drawCircle(
        color = palette.accent,
        radius = geometry.selectedRingRadius * pulse,
        center = center,
        style = Stroke(width = palette.ringWidthPx),
    )
}

/**
 * Draws the revealed winning-move hint (D-01): a `tertiary` ring around the destination
 * hole (B), a slightly larger glow ring around the peg to move (A), and a faint A→B line
 * connecting them. Render-only — it reads [source]/[target] and never mutates game state
 * nor performs the jump; the caller suppresses it entirely in read-only previews.
 *
 * Draw order matches the UI-SPEC: target ring → source glow ring → arrow, so the source
 * glow sits above the connector where they overlap.
 */
private fun DrawScope.drawHintHighlight(
    geometry: BoardGeometry,
    palette: BoardPalette,
    source: Position,
    target: Position,
) {
    val sourceCenter = geometry.center(source)
    val targetCenter = geometry.center(target)
    val ringColor = palette.hint.copy(alpha = BoardMetrics.HINT_HIGHLIGHT_ALPHA)
    drawCircle(
        color = ringColor,
        radius = geometry.hintTargetRingRadius,
        center = targetCenter,
        style = Stroke(width = palette.ringWidthPx),
    )
    drawCircle(
        color = ringColor,
        radius = geometry.hintSourceRingRadius,
        center = sourceCenter,
        style = Stroke(width = palette.ringWidthPx),
    )
    drawLine(
        color = palette.hint.copy(alpha = BoardMetrics.HINT_ARROW_ALPHA),
        start = sourceCenter,
        end = targetCenter,
        strokeWidth = BoardMetrics.HINT_ARROW_WIDTH_RATIO * geometry.cellSize,
    )
}

/** Removal animation end state: the jumped peg has fully faded + shrunk away. */
private const val REMOVAL_END = 0f

/** A settled (non-animating) scale. */
private const val SETTLED_SCALE = 1f

/** Fully opaque draw. */
private const val FULL_ALPHA = 1f

/** No horizontal nudge for the dimple fill. */
private const val NO_OFFSET = 0f

/** Radial PegGradient stop fractions + the highlight/body/rim color indices. */
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 1f
private const val HIGHLIGHT_INDEX = 0
private const val BODY_INDEX = 1
private const val RIM_INDEX = 2
