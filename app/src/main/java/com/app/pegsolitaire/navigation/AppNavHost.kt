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

/** The Results actions, hoisted so [AppNavHost]'s content is test-injectable. */
private typealias ResultsContent =
    @Composable (
        onReplay: (String) -> Unit,
        onCatalog: () -> Unit,
        onHome: () -> Unit,
        onNextLevel: (Int) -> Unit,
        onRetry: (Int) -> Unit,
        onNavigateBack: () -> Unit,
    ) -> Unit

/**
 * The type-safe navigation host wiring the full five-screen spine (NAV-02).
 *
 * `NavHost` over the five `@Serializable` [Routes] with `startDestination = Home`:
 * Home → ClassicCatalog → Gameplay(levelId) → Results(...). Back arrows pop
 * (Catalog→Home, Gameplay→Catalog, Results→Catalog); the Results Replay action
 * navigates → `Gameplay(id)` (popping up to `ClassicCatalog` so board-hops don't grow
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
    homeContent: @Composable (onPlayClassic: () -> Unit, onPlayChallenge: () -> Unit) -> Unit =
        { onPlayClassic, onPlayChallenge ->
            HomeRoute(onPlayClassic = onPlayClassic, onPlayChallenge = onPlayChallenge)
        },
    catalogContent: @Composable (onSelect: (String) -> Unit, onNavigateBack: () -> Unit) -> Unit =
        { onSelect, onNavigateBack ->
            ClassicCatalogRoute(onSelect = onSelect, onNavigateBack = onNavigateBack)
        },
    gameplayContent: @Composable (onFinished: (Results) -> Unit, onNavigateBack: () -> Unit) -> Unit =
        { onFinished, onNavigateBack ->
            GameRoute(onFinished = onFinished, onNavigateBack = onNavigateBack)
        },
    resultsContent: ResultsContent = { onReplay, onCatalog, onHome, onNextLevel, onRetry, onNavigateBack ->
        ResultsRoute(
            onReplay = onReplay,
            onCatalog = onCatalog,
            onHome = onHome,
            onNextLevel = onNextLevel,
            onRetry = onRetry,
            onNavigateBack = onNavigateBack,
        )
    },
) {
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            // homeContent slot: onPlayClassic, onPlayChallenge
            homeContent(
                { navController.navigate(ClassicCatalog) },
                { navController.navigate(Gameplay(challengeLevel = CHALLENGE_ENTRY_LEVEL)) },
            )
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
            // resultsContent slot: onReplay, onCatalog, onHome, onNextLevel, onRetry, onNavigateBack
            resultsContent(
                { levelId -> navController.toBoardFromCatalog(levelId) },
                { navController.popBackStack(ClassicCatalog, inclusive = false) },
                { navController.popBackStack(Home, inclusive = false) },
                { nextLevel -> navController.toChallengeBoard(nextLevel) },
                { sameLevel -> navController.toChallengeBoard(sameLevel) },
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

/**
 * Hops to a Challenge board (`Gameplay(challengeLevel = level)`) while popping up to
 * [Home], so the infinite Challenge ladder never grows the back stack unboundedly
 * (T-03-11): each Next-level/Retry hop replaces the current gameplay+results pair, leaving
 * only Home + the current board. Next level passes N+1 (D-01); Retry passes the same N and
 * so reloads the identical deterministic board (D-10).
 */
private fun NavHostController.toChallengeBoard(level: Int) {
    navigate(Gameplay(challengeLevel = level)) { popUpTo<Home>() }
}

/**
 * The Challenge level the Home tile enters (D-04). This phase tracks progress in memory
 * and always starts a Challenge run at level 1 (the `ChallengeProgress` initial value);
 * Phase 5 (CHAL-05) will source the highest-level-reached from persistence here.
 */
private const val CHALLENGE_ENTRY_LEVEL = 1
