package com.app.pegsolitaire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.BoardMarkRecipe
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.recipe
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min

/**
 * Pre-computed, hoisted board geometry for one canvas size.
 *
 * Built once per `(spec, canvasSize)` via [forCanvas] and `remember`-ed by the
 * Canvas so nothing is allocated inside `DrawScope` (RESEARCH Pitfall 8). It owns
 * the pure pixel math: per-[Position] center [Offset]s, the proportional radii
 * (peg/hole/dot/ring), and the tap hit-test ([positionAt]).
 *
 * It depends only on `Offset`/`Size` value classes + domain types (no Android
 * framework), so it is exercised directly by a fast JVM unit test.
 */
class BoardGeometry private constructor(
    private val spec: BoardSpec,
    /**
     * The square grid dimension (cells per side) derived from the [spec] bounds:
     * `max(maxRow, maxCol) + 1`. English-33/European-37 = 7, Asymmetric-39 = 8,
     * Diamond-41/Wiegleb-45 = 9. Drives both the cell size and the [positionAt]
     * bounds-range, so every footprint renders and hit-tests in bounds.
     */
    val gridDimension: Int,
    /** Edge length of one grid cell in pixels (0 until the canvas has been measured). */
    val cellSize: Float,
    private val originX: Float,
    private val originY: Float,
    /** The palette's mark geometry; only the three ring radii below read it (docs/ux/colour-vision.md §3.4). */
    private val recipe: BoardMarkRecipe,
) {
    /** The valid in-grid index range for both row and col (per-board, T-04-01). */
    private val gridRange: IntRange = 0 until gridDimension

    /** Whether the canvas has been measured to a usable size. */
    val isReady: Boolean get() = cellSize > NO_SIZE

    /** Resting/jumping peg radius. */
    val pegRadius: Float get() = BoardMetrics.PEG_RADIUS_RATIO * cellSize

    /** Empty-hole indicator radius. */
    val holeRadius: Float get() = BoardMetrics.HOLE_RADIUS_RATIO * cellSize

    /** Legal-destination dot radius. */
    val destinationDotRadius: Float get() = BoardMetrics.DESTINATION_DOT_RATIO * cellSize

    /** Outer radius of the selected-peg ring. */
    val selectedRingRadius: Float get() = recipe.selectedRingRatio * cellSize

    /** Glow-ring radius around the peg to move (A) in a revealed hint. */
    val hintSourceRingRadius: Float get() = recipe.hintSourceRingRatio * cellSize

    /** Ring radius around the destination hole (B) in a revealed hint. */
    val hintTargetRingRadius: Float get() = recipe.hintTargetRingRatio * cellSize

    /** The pixel center of [position]'s cell. */
    fun center(position: Position): Offset =
        Offset(
            x = originX + (position.col + BoardMetrics.CELL_CENTER_FRACTION) * cellSize,
            y = originY + (position.row + BoardMetrics.CELL_CENTER_FRACTION) * cellSize,
        )

    /**
     * Maps a tap [offset] to the in-play [Position] it lands on, or `null` if the
     * tap is outside the grid, on a blocked (off-footprint) coordinate, or in the
     * dead corner of a cell beyond its inscribed hit area.
     *
     * This is the T-04-01 bounds-check: an untrusted pixel offset is validated
     * against the [spec] footprint AND the per-cell hit radius before the Canvas is
     * allowed to emit it upward. Legality is still re-decided by the ViewModel.
     */
    fun positionAt(offset: Offset): Position? {
        if (!isReady) return null
        val col = floor((offset.x - originX) / cellSize).toInt()
        val row = floor((offset.y - originY) / cellSize).toInt()
        val position = Position(row = row, col = col)
        return position.takeIf {
            row in gridRange && col in gridRange && it in spec && withinHitRadius(offset, it)
        }
    }

    private fun withinHitRadius(
        offset: Offset,
        position: Position,
    ): Boolean {
        val center = center(position)
        val distance = hypot(offset.x - center.x, offset.y - center.y)
        return distance <= BoardMetrics.HIT_RADIUS_RATIO * cellSize
    }

    companion object {
        private const val NO_SIZE = 0f

        /** One-cell padding to convert a max index into a count (dimension = maxIndex + 1). */
        private const val INDEX_TO_COUNT = 1

        /**
         * Builds geometry for [spec] centered in a [size] canvas. A zero/degenerate
         * size yields a not-[isReady] instance whose [positionAt] always returns
         * `null` (no taps before the first layout pass). [recipe] sizes the selection and hint
         * rings only — the standard palette's by default, so every caller that draws no palette
         * (the read-only board, hit-testing) reads today's values; cell centres, the hit test and
         * the peg, hole and dot radii never depend on it.
         */
        fun forCanvas(
            spec: BoardSpec,
            size: Size,
            recipe: BoardMarkRecipe = BoardMarkPalette.Standard.recipe,
        ): BoardGeometry {
            val gridDimension = gridDimensionOf(spec)
            val cellSize = min(size.width, size.height) / gridDimension
            val gridExtent = cellSize * gridDimension
            val originX = (size.width - gridExtent) * BoardMetrics.CELL_CENTER_FRACTION
            val originY = (size.height - gridExtent) * BoardMetrics.CELL_CENTER_FRACTION
            return BoardGeometry(
                spec = spec,
                gridDimension = gridDimension,
                cellSize = cellSize,
                originX = originX,
                originY = originY,
                recipe = recipe,
            )
        }

        /**
         * Derives the square grid dimension from the [spec]'s in-play bounds:
         * `max(maxRow, maxCol) + 1`. This replaces the former hardcoded `7`, so
         * the 8-wide (Asymmetric-39) and 9-wide (Diamond-41/Wiegleb-45) footprints
         * size and hit-test correctly.
         */
        private fun gridDimensionOf(spec: BoardSpec): Int {
            val rows = spec.inPlay.maxOf { it.row } + INDEX_TO_COUNT
            val cols = spec.inPlay.maxOf { it.col } + INDEX_TO_COUNT
            return maxOf(rows, cols)
        }
    }
}
