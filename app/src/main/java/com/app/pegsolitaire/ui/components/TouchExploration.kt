package com.app.pegsolitaire.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Whether touch exploration is on (`AccessibilityManager.isTouchExplorationEnabled`): the platform's
 * screen-reader signal, which TalkBack always sets (D-TBa item 5, as its loop-5 line names the gate).
 * The board's cells and its board stop take input focus only while this is `true`, and no focus move
 * is requested while it is `false`, so with TalkBack off Tab and D-pad traversal are exactly what they
 * were (TB-11).
 *
 * Kept current by the manager's own change listener, added and removed with the composition. Called
 * only from interactive boards, so a read-only board registers nothing (Home's first frame, §12 of
 * the build brief).
 */
@Composable
internal fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        // Re-read once subscribed, so a change between composition and this effect is not lost.
        enabled = manager?.isTouchExplorationEnabled == true
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

/**
 * How long a focus move waits between clearing input focus and requesting it on its target: longer
 * than Compose's 100 ms batching of accessibility events, so the cleared state is sent first and the
 * target's focus is always a change — and TalkBack follows it even when the target had input focus
 * already (loop 1, F-2).
 */
internal const val FOCUS_MOVE_GAP_MS = 200L

/**
 * Moves input focus to [target] so that it is always a change TalkBack is sent (loop 1, F-2): a
 * request on a target that already has input focus changes nothing, so no focus event is sent and
 * TalkBack stays where it was. Clearing first, then waiting [FOCUS_MOVE_GAP_MS], makes the target's
 * focus go from false to true every time. Callers make this move only while touch exploration is on.
 */
internal suspend fun FocusManager.moveInputFocusTo(target: FocusRequester) {
    clearFocus(force = true)
    delay(FOCUS_MOVE_GAP_MS)
    target.requestFocus()
}
