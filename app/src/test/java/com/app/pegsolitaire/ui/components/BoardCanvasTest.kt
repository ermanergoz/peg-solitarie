package com.app.pegsolitaire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.MoveTransition
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

    /**
     * The winning-move hint overlay (D-01) is driven by these two proportional radii. The
     * source glow (A) must be strictly larger than the selection ring so a hint never reads
     * as a selection, and the target ring (B) strictly larger than the legal-move dot so it
     * reads as a target, not a dot — the visual-distinctness contract the highlight relies on.
     */
    @Test
    fun hintRingRadiiAreDistinctFromSelectionAndDot() {
        assertThat(geometry.hintSourceRingRadius).isGreaterThan(geometry.selectedRingRadius)
        assertThat(geometry.hintTargetRingRadius).isGreaterThan(geometry.destinationDotRadius)
    }

    /** Both hint rings scale proportionally with the measured cell size (resolution-independent). */
    @Test
    fun hintRingRadiiAreProportionalToCellSize() {
        assertThat(geometry.hintSourceRingRadius)
            .isEqualTo(BoardMetrics.HINT_SOURCE_RING_RATIO * geometry.cellSize)
        assertThat(geometry.hintTargetRingRadius)
            .isEqualTo(BoardMetrics.HINT_TARGET_RING_RATIO * geometry.cellSize)
    }

    /**
     * Regression (first-move flicker): the draw phase renders the in-flight jumper as soon
     * as `animating` is non-null, but the `Animatable` holding its position is snapped onto
     * board coordinates by a `LaunchedEffect` that only resumes after that frame is drawn.
     * On a game's very first move the Animatable still holds its initial [Offset.Zero] — the
     * canvas top-left — so the jumper must NOT be positioned from it until the effect reports
     * the move as prepared.
     */
    @Test
    fun jumperRendersAtItsSourceHoleBeforeTheAnimationIsPrepared() {
        val transition = MoveTransition(from = Position(1, 3), to = Position(3, 3), removed = Position(2, 3), id = 1)

        val center =
            jumperCenter(
                transition = transition,
                preparedMoveId = null,
                geometry = geometry,
                animatedOffset = Offset.Zero,
            )

        assertThat(center).isEqualTo(geometry.center(transition.from))
        assertThat(center).isNotEqualTo(Offset.Zero)
    }

    /** Once the effect has snapped the Animatable for this move, the animated value wins. */
    @Test
    fun jumperFollowsTheAnimationOncePrepared() {
        val transition = MoveTransition(from = Position(1, 3), to = Position(3, 3), removed = Position(2, 3), id = 1)
        val midFlight = Offset(MID_FLIGHT_PX, MID_FLIGHT_PX)

        val center =
            jumperCenter(
                transition = transition,
                preparedMoveId = transition.id,
                geometry = geometry,
                animatedOffset = midFlight,
            )

        assertThat(center).isEqualTo(midFlight)
    }

    /**
     * `MoveTransition.id` is `moveHistory.size`, so ids recycle after an undo. A replayed
     * move id must not be treated as already-prepared just because a prior move happened to
     * carry the same id — otherwise the jumper would render for a frame at the previous
     * move's destination.
     */
    @Test
    fun aRecycledMoveIdIsNotTreatedAsPrepared() {
        val replayed = MoveTransition(from = Position(3, 1), to = Position(3, 3), removed = Position(3, 2), id = 1)
        val staleDestination = geometry.center(Position(3, 3))

        val center =
            jumperCenter(
                transition = replayed,
                preparedMoveId = null,
                geometry = geometry,
                animatedOffset = staleDestination,
            )

        assertThat(center).isEqualTo(geometry.center(replayed.from))
    }

    /**
     * Regression (removal blink): the jumped-over peg's fade is driven by the same
     * asynchronously-prepared Animatable, which rests at fully-faded once a move ends. On
     * every move after the first it must not be drawn from that stale value, or the doomed
     * peg blinks out and back in for a frame before its fade actually starts.
     */
    @Test
    fun doomedPegIsWholeBeforeTheRemovalFadeIsPrepared() {
        val transition = MoveTransition(from = Position(1, 3), to = Position(3, 3), removed = Position(2, 3), id = 2)
        val fadedOutByPreviousMove = 0f

        val progress =
            removalAlpha(
                transition = transition,
                preparedMoveId = null,
                animatedProgress = fadedOutByPreviousMove,
            )

        assertThat(progress).isEqualTo(1f)
    }

    /** Once prepared, the removal fade follows its animation. */
    @Test
    fun removalFadeFollowsTheAnimationOncePrepared() {
        val transition = MoveTransition(from = Position(1, 3), to = Position(3, 3), removed = Position(2, 3), id = 2)
        val midFade = 0.4f

        val progress =
            removalAlpha(
                transition = transition,
                preparedMoveId = transition.id,
                animatedProgress = midFade,
            )

        assertThat(progress).isEqualTo(midFade)
    }

    /** With no move in flight the jumper is not drawn, so the animated value passes through. */
    @Test
    fun noTransitionLeavesTheAnimatedValueUntouched() {
        val resting = Offset(MID_FLIGHT_PX, MID_FLIGHT_PX)

        val center =
            jumperCenter(
                transition = null,
                preparedMoveId = null,
                geometry = geometry,
                animatedOffset = resting,
            )

        assertThat(center).isEqualTo(resting)
    }

    private companion object {
        const val CANVAS_EDGE_PX = 700f
        const val FAR_OUTSIDE_PX = 1400f
        const val MID_FLIGHT_PX = 123f
    }
}
