package com.app.pegsolitaire.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.ui.classic.ClassicCatalogRoute
import com.app.pegsolitaire.ui.game.GameRoute
import com.app.pegsolitaire.ui.home.HomeRoute
import com.app.pegsolitaire.ui.onboarding.TutorialRoute
import com.app.pegsolitaire.ui.results.ResultsRoute
import com.app.pegsolitaire.ui.settings.SettingsRoute

/**
 * The Home actions, hoisted so [AppNavHost]'s content is test-injectable: the two mode
 * entries, the two PERS-03 Continue entries (each carrying what its resume needs), the
 * Settings gear, and the first-run tutorial auto-start (D-02a).
 *
 * `onStartTutorial` is the only one of the six the player cannot press: Home carries no
 * tutorial affordance (D-02), so the route fires it from its own effect while the standing
 * flag says the tutorial is still owed.
 */
private typealias HomeContent =
    @Composable (
        onPlayClassic: () -> Unit,
        onPlayChallenge: (level: Int) -> Unit,
        onResumeClassic: (levelId: String) -> Unit,
        onResumeChallenge: (level: Int) -> Unit,
        onOpenSettings: () -> Unit,
        onStartTutorial: () -> Unit,
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
 * Finishing a board reaches Results *through* [toResults], which pops the completed
 * `Gameplay` entry, so Back from Results cannot return to the board just finished — it
 * lands on the catalog (Classic launched from it) or Home (Challenge, and Classic resumed
 * via Continue). This replaces the Phase-1 direct `GameRoute()` host in `MainActivity`.
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
 * @param tutorialContent the Tutorial destination body (defaults to [TutorialRoute], D-01g).
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    homeContent: HomeContent =
        { onPlayClassic, onPlayChallenge, onResumeClassic, onResumeChallenge, onOpenSettings, onStartTutorial ->
            HomeRoute(
                onPlayClassic = onPlayClassic,
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
                onOpenSettings = onOpenSettings,
                onStartTutorial = onStartTutorial,
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
    settingsContent: @Composable (onBack: () -> Unit, onShowHowToPlay: () -> Unit) -> Unit =
        { onBack, onShowHowToPlay ->
            SettingsRoute(onBack = onBack, onShowHowToPlay = onShowHowToPlay)
        },
    tutorialContent: @Composable (onExit: () -> Unit) -> Unit = { onExit ->
        TutorialRoute(onExit = onExit)
    },
) {
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            // homeContent slot: onPlayClassic, onPlayChallenge(level),
            // onResumeClassic(levelId), onResumeChallenge(level), onOpenSettings,
            // onStartTutorial.
            // Every value comes from Home's UiState (the persisted ChallengeProgress level,
            // CHAL-05; the stored snapshot's board id, PERS-03) — the host carries what it
            // is handed, it never picks a level or a board itself.
            homeContent(
                { navController.navigate(ClassicCatalog) },
                { level -> navController.toChallengeBoard(level) },
                { levelId -> navController.navigate(Gameplay(levelId = levelId, resume = true)) },
                { level -> navController.toChallengeBoard(level, resume = true) },
                { navController.navigate(Settings) },
                { navController.toTutorial() },
            )
        }
        composable<Settings> {
            // settingsContent slot: onBack (pops back to Home, D-14), onShowHowToPlay
            // (the D-02a replay entry point — the tutorial's only permanent affordance).
            settingsContent({ navController.popBackStack() }, { navController.toTutorial() })
        }
        composable<Tutorial> {
            // tutorialContent slot: onExit — and NOTHING else. `popBackStack()` is the
            // destination's only outbound edge (§5.1 prohibition 4), which lands the player
            // back where they entered from: Home on first run, Settings on a replay. Any
            // forward navigation call added here would give the tutorial a route to a screen
            // that can bank a star, so the grep gate over this block must stay at zero.
            tutorialContent { navController.popBackStack() }
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
                { results -> navController.toResults(results) },
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
 * Pushes the [Tutorial] destination (ONB-01, D-01g/D-02a) — the **one** helper both entry
 * points share: Home's first-run auto-start and the Settings › How to play row.
 *
 * `launchSingleTop` is defensive rather than load-bearing: the auto-start effect fires once
 * per transition into "pending", and the Settings row cannot be tapped while the tutorial is
 * already on top. It costs nothing and it makes a double push structurally impossible, which
 * matters because the tutorial's exit pops exactly one entry.
 *
 * Deliberately **no** `popUpTo`. The destination's whole navigation contract is that it pops
 * back to wherever it was entered from (§5.1 prohibition 4) — Home on first run, Settings on
 * a replay — and any pop target here would break the second case.
 */
private fun NavHostController.toTutorial() {
    navigate(Tutorial) { launchSingleTop = true }
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

/**
 * Navigates to [Results] while popping the board that was just finished, so system Back
 * from Results returns the player to where they launched the level from instead of
 * re-showing the completed game (NAV-02). Not named `toResultsFromWin`: `GameViewModel`
 * also emits `finished` for a stuck, unsolvable board (the Challenge Retry path), so this
 * hop is not win-only.
 *
 * Each mode mirrors the pop target of the helper that entered the board: a Classic game
 * pops up to [ClassicCatalog] exactly as [toBoardFromCatalog] does, a Challenge level pops
 * up to [Home] exactly as [toChallengeBoard] does. Both pops are non-inclusive, so the
 * landing destination survives and only the finished `Gameplay` entry above it is removed.
 *
 * A Classic game resumed through the Home Continue affordance (PERS-03) is entered
 * straight from Home and so never visited the catalog; `popUpTo<ClassicCatalog>` against a
 * route absent from the back stack is a documented no-op, which would silently leave the
 * finished board reachable by Back. Those finishes therefore fall back to [Home] — the
 * start destination, and the only thing actually beneath them.
 *
 * The catalog test reads [NavHostController.previousBackStackEntry] — the entry directly
 * beneath the finished board, i.e. the destination it was launched from. That is the exact
 * question being asked, and unlike `currentBackStack` it is public API rather than an
 * androidx `@RestrictTo(LIBRARY_GROUP)` one (which Android Lint rejects in app code).
 */
private fun NavHostController.toResults(results: Results) {
    val launchedFromCatalog =
        results.challengeLevel == null &&
            previousBackStackEntry?.destination?.hasRoute<ClassicCatalog>() == true
    if (launchedFromCatalog) {
        navigate(results) { popUpTo<ClassicCatalog>() }
    } else {
        navigate(results) { popUpTo<Home>() }
    }
}
