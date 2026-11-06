package com.app.pegsolitaire.ui.results

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The confetti's pure model (ux-spec D-RC-4, D-RC-5, §4.7; criterion RC-9): the burst is a pure
 * function of its seed, has the counts, shapes, colours and sizes the spec names, and every piece's
 * pose stays where the spec puts it — above the top edge at the start, gone or invisible at the end,
 * never rising, never swaying past its amplitude. No frame clock, no pixel.
 */
class ConfettiBurstTest {
    private val burst = ConfettiBurst.create(SEED, CelebrationFx.CONFETTI_PIECE_COUNT, SPAN)

    @Test
    fun theSameSeedGivesTheSameBurstAndAnotherSeedADifferentOne() {
        assertWithMessage("same seed")
            .that(ConfettiBurst.create(SEED, CelebrationFx.CONFETTI_PIECE_COUNT, SPAN))
            .isEqualTo(burst)
        assertWithMessage("another seed")
            .that(ConfettiBurst.create(SEED + 1, CelebrationFx.CONFETTI_PIECE_COUNT, SPAN))
            .isNotEqualTo(burst)
    }

    @Test
    fun theBurstIsNinetyPiecesThirtyOfEachShape() {
        assertWithMessage("pieces").that(burst).hasSize(NINETY)
        ConfettiShape.entries.forEach { shape ->
            assertWithMessage("$shape").that(burst.count { it.shape == shape }).isEqualTo(THIRTY)
        }
    }

    @Test
    fun everyPieceDrawsOneOfTheSixColoursAtASizeAndSwayWithinItsTokens() {
        burst.forEach { piece ->
            assertWithMessage("colour index").that(piece.colourIndex).isIn(0 until SIX)
            assertWithMessage("size").that(piece.sizePx).isIn(Range.closed(SIZE_MIN, SIZE_MAX))
            assertWithMessage("sway").that(piece.swayPx).isIn(Range.closed(SWAY_MIN, SWAY_MAX))
            assertWithMessage("fall")
                .that(piece.fallMs)
                .isIn(
                    Range.closed(
                        CelebrationFx.CONFETTI_FALL_MIN_MS.toFloat(),
                        CelebrationFx.CONFETTI_FALL_MAX_MS.toFloat(),
                    ),
                )
            assertWithMessage("spawn")
                .that(piece.spawnMs)
                .isIn(Range.closed(0f, CelebrationFx.CONFETTI_SPAWN_MS.toFloat()))
        }
        assertWithMessage("all six colours used").that(burst.map { it.colourIndex }.toSet()).hasSize(SIX)
    }

    @Test
    fun atTheStartEveryPieceIsUnspawnedOrAboveTheTopEdge() {
        burst.forEach { piece ->
            val pose = piece.poseAt(0f, WIDTH, HEIGHT)
            if (pose != null) {
                assertWithMessage("y at the start").that(pose.y).isAtMost(-piece.sizePx)
            }
        }
    }

    @Test
    fun atTheEndEveryPieceIsGoneOrInvisible() {
        burst.forEach { piece ->
            val pose = piece.poseAt(CelebrationFx.CONFETTI_TOTAL_MS.toFloat(), WIDTH, HEIGHT)
            if (pose != null) {
                assertWithMessage("alpha at the end").that(pose.alpha).isWithin(EPSILON).of(0f)
            }
        }
    }

    @Test
    fun noPieceRisesOrSwaysPastItsBand() {
        burst.forEach { piece ->
            var lastY = -Float.MAX_VALUE
            for (ms in 0..CelebrationFx.CONFETTI_TOTAL_MS) {
                val pose = piece.poseAt(ms.toFloat(), WIDTH, HEIGHT) ?: continue
                assertWithMessage("y never decreases at $ms").that(pose.y).isAtLeast(lastY)
                lastY = pose.y
                assertWithMessage("x at $ms")
                    .that(pose.x)
                    .isIn(
                        Range.closed(
                            -piece.sizePx - piece.swayPx - EPSILON,
                            WIDTH + piece.sizePx + piece.swayPx + EPSILON,
                        ),
                    )
                assertWithMessage("alpha at $ms").that(pose.alpha).isIn(Range.closed(0f, ALPHA))
                assertWithMessage("flip at $ms").that(pose.scaleX).isIn(Range.closed(-1f, 1f))
            }
        }
    }

    @Test
    fun aPieceFallsPastTheBottomEdgeAndFadesOnlyOverItsLastFifth() {
        val piece = burst.first()
        val end = piece.spawnMs + piece.fallMs
        val landed = checkNotNull(piece.poseAt(end, WIDTH, HEIGHT))
        assertWithMessage("below the bottom edge").that(landed.y).isWithin(EPSILON).of(HEIGHT + 2 * piece.sizePx)
        val beforeFade = checkNotNull(piece.poseAt(piece.spawnMs + piece.fallMs * BEFORE_FADE, WIDTH, HEIGHT))
        assertWithMessage("full alpha before the fade").that(beforeFade.alpha).isWithin(EPSILON).of(ALPHA)
        val midFade = checkNotNull(piece.poseAt(piece.spawnMs + piece.fallMs * MID_FADE, WIDTH, HEIGHT))
        assertWithMessage("half alpha half-way through the fade").that(midFade.alpha).isWithin(EPSILON).of(ALPHA / 2)
        assertWithMessage("gone after its fall").that(piece.poseAt(end + 1f, WIDTH, HEIGHT)).isNull()
        assertWithMessage("absent before its spawn").that(piece.poseAt(piece.spawnMs - 1f, WIDTH, HEIGHT)).isNull()
    }

    @Test
    fun theSeedIsAPureFunctionOfTheResult() {
        assertWithMessage("same result")
            .that(confettiSeed(REPLAY_ID, LEVEL))
            .isEqualTo(confettiSeed(REPLAY_ID, LEVEL))
        assertWithMessage("the spec's formula")
            .that(confettiSeed(REPLAY_ID, LEVEL))
            .isEqualTo(REPLAY_ID.hashCode() * SEED_MULTIPLIER + LEVEL)
        assertWithMessage("Classic: no level")
            .that(confettiSeed(REPLAY_ID, null))
            .isEqualTo(REPLAY_ID.hashCode() * SEED_MULTIPLIER)
        assertWithMessage("another level")
            .that(confettiSeed(REPLAY_ID, LEVEL + 1))
            .isNotEqualTo(confettiSeed(REPLAY_ID, LEVEL))
    }

    private companion object {
        const val SEED = 20_261_106
        const val NINETY = 90
        const val THIRTY = 30
        const val SIX = 6
        const val SIZE_MIN = 24f
        const val SIZE_MAX = 36f
        const val SWAY_MIN = 24f
        const val SWAY_MAX = 48f
        val SPAN = ConfettiSpan(sizeMinPx = SIZE_MIN, sizeMaxPx = SIZE_MAX, swayMinPx = SWAY_MIN, swayMaxPx = SWAY_MAX)
        const val WIDTH = 1_080f
        const val HEIGHT = 2_400f
        const val EPSILON = 1e-3f
        const val ALPHA = 0.92f
        const val BEFORE_FADE = 0.79f
        const val MID_FADE = 0.90f
        const val REPLAY_ID = "english33"
        const val LEVEL = 7
        const val SEED_MULTIPLIER = 31
    }
}
