package com.app.pegsolitaire.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.ui.classic.ClassicCatalogRoute
import com.app.pegsolitaire.ui.game.GameRoute
import com.app.pegsolitaire.ui.home.HomeRoute
import com.app.pegsolitaire.ui.results.ResultsRoute

/** The five Results actions, hoisted so [AppNavHost]'s content is test-injectable. */
private typealias ResultsContent =
    @Composable (
        onNextBoard: (String) -> Unit,
        onReplay: (String) -> Unit,
        onCatalog: () -> Unit,
        onHome: () -> Unit,
        onNavigateBack: () -> Unit,
    ) -> Unit

/**
 * The type-safe navigation host wiring the full five-screen spine (NAV-02).
 *
 * `NavHost` over the five `@Serializable` [Routes] with `startDestination = Home`:
 * Home → ClassicCatalog → Gameplay(levelId) → Results(...). Back arrows pop
 * (Catalog→Home, Gameplay→Catalog, Results→Catalog); the Results actions navigate
 * Next/Replay → `Gameplay(id)` (popping up to `ClassicCatalog` so board-hops don't grow
 * the back stack unboundedly — T-02-08-NAV) and Catalog/Home pop back to those
 * destinations. This replaces the Phase-1 direct `GameRoute()` host in `MainActivity`.
 *
 * Each destination's body is a default-valued slot wired to the real `*Route` composable;
 * the slots exist so `NavFlowTest` can drive the graph with lightweight stand-ins (the
 * real routes call `hiltViewModel()`, which needs a Hilt host) while still exercising
 * this function's actual navigation lambdas.
 *
 * @param navController the host controller (defaults to a remembered one).
 * @param homeContent the Home destination body (defaults to [HomeRoute]).
 * @param catalogContent the ClassicCatalog destination body (defaults to [ClassicCatalogRoute]).
 * @param gameplayContent the Gameplay destination body (defaults to [GameRoute]).
 * @param resultsContent the Results destination body (defaults to [ResultsRoute]).
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    homeContent: @Composable (onPlayClassic: () -> Unit) -> Unit = { onPlayClassic ->
        HomeRoute(onPlayClassic = onPlayClassic)
    },
    catalogContent: @Composable (onSelect: (String) -> Unit, onNavigateBack: () -> Unit) -> Unit =
        { onSelect, onNavigateBack ->
            ClassicCatalogRoute(onSelect = onSelect, onNavigateBack = onNavigateBack)
        },
    gameplayContent: @Composable (onFinished: (Results) -> Unit, onNavigateBack: () -> Unit) -> Unit =
        { onFinished, onNavigateBack ->
            GameRoute(onFinished = onFinished, onNavigateBack = onNavigateBack)
        },
    resultsContent: ResultsContent = { onNextBoard, onReplay, onCatalog, onHome, onNavigateBack ->
        ResultsRoute(
            onNextBoard = onNextBoard,
            onReplay = onReplay,
            onCatalog = onCatalog,
            onHome = onHome,
            onNavigateBack = onNavigateBack,
        )
    },
) {
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            // homeContent slot: onPlayClassic
            homeContent { navController.navigate(ClassicCatalog) }
        }
        composable<ClassicCatalog> {
            // catalogContent slot: onSelect, onNavigateBack
            catalogContent(
                { levelId -> navController.navigate(Gameplay(levelId)) },
                { navController.popBackStack() },
            )
        }
        composable<Gameplay> {
            // gameplayContent slot: onFinished, onNavigateBack
            gameplayContent(
                { results -> navController.navigate(results) },
                { navController.popBackStack() },
            )
        }
        composable<Results> {
            // resultsContent slot: onNextBoard, onReplay, onCatalog, onHome, onNavigateBack
            resultsContent(
                { levelId -> navController.toBoardFromCatalog(levelId) },
                { levelId -> navController.toBoardFromCatalog(levelId) },
                { navController.popBackStack(ClassicCatalog, inclusive = false) },
                { navController.popBackStack(Home, inclusive = false) },
                { navController.popBackStack() },
            )
        }
    }
}

/**
 * Navigates to `Gameplay(levelId)` while popping up to `ClassicCatalog`, so replaying or
 * advancing to the next board from Results never grows the back stack across board hops
 * (T-02-08-NAV). Back from the new game returns to the catalog.
 */
private fun NavHostController.toBoardFromCatalog(levelId: String) {
    navigate(Gameplay(levelId)) { popUpTo<ClassicCatalog>() }
}
