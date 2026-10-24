package com.app.pegsolitaire.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

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
