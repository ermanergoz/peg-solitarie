package com.app.pegsolitaire.ui.backdrop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.MotionDurationScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.coroutineContext

/** What the backdrop's clock is doing, for the semantics hook (the tech-lead's A2, E5). */
enum class BackdropMotionState {
    /** Moving at full rate. */
    Running,

    /** Easing its rate to 0 after idle, or back to 1 on a wake. */
    Settling,

    /** Still: animator scale 0, battery saver, a settled clock, or a Sparse screen. */
    AtRest,
}

/**
 * The backdrop's one clock (B-24, the tech-lead's A2): the scene and the gel sheen read [timeMs]
 * in their draw phase only, so a frame of motion re-records one draw and recomposes nothing. When
 * [isStatic] they draw the rest pose instead.
 */
@Stable
interface BackdropClock {
    /** Backdrop time in ms: scaled by the animator duration scale, paused while settled. */
    val timeMs: Long

    /** True when the rest pose is drawn: animator scale 0, battery saver, or no clock at all. */
    val isStatic: Boolean

    /** Running, settling or at rest. */
    val motion: BackdropMotionState

    /** A pointer-down or key-down somewhere in the window: restarts the idle timer, wakes a settled clock. */
    fun onInput() = Unit

    /** The density now behind the screen, which sets the idle delay; a change wakes the clock. */
    fun showing(density: BackdropDensity) = Unit
}

/** No clock: always the rest pose. The default outside the host, so a preview or a lone tile draws no sheen. */
object StaticBackdropClock : BackdropClock {
    override val timeMs: Long = 0L
    override val isStatic: Boolean = true
    override val motion: BackdropMotionState = BackdropMotionState.AtRest
}

/** A clock stopped at [timeMs], for previews and same-run pixel comparisons that need one exact pose. */
class FixedBackdropClock(
    override val timeMs: Long,
) : BackdropClock {
    override val isStatic: Boolean = false
    override val motion: BackdropMotionState = BackdropMotionState.AtRest
}

/**
 * The seam the clock waits for frames through. Production waits with
 * [withInfiniteAnimationFrameNanos], the API `rememberInfiniteTransition` uses, so the Compose test
 * harness treats the backdrop exactly as it treated the retired Home orbs; a test that needs the
 * clock to run injects frames from the test clock.
 */
fun interface BackdropFrames {
    suspend fun awaitFrameNanos(): Long
}

/** Production frames: the infinite-animation frame clock. */
val InfiniteBackdropFrames = BackdropFrames { withInfiniteAnimationFrameNanos { it } }

/** The clock the host provides; [StaticBackdropClock] outside it. */
val LocalBackdropClock = staticCompositionLocalOf<BackdropClock> { StaticBackdropClock }

/** The host's live clock. Created once in `AppNavHost`; driven by [BackdropClockDriver]. */
@Composable
fun rememberBackdropClock(): BackdropClock = remember { LiveBackdropClock() }

/**
 * Runs [clock] once [sceneReady] flips (A6): nothing here, not even the battery-saver read, happens
 * on the host's first frame. Reads the flag in its own scope, so the flip recomposes only this.
 */
@Composable
internal fun BackdropClockDriver(
    clock: BackdropClock,
    sceneReady: State<Boolean>,
    frames: BackdropFrames,
) {
    val live = clock as? LiveBackdropClock ?: return
    if (!sceneReady.value) return
    val powerSave by rememberPowerSaveMode()
    val animationsEnabled = rememberAnimationsEnabled()
    val runs = animationsEnabled && !powerSave
    SideEffect { live.static = !runs }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(live, runs, lifecycle, frames) {
        if (!runs) {
            live.motionState = BackdropMotionState.AtRest
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { live.run(frames) }
    }
}

/**
 * The live clock's state machine (B-22 and the tech-lead's A5): the rate eases to 0 over
 * `BACKDROP_SETTLE_MS` after the density's idle delay without input, and back to 1 on a wake (an
 * input, a navigation or a resume). Only the backdrop time is scaled by the animator scale; the
 * idle timer and the eases run on unscaled frame time, so delays set in seconds stay seconds.
 * Settled, it awaits no frame at all until something wakes it.
 */
internal class LiveBackdropClock : BackdropClock {
    private val time = mutableLongStateOf(0L)
    private var timeExact = 0.0
    override val timeMs: Long get() = time.longValue

    var static by mutableStateOf(true)
    override val isStatic: Boolean get() = static

    var motionState by mutableStateOf(BackdropMotionState.AtRest)
    override val motion: BackdropMotionState get() = motionState

    private var density by mutableStateOf(BackdropDensity.Landscape)
    private val inputs = Channel<Unit>(Channel.CONFLATED)
    private var rate = 0f
    private var started = false

    override fun onInput() {
        inputs.trySend(Unit)
    }

    override fun showing(density: BackdropDensity) {
        this.density = density
    }

    suspend fun run(frames: BackdropFrames) {
        val ease = Ease()
        // A resume counts as input (A5.2): a clock that had settled eases back in rather than staying frozen.
        if (!started) {
            rate = RATE_FULL
        } else if (rate != RATE_FULL) {
            ease.towards(rate, RATE_FULL)
        }
        started = true
        var idleMs = 0.0
        var shown = density
        var lastNanos = NO_FRAME
        while (true) {
            if (inputs.tryReceive().isSuccess || density != shown) {
                shown = density
                idleMs = 0.0
                if (ease.target != RATE_FULL) ease.towards(rate, RATE_FULL)
            }
            if (ease.done && rate == RATE_NONE) {
                motionState = BackdropMotionState.AtRest
                awaitWake()
                ease.towards(RATE_NONE, RATE_FULL)
                idleMs = 0.0
                shown = density
                lastNanos = NO_FRAME
            }
            motionState = if (ease.done) BackdropMotionState.Running else BackdropMotionState.Settling
            val now = frames.awaitFrameNanos()
            val dtMs = if (lastNanos == NO_FRAME) 0.0 else (now - lastNanos) / NANOS_PER_MS
            lastNanos = now
            idleMs += dtMs
            if (ease.target == RATE_FULL && idleMs >= idleDelayMs(shown)) ease.towards(rate, RATE_NONE)
            rate = ease.step(dtMs)
            val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: DEFAULT_SCALE
            // "Remove animations" can land between two frames: the next frame's callbacks run
            // before the recomposition that stops this loop, so a frame may read scale 0. Such a
            // frame is the rest path and leaves the time where it is; dividing by it would make
            // the time infinite (or NaN) for good (the code-auditor's F1).
            if (scale > NO_SCALE && scale.isFinite()) {
                timeExact += dtMs * rate / scale
                time.longValue = timeExact.toLong()
            }
        }
    }

    private suspend fun awaitWake() {
        merge(inputs.receiveAsFlow(), snapshotFlow { density }.drop(1)).first()
    }

    private fun idleDelayMs(density: BackdropDensity): Long =
        when (density) {
            BackdropDensity.Landscape -> BackdropMotion.BACKDROP_HOME_IDLE_MS
            BackdropDensity.Quiet -> BackdropMotion.BACKDROP_GAME_IDLE_MS
            // Nothing moves on Sparse: it settles as soon as the cross-fade into it has ended.
            BackdropDensity.Sparse -> ClayAnim.NAV_TRANSITION_MS.toLong()
        }

    /** One rate ease, `FastOutSlowIn` over [BackdropMotion.BACKDROP_SETTLE_MS]. */
    private class Ease {
        var target = RATE_FULL
            private set
        private var from = RATE_FULL
        private var elapsedMs = SETTLE_MS
        val done: Boolean get() = elapsedMs >= SETTLE_MS

        fun towards(
            current: Float,
            to: Float,
        ) {
            from = current
            target = to
            elapsedMs = 0.0
        }

        fun step(dtMs: Double): Float {
            elapsedMs = (elapsedMs + dtMs).coerceAtMost(SETTLE_MS)
            val f = FastOutSlowInEasing.transform((elapsedMs / SETTLE_MS).toFloat())
            return from + (target - from) * f
        }
    }

    private companion object {
        const val RATE_FULL = 1f
        const val RATE_NONE = 0f
        const val NO_FRAME = -1L
        const val NANOS_PER_MS = 1_000_000.0
        const val DEFAULT_SCALE = 1f

        /** The animator scale "Remove animations" reports; time only runs above it. */
        const val NO_SCALE = 0f
        val SETTLE_MS: Double = BackdropMotion.BACKDROP_SETTLE_MS.toDouble()
    }
}
