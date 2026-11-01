package com.app.pegsolitaire.navigation

import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.app.pegsolitaire.ui.window.LocalSystemBars
import com.app.pegsolitaire.ui.window.SystemBarsState

/**
 * The bars a destination asks for (docs/ux-spec.md D-05g part 1; docs/tech-design.md §3.8 (a)), in
 * the shape of [backdropDensity] beside it: Home (D-05h, founder 2026-11-02), the game screen —
 * Classic and Challenge alike, one destination — and Results hide both bars, and everything else
 * shows them. "Everything else" is
 * deliberately the `else` branch, so a destination added later shows its bars.
 */
fun NavDestination?.systemBarsState(): SystemBarsState =
    when {
        this?.hasRoute<Home>() == true -> SystemBarsState.Hidden
        this?.hasRoute<Gameplay>() == true -> SystemBarsState.Hidden
        this?.hasRoute<Results>() == true -> SystemBarsState.Hidden
        else -> SystemBarsState.Shown
    }

/**
 * The thin adapter between the back stack and the window's bars (§3.8 (a), (b)). It alone reads the
 * committed destination for the bars, so a navigation recomposes it and never `AppNavHost`'s body.
 *
 * - **In the frame the destination is first composed.** It must be composed **after** the `NavHost`
 *   in the same `Box`, as [NavBackdrop] is: the `NavHost` sets its graph, and restores a saved back
 *   stack, during its own composition, so the synchronous read here gives the right state on the
 *   first frame, a process-death restore whose top is the board included. The request is made in a
 *   `SideEffect`, which runs before that frame's layout and draw.
 * - **Again on every resume.** An ad's own activity, a Play Games UI, the launcher, the lock screen or
 *   the shade can leave the window's bars shown; [LifecycleResumeEffect] asks again at each
 *   `ON_RESUME` of the composition's lifecycle owner — the activity's, since this sits outside the
 *   `NavHost`'s per-entry content. A pause gives nothing back.
 * - **For the dialogs.** [barsState] is the host's own state, which it provides to the dialog windows
 *   that mirror it (`MirrorSystemBars`); it is kept current even with no [hostWindow].
 *
 * With no [hostWindow] (previews, and the host tests that hand none) nothing is applied.
 */
@Composable
internal fun NavSystemBars(
    navController: NavHostController,
    hostWindow: Window?,
    barsState: MutableState<SystemBarsState>,
) {
    val entry by remember(navController) { navController.currentBackStackEntryFlow }
        .collectAsState(initial = navController.currentBackStackEntry)
    val state = entry?.destination.systemBarsState()
    val seam = LocalSystemBars.current
    SideEffect {
        barsState.value = state
        if (hostWindow != null) seam.apply(hostWindow, state)
    }
    LifecycleResumeEffect(state, hostWindow, seam) {
        if (hostWindow != null) seam.apply(hostWindow, state)
        // Nothing to undo on a pause: the bars stay as asked until the next request.
        onPauseOrDispose {}
    }
}
