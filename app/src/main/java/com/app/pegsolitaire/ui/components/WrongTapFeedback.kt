package com.app.pegsolitaire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.GameAnim
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.game.WrongTapKind
import com.app.pegsolitaire.ui.game.WrongTapMotion
import com.app.pegsolitaire.ui.game.WrongTapRedirect
import com.app.pegsolitaire.ui.game.wrongTapRedirectFor
import kotlinx.coroutines.delay
import kotlin.math.PI

/**
 * How the board corrects a wrong tap on the tapped cell itself (D-06, D-06a), when it corrects at
 * all: the gameplay board does, the tutorial never does (D-01j(2)).
 */
internal enum class WrongTapCorrection {
    /** No correction: the tutorial, which answers a wrong tap with its redirect alone. */
    None,

    /** The head-shake of the tapped cell, with animations on. */
    Shake,

    /** The still, dashed mark around the tapped cell, with animations off (D-06a). */
    Mark,
}

/**
 * The state a successful action changes. Any change to it — a selection, a put-down, a jump, an
 * undo that rewinds, a hint reveal, a restart or any board install — ends the board's answer to a
 * wrong tap at once and for good (`docs/ux/invalid-tap-feedback.md` §3.3, rule 3). The tapped cell
 * is part of it because every board install clears that cell, so even a restart that leaves an
 * untouched board exactly as it was still ends the answer.
 */
internal data class WrongTapContext(
    val board: Board,
    val selected: Position?,
    val hintSource: Position?,
    val rejectedTapCell: Position?,
) {
    companion object {
        fun of(state: GameUiState): WrongTapContext =
            WrongTapContext(state.board, state.selected, state.hintSource, state.rejectedTapCell)
    }
}

/**
 * The board's answer to one wrong tap, fixed in the frame its ordinal arrived, from the state of
 * that frame. Compared by identity: each wrong tap gets its own answer, even on the same cell.
 */
@Immutable
internal class WrongTapAnswer(
    val cell: Position,
    val correction: WrongTapCorrection,
    val redirect: WrongTapRedirect,
    val context: WrongTapContext,
)

/**
 * What the board draws for the latest wrong tap this frame, read in the draw phase only.
 *
 * Built in composition from the live answer, if any, and the elapsed-time [clock]; the
 * per-frame values come from [WrongTapMotion], so nothing is allocated while drawing. The clock
 * is read only once [timing] names this very answer: until its effect has restarted the clock, a
 * new answer is drawn at t = 0, never at the phase an interrupted one left behind.
 *
 * @property dotRePops a counter the destination dots' pop-in is keyed on: it advances once per
 *   wrong tap whose redirect re-pops them, so the dots replay the exact animation a selection
 *   plays (§3.1), from `GameAnim.DOT_POP_START` on `ClayAnim.overshootSpring()`.
 */
internal class WrongTapFeedback(
    private val answer: WrongTapAnswer?,
    private val clock: Animatable<Float, AnimationVector1D>,
    private val timing: State<WrongTapAnswer?>,
    val dotRePops: Int,
) {
    /**
     * How far right (positive) or left the drawing of [position] is moved this frame: non-zero only
     * for the shaking cell. The shake moves the cell's own drawing — the peg sphere and its
     * specular dot, or the two tones of the hole's dimple — and never a ring, a dot or an overlay.
     */
    fun shakeAt(
        position: Position,
        cellSize: Float,
    ): Float {
        val shaking = answer?.takeIf { it.correction == WrongTapCorrection.Shake && it.cell == position }
        return if (shaking == null) {
            NO_SHAKE
        } else {
            WrongTapMotion.shakeOffset(elapsedMs(), BoardMetrics.WRONG_TAP_SHAKE_RATIO * cellSize)
        }
    }

    /**
     * The cell the reduced-motion mark is drawn on this frame, or `null`: whole from the tap's own
     * frame for as long as the answer holds, and gone the moment it ends — never a fade (D-06a).
     */
    fun markedCell(): Position? = answer?.takeIf { it.correction == WrongTapCorrection.Mark }?.cell

    /** The held peg's ring radius this frame, as a multiple of its resting radius. */
    fun ringNudge(): Float =
        if (answer?.redirect == WrongTapRedirect.RingNudge) WrongTapMotion.ringNudgeScale(elapsedMs()) else NO_NUDGE

    private fun elapsedMs(): Float = if (answer != null && timing.value === answer) clock.value else CLOCK_START
}

/**
 * The board's answer to the wrong taps of [state] (D-06, `docs/ux/invalid-tap-feedback.md` §3).
 *
 * Each new wrong-tap ordinal fixes a fresh answer on its cell in the very frame it arrives, from
 * that frame's state (§3.3, rule 2): so the mark is drawn in the tap's own frame, and a newer
 * wrong tap leaves the old cell at rest in that same frame. Never for the ordinal the composition
 * first saw, whatever it is, so neither a fresh board at 0 nor one recomposed from scratch with a
 * carried ordinal — a rotation — replays an old answer (WT-6). A read-only board ([interactive]
 * `false`) never answers (WT-17).
 *
 * Effects only end an answer: its own time running out, or any successful action — a change of
 * [WrongTapContext] — which ends it at once and for good, so a put-down right after a selection
 * cannot bring a stale shake back mid-swing (rule 3).
 *
 * **Two clocks, on purpose.** The shake and the nudge read an elapsed-time `Animatable` on the
 * frame clock. The reduced-motion mark is held by a plain `delay`, because Compose scales every
 * animation by the system animator scale: at the scale of 0 that turns animations off, an
 * animated hold would finish in its first frame and the mark — which exists for exactly that
 * player — would never be seen. Its 450 ms is functional, not decorative (D-06a).
 *
 * @param correction whether this board corrects the tapped cell — the gameplay board's shake, or
 *   its mark with animations off — or only redirects, as the tutorial does.
 */
@Composable
internal fun rememberWrongTapFeedback(
    state: GameUiState,
    interactive: Boolean,
    correction: Boolean,
    animationsEnabled: Boolean,
): WrongTapFeedback {
    val clock = remember { Animatable(CLOCK_START) }
    val timing = remember { mutableStateOf<WrongTapAnswer?>(null) }
    var ended by remember { mutableStateOf<WrongTapAnswer?>(null) }
    var dotRePops by remember { mutableIntStateOf(0) }
    val firstOrdinal = remember { state.rejectedTapTick }
    val context = WrongTapContext.of(state)

    val arrived =
        remember(state.rejectedTapTick) {
            if (interactive && state.rejectedTapTick > firstOrdinal) {
                answerTo(state, correction, animationsEnabled)
            } else {
                null
            }
        }

    LaunchedEffect(arrived) {
        val answer = arrived ?: return@LaunchedEffect
        if (answer.redirect == WrongTapRedirect.DotRePop) dotRePops++
        if (answer.correction == WrongTapCorrection.Mark) {
            delay(GameAnim.WRONG_TAP_MARK_HOLD_MS.toLong())
        } else {
            clock.snapTo(CLOCK_START)
            timing.value = answer
            clock.animateTo(
                targetValue = WrongTapMotion.ANIMATED_MS.toFloat(),
                animationSpec = tween(durationMillis = WrongTapMotion.ANIMATED_MS, easing = LinearEasing),
            )
        }
        ended = answer
    }

    LaunchedEffect(context) {
        val answer = arrived
        if (answer != null && answer.context != context) {
            ended = answer
            clock.stop()
        }
    }

    val live = arrived?.takeIf { it !== ended && it.context == context }
    return WrongTapFeedback(answer = live, clock = clock, timing = timing, dotRePops = dotRePops)
}

/**
 * The answer [state]'s latest wrong tap gets, or `null` when there is nothing to draw: no cell, a
 * tutorial W1, or any tutorial wrong tap with animations off.
 */
private fun answerTo(
    state: GameUiState,
    correction: Boolean,
    animationsEnabled: Boolean,
): WrongTapAnswer? {
    val cell = state.rejectedTapCell ?: return null
    val answer =
        WrongTapAnswer(
            cell = cell,
            correction = correctionFor(correction, animationsEnabled),
            redirect =
                wrongTapRedirectFor(
                    kind = WrongTapKind.of(state.board, state.selected, cell),
                    showHints = state.showHints,
                    heldDestinations = state.legalTargets.size,
                    animationsEnabled = animationsEnabled,
                ),
            context = WrongTapContext.of(state),
        )
    return answer.takeUnless { it.correction == WrongTapCorrection.None && it.redirect == WrongTapRedirect.None }
}

/**
 * The reduced-motion mark (D-06a, §3.2): a ring of [BoardMetrics.WRONG_TAP_MARK_DASHES] equal
 * dashes at a 50 % duty cycle, [BoardMetrics.WRONG_TAP_MARK_RING_RATIO] × cellSize from the
 * tapped cell's centre, in [color] — a shape and a pattern, so colour is never the only channel,
 * and the dashes tell it from the solid selection and hint rings.
 */
internal fun DrawScope.drawWrongTapMark(
    center: Offset,
    geometry: BoardGeometry,
    color: Color,
    stroke: Stroke,
) {
    drawCircle(
        color = color,
        radius = BoardMetrics.WRONG_TAP_MARK_RING_RATIO * geometry.cellSize,
        center = center,
        style = stroke,
    )
}

/**
 * The dashed stroke [drawWrongTapMark] draws with, for a mark of [radius] px and a stroke of
 * [widthPx]: the circumference cut into twice as many equal intervals as there are dashes, every
 * other one drawn. Built in composition, never in the draw phase.
 */
internal fun wrongTapMarkStroke(
    radius: Float,
    widthPx: Float,
): Stroke {
    val interval = (FULL_TURN * radius) / (BoardMetrics.WRONG_TAP_MARK_DASHES * DASH_AND_GAP)
    return Stroke(width = widthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval)))
}

/** The gameplay board corrects with a shake, or with the mark when animations are off. */
private fun correctionFor(
    correction: Boolean,
    animationsEnabled: Boolean,
): WrongTapCorrection =
    when {
        !correction -> WrongTapCorrection.None
        animationsEnabled -> WrongTapCorrection.Shake
        else -> WrongTapCorrection.Mark
    }

/** The answer's clock reads milliseconds since the tap, from zero. */
private const val CLOCK_START = 0f

/** A cell drawn at its true centre. */
private const val NO_SHAKE = 0f

/** A ring at its resting radius. */
private const val NO_NUDGE = 1f

/** A full turn, in radians. */
private const val FULL_TURN = 2 * PI.toFloat()

/** Each dash is followed by a gap of the same length. */
private const val DASH_AND_GAP = 2
