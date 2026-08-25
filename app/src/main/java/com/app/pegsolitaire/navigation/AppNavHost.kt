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
import com.app.pegsolitaire.ui.settings.SettingsRoute

/**
 * The Home actions, hoisted so [AppNavHost]'s content is test-injectable: the two mode
 * entries, the two PERS-03 Continue entries (each carrying what its resume needs), and the
 * Settings gear.
 */
private typealias HomeContent =
    @Composable (
        onPlayClassic: () -> Unit,
        onPlayChallenge: (level: Int) -> Unit,
        onResumeClassic: (levelId: String) -> Unit,
        onResumeChallenge: (level: Int) -> Unit,
        onOpenSettings: () -> Unit,
    ) -> Unit

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
 * The type-safe navigation host wiring the full six-screen spine (NAV-02).
 *
 * `NavHost` over the six `@Serializable` [Routes] with `startDestination = Home`:
 * Home → ClassicCatalog → Gameplay(levelId) → Results(...), plus Home → Settings
 * (the top-bar gear, D-14). Back arrows pop (Catalog→Home, Gameplay→Catalog,
 * Results→Catalog, Settings→Home); the Results Replay action navigates →
 * `Gameplay(id)` (popping up to `ClassicCatalog` so board-hops don't grow the back
 * stack unboundedly — T-02-08-NAV) and Catalog/Home pop back to those destinations.
 * This replaces the Phase-1 direct `GameRoute()` host in `MainActivity`.
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
 * @param settingsContent the Settings destination body (defaults to [SettingsRoute], D-14).
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    homeContent: HomeContent =
        { onPlayClassic, onPlayChallenge, onResumeClassic, onResumeChallenge, onOpenSettings ->
            HomeRoute(
                onPlayClassic = onPlayClassic,
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
                onOpenSettings = onOpenSettings,
            )
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
    settingsContent: @Composable (onBack: () -> Unit) -> Unit = { onBack ->
        SettingsRoute(onBack = onBack)
    },
) {
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            // homeContent slot: onPlayClassic, onPlayChallenge(level),
            // onResumeClassic(levelId), onResumeChallenge(level), onOpenSettings.
            // Every value comes from Home's UiState (the persisted ChallengeProgress level,
            // CHAL-05; the stored snapshot's board id, PERS-03) — the host carries what it
            // is handed, it never picks a level or a board itself.
            homeContent(
                { navController.navigate(ClassicCatalog) },
                { level -> navController.toChallengeBoard(level) },
                { levelId -> navController.navigate(Gameplay(levelId = levelId, resume = true)) },
                { level -> navController.toChallengeBoard(level, resume = true) },
                { navController.navigate(Settings) },
            )
        }
        composable<Settings> {
            // settingsContent slot: onBack (pops back to Home, D-14)
            settingsContent { navController.popBackStack() }
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
 * (T-03-11): each entry/Next-level/Retry hop replaces the current gameplay+results pair,
 * leaving only Home + the current board. The Home tile passes the player's persisted
 * current level (CHAL-05); Next level passes N+1 (D-01); Retry passes the same N and so
 * reloads the identical deterministic board (D-10). An out-of-range level is clamped at
 * the `GameViewModel` boundary, so a corrupt stored value can never load a bad board
 * (T-05-11).
 *
 * [resume] is set only by the Home Continue affordance (PERS-03): the same destination,
 * asked to rebuild the saved in-progress game instead of starting the level fresh.
 */
private fun NavHostController.toChallengeBoard(
    level: Int,
    resume: Boolean = false,
) {
    navigate(Gameplay(challengeLevel = level, resume = resume)) { popUpTo<Home>() }
}
