package com.app.pegsolitaire.ui.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos

/**
 * Whether the backdrop's scene may be composed yet (the tech-lead's R3 and A6): false on the
 * host's first frame, true from the next. The waves, the lights, the clock's start and the
 * battery-saver read all sit behind it, so Home's first frame costs what a static gradient costs
 * (backdrop spec §7, B-11a).
 *
 * The `State` itself is provided, not its value, so only the readers that call `.value` recompose
 * when it flips. The default is always ready, so a preview or a component test outside the host
 * shows the whole scene.
 */
val LocalBackdropSceneReady = staticCompositionLocalOf<State<Boolean>> { AlwaysReady }

/**
 * The flag, set one frame late: a `LaunchedEffect` starts only after its first composition is
 * applied, and `withFrameNanos` resumes on the next frame, so the scene is never on frame 1. It
 * runs on the Compose frame clock, so the test clock steps it exactly.
 */
@Composable
fun rememberBackdropSceneReady(): State<Boolean> {
    val ready = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        ready.value = true
    }
    return ready
}

private object AlwaysReady : State<Boolean> {
    override val value: Boolean = true
}
