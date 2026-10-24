package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.app.pegsolitaire.ui.components.ChangeEffect
import com.app.pegsolitaire.ui.components.moveInputFocusTo

/** The rescue banner's two actions that move focus to the board stop once they have happened. */
internal enum class BannerAction { Undo, Restart }

/**
 * The rescue banner's focus move (`docs/ux-spec.md` §11.1's focus table: "Undo or Restart from the
 * rescue banner → the board stop", because the banner leaves with the button that had focus).
 *
 * - **Only when the action happened** (loop 1, F-3): the move waits for the undo's or the restart's own
 *   ordinal to advance. An Undo that opens the ad opt-in moves nothing — the banner is still there —
 *   and the offer withdraws the move. A Restart that opens its confirmation waits on it: confirmed,
 *   the restart happens and focus moves; declined, the move is withdrawn (loop 2, L1-1). The banner
 *   leaving for any other reason withdraws either.
 * - **Always a change** (loop 1, F-2): the move goes through [moveInputFocusTo], so a second rescue
 *   still moves TalkBack when the board stop kept input focus from the first.
 * - **Only while touch exploration is on** (D-TBa item 5, F-39): otherwise nothing is awaited.
 *
 * @return what the banner's buttons call right after their action.
 */
@Composable
internal fun rememberBannerFocusMove(
    state: GameUiState,
    boardStopFocus: FocusRequester,
    touchExploration: Boolean,
): (BannerAction) -> Unit {
    val focusManager = LocalFocusManager.current
    val awaited = remember { AwaitedBannerAction() }
    NewOrdinalEffect(state.undoTick) {
        if (awaited.take(BannerAction.Undo)) focusManager.moveInputFocusTo(boardStopFocus)
    }
    NewOrdinalEffect(state.restartTick) {
        if (awaited.take(BannerAction.Restart)) focusManager.moveInputFocusTo(boardStopFocus)
    }
    // After the two ordinals, so an action that happened is taken before anything withdraws it.
    val withdrawn = state.pendingAdOffer != null || !state.showRescueBanner
    LaunchedEffect(withdrawn) {
        if (withdrawn) awaited.action = null
    }
    // The restart confirmation closing without a restart is a decline (a confirm advanced the ordinal).
    ChangeEffect(state.confirmRestart) { wasOpen, isOpen ->
        if (wasOpen && !isOpen) awaited.action = null
    }
    return remember(awaited, touchExploration) { { action -> awaited.action = action.takeIf { touchExploration } } }
}

/** The banner action whose focus move is awaited. Plain state: read and written on the main thread only. */
private class AwaitedBannerAction(
    var action: BannerAction? = null,
) {
    /** Whether [expected] was awaited; either way, nothing is awaited afterwards. */
    fun take(expected: BannerAction): Boolean = (action == expected).also { action = null }
}
