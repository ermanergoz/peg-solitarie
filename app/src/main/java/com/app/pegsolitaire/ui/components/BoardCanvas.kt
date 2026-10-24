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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * animation. The default ([interactive] `= true`) keeps the full gameplay behavior, and adds one
 * TalkBack stop per in-play cell over the canvas (D-TB, `docs/ux-spec.md` §11.1), a layer that takes
 * no pointer input; the board stop itself sits on the host's slab. A read-only board gains no stop.
 *
 * **The wrong tap (D-06, `docs/ux/invalid-tap-feedback.md` §3).** The leaf answers the latest
 * wrong tap the state records, on the tapped cell, by drawing and by nothing else: while a peg
 * is held, a **redirect** — the held ring nudges, or the held peg's dots re-pop — and, when
 * [wrongTapCorrection] is on, a **correction** of the tapped cell: a head-shake, or with
 * animations off a still dashed mark. It plays no cue and fires no haptic (§3.8): those are
 * screen-level effects beside `GameScreen`, which the tutorial — sharing this leaf — must not
 * inherit. Nor does it choose any words: the host's announcer does, and the canvas only carries the
 * [announcement] it is given (§3.8 as D-TBa's F-44 reads it). A read-only board never draws any of it.
 *
 * @param state the immutable board state to render (single source of truth).
 * @param onCellTap emits the bounds-checked tapped [Position] up to the ViewModel
 *   (never called when [interactive] is `false`).
 * @param onAnimationFinished invoked once the jump slide completes.
 * @param modifier layout modifier for the (square) canvas.
 * @param interactive whether the board accepts taps and draws the selection/destination/
 *   animation overlays; `false` for the static catalog preview.
 * @param wrongTapCorrection whether a wrong tap is corrected on the tapped cell (the shake, or
 *   the mark with animations off): on for the gameplay screen, off for the tutorial (D-01j(2))
 *   and for every read-only board.
 * @param announcement the text the host's announcer has for the board's polite channel right now,
 *   or `null`; the canvas carries it and chooses no wording (D-TBa, F-44).
 * @param announces whether the host has a channel at all, so the canvas node is a polite live region
 *   (the gameplay screen); the tutorial gains its channel later on this branch.
 */
@Suppress("LongMethod") // One cohesive render unit: state capture + palette + the animation set.
@Composable
fun BoardCanvas(
    state: GameUiState,
    onCellTap: (Position) -> Unit,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    wrongTapCorrection: Boolean = false,
    announcement: String? = null,
    announces: Boolean = false,
) {
    val spec = state.board.spec
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val geometry = remember(spec, canvasSize) { BoardGeometry.forCanvas(spec, canvasSize) }

    // Colors + stroke widths are resolved in composition; DrawScope cannot read
    // MaterialTheme or resources, so they are captured into a palette and passed in.
    // Stroke dimens are converted dp → px here (DrawScope draws in pixels).
    val density = LocalDensity.current
    val clay = LocalClayColors.current
    val markWidthPx = with(density) { dimensionResource(R.dimen.wrong_tap_mark_width).toPx() }
    val markStroke =
        remember(geometry.cellSize, markWidthPx) {
            wrongTapMarkStroke(
                radius = BoardMetrics.WRONG_TAP_MARK_RING_RATIO * geometry.cellSize,
                widthPx = markWidthPx,
            )
        }
    val palette =
        BoardPalette(
            pegGradient = clay.pegGradient,
            pegHighlight = clay.pegHighlight,
            dimpleFill = clay.dimpleFill,
            dimpleShadow = clay.dimpleShadow,
            accent = MaterialTheme.colorScheme.primary,
            hint = MaterialTheme.colorScheme.tertiary,
            ringWidthPx = with(density) { dimensionResource(R.dimen.peg_selected_ring_width).toPx() },
            mark = MaterialTheme.colorScheme.onSurfaceVariant,
            markStroke = markStroke,
        )

    val animationsEnabled = rememberAnimationsEnabled()
    val wrongTap = rememberWrongTapFeedback(state, interactive, wrongTapCorrection, animationsEnabled)
    val transition = state.animating
    val jumpOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val removalProgress = remember { Animatable(REMOVAL_START) }
    val landingSquash = remember { Animatable(SETTLED_SCALE) }
    var landing by remember { mutableStateOf<Position?>(null) }

    // Which move id [jumpOffset] has actually been snapped to its source hole for.
    // The draw phase renders the jumper as soon as `animating` is non-null, but the
    // effect below only resumes AFTER that frame has been drawn — so without this
    // marker the first drawn frame uses a stale Animatable value (Offset.Zero, the
    // canvas top-left, on a game's first move). Reset whenever the animation key
    // changes because the move id (`moveHistory.size`) recycles after an undo.
    var preparedMoveId by remember { mutableStateOf<Int?>(null) }

    // The squash runs on the composition scope, NOT inside the keyed effect below:
    // onAnimationFinished() clears `animating`, which changes the effect key to null
    // and cancels the effect's coroutine — a squash launched there dies between its
    // snapTo and its settle animateTo, stranding the landed peg at
    // LANDING_SQUASH_SCALE (visibly smaller) with `landing` never cleared.
    val squashScope = rememberCoroutineScope()

    LaunchedEffect(transition?.id, geometry, interactive) {
        preparedMoveId = null
        if (interactive && transition != null && geometry.isReady) {
            runJumpAnimation(
                transition = transition,
                geometry = geometry,
                animationsEnabled = animationsEnabled,
                jumpOffset = jumpOffset,
                removalProgress = removalProgress,
                setPrepared = { preparedMoveId = it },
                onAnimationFinished = onAnimationFinished,
                startLandingSquash = {
                    squashScope.launch {
                        landing = transition.to
                        landingSquash.snapTo(BoardMetrics.LANDING_SQUASH_SCALE)
                        landingSquash.animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())
                        landing = null
                    }
                },
            )
        }
    }

    val dotPop = remember { Animatable(GameAnim.DOT_POP_END) }
    // Keyed on the wrong tap's re-pop counter too: a W3 redirect replays this same pop-in (§3.1).
    LaunchedEffect(state.selected, interactive, wrongTap.dotRePops) {
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
            jumpOffset = {
                jumperCenter(
                    transition = transition,
                    preparedMoveId = preparedMoveId,
                    geometry = geometry,
                    animatedOffset = jumpOffset.value,
                )
            },
            removalProgress = {
                removalAlpha(
                    transition = transition,
                    preparedMoveId = preparedMoveId,
                    animatedProgress = removalProgress.value,
                )
            },
            dotPop = { dotPop.value },
            pulse = { pulseScale?.value ?: SETTLED_SCALE },
            landingScale = { landingSquash.value },
            landing = landing,
            wrongTap = wrongTap,
        )

    val draw: DrawScope.() -> Unit = {
        drawBoard(
            state = state,
            geometry = geometry,
            palette = palette,
            interactive = interactive,
            animations = animations,
        )
    }
    if (interactive) {
        // The playable board: the canvas with its tap handler exactly as before — its tag, its size
        // callback and its pointerInput, so a finger, the drawing and every test that taps the canvas
        // by its bounds are unchanged (TB-11) — and over it one TalkBack stop per in-play cell (D-TB).
        val speech =
            remember(state.board, state.selected, state.legalTargets, state.hintSource, state.hintTarget) {
                BoardSpeechModel.of(
                    board = state.board,
                    selected = state.selected,
                    legalTargets = state.legalTargets,
                    hintSource = state.hintSource,
                    hintTarget = state.hintTarget,
                )
            }
        Box(modifier = modifier.aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO)) {
            Canvas(
                modifier =
                    Modifier
                        .matchParentSize()
                        .testTag(BOARD_CANVAS_TEST_TAG)
                        .onSizeChanged { canvasSize = it.toSize() }
                        .pointerInput(geometry) {
                            detectTapGestures { offset -> geometry.positionAt(offset)?.let(onCellTap) }
                        }.then(if (announces) Modifier.boardAnnouncement(announcement) else Modifier),
                onDraw = draw,
            )
            // After the canvas, so the stops are above it for explore-by-touch.
            BoardCellOverlay(
                spec = spec,
                speech = speech,
                touchExploration = rememberTouchExplorationEnabled(),
                onCellTap = onCellTap,
                modifier = Modifier.matchParentSize().testTag(BOARD_CELLS_TEST_TAG),
            )
        }
    } else {
        // Read-only previews compose today's canvas and nothing else: no tap handler, so they can
        // never emit onCellTap (the parent Card owns the tap), and no TalkBack stop (D-TBa item 9).
        Canvas(
            modifier =
                modifier
                    .aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO)
                    .testTag(BOARD_CANVAS_TEST_TAG)
                    .onSizeChanged { canvasSize = it.toSize() },
            onDraw = draw,
        )
    }
}

/**
 * Where the in-flight jumper is drawn this frame.
 *
 * The draw phase renders the jumper overlay the moment `GameUiState.animating`
 * becomes non-null, but the `Animatable` carrying its position is only moved onto
 * board coordinates by a `LaunchedEffect`, which first resumes AFTER that frame has
 * already been drawn. So the animated value is authoritative only once the effect has
 * reported (via [preparedMoveId]) that it snapped the Animatable for this exact move.
 *
 * Until then this returns the source hole the peg is about to leave — the cell it
 * visually occupied a frame earlier — instead of a stale coordinate left over from
 * whatever the Animatable last held. That stale value is `Offset.Zero` (the canvas
 * top-left) on a game's first move, which is what produced the first-move flicker.
 *
 * [preparedMoveId] is compared against [MoveTransition.id] rather than merely
 * null-checked because the id is `moveHistory.size` and therefore recycles after an
 * undo — a replayed move must not be mistaken for an already-prepared one.
 */
internal fun jumperCenter(
    transition: MoveTransition?,
    preparedMoveId: Int?,
    geometry: BoardGeometry,
    animatedOffset: Offset,
): Offset =
    if (transition != null && preparedMoveId != transition.id) {
        geometry.center(transition.from)
    } else {
        animatedOffset
    }

/**
 * How far through its fade + shrink the jumped-over peg is drawn this frame.
 *
 * The mirror of [jumperCenter] on the removal channel, and the same hazard: the peg is
 * drawn as soon as `GameUiState.animating` is non-null, but its progress `Animatable`
 * is only reset to [REMOVAL_START] by the `LaunchedEffect` a frame later. Left to the
 * raw animated value the doomed peg would be drawn at the PREVIOUS move's end progress
 * — [REMOVAL_END], i.e. fully transparent — so it would blink out and back in for one
 * frame on every move after the first.
 *
 * Until the move is prepared this therefore reports [REMOVAL_START]: the peg is still
 * whole, which is exactly what it looked like a frame ago.
 */
internal fun removalAlpha(
    transition: MoveTransition?,
    preparedMoveId: Int?,
    animatedProgress: Float,
): Float =
    if (transition != null && preparedMoveId != transition.id) {
        REMOVAL_START
    } else {
        animatedProgress
    }

/**
 * Runs the jump slide + removal fade for one committed move, then the landing
 * squash. With animations disabled everything resolves instantly (functional
 * transition, no decorative motion) and [onAnimationFinished] fires right away.
 *
 * [setPrepared] is invoked with the move id the instant [jumpOffset] has been
 * snapped onto this move's board coordinates; until then the draw phase must
 * position the jumper itself, because this function first resumes a frame after
 * the draw that already rendered the overlay.
 */
private suspend fun runJumpAnimation(
    transition: MoveTransition,
    geometry: BoardGeometry,
    animationsEnabled: Boolean,
    jumpOffset: Animatable<Offset, *>,
    removalProgress: Animatable<Float, *>,
    setPrepared: (Int) -> Unit,
    onAnimationFinished: () -> Unit,
    startLandingSquash: () -> Unit,
) {
    if (!animationsEnabled) {
        jumpOffset.snapTo(geometry.center(transition.to))
        setPrepared(transition.id)
        removalProgress.snapTo(REMOVAL_END)
        onAnimationFinished()
        return
    }
    coroutineScope {
        jumpOffset.snapTo(geometry.center(transition.from))
        setPrepared(transition.id)
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
        // decoration on the settled peg, so the screen is notified first. The squash
        // is launched on a scope that survives this effect — onAnimationFinished()
        // clears `animating`, restarting the keyed effect and cancelling this
        // coroutine before an in-place settle animation could finish.
        startLandingSquash()
        onAnimationFinished()
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

/**
 * Resolved theme + clay colors and pixel stroke widths handed to the draw phase, plus the wrong
 * tap's mark: `onSurfaceVariant` (§3.2) and its dashed stroke.
 */
private class BoardPalette(
    val pegGradient: List<Color>,
    val pegHighlight: Color,
    val dimpleFill: Color,
    val dimpleShadow: Color,
    val accent: Color,
    val hint: Color,
    val ringWidthPx: Float,
    val mark: Color,
    val markStroke: Stroke,
)

/**
 * Animated values read through lambdas so only the draw phase re-runs while an
 * animation plays (RESEARCH Pitfall 7/8), plus the cell currently landing and the board's
 * answer to the latest wrong tap.
 */
private class BoardAnimationValues(
    val jumpOffset: () -> Offset,
    val removalProgress: () -> Float,
    val dotPop: () -> Float,
    val pulse: () -> Float,
    val landingScale: () -> Float,
    val landing: Position?,
    val wrongTap: WrongTapFeedback,
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

    // The wrong tap's reduced-motion mark (D-06a): above the cell it marks, below any hint ring.
    animations.wrongTap.markedCell()?.let { marked ->
        drawWrongTapMark(geometry.center(marked), geometry, palette.mark, palette.markStroke)
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
    // The wrong tap's shake moves this cell's own drawing — never a ring, a dot or an overlay.
    val shaken = center + Offset(animations.wrongTap.shakeAt(position, geometry.cellSize), NO_OFFSET)
    when (state.board.cellAt(position)) {
        Cell.Empty -> {
            drawDimple(shaken, geometry, palette)
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
                    drawSelectedPeg(center, geometry, palette, animations.pulse(), animations.wrongTap.ringNudge())
                interactive && position == animations.landing ->
                    drawCandyPeg(
                        palette = palette,
                        center = shaken,
                        radius = geometry.pegRadius * animations.landingScale(),
                    )
                else -> drawCandyPeg(palette = palette, center = shaken, radius = geometry.pegRadius)
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

/**
 * The selected peg: enlarged candy sphere breathing at [pulse], plus the accent ring, whose radius
 * a wrong tap's [nudge] multiplies on top of the pulse (§3.1). The peg itself never nudges.
 */
private fun DrawScope.drawSelectedPeg(
    center: Offset,
    geometry: BoardGeometry,
    palette: BoardPalette,
    pulse: Float,
    nudge: Float,
) {
    drawCandyPeg(
        palette = palette,
        center = center,
        radius = geometry.pegRadius * BoardMetrics.SELECTED_PEG_SCALE * pulse,
    )
    drawCircle(
        color = palette.accent,
        radius = geometry.selectedRingRadius * pulse * nudge,
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
