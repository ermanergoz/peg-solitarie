package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope

/**
 * Runs [onNew] each time [ordinal] rises past the last value this composition saw — and never for
 * the value the composition first sees, whatever it is.
 *
 * The wrong tap's screen-level channels — the reject haptic and the announcement (D-06) — fire
 * through this rather than through the house `LaunchedEffect(tick) { if (tick > 0) }` idiom. Both
 * keep a fresh state at 0 quiet (WT-6), but only this one also keeps quiet a screen composed from
 * scratch with a carried ordinal: a rotation recreates the Activity while the ViewModel keeps its
 * state, and a `> 0` guard would then buzz and announce a wrong tap the player made before it.
 * "Once per wrong tap" (§3.4) is the contract, and this is how it is kept. The board's own answer
 * keeps the same rule in composition (`rememberWrongTapFeedback`), so its mark can show in the
 * tap's own frame.
 */
@Composable
internal fun NewOrdinalEffect(
    ordinal: Int,
    onNew: suspend CoroutineScope.() -> Unit,
) {
    val watch = remember { OrdinalWatch(ordinal) }
    val latestOnNew by rememberUpdatedState(onNew)
    LaunchedEffect(ordinal) {
        if (watch.advancedTo(ordinal)) latestOnNew()
    }
}

/**
 * The last ordinal an effect saw. Plain state, not snapshot state: it is read and written only
 * inside the effect, never in composition.
 */
internal class OrdinalWatch(
    private var last: Int,
) {
    /** Whether [ordinal] is above the last value seen; either way it becomes the last value seen. */
    fun advancedTo(ordinal: Int): Boolean {
        val advanced = ordinal > last
        last = ordinal
        return advanced
    }
}
