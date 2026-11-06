package com.app.pegsolitaire.ui.results

import com.app.pegsolitaire.ui.backdrop.BackdropFrames
import com.app.pegsolitaire.ui.backdrop.BackdropMotion
import com.app.pegsolitaire.ui.backdrop.BackdropMotionState
import com.app.pegsolitaire.ui.backdrop.LiveBackdropClock
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The rays' clock (ux-spec D-RC-3, §4.7's Clock row; criterion RC-5's JVM half), in the backdrop's
 * idiom: the rays reuse the backdrop's clock type with the game screen's idle delay fixed, whatever
 * density the host shows. Driven by virtual-time frames, no composition.
 */
class ResultsRaysClockTest {
    @Test
    fun theRaysClockSettlesAfterTheGameIdleDelayAndThenAwaitsNoFrame() =
        runTest {
            val frames = VirtualFrames()
            val clock = LiveBackdropClock(fixedIdleDelayMs = BackdropMotion.BACKDROP_GAME_IDLE_MS)
            val job = launch { clock.run(frames) }
            delay(BackdropMotion.BACKDROP_GAME_IDLE_MS - BEFORE_IDLE_MS)
            assertWithMessage("running before the idle delay").that(clock.motion).isEqualTo(BackdropMotionState.Running)
            assertWithMessage("time moves").that(clock.timeMs).isGreaterThan(0L)
            delay(BEFORE_IDLE_MS + BackdropMotion.BACKDROP_SETTLE_MS + SLACK_MS)
            assertWithMessage("settled").that(clock.motion).isEqualTo(BackdropMotionState.AtRest)
            val awaited = frames.awaited
            val settledAt = clock.timeMs
            delay(WATCH_MS)
            assertWithMessage("no frame awaited while settled").that(frames.awaited).isEqualTo(awaited)
            assertWithMessage("time still while settled").that(clock.timeMs).isEqualTo(settledAt)

            clock.onInput()
            delay(FRAME_MS * 2)
            assertWithMessage("an input wakes it").that(clock.motion).isEqualTo(BackdropMotionState.Settling)
            delay(BackdropMotion.BACKDROP_SETTLE_MS + SLACK_MS)
            assertWithMessage("awake").that(clock.motion).isEqualTo(BackdropMotionState.Running)
            job.cancel()
        }

    /** One frame every [FRAME_MS] of virtual time, counted. */
    private class VirtualFrames : BackdropFrames {
        var awaited = 0
            private set
        private var nanos = 0L

        override suspend fun awaitFrameNanos(): Long {
            awaited++
            delay(FRAME_MS)
            nanos += FRAME_MS * NANOS_PER_MS
            return nanos
        }
    }

    private companion object {
        const val FRAME_MS = 16L
        const val NANOS_PER_MS = 1_000_000L
        const val BEFORE_IDLE_MS = 1_000L
        const val SLACK_MS = 200L
        const val WATCH_MS = 10_000L
    }
}
