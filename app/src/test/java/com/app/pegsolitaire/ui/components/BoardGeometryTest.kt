package com.app.pegsolitaire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Fast JVM coverage that [BoardGeometry] derives its grid dimension per board from
 * the [BoardSpec] bounds (7/8/9) instead of a hardcoded 7, so all five Classic
 * footprints render and hit-test in bounds.
 *
 * For every footprint and grid dimension it asserts the round-trip
 * `positionAt(center(p)) == p` for each in-play cell (with the center landing
 * inside the canvas), and that an off-footprint or off-canvas offset is dropped
 * (the T-04-01 bounds-check, now generalized across dimensions — T-02-04-TAP).
 */
class BoardGeometryTest {
    @Test
    fun englishBoardDerivesGridDimensionSeven() {
        assertThat(geometryFor(Boards.English33).gridDimension).isEqualTo(DIMENSION_SEVEN)
    }

    @Test
    fun europeanBoardDerivesGridDimensionSeven() {
        assertThat(geometryFor(Boards.European37).gridDimension).isEqualTo(DIMENSION_SEVEN)
    }

    @Test
    fun asymmetricBoardDerivesGridDimensionEight() {
        assertThat(geometryFor(Boards.Asymmetric39).gridDimension).isEqualTo(DIMENSION_EIGHT)
    }

    @Test
    fun diamondBoardDerivesGridDimensionNine() {
        assertThat(geometryFor(Boards.Diamond41).gridDimension).isEqualTo(DIMENSION_NINE)
    }

    @Test
    fun wieglebBoardDerivesGridDimensionNine() {
        assertThat(geometryFor(Boards.Wiegleb45).gridDimension).isEqualTo(DIMENSION_NINE)
    }

    @Test
    fun everyInPlayCellRoundTripsInsideTheCanvasForAllFootprints() {
        ALL_BOARDS.forEach { spec ->
            val geometry = geometryFor(spec)
            spec.inPlay.forEach { position ->
                val center = geometry.center(position)

                // The cell center falls inside the square canvas...
                assertThat(center.x).isAtLeast(CANVAS_MIN_PX)
                assertThat(center.y).isAtLeast(CANVAS_MIN_PX)
                assertThat(center.x).isAtMost(CANVAS_EDGE_PX)
                assertThat(center.y).isAtMost(CANVAS_EDGE_PX)
                // ...and hit-tests back to its own position.
                assertThat(geometry.positionAt(center)).isEqualTo(position)
            }
        }
    }

    @Test
    fun offFootprintAndOffCanvasOffsetsReturnNullOnALargerFootprint() {
        // Asymmetric-39 (dimension 8): (0,0) is inside the grid but off the cross.
        val geometry = geometryFor(Boards.Asymmetric39)
        val blockedCorner = Position(row = 0, col = 0)

        assertThat(blockedCorner in Boards.Asymmetric39).isFalse()
        assertThat(geometry.positionAt(geometry.center(blockedCorner))).isNull()
        assertThat(geometry.positionAt(Offset(-CANVAS_EDGE_PX, -CANVAS_EDGE_PX))).isNull()
        assertThat(geometry.positionAt(Offset(FAR_OUTSIDE_PX, FAR_OUTSIDE_PX))).isNull()
    }

    @Test
    fun aTapInACellsDeadCornerIsDroppedWhileOneInsideItsHitCircleLands() {
        // WT-5 (D-06d): the corners of an in-play cell's square lie outside its inscribed hit
        // circle (0.5 x cellSize), so a tap there is dropped before the board sees it: it selects
        // nothing and is no wrong tap either, so no feedback of any kind follows.
        val geometry = geometryFor(Boards.English33)
        val cell = Position(row = 3, col = 3)
        val center = geometry.center(cell)
        val nearTheEdge = geometry.cellSize * NEAR_THE_EDGE

        // Diagonal: inside the cell's square (< 0.5 per axis), outside its circle (0.45 x sqrt 2 > 0.5).
        assertThat(geometry.positionAt(center + Offset(nearTheEdge, nearTheEdge))).isNull()
        assertThat(geometry.positionAt(center + Offset(-nearTheEdge, -nearTheEdge))).isNull()
        // Straight out the same distance: inside the circle, so the same tap lands on the cell.
        assertThat(geometry.positionAt(center + Offset(nearTheEdge, 0f))).isEqualTo(cell)
        assertThat(geometry.positionAt(center + Offset(0f, -nearTheEdge))).isEqualTo(cell)
    }

    private fun geometryFor(spec: BoardSpec): BoardGeometry =
        BoardGeometry.forCanvas(spec, Size(CANVAS_EDGE_PX, CANVAS_EDGE_PX))

    private companion object {
        const val CANVAS_EDGE_PX = 720f
        const val CANVAS_MIN_PX = 0f
        const val FAR_OUTSIDE_PX = 1440f

        /** A fraction of a cell short of its edge: inside the square, but past the circle diagonally. */
        const val NEAR_THE_EDGE = 0.45f

        const val DIMENSION_SEVEN = 7
        const val DIMENSION_EIGHT = 8
        const val DIMENSION_NINE = 9

        val ALL_BOARDS =
            listOf(
                Boards.English33,
                Boards.European37,
                Boards.Asymmetric39,
                Boards.Diamond41,
                Boards.Wiegleb45,
            )
    }
}
