package com.app.pegsolitaire.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.ClassicLevelIds
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
 * Home → ClassicCatalog → Gameplay(levelId) → Results(...), asserting each of the five
 * `@Serializable` routes is reached via `currentDestination.hasRoute<...>()` and that
 * the selected-board `levelId` round-trips through `Gameplay`/`Results` args.
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
                homeContent = { onPlayClassic -> StubDestination(HOME_TAG, onPlayClassic) },
                catalogContent = { onSelect, _ -> StubDestination(CATALOG_TAG) { onSelect(SELECTED_LEVEL_ID) } },
                gameplayContent = { onFinished, _ -> StubDestination(GAMEPLAY_TAG) { onFinished(sampleResults()) } },
                resultsContent = { _, _, _, _ -> StubDestination(RESULTS_TAG) {} },
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

    /** The current destination's serialized route pattern (its `@Serializable` qualified name). */
    private fun currentRoute(): String = navController.currentDestination?.route.orEmpty()

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

// Each route's serialized pattern is its fully-qualified class name; the simple name is
// a stable, unambiguous substring of `currentDestination.route` for the reachability check.
private const val HOME_ROUTE = "Home"
private const val CATALOG_ROUTE = "ClassicCatalog"
private const val GAMEPLAY_ROUTE = "Gameplay"
private const val RESULTS_ROUTE = "Results"

/** A real catalog id, proving navigation carries the selected board through the graph. */
private val SELECTED_LEVEL_ID = ClassicLevelIds.EUROPEAN37

private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS_REMAINING = 5
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1280
