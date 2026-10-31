package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.StandInTags
import com.app.pegsolitaire.testing.backdropSemantics
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BD-06 and BD-07 on the real `AppNavHost` (the tech-lead's R1, A10): the backdrop's density follows
 * the committed destination — Home the landscape, the board the quiet scene, every other screen the
 * wash — its layers follow the density, and a restored back stack whose top is the board is quiet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DENSITY_ROBOLECTRIC_SDK])
class BackdropDensityTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var navController: NavHostController

    @Test
    fun homeDrawsTheLandscapeTheBoardTheQuietSceneAndEveryOtherScreenTheWash() {
        rule.setContent {
            navController = rememberNavController()
            AppNavHostWithStandIns(navController, InfiniteBackdropFrames)
        }
        assertShows(BackdropDensity.Landscape, "Home")
        tap(StandInTags.HOME_CATALOG)
        assertShows(BackdropDensity.Sparse, "the catalogue")
        tap(StandInTags.CATALOG)
        assertShows(BackdropDensity.Quiet, "a Classic board")
        tap(StandInTags.BOARD_FINISH)
        assertShows(BackdropDensity.Sparse, "Results")
        tap(StandInTags.RESULTS_HOME)
        assertShows(BackdropDensity.Landscape, "Home again")
        tap(StandInTags.HOME_SETTINGS)
        assertShows(BackdropDensity.Sparse, "Settings")
        tap(StandInTags.SETTINGS_TUTORIAL)
        assertShows(BackdropDensity.Sparse, "the tutorial")
        tap(StandInTags.TUTORIAL_EXIT)
        rule.runOnIdle { navController.popBackStack() }
        assertShows(BackdropDensity.Landscape, "Home after Settings")
        tap(StandInTags.HOME_CHALLENGE)
        assertShows(BackdropDensity.Quiet, "a Challenge level")
    }

    @Test
    fun theLayersAreTheSkyAloneOnTheWashAndTheWholeSceneOnHomeAndTheBoard() {
        rule.setContent {
            navController = rememberNavController()
            AppNavHostWithStandIns(navController, InfiniteBackdropFrames)
        }
        assertLayers(BackdropLayer.Sky, BackdropLayer.Waves, BackdropLayer.Lights)
        tap(StandInTags.HOME_CATALOG)
        assertLayers(BackdropLayer.Sky)
        tap(StandInTags.CATALOG)
        assertLayers(BackdropLayer.Sky, BackdropLayer.Waves, BackdropLayer.Lights)
    }

    @Test
    fun aRestoredBackStackWhoseTopIsTheBoardIsQuiet() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            navController = rememberNavController()
            AppNavHostWithStandIns(navController, InfiniteBackdropFrames)
        }
        tap(StandInTags.HOME_CHALLENGE)
        assertShows(BackdropDensity.Quiet, "before the restore")
        restoration.emulateSavedInstanceStateRestore()
        assertShows(BackdropDensity.Quiet, "the restored board")
    }

    private fun tap(tag: String) {
        rule.onNodeWithTag(tag).performClick()
        rule.waitForIdle()
    }

    private fun assertShows(
        density: BackdropDensity,
        where: String,
    ) {
        assertWithMessage(where).that(rule.backdropSemantics()[BackdropDensityKey]).isEqualTo(density)
    }

    private fun assertLayers(vararg layers: BackdropLayer) {
        assertWithMessage("layers").that(rule.backdropSemantics()[BackdropLayersKey]).containsExactly(*layers)
    }
}

private const val DENSITY_ROBOLECTRIC_SDK = 34
