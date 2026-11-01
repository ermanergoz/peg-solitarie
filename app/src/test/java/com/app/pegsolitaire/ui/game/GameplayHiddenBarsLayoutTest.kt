package com.app.pegsolitaire.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.DisplayCutoutInsets
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Criterion 30e (docs/ux-spec.md D-05g part 3, §4.3.1's 2026-11-01 pointer): criterion 18b's
 * numbers, read with both bars **hidden**. On the 360×640dp design baseline, a 24dp status bar and a
 * 48dp navigation bar are dispatched as hidden — reported at zero, stable at 24 and 48 — and the
 * strip still starts at 24dp and the dock slab still ends at 640 − 48 − 16 = 576dp, while the board
 * slab keeps its full 328dp at font scale 1.0 and 2.0 (18c). A cutout deeper than the status bar
 * moves the strip below the cutout, and a cutout at the start edge moves it in by the cutout.
 *
 * The harness is `GameStatusStripTest`'s: the real screen in the app's theme, NATIVE graphics so
 * text is measured as a device measures it, and ±1dp on every edge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HIDDEN_BARS_ROBOLECTRIC_SDK], qualifiers = DESIGN_BASELINE_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameplayHiddenBarsLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun withBothBarsHiddenTheStripAndTheDockSitWhere18bPutsThem() {
        renderGame()
        hideTheBars()

        assertWithMessage("the strip's top edge, in dp, with both bars hidden")
            .that(strip().top.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(STATUS_BAR_DP)
        assertWithMessage("the dock's bottom edge, in dp, with both bars hidden")
            .that(bounds(GAME_ACTION_DOCK_TEST_TAG).bottom.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(DOCK_BOTTOM_DP)
    }

    @Test
    fun withBothBarsHiddenTheBoardKeepsItsFullWidthAtDefaultFontScale() {
        renderGame()
        hideTheBars()

        assertTheSlabIsFullWidth("at font scale 1.0")
    }

    @Test
    fun withBothBarsHiddenTheBoardKeepsItsFullWidthAtDoubleFontScale() {
        renderGame(fontScale = DOUBLE_FONT_SCALE)
        hideTheBars()

        assertTheSlabIsFullWidth("at font scale 2.0")
    }

    @Test
    fun aTopCutoutDeeperThanTheStatusBarPutsTheStripBelowTheCutout() {
        renderGame()
        hideTheBars(cutout = DisplayCutoutInsets(top = CUTOUT_DP.dp))

        assertWithMessage("the strip's top edge, in dp, under a ${CUTOUT_DP}dp top cutout with both bars hidden")
            .that(strip().top.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(maxOf(STATUS_BAR_DP, CUTOUT_DP))
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIER)
    fun aStartSideCutoutInLandscapeMovesTheStripInByTheCutout() {
        renderGame()
        hideTheBars()
        val withoutCutout = strip().left.value

        hideTheBars(cutout = DisplayCutoutInsets(left = CUTOUT_DP.dp))

        assertWithMessage("how far a ${CUTOUT_DP}dp start-side cutout moves the strip's start edge in, in dp")
            .that(strip().left.value - withoutCutout)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(CUTOUT_DP)
    }

    private fun renderGame(fontScale: Float = DEFAULT_FONT_SCALE) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
            ) {
                AppTheme(darkTheme = false) {
                    GameScreen(
                        state = GameUiState.initial(),
                        onCellTap = {},
                        onRestart = {},
                        onConfirmRestart = {},
                        onDismissRestart = {},
                        onAnimationFinished = {},
                        onToggleHints = {},
                        onUndo = {},
                        onHint = {},
                        onAcceptAdOffer = {},
                        onDismissAdOffer = {},
                        onHome = {},
                        onNavigateBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Both bars hidden: reported at zero, stable at the baseline's 24dp and 48dp. */
    private fun hideTheBars(cutout: DisplayCutoutInsets = DisplayCutoutInsets.NONE) {
        composeRule.applySystemBarInsets(
            top = STATUS_BAR_DP.dp,
            bottom = NAVIGATION_BAR_DP.dp,
            visible = false,
            cutout = cutout,
        )
    }

    private fun assertTheSlabIsFullWidth(where: String) {
        val slab = bounds(GAME_BOARD_SLAB_TEST_TAG)
        assertWithMessage("the board slab's width, in dp, $where, with both bars hidden")
            .that((slab.right - slab.left).value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(FULL_SLAB_DP)
        assertWithMessage("the board slab's height, in dp, $where, with both bars hidden")
            .that((slab.bottom - slab.top).value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(FULL_SLAB_DP)
    }

    private fun strip(): DpRect = bounds(GAME_STATUS_STRIP_TEST_TAG)

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val HIDDEN_BARS_ROBOLECTRIC_SDK = 34

/** §4.3.1's design baseline: a 360x640dp phone. */
private const val DESIGN_BASELINE_QUALIFIER = "w360dp-h640dp"

/** The baseline turned on its side, for the start-side cutout. */
private const val LANDSCAPE_QUALIFIER = "w640dp-h360dp-land"

/** The baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24f
private const val NAVIGATION_BAR_DP = 48f

/** 640 − the 48dp navigation bar − the dock's 16dp margin (criterion 18b). */
private const val DOCK_BOTTOM_DP = 576f

/** 360 − 2 × the 16dp `board_screen_margin` (criterion 18c). */
private const val FULL_SLAB_DP = 328f

/** Criterion 30e's cutout, deeper than the 24dp status bar. */
private const val CUTOUT_DP = 34f

/** Criterion 18b's ±1dp. */
private const val EDGE_TOLERANCE_DP = 1f

private const val DEFAULT_FONT_SCALE = 1f
private const val DOUBLE_FONT_SCALE = 2f
