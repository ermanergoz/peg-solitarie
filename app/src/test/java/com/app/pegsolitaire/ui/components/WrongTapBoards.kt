package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Two real [BoardCanvas]es for reading what the board draws for a wrong tap
 * (`docs/ux/invalid-tap-feedback.md` §3): the board under test, and a **control** board below it
 * that receives every same action — each selection, put-down, board install and theme change, in
 * the same frame — except the wrong taps.
 *
 * So in any frame, the control shows exactly what the board would draw had the wrong tap never
 * happened: the selection pulse and the dots' pop-in run in lockstep on both, and "nothing else
 * moves" is the plain fact that a cell's pixels equal the control's. Each board is a 3×3 square of
 * 300 px at density 1, so a cell is 100 px and the shake's amplitude A is 10 px. Pegs sit at
 * [HELD_PEG], (0,1) and [FAR_PEG]: holding [HELD_PEG] offers one destination, [DESTINATION].
 *
 * State changes are written straight into the hoisted [GameUiState]s, the way `GameViewModel`
 * writes them: a wrong tap advances `rejectedTapTick` and records `rejectedTapCell`, nothing else.
 * Time runs on a paused clock, one [frame] at a time. Distances are intensity-weighted centroids,
 * so anti-aliasing never decides a result.
 */
internal class WrongTapBoards(
    private val rule: ComposeContentTestRule,
) {
    private val state = mutableStateOf(GameUiState.initial(BOARD))
    private val controlState = mutableStateOf(GameUiState.initial(BOARD))
    private val dark = mutableStateOf(false)

    /** The theme's colours as the boards were last composed with them. */
    lateinit var scheme: ColorScheme
        private set

    /** The board under test's state, for a test that sets up a carried ordinal before [show]. */
    var underTest: GameUiState
        get() = state.value
        set(value) {
            state.value = value
        }

    // ---- Driving the boards ----------------------------------------------------------------

    /**
     * Composes both boards on the slab colour and lays them out. [correction] is the gameplay
     * board's `wrongTapCorrection`; [interactive] `false` makes both read-only previews.
     */
    fun show(
        correction: Boolean = true,
        interactive: Boolean = true,
    ) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(darkTheme = dark.value) {
                scheme = MaterialTheme.colorScheme
                Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    listOf(state to UNDER_TEST_TAG, controlState to CONTROL_TAG).forEach { (board, tag) ->
                        Box(Modifier.testTag(tag)) {
                            BoardCanvas(
                                state = board.value,
                                onCellTap = {},
                                onAnimationFinished = {},
                                modifier = Modifier.size(CANVAS_DP.dp),
                                interactive = interactive,
                                wrongTapCorrection = correction,
                            )
                        }
                        Spacer(Modifier.height(GAP_DP.dp))
                    }
                }
            }
        }
        repeat(LAYOUT_FRAMES) { frame() }
    }

    /**
     * What `GameViewModel` does on W1, W2 or W3, on the board under test only: the ordinal
     * advances by one and the cell is recorded. The selection, the dots and the board stay.
     */
    fun wrongTapOn(cell: Position) {
        state.value = state.value.copy(rejectedTapTick = state.value.rejectedTapTick + 1, rejectedTapCell = cell)
    }

    /** Picks up [peg] on both boards, with its legal destinations. */
    fun select(peg: Position) =
        both {
            it.copy(
                selected = peg,
                legalTargets =
                    RulesEngine
                        .legalMoves(it.board)
                        .filter { move -> move.from == peg }
                        .map { move -> move.to },
                selectionTick = it.selectionTick + 1,
            )
        }

    /** Puts the held peg back down on both boards. */
    fun putDown() =
        both { it.copy(selected = null, legalTargets = emptyList(), deselectionTick = it.deselectionTick + 1) }

    /** Applies the same successful action to both boards in the same frame. */
    fun both(change: (GameUiState) -> GameUiState) {
        state.value = change(state.value)
        controlState.value = change(controlState.value)
    }

    /**
     * A board install on both, in [darkTheme]: a fresh state on the same board, carrying the
     * ordinal the way all four of `GameViewModel`'s install sites do (and clearing the cell).
     */
    fun install(darkTheme: Boolean = dark.value) {
        dark.value = darkTheme
        state.value = GameUiState.initial(BOARD).copy(rejectedTapTick = state.value.rejectedTapTick)
        controlState.value = GameUiState.initial(BOARD)
    }

    /**
     * One frame, after the main thread has idled: so a state written since the last frame is
     * composed in this one, as it is on a device, where the snapshot's apply notification reaches
     * the main thread before the next vsync.
     */
    fun frame() {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
    }

    /** Lets every pop-in and every spring on both boards come to rest. */
    fun settle() = repeat(SETTLE_FRAMES) { frame() }

    /** The paused clock's time, in ms. */
    val now: Long get() = rule.mainClock.currentTime

    /** The window as drawn by the latest frame. */
    fun render(): Bitmap = rule.renderedWindow()

    // ---- Reading the boards ----------------------------------------------------------------

    /** The board under test's bounds in the window. */
    val canvas: Rect get() = rule.onNodeWithTag(UNDER_TEST_TAG).fetchSemanticsNode().boundsInRoot

    private val control: Rect get() = rule.onNodeWithTag(CONTROL_TAG).fetchSemanticsNode().boundsInRoot

    private val geometry: BoardGeometry
        get() = BoardGeometry.forCanvas(BOARD.spec, Size(canvas.width, canvas.height))

    /** How far right (positive) [cell]'s drawing sits from where the control draws it, in px. */
    fun shakeOf(
        bitmap: Bitmap,
        cell: Position,
    ): Float = centroid(bitmap, box(cell, canvas), X) - centroid(bitmap, box(cell, control), X)

    /** How far down [cell]'s drawing sits from where the control draws it, in px. */
    fun driftOf(
        bitmap: Bitmap,
        cell: Position,
    ): Float = centroid(bitmap, box(cell, canvas), Y) - centroid(bitmap, box(cell, control), Y)

    /**
     * How many pixels of [cell] on the board under test differ from [against] — on the control
     * board by default, or on the board under test itself when [onControl] is `false` — by more
     * than [PIXEL_NOISE].
     */
    fun differingPixels(
        bitmap: Bitmap,
        cell: Position,
        against: Position = cell,
        onControl: Boolean = true,
    ): Int {
        val mine = box(cell, canvas)
        val theirs = box(against, if (onControl) control else canvas)
        var differing = 0
        forEachPixel(mine) { x, y ->
            val other =
                bitmap.getPixel(x - mine.left.toInt() + theirs.left.toInt(), y - mine.top.toInt() + theirs.top.toInt())
            if (distance(bitmap.getPixel(x, y), other) > PIXEL_NOISE) differing++
        }
        return differing
    }

    /** How many pixels of the whole board under test differ from the control board. */
    fun differingPixelsAnywhere(bitmap: Bitmap): Int = ALL_CELLS.sumOf { differingPixels(bitmap, it) }

    /** [peg]'s selection-ring radius on the board under test over the control's: the nudge. */
    fun nudgeOf(
        bitmap: Bitmap,
        peg: Position,
    ): Float = ringRadius(bitmap, peg, canvas) / ringRadius(bitmap, peg, control)

    /**
     * The radius of [peg]'s selection ring on the board under test, from the ring's own pixels:
     * along the row through the peg's centre, toward the board's inside, the `primary`-weighted
     * centroid between the held peg's edge and the next cell. Sub-pixel, so a 1.18× nudge reads
     * as itself.
     */
    fun ringRadius(
        bitmap: Bitmap,
        peg: Position,
        on: Rect = canvas,
    ): Float {
        val c = on.topLeft + geometry.center(peg)
        val inward = if (peg.col == 0) 1 else -1
        val primary = scheme.primary.toArgb()
        var weighted = 0.0
        var total = 0.0
        val from = (geometry.cellSize * RING_SCAN_FROM).roundToInt()
        val to = (geometry.cellSize * RING_SCAN_TO).roundToInt()
        for (step in from..to) {
            val x = (c.x + inward * step).toInt()
            val closeness = RING_CLOSENESS - distance(bitmap.getPixel(x, c.y.toInt()), primary)
            val weight = closeness.coerceAtLeast(0).toDouble()
            weighted += weight * abs(x + PIXEL_CENTRE - c.x)
            total += weight
        }
        check(total > 0.0) { "no selection ring around $peg" }
        return (weighted / total).toFloat()
    }

    /** A pixel on [peg]'s selection ring on the board under test, at the ring's measured radius. */
    fun ringPixel(
        bitmap: Bitmap,
        peg: Position,
    ): Int {
        val c = canvas.topLeft + geometry.center(peg)
        val inward = if (peg.col == 0) 1 else -1
        return bitmap.getPixel((c.x + inward * ringRadius(bitmap, peg)).toInt(), c.y.toInt())
    }

    /**
     * How many pixels of [cell]'s own drawing — the square inside any mark around it — differ
     * from the control's: a displacement shows here, a mark around the cell does not.
     */
    fun differingDrawingPixels(
        bitmap: Bitmap,
        cell: Position,
    ): Int {
        val half = geometry.cellSize * DRAWING_HALF_RATIO
        val mine = geometry.center(cell)
        val board = canvas
        val dx = control.left.toInt() - board.left.toInt()
        val dy = control.top.toInt() - board.top.toInt()
        var differing = 0
        val inner = Rect(mine.x - half, mine.y - half, mine.x + half, mine.y + half)
        forEachPixel(inner.translate(board.topLeft)) { x, y ->
            if (distance(bitmap.getPixel(x, y), bitmap.getPixel(x + dx, y + dy)) > PIXEL_NOISE) differing++
        }
        return differing
    }

    /** How many separate `onSurfaceVariant` dashes the mark ring around [cell] shows. */
    fun dashesAround(
        bitmap: Bitmap,
        cell: Position,
    ): Int {
        val onDash = markSamples(bitmap, cell)
        return onDash.indices.count { i -> onDash[i] && !onDash[(i - 1 + onDash.size) % onDash.size] }
    }

    /** The pixel in the middle of the first dash the mark ring around [cell] shows. */
    fun firstDashMiddle(
        bitmap: Bitmap,
        cell: Position,
    ): Int = markPixel(bitmap, cell, firstDashMiddleSample(bitmap, cell))

    /**
     * The mark's stroke around [cell], measured along the ray through the middle of its first dash:
     * the coverage-weighted mean distance from the cell's centre (the mark's radius) and the summed
     * coverage (its width), both in px. Coverage is read from colour: the mark over the slab.
     */
    fun markStroke(
        bitmap: Bitmap,
        cell: Position,
    ): Pair<Float, Float> {
        val angle = firstDashMiddleSample(bitmap, cell) * FULL_TURN / RING_SAMPLES
        val c = canvas.topLeft + geometry.center(cell)
        val mark = scheme.onSurfaceVariant.toArgb()
        val full = distance(scheme.surfaceContainerHigh.toArgb(), mark).toFloat()
        var weighted = 0.0
        var total = 0.0
        var radius = geometry.cellSize * PROFILE_FROM
        while (radius <= geometry.cellSize * PROFILE_TO) {
            val pixel = bitmap.getPixel((c.x + radius * cos(angle)).toInt(), (c.y + radius * sin(angle)).toInt())
            val coverage = (1f - distance(pixel, mark) / full).coerceIn(0f, 1f) * PROFILE_STEP
            weighted += coverage * radius
            total += coverage
            radius += PROFILE_STEP
        }
        return (weighted / total).toFloat() to total.toFloat()
    }

    /** Asserts [pixel] is [expected], to within anti-aliasing's rounding. */
    fun assertColour(
        what: String,
        pixel: Int,
        expected: Color,
    ) {
        assertWithMessage("$what: #${Integer.toHexString(pixel)} against #${Integer.toHexString(expected.toArgb())}")
            .that(distance(pixel, expected.toArgb()))
            .isAtMost(EXACT_CLOSENESS)
    }

    private fun firstDashMiddleSample(
        bitmap: Bitmap,
        cell: Position,
    ): Int {
        val onDash = markSamples(bitmap, cell)
        val start = onDash.indices.first { i -> onDash[i] && !onDash[(i - 1 + onDash.size) % onDash.size] }
        val length = (0 until onDash.size).first { !onDash[(start + it) % onDash.size] }
        return (start + length / 2) % onDash.size
    }

    /** Whether each of [RING_SAMPLES] points on the mark's ring around [cell] is on a dash. */
    private fun markSamples(
        bitmap: Bitmap,
        cell: Position,
    ): List<Boolean> {
        val mark = scheme.onSurfaceVariant.toArgb()
        return (0 until RING_SAMPLES).map { distance(markPixel(bitmap, cell, it), mark) <= MARK_CLOSENESS }
    }

    private fun markPixel(
        bitmap: Bitmap,
        cell: Position,
        sample: Int,
    ): Int {
        val c = canvas.topLeft + geometry.center(cell)
        val radius = geometry.cellSize * MARK_RING_RATIO
        val angle = sample * FULL_TURN / RING_SAMPLES
        return bitmap.getPixel((c.x + radius * cos(angle)).toInt(), (c.y + radius * sin(angle)).toInt())
    }

    /** [cell]'s own square on [on], in window px. */
    private fun box(
        cell: Position,
        on: Rect,
    ): Rect {
        val size = geometry.cellSize
        val left = on.left + cell.col * size
        val top = on.top + cell.row * size
        return Rect(left, top, left + size, top + size)
    }

    /** The intensity-weighted centroid of [box] along [axis], relative to [box]'s own corner. */
    private fun centroid(
        bitmap: Bitmap,
        box: Rect,
        axis: Int,
    ): Float {
        val slab = scheme.surfaceContainerHigh.toArgb()
        var weighted = 0.0
        var total = 0.0
        forEachPixel(box) { x, y ->
            val weight = distance(bitmap.getPixel(x, y), slab).toDouble()
            weighted += weight * (if (axis == X) x - box.left else y - box.top)
            total += weight
        }
        return (weighted / total).toFloat()
    }

    private inline fun forEachPixel(
        box: Rect,
        action: (Int, Int) -> Unit,
    ) {
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) action(x, y)
        }
    }

    internal companion object {
        /** A 3×3 square with pegs at (0,0), (0,1) and (2,2). */
        val BOARD: Board =
            Board(
                spec =
                    BoardSpec(
                        inPlay = (0 until SIDE).flatMap { r -> (0 until SIDE).map { c -> Position(r, c) } },
                        target = null,
                    ),
                pegs = 0L,
            ).withPeg(Position(0, 0)).withPeg(Position(0, 1)).withPeg(Position(2, 2))

        /** Held, it offers exactly one destination: [DESTINATION], over (0,1). */
        val HELD_PEG = Position(0, 0)
        val DESTINATION = Position(0, 2)

        /** A peg far from [HELD_PEG], with no legal jump of its own. */
        val FAR_PEG = Position(2, 2)

        /** Holes nothing points at while [HELD_PEG] is held. */
        val CENTRE_HOLE = Position(1, 1)
        val FAR_HOLE = Position(2, 0)

        val ALL_CELLS: List<Position> = BOARD.spec.inPlay

        /** One frame of the paused clock, in ms. */
        const val FRAME_MS = 16L

        /** The shake's amplitude A on these boards: 0.10 × a 100 px cell. */
        const val AMPLITUDE_PX = 10f

        /** The summed per-channel distance of two opaque colours. */
        fun distance(
            a: Int,
            b: Int,
        ): Int = CHANNEL_SHIFTS.sumOf { shift -> abs((a shr shift and CHANNEL) - (b shr shift and CHANNEL)) }

        private const val SIDE = 3
        private const val CANVAS_DP = 300

        /**
         * Puts the control 320 px below the board under test: a multiple of 16, so Skia's ordered
         * dither of the pegs' gradients falls on both boards in the same phase.
         */
        private const val GAP_DP = 20
        private const val LAYOUT_FRAMES = 3
        private const val UNDER_TEST_TAG = "wrongTapBoardUnderTest"
        private const val CONTROL_TAG = "wrongTapControlBoard"

        /** Where a ring is looked for, as fractions of a cell from its peg's centre. */
        private const val RING_SCAN_FROM = 0.37f
        private const val RING_SCAN_TO = 0.62f
        private const val RING_CLOSENESS = 150
        private const val PIXEL_CENTRE = 0.5

        private const val MARK_RING_RATIO = 0.40f

        /** Where the mark's stroke is profiled: outside a peg's edge, inside the next cell. */
        private const val PROFILE_FROM = 0.36f
        private const val PROFILE_TO = 0.46f
        private const val PROFILE_STEP = 0.25f

        /**
         * A cell's own drawing, inside any mark around it: half a square side, per cell. The
         * square's corners stay inside the mark's inner edge (0.26 × √2 < 0.40 − half its width).
         */
        private const val DRAWING_HALF_RATIO = 0.26f

        private const val EXACT_CLOSENESS = 6

        /**
         * How far apart two pixels may be and still count as the same drawing. Skia anti-aliases
         * the same ring, drawn 320 px lower, a little differently: up to 20 in [distance] on these
         * boards (measured over 160 frames of a pulsing selection, no wrong tap). A displacement,
         * a nudge, a dot or a mark changes the pixels it touches by more than 100.
         */
        private const val PIXEL_NOISE = 30
        private const val SETTLE_FRAMES = 60
        private const val RING_SAMPLES = 720
        private const val MARK_CLOSENESS = 30
        private const val FULL_TURN = (2 * Math.PI).toFloat()

        private const val X = 0
        private const val Y = 1
        private val CHANNEL_SHIFTS = listOf(16, 8, 0)
        private const val CHANNEL = 0xFF
    }
}

/**
 * The compose rule the wrong-tap board tests run on. With [animationsOn] `false` its effects see a
 * `MotionDurationScale` of 0, as every effect does on a phone whose animator scale is 0 — so an
 * `Animatable` finishes in its first frame there, exactly as it would on that phone. Compose's
 * test rule does not apply the system setting by itself.
 */
internal fun createWrongTapComposeRule(animationsOn: Boolean): ComposeContentTestRule =
    if (animationsOn) createComposeRule() else createComposeRule(effectContext = AnimationsOff)

private object AnimationsOff : MotionDurationScale {
    override val scaleFactor: Float get() = 0f
}

/** The SDK the board pixel tests render on. */
internal const val WRONG_TAP_CANVAS_SDK = 34

/** A density of 1, so the 300 dp boards are 300 px and every px figure is exact. */
internal const val WRONG_TAP_CANVAS_QUALIFIERS = "w400dp-h800dp-mdpi"
