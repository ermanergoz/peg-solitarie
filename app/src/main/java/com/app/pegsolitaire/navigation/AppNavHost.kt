package com.app.pegsolitaire.navigation

import android.view.Window
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.ui.backdrop.BackdropClockDriver
import com.app.pegsolitaire.ui.backdrop.BackdropFrames
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
import com.app.pegsolitaire.ui.backdrop.InfiniteBackdropFrames
import com.app.pegsolitaire.ui.backdrop.LocalBackdropClock
import com.app.pegsolitaire.ui.backdrop.LocalBackdropSceneReady
import com.app.pegsolitaire.ui.backdrop.backdropInputObserver
import com.app.pegsolitaire.ui.backdrop.rememberBackdropClock
import com.app.pegsolitaire.ui.backdrop.rememberBackdropSceneReady
import com.app.pegsolitaire.ui.classic.ClassicCatalogRoute
import com.app.pegsolitaire.ui.game.GameRoute
import com.app.pegsolitaire.ui.home.HomeRoute
import com.app.pegsolitaire.ui.licenses.GoogleNoticeTextRoute
import com.app.pegsolitaire.ui.licenses.GoogleNoticesRoute
import com.app.pegsolitaire.ui.licenses.LicenseTextRoute
import com.app.pegsolitaire.ui.licenses.LicensesRoute
import com.app.pegsolitaire.ui.onboarding.TutorialRoute
import com.app.pegsolitaire.ui.results.ResultsRoute
import com.app.pegsolitaire.ui.settings.SettingsRoute
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.window.LocalSystemBarsState
import com.app.pegsolitaire.ui.window.SystemBarsState

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
 * The type-safe navigation host wiring the full ten-route spine (NAV-02).
 *
 * `NavHost` over the ten `@Serializable` [Routes] with `startDestination = Home`:
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
 * @param hostWindow the window whose system bars the destinations govern (D-05g; tech design §3.8
 *   (a)): `MainActivity` passes its own. `null`, the default for previews and host tests, applies
 *   nothing to any window and still keeps the bar state the dialogs mirror current.
 * @param navController the host controller (defaults to a remembered one).
 * @param homeContent the Home destination body (defaults to [HomeRoute]).
 * @param catalogContent the ClassicCatalog destination body (defaults to [ClassicCatalogRoute]).
 * @param gameplayContent the Gameplay destination body (defaults to [GameRoute]).
 * @param resultsContent the Results destination body (defaults to [ResultsRoute]).
 * @param settingsContent the Settings destination body (defaults to [SettingsRoute], D-14).
 * @param tutorialContent the Tutorial destination body (defaults to [TutorialRoute], D-01g).
 *
 * The licences spine (spec LS-09; tech design §3.6, §13.1) — Settings → Licenses → License text, and
 * Licenses → Google notices → Google notice text — lives in [licensesGraph], every edge a plain push
 * popped by back; its four bodies are the last four slots.
 *
 * @param licensesContent the Licenses destination body (defaults to [LicensesRoute]).
 * @param licenseTextContent the License text destination body (defaults to [LicenseTextRoute]).
 * @param googleNoticesContent the Google notices destination body (defaults to [GoogleNoticesRoute]).
 * @param googleNoticeTextContent the Google notice text body (defaults to [GoogleNoticeTextRoute]).
 * @param backdropFrames where the backdrop's clock waits for frames: the infinite-animation frame
 *   clock in production; a test that needs the clock to run injects frames from the test clock.
 *
 * ## The backdrop host (backdrop spec, the tech-lead's R1, A2, A5, A6)
 *
 * The root is the window-filling theme `Surface` the app lacked (F-32, D1): text that names no
 * colour takes `onBackground` in both themes even though every screen's container is transparent.
 * Inside it the scene-ready flag and the one backdrop clock are provided, the clock is driven, and
 * one `Box` — whose observer tells the clock about every pointer-down and key-down without
 * consuming any — holds the `NavHost` and, **composed after it**, the backdrop drawn behind it at
 * `BACKDROP_Z_INDEX`. The order matters: the `NavHost` sets its graph, and restores a saved back
 * stack, during its own composition, so the backdrop composed after it reads the right density on
 * the first frame. The four transitions are the `NavHost`'s own default fade made explicit on
 * `ClayAnim.NAV_TRANSITION_MS`, the one duration the backdrop's cross-fade also uses (D2). The
 * four licence destinations take the density function's `else`, the Sparse wash (E7).
 *
 * ## The window's bars (D-05g; tech design §3.8)
 *
 * The board and Results hide both system bars; every other destination shows them. The host owns
 * that state and provides it, as a `State`, above the `NavHost` for the dialog windows that mirror
 * it ([LocalSystemBarsState]); [NavSystemBars] applies it to [hostWindow]. It is composed after the
 * `NavHost`, beside the backdrop and for the backdrop's reason: the host's graph and a restored back
 * stack exist only once the `NavHost` has composed, so a restored board is immersive on its first
 * frame.
 */
@Composable
fun AppNavHost(
    hostWindow: Window? = null,
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
    settingsContent: @Composable (onBack: () -> Unit, onShowHowToPlay: () -> Unit, onOpenLicenses: () -> Unit) -> Unit =
        { onBack, onShowHowToPlay, onOpenLicenses ->
            SettingsRoute(onBack = onBack, onShowHowToPlay = onShowHowToPlay, onOpenLicenses = onOpenLicenses)
        },
    tutorialContent: @Composable (onExit: () -> Unit) -> Unit = { onExit ->
        TutorialRoute(onExit = onExit)
    },
    licensesContent: LicensesContent = { onOpenEntry, onOpenGoogleNotices, onBack ->
        LicensesRoute(onOpenEntry = onOpenEntry, onOpenGoogleNotices = onOpenGoogleNotices, onBack = onBack)
    },
    licenseTextContent: @Composable (onBack: () -> Unit) -> Unit = { onBack -> LicenseTextRoute(onBack = onBack) },
    googleNoticesContent: @Composable (onOpenNotice: (String) -> Unit, onBack: () -> Unit) -> Unit =
        { onOpenNotice, onBack -> GoogleNoticesRoute(onOpenNotice = onOpenNotice, onBack = onBack) },
    googleNoticeTextContent: @Composable (onBack: () -> Unit) -> Unit =
        { onBack -> GoogleNoticeTextRoute(onBack = onBack) },
    backdropFrames: BackdropFrames = InfiniteBackdropFrames,
) {
    val sceneReady = rememberBackdropSceneReady()
    val clock = rememberBackdropClock()
    val barsState = remember { mutableStateOf(SystemBarsState.Shown) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(
            LocalBackdropSceneReady provides sceneReady,
            LocalBackdropClock provides clock,
            LocalSystemBarsState provides barsState,
        ) {
            BackdropClockDriver(clock = clock, sceneReady = sceneReady, frames = backdropFrames)
            Box(modifier = Modifier.fillMaxSize().backdropInputObserver(clock)) {
                AppNavGraph(
                    navController = navController,
                    homeContent = homeContent,
                    catalogContent = catalogContent,
                    gameplayContent = gameplayContent,
                    resultsContent = resultsContent,
                    settingsContent = settingsContent,
                    tutorialContent = tutorialContent,
                    licensesContent = licensesContent,
                    licenseTextContent = licenseTextContent,
                    googleNoticesContent = googleNoticesContent,
                    googleNoticeTextContent = googleNoticeTextContent,
                )
                NavBackdrop(
                    navController = navController,
                    modifier = Modifier.matchParentSize().zIndex(BackdropMetrics.BACKDROP_Z_INDEX),
                )
                NavSystemBars(navController = navController, hostWindow = hostWindow, barsState = barsState)
            }
        }
    }
}

/** The ten-route graph itself, with the four explicit fades on [ClayAnim.NAV_TRANSITION_MS]. */
@Composable
private fun AppNavGraph(
    navController: NavHostController,
    homeContent: HomeContent,
    catalogContent: @Composable (onSelect: (String) -> Unit, onNavigateBack: () -> Unit) -> Unit,
    gameplayContent: @Composable (onFinished: (Results) -> Unit, onNavigateBack: () -> Unit) -> Unit,
    resultsContent: ResultsContent,
    settingsContent: @Composable (onBack: () -> Unit, onShowHowToPlay: () -> Unit, onOpenLicenses: () -> Unit) -> Unit,
    tutorialContent: @Composable (onExit: () -> Unit) -> Unit,
    licensesContent: LicensesContent,
    licenseTextContent: @Composable (onBack: () -> Unit) -> Unit,
    googleNoticesContent: @Composable (onOpenNotice: (String) -> Unit, onBack: () -> Unit) -> Unit,
    googleNoticeTextContent: @Composable (onBack: () -> Unit) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        enterTransition = { fadeIn(tween(ClayAnim.NAV_TRANSITION_MS)) },
        exitTransition = { fadeOut(tween(ClayAnim.NAV_TRANSITION_MS)) },
        popEnterTransition = { fadeIn(tween(ClayAnim.NAV_TRANSITION_MS)) },
        popExitTransition = { fadeOut(tween(ClayAnim.NAV_TRANSITION_MS)) },
    ) {
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
            // (the D-02a replay entry point — the tutorial's only permanent affordance),
            // onOpenLicenses (Settings › About › Licenses, spec LS-01).
            settingsContent(
                { navController.popBackStack() },
                { navController.toTutorial() },
                { navController.toLicenses() },
            )
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
        licensesGraph(
            navController = navController,
            licensesContent = licensesContent,
            licenseTextContent = licenseTextContent,
            googleNoticesContent = googleNoticesContent,
            googleNoticeTextContent = googleNoticeTextContent,
        )
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
