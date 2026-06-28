package com.app.pegsolitaire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.BoardMetrics
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
    /** Edge length of one grid cell in pixels (0 until the canvas has been measured). */
    val cellSize: Float,
    private val originX: Float,
    private val originY: Float,
) {
    /** Whether the canvas has been measured to a usable size. */
    val isReady: Boolean get() = cellSize > NO_SIZE

    /** Resting/jumping peg radius. */
    val pegRadius: Float get() = BoardMetrics.PEG_RADIUS_RATIO * cellSize

    /** Empty-hole indicator radius. */
    val holeRadius: Float get() = BoardMetrics.HOLE_RADIUS_RATIO * cellSize

    /** Legal-destination dot radius. */
    val destinationDotRadius: Float get() = BoardMetrics.DESTINATION_DOT_RATIO * cellSize

    /** Outer radius of the selected-peg ring. */
    val selectedRingRadius: Float get() = BoardMetrics.SELECTED_RING_RATIO * cellSize

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
            row in GRID_RANGE && col in GRID_RANGE && it in spec && withinHitRadius(offset, it)
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
        private val GRID_RANGE = 0 until BoardMetrics.GRID_DIMENSION

        /**
         * Builds geometry for [spec] centered in a [size] canvas. A zero/degenerate
         * size yields a not-[isReady] instance whose [positionAt] always returns
         * `null` (no taps before the first layout pass).
         */
        fun forCanvas(
            spec: BoardSpec,
            size: Size,
        ): BoardGeometry {
            val cellSize = min(size.width, size.height) / BoardMetrics.GRID_DIMENSION
            val gridExtent = cellSize * BoardMetrics.GRID_DIMENSION
            val originX = (size.width - gridExtent) * BoardMetrics.CELL_CENTER_FRACTION
            val originY = (size.height - gridExtent) * BoardMetrics.CELL_CENTER_FRACTION
            return BoardGeometry(spec = spec, cellSize = cellSize, originX = originX, originY = originY)
        }
    }
}
