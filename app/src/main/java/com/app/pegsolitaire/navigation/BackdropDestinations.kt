package com.app.pegsolitaire.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.LocalBackdropClock
import com.app.pegsolitaire.ui.backdrop.SceneBackdrop

/**
 * The density a destination carries (backdrop spec §2, the tech-lead's R1): Home the
 * landscape, the game screen — Classic and Challenge alike, one destination — the quiet scene, and
 * everything else the wash. "Everything else" is deliberately the `else` branch, so a destination
 * added later gets Sparse, which is text-safe from edge to edge.
 */
fun NavDestination?.backdropDensity(): BackdropDensity =
    when {
        this?.hasRoute<Home>() == true -> BackdropDensity.Landscape
        this?.hasRoute<Gameplay>() == true -> BackdropDensity.Quiet
        else -> BackdropDensity.Sparse
    }

/**
 * The thin adapter between the back stack and [SceneBackdrop] (R1): it alone reads the committed
 * destination, so a navigation recomposes it and the backdrop, never `AppNavHost`'s body.
 *
 * It must be composed **after** the `NavHost` in the same `Box`: the `NavHost` sets its graph — and
 * restores a saved back stack — synchronously during its own composition, so the synchronous read
 * here gives the right density on the first frame, a process-death restore included (BD-06).
 */
@Composable
internal fun NavBackdrop(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val entry by remember(navController) { navController.currentBackStackEntryFlow }
        .collectAsState(initial = navController.currentBackStackEntry)
    val density = entry?.destination.backdropDensity()
    val clock = LocalBackdropClock.current
    SideEffect { clock.showing(density) }
    SceneBackdrop(density = density, modifier = modifier)
}
