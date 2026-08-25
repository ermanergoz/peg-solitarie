package com.app.pegsolitaire.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.ui.results.ResultsHeadlineTier
import com.app.pegsolitaire.ui.results.ResultsScreen
import com.app.pegsolitaire.ui.results.ResultsUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose reachability proof for the type-safe nav graph (NAV-02).
 *
 * Hosts [AppNavHost] over a real captured [NavHostController] and drives the spine
 * Home → ClassicCatalog → Gameplay(levelId) → Results(...) plus the Home → Settings
 * branch (D-14), asserting each of the six `@Serializable` routes is reached via
 * `currentDestination.hasRoute<...>()` and that the selected-board `levelId`
 * round-trips through `Gameplay`/`Results` args.
 *
 * The destination bodies are lightweight stand-ins (each a tappable [Box] that fires the
 * nav callback `AppNavHost` supplies), so the test exercises `AppNavHost`'s real
 * navigation lambdas without a Hilt host for the production `hiltViewModel()` routes. The
 * Robolectric runtime SDK is pinned independently of `compileSdk 36` (no Android 16
 * android-all jar yet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NavFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    @Test
    fun drivesHomeToCatalogToGameplayToResultsReachingEveryRoute() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHost(
                navController = navController,
                homeContent = { onPlayClassic, _, _ -> StubDestination(HOME_TAG, onPlayClassic) },
                catalogContent = { onSelect, _ -> StubDestination(CATALOG_TAG) { onSelect(SELECTED_LEVEL_ID) } },
                gameplayContent = { onFinished, _ -> StubDestination(GAMEPLAY_TAG) { onFinished(sampleResults()) } },
                resultsContent = { _, _, _, _, _, _ -> StubDestination(RESULTS_TAG) {} },
            )
        }

        // Start destination: Home.
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(HOME_ROUTE)

        // Home → ClassicCatalog.
        composeRule.onNodeWithTag(HOME_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(CATALOG_ROUTE)

        // ClassicCatalog → Gameplay(levelId), with the selected board's id carried.
        composeRule.onNodeWithTag(CATALOG_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(GAMEPLAY_ROUTE)
        assertThat(navController.currentBackStackEntry?.toRoute<Gameplay>()?.levelId)
            .isEqualTo(SELECTED_LEVEL_ID)

        // Gameplay → Results(...), with the played board's id carried through.
        composeRule.onNodeWithTag(GAMEPLAY_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(RESULTS_ROUTE)
        assertThat(navController.currentBackStackEntry?.toRoute<Results>()?.levelId)
            .isEqualTo(SELECTED_LEVEL_ID)
    }

    @Test
    fun homeChallengeTileEntersGameplayAtThePersistedLevel() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHost(
                navController = navController,
                // The Home tile supplies the level from its UiState (the persisted
                // ChallengeProgress.currentLevel) — the host must carry it through, not
                // substitute a hardcoded entry level (CHAL-05).
                homeContent = { _, onPlayChallenge, _ ->
                    StubDestination(HOME_TAG) { onPlayChallenge(PERSISTED_CHALLENGE_LEVEL) }
                },
                catalogContent = { _, _ -> StubDestination(CATALOG_TAG) {} },
                gameplayContent = { _, _ -> StubDestination(GAMEPLAY_TAG) {} },
                resultsContent = { _, _, _, _, _, _ -> StubDestination(RESULTS_TAG) {} },
            )
        }

        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(HOME_ROUTE)

        // Home Challenge tile → Gameplay carrying the PERSISTED Challenge level.
        composeRule.onNodeWithTag(HOME_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(GAMEPLAY_ROUTE)
        assertThat(navController.currentBackStackEntry?.toRoute<Gameplay>()?.challengeLevel)
            .isEqualTo(PERSISTED_CHALLENGE_LEVEL)
    }

    @Test
    fun homeGearOpensSettingsAndBackReturnsToHome() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHost(
                navController = navController,
                homeContent = { _, _, onOpenSettings -> StubDestination(HOME_TAG) { onOpenSettings() } },
                catalogContent = { _, _ -> StubDestination(CATALOG_TAG) {} },
                gameplayContent = { _, _ -> StubDestination(GAMEPLAY_TAG) {} },
                resultsContent = { _, _, _, _, _, _ -> StubDestination(RESULTS_TAG) {} },
                settingsContent = { onBack -> StubDestination(SETTINGS_TAG) { onBack() } },
            )
        }

        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(HOME_ROUTE)

        // Home gear → the Settings destination (D-14).
        composeRule.onNodeWithTag(HOME_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(SETTINGS_ROUTE)

        // Settings back nav pops straight back to Home (no arg surface, T-05-08).
        composeRule.onNodeWithTag(SETTINGS_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(HOME_ROUTE)
    }

    @Test
    fun challengeRetryReloadsSameBoardWithoutGrowingBackStack() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHost(
                navController = navController,
                homeContent = { _, onPlayChallenge, _ ->
                    StubDestination(HOME_TAG) { onPlayChallenge(PERSISTED_CHALLENGE_LEVEL) }
                },
                catalogContent = { _, _ -> StubDestination(CATALOG_TAG) {} },
                gameplayContent = { onFinished, _ ->
                    StubDestination(GAMEPLAY_TAG) { onFinished(stuckChallengeResults()) }
                },
                resultsContent = { _, _, _, _, onRetry, _ ->
                    StubDestination(RESULTS_TAG) { onRetry(PERSISTED_CHALLENGE_LEVEL) }
                },
            )
        }

        // Home → Challenge Gameplay(persisted level) → stuck Results → Retry.
        composeRule.onNodeWithTag(HOME_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(GAMEPLAY_TAG).performClick()
        composeRule.waitForIdle()
        assertThat(currentRoute()).contains(RESULTS_ROUTE)

        composeRule.onNodeWithTag(RESULTS_TAG).performClick()
        composeRule.waitForIdle()

        // Retry reloads the identical deterministic board (same challenge level)...
        assertThat(currentRoute()).contains(GAMEPLAY_ROUTE)
        assertThat(navController.currentBackStackEntry?.toRoute<Gameplay>()?.challengeLevel)
            .isEqualTo(PERSISTED_CHALLENGE_LEVEL)
        // ...and popUpTo<Home> collapsed the prior Gameplay+Results so the ladder stays bounded (T-03-11).
        val belowDestination = navController.previousBackStackEntry?.destination
        val belowRoute = belowDestination?.route.orEmpty()
        assertThat(belowRoute).contains(HOME_ROUTE)
    }

    @Test
    fun stuckChallengeResultsSurfacesRetryNotNextLevel() {
        composeRule.setContent {
            ResultsScreen(
                state = stuckChallengeState(),
                onReplay = {},
                onCatalog = {},
                onHome = {},
                onNextLevel = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(RETRY_LABEL).assertCountEquals(1)
        composeRule.onAllNodesWithText(NEXT_LEVEL_LABEL).assertCountEquals(0)
    }

    /** The current destination's serialized route pattern (its `@Serializable` qualified name). */
    private fun currentRoute(): String = navController.currentDestination?.route.orEmpty()

    private fun stuckChallengeResults(): Results =
        Results(
            levelId = "",
            stars = STUCK_STARS,
            pegsRemaining = STUCK_PEGS_REMAINING,
            elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
            score = SAMPLE_SCORE,
            challengeLevel = PERSISTED_CHALLENGE_LEVEL,
        )

    private fun stuckChallengeState(): ResultsUiState =
        ResultsUiState(
            stars = STUCK_STARS,
            pegsRemaining = STUCK_PEGS_REMAINING,
            elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
            score = SAMPLE_SCORE,
            headlineTier = ResultsHeadlineTier.STUCK,
            replayId = "",
            challengeLevel = PERSISTED_CHALLENGE_LEVEL,
        )

    /** A tappable full-screen stand-in destination that fires [onClick] (a nav callback). */
    @Composable
    private fun StubDestination(
        tag: String,
        onClick: () -> Unit = {},
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(tag)
                    .clickable { onClick() },
        )
    }

    private fun sampleResults(): Results =
        Results(
            levelId = SELECTED_LEVEL_ID,
            stars = SAMPLE_STARS,
            pegsRemaining = SAMPLE_PEGS_REMAINING,
            elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
            score = SAMPLE_SCORE,
        )
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

private const val HOME_TAG = "stub_home"
private const val CATALOG_TAG = "stub_catalog"
private const val GAMEPLAY_TAG = "stub_gameplay"
private const val RESULTS_TAG = "stub_results"
private const val SETTINGS_TAG = "stub_settings"

// Each route's serialized pattern is its fully-qualified class name; the simple name is
// a stable, unambiguous substring of `currentDestination.route` for the reachability check.
private const val HOME_ROUTE = "Home"
private const val CATALOG_ROUTE = "ClassicCatalog"
private const val GAMEPLAY_ROUTE = "Gameplay"
private const val RESULTS_ROUTE = "Results"
private const val SETTINGS_ROUTE = "Settings"

/** A real catalog id, proving navigation carries the selected board through the graph. */
private val SELECTED_LEVEL_ID = ClassicLevelIds.EUROPEAN37

private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS_REMAINING = 5
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1280

/**
 * The Challenge level the Home tile carries in these tests: the player's *persisted*
 * level (CHAL-05). Deliberately not 1, so a host that silently substituted a hardcoded
 * entry level would fail the assertion instead of accidentally matching it.
 */
private const val PERSISTED_CHALLENGE_LEVEL = 7

/** A stuck Challenge board floors at one star with 2+ pegs left → Retry, not Next level (D-10). */
private const val STUCK_STARS = 1
private const val STUCK_PEGS_REMAINING = 3

/** Visible action labels asserted by the stuck-Results render (verbatim from `strings.xml`). */
private const val RETRY_LABEL = "Retry"
private const val NEXT_LEVEL_LABEL = "Next level"
