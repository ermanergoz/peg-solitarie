package com.app.pegsolitaire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * Rendering follows the UI-SPEC per-cell table (holes, pegs, blocked cells, the
 * selected ring, legal-destination dots) using only `MaterialTheme.colorScheme`
 * roles. A jump animates the jumper straight from source to destination (D-07)
 * while the jumped peg fades + shrinks (D-08), both keyed on the move id so a new
 * move restarts the animation cleanly; [onAnimationFinished] fires when the slide
 * lands (the move is already committed to domain state).
 *
 * @param state the immutable board state to render (single source of truth).
 * @param onCellTap emits the bounds-checked tapped [Position] up to the ViewModel.
 * @param onAnimationFinished invoked once the jump slide completes.
 * @param modifier layout modifier for the (square) canvas.
 */
@Composable
fun BoardCanvas(
    state: GameUiState,
    onCellTap: (Position) -> Unit,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = state.board.spec
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val geometry = remember(spec, canvasSize) { BoardGeometry.forCanvas(spec, canvasSize) }

    // Colors + stroke widths are resolved in composition; DrawScope cannot read
    // MaterialTheme or resources, so they are captured into a palette and passed in.
    // Stroke dimens are converted dp → px here (DrawScope draws in pixels).
    val density = LocalDensity.current
    val palette =
        BoardPalette(
            peg = MaterialTheme.colorScheme.secondary,
            holeFill = MaterialTheme.colorScheme.surfaceContainerLowest,
            holeOutline = MaterialTheme.colorScheme.outlineVariant,
            accent = MaterialTheme.colorScheme.primary,
            ringWidthPx = with(density) { dimensionResource(R.dimen.peg_selected_ring_width).toPx() },
            holeOutlinePx = with(density) { dimensionResource(R.dimen.hole_outline_width).toPx() },
        )

    val transition = state.animating
    val jumpOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val removalProgress = remember { Animatable(REMOVAL_START) }

    LaunchedEffect(transition?.id, geometry) {
        if (transition != null && geometry.isReady) {
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
            onAnimationFinished()
        }
    }

    Canvas(
        modifier =
            modifier
                .aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO)
                .testTag(BOARD_CANVAS_TEST_TAG)
                .onSizeChanged { canvasSize = it.toSize() }
                .pointerInput(geometry) {
                    detectTapGestures { offset -> geometry.positionAt(offset)?.let(onCellTap) }
                },
    ) {
        drawBoard(
            state = state,
            geometry = geometry,
            palette = palette,
            jumpOffset = { jumpOffset.value },
            removalProgress = { removalProgress.value },
        )
    }
}

/** Resolved theme colors + pixel stroke widths handed to the draw phase. */
private class BoardPalette(
    val peg: Color,
    val holeFill: Color,
    val holeOutline: Color,
    val accent: Color,
    val ringWidthPx: Float,
    val holeOutlinePx: Float,
)

/**
 * Draws the whole board for one frame. Animated values are read through the
 * [jumpOffset]/[removalProgress] lambdas so only the draw phase re-runs while the
 * jump plays (RESEARCH Pitfall 7/8).
 */
private fun DrawScope.drawBoard(
    state: GameUiState,
    geometry: BoardGeometry,
    palette: BoardPalette,
    jumpOffset: () -> Offset,
    removalProgress: () -> Float,
) {
    if (!geometry.isReady) return
    val board = state.board
    val transition = state.animating
    val jumperLanding = transition?.to

    for (position in board.spec.inPlay) {
        val center = geometry.center(position)
        when (board.cellAt(position)) {
            Cell.Empty -> {
                drawHole(center, geometry, palette)
                if (position in state.legalTargets) {
                    drawCircle(
                        color = palette.accent.copy(alpha = BoardMetrics.DESTINATION_DOT_ALPHA),
                        radius = geometry.destinationDotRadius,
                        center = center,
                    )
                }
            }

            Cell.Peg ->
                when {
                    // The in-flight jumper lands here; draw the hole, the moving peg is an overlay.
                    position == jumperLanding -> drawHole(center, geometry, palette)
                    position == state.selected -> drawSelectedPeg(center, geometry, palette)
                    else -> drawCircle(color = palette.peg, radius = geometry.pegRadius, center = center)
                }

            Cell.Blocked -> Unit
        }
    }

    if (transition != null) {
        transition.removed.let { removed ->
            val progress = removalProgress()
            val scaleFactor =
                BoardMetrics.REMOVAL_SCALE_TARGET +
                    (REMOVAL_START - BoardMetrics.REMOVAL_SCALE_TARGET) * progress
            drawCircle(
                color = palette.peg.copy(alpha = progress),
                radius = geometry.pegRadius * scaleFactor,
                center = geometry.center(removed),
            )
        }
        drawCircle(color = palette.peg, radius = geometry.pegRadius, center = jumpOffset())
    }
}

/** A recessed empty hole: a filled disc plus a thin outline ring. */
private fun DrawScope.drawHole(
    center: Offset,
    geometry: BoardGeometry,
    palette: BoardPalette,
) {
    drawCircle(color = palette.holeFill, radius = geometry.holeRadius, center = center)
    drawCircle(
        color = palette.holeOutline,
        radius = geometry.holeRadius,
        center = center,
        style = Stroke(width = palette.holeOutlinePx),
    )
}

/** The selected peg: slightly enlarged fill plus the accent selection ring. */
private fun DrawScope.drawSelectedPeg(
    center: Offset,
    geometry: BoardGeometry,
    palette: BoardPalette,
) {
    drawCircle(
        color = palette.peg,
        radius = geometry.pegRadius * BoardMetrics.SELECTED_PEG_SCALE,
        center = center,
    )
    drawCircle(
        color = palette.accent,
        radius = geometry.selectedRingRadius,
        center = center,
        style = Stroke(width = palette.ringWidthPx),
    )
}

/** Removal animation end state: the jumped peg has fully faded + shrunk away. */
private const val REMOVAL_END = 0f
