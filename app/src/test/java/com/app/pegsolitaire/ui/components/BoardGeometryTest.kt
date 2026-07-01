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

    private fun geometryFor(spec: BoardSpec): BoardGeometry =
        BoardGeometry.forCanvas(spec, Size(CANVAS_EDGE_PX, CANVAS_EDGE_PX))

    private companion object {
        const val CANVAS_EDGE_PX = 720f
        const val CANVAS_MIN_PX = 0f
        const val FAR_OUTSIDE_PX = 1440f

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
