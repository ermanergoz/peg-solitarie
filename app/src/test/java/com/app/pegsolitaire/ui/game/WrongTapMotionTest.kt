package com.app.pegsolitaire.ui.game

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

/**
 * The wrong tap's two motions as pure functions of the time since the tap
 * (`docs/ux/invalid-tap-feedback.md` §3.1; WT-7 and WT-8's timing). The reduced-motion mark's
 * hold is a delay, not a motion, and `BoardCanvasWrongTapTest` times it on the board itself.
 *
 * WT-7 asks for exactly this shape — the shake written as a function of elapsed time and sampled
 * at its five keyframes — because it is exact and fast where a clock-driven UI test is neither.
 * Every expected value below is typed in from the spec's table, never computed from the
 * constants the code reads, so a moved keyframe fails here. Runner-free.
 */
class WrongTapMotionTest {
    @Test
    fun theShakePassesThroughEachKeyframeOfTheSpec() {
        // §3.1: 0 ms → 0 · 46 → +1.0 A · 105 → −0.75 A · 165 → +0.45 A · 219 → −0.2 A · 270 → 0.
        val keyframes =
            listOf(
                0f to 0f,
                46f to 1.0f,
                105f to -0.75f,
                165f to 0.45f,
                219f to -0.2f,
                270f to 0f,
            )

        keyframes.forEach { (atMs, want) ->
            assertWithMessage("offset at $atMs ms, in amplitudes")
                .that(WrongTapMotion.shakeOffset(atMs, AMPLITUDE))
                .isWithin(KEYFRAME_TOLERANCE)
                .of(want * AMPLITUDE)
        }
    }

    @Test
    fun theShakeIsScaledByItsAmplitude() {
        // A = 0.10 × cellSize, so the same moment moves a bigger board's cell further.
        val small = WrongTapMotion.shakeOffset(PEAK_MS, SMALL_AMPLITUDE_PX)
        val large = WrongTapMotion.shakeOffset(PEAK_MS, LARGE_AMPLITUDE_PX)

        assertThat(small).isWithin(PX_TOLERANCE).of(SMALL_AMPLITUDE_PX)
        assertThat(large).isWithin(PX_TOLERANCE).of(LARGE_AMPLITUDE_PX)
    }

    @Test
    fun theShakeNeverMovesFurtherThanItsAmplitudeAndRestsOutsideItsWindow() {
        (0..SHAKE_WINDOW_MS).forEach { atMs ->
            assertWithMessage("|offset| at $atMs ms")
                .that(abs(WrongTapMotion.shakeOffset(atMs.toFloat(), AMPLITUDE)))
                .isAtMost(AMPLITUDE)
        }
        assertThat(WrongTapMotion.shakeOffset(BEFORE_THE_TAP_MS, AMPLITUDE)).isEqualTo(0f)
        assertThat(WrongTapMotion.shakeOffset(LONG_AFTER_MS, AMPLITUDE)).isEqualTo(0f)
    }

    @Test
    fun theRingNudgeReaches118PercentAt110MsAndIsBackTo100By260() {
        // §3.1: 1.0× → 1.18× over 110 ms, then back to 1.0× over 150 ms (WT-8).
        assertThat(WrongTapMotion.ringNudgeScale(0f)).isEqualTo(1f)
        assertThat(WrongTapMotion.ringNudgeScale(110f)).isWithin(SCALE_TOLERANCE).of(1.18f)
        assertThat(WrongTapMotion.ringNudgeScale(260f)).isWithin(SCALE_TOLERANCE).of(1f)
        assertThat(WrongTapMotion.ringNudgeScale(LONG_AFTER_MS)).isEqualTo(1f)
        val peak = (0..NUDGE_WINDOW_MS).maxOf { WrongTapMotion.ringNudgeScale(it.toFloat()) }
        assertThat(peak).isWithin(SCALE_TOLERANCE).of(1.18f)
    }

    private companion object {
        const val AMPLITUDE = 1f
        const val KEYFRAME_TOLERANCE = 0.01f
        const val PEAK_MS = 46f
        const val SMALL_AMPLITUDE_PX = 3.3f
        const val LARGE_AMPLITUDE_PX = 6.4f
        const val PX_TOLERANCE = 0.05f
        const val SCALE_TOLERANCE = 0.001f
        const val SHAKE_WINDOW_MS = 270
        const val NUDGE_WINDOW_MS = 260
        const val BEFORE_THE_TAP_MS = -1f
        const val LONG_AFTER_MS = 10_000f
    }
}
