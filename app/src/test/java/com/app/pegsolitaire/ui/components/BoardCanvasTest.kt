package com.app.pegsolitaire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Fast JVM coverage of the tappable surface the [BoardCanvas] exposes — its
 * hoisted [BoardGeometry]. Driving the math directly (no Robolectric) validates
 * the security-relevant behavior the Canvas depends on: a tap is mapped to an
 * in-play [Position] only when it lands inside the English-33 footprint and within
 * a cell's hit radius (T-04-01), otherwise it is dropped.
 *
 * Full on-device render + tap-to-jump is covered end-to-end by Plan 05's flow test.
 */
class BoardCanvasTest {
    private val spec = Boards.English33
    private val geometry = BoardGeometry.forCanvas(spec, Size(CANVAS_EDGE_PX, CANVAS_EDGE_PX))

    @Test
    fun zeroSizeGeometryIsNotReadyAndDropsTaps() {
        val unmeasured = BoardGeometry.forCanvas(spec, Size.Zero)

        assertThat(unmeasured.isReady).isFalse()
        assertThat(unmeasured.positionAt(Offset.Zero)).isNull()
    }

    @Test
    fun cellSizeFillsTheSquareCanvas() {
        assertThat(geometry.isReady).isTrue()
        assertThat(geometry.cellSize).isEqualTo(CANVAS_EDGE_PX / geometry.gridDimension)
    }

    @Test
    fun tappingACellCenterMapsToThatPosition() {
        val centerHole = Position(row = 3, col = 3)

        assertThat(geometry.positionAt(geometry.center(centerHole))).isEqualTo(centerHole)
    }

    @Test
    fun everyInPlayCellCenterHitTestsToItself() {
        spec.inPlay.forEach { position ->
            assertThat(geometry.positionAt(geometry.center(position))).isEqualTo(position)
        }
    }

    @Test
    fun tappingABlockedCornerIsDropped() {
        val corner = Position(row = 0, col = 0)

        assertThat(corner in spec).isFalse()
        assertThat(geometry.positionAt(geometry.center(corner))).isNull()
    }

    @Test
    fun tappingOutsideTheBoardIsDropped() {
        assertThat(geometry.positionAt(Offset(-CANVAS_EDGE_PX, -CANVAS_EDGE_PX))).isNull()
        assertThat(geometry.positionAt(Offset(FAR_OUTSIDE_PX, FAR_OUTSIDE_PX))).isNull()
    }

    private companion object {
        const val CANVAS_EDGE_PX = 700f
        const val FAR_OUTSIDE_PX = 1400f
    }
}
