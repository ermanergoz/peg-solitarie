package com.app.pegsolitaire.ui.backdrop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the backdrop's scene may be composed yet (the tech-lead's R3, A6 and J-4b): false on the
 * host's first frame, and true once Home's entrance cascade has ended (B-11b). The waves, the
 * lights, the clock's start and the battery-saver read all sit behind it, so Home's first frame
 * costs what a static gradient costs (backdrop spec §7, B-11a), and the scene's first draw lands on
 * a still Home rather than inside its entrance.
 *
 * The `State` itself is provided, not its value, so only the readers that call `.value` recompose
 * when it flips. The default is always ready, so a preview or a component test outside the host
 * shows the whole scene.
 */
val LocalBackdropSceneReady = staticCompositionLocalOf<State<Boolean>> { AlwaysReady }

/**
 * The flag, set [BackdropMotion.BACKDROP_SCENE_OPEN_MS] after the gate's first awaited frame (the
 * tech-lead's J-4b A1). A `LaunchedEffect` starts only after its first composition is applied, so
 * the wait's first frame is the host's second and the scene is never on frame 1.
 *
 * The wait is a throwaway tween on the Compose frame clock, the clock `clayEntrance`'s tweens run
 * on, under the same `MotionDurationScale`: it elapses in animator-scaled time, as Home's cascade
 * does, so at scale 2 it waits twice as long and the open still follows the cascade's end. At scale
 * 0 a tween ends on its first frame, so with animations off the flag flips on frame 2, as it did
 * before B-11b. No wall clock, and no effect keyed on the reduced-motion gate (tech design §16 #28).
 */
@Composable
fun rememberBackdropSceneReady(): State<Boolean> {
    val ready = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Animatable(OPEN_WAIT_START).animateTo(
            targetValue = OPEN_WAIT_END,
            animationSpec = tween(BackdropMotion.BACKDROP_SCENE_OPEN_MS, easing = LinearEasing),
        )
        ready.value = true
    }
    return ready
}

private object AlwaysReady : State<Boolean> {
    override val value: Boolean = true
}

/** The open's wait runs a value it never shows from here to [OPEN_WAIT_END]: only its length counts. */
private const val OPEN_WAIT_START = 0f
private const val OPEN_WAIT_END = 1f
