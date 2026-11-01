package com.app.pegsolitaire.ui.window

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.components.BoardCellPositionKey
import com.app.pegsolitaire.ui.game.GAME_BOARD_SLAB_TEST_TAG
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.results.ResultsHeadlineTier
import com.app.pegsolitaire.ui.results.ResultsScreen
import com.app.pegsolitaire.ui.results.ResultsUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Criterion 30f (docs/ux-spec.md D-05g part 3, §11's 2026-11-01 line): with both bars hidden, no
 * touch target lies in a band a hidden bar occupies when shown — the top band `[0, stable status)`
 * and the bottom band `[height − stable navigation, height)` — so the translucent bar a swipe reveals
 * never covers a control, and a tap made while it shows reaches what it always reached. On gameplay
 * the board's cells are inside the slab, and the slab inside the content between the bands.
 *
 * Every node with a click, long-click or toggle is read at its **touch** bounds, which include any
 * expansion to the minimum touch target, under both of criterion 30d's inset pairs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BAND_TARGETS_ROBOLECTRIC_SDK], qualifiers = DESIGN_BASELINE_QUALIFIER)
class BandTouchTargetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun noGameplayTouchTargetLiesInABandWhileTheBarsAreHidden() {
        show {
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

        INSET_PAIRS.forEach { (status, navigation) ->
            hideTheBars(status, navigation)
            assertNoTouchTargetInTheBands("gameplay", status, navigation)
            assertTheCellsAreInsideTheSlabInsideTheContent(status, navigation)
        }
    }

    @Test
    fun noResultsTouchTargetLiesInABandWhileTheBarsAreHidden() {
        show {
            ResultsScreen(
                state = CLASSIC_RESULTS,
                onReplay = {},
                onCatalog = {},
                onHome = {},
                onNextLevel = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        INSET_PAIRS.forEach { (status, navigation) ->
            hideTheBars(status, navigation)
            assertNoTouchTargetInTheBands("Results", status, navigation)
        }
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { AppTheme(darkTheme = false) { content() } }
        composeRule.waitForIdle()
    }

    private fun hideTheBars(
        status: Dp,
        navigation: Dp,
    ) = composeRule.applySystemBarInsets(top = status, bottom = navigation, visible = false)

    private fun assertNoTouchTargetInTheBands(
        screen: String,
        status: Dp,
        navigation: Dp,
    ) {
        val bands = bandsFor(status, navigation)
        val targets = composeRule.onAllNodes(INTERACTIVE, useUnmergedTree = true).fetchSemanticsNodes()
        assertWithMessage("how many touch targets $screen has").that(targets.size).isAtLeast(MIN_TARGETS)
        targets.forEach { node ->
            val touch = node.touchBoundsInRoot
            val where =
                "$screen, ${describe(node)}, touch bounds $touch, with a ${status.value}dp status bar and " +
                    "a ${navigation.value}dp navigation bar hidden"
            assertWithMessage("$where: its top edge against the top band's end").that(touch.top).isAtLeast(bands.top)
            assertWithMessage("$where: its bottom edge against the bottom band's start")
                .that(touch.bottom)
                .isAtMost(bands.bottom)
        }
    }

    private fun assertTheCellsAreInsideTheSlabInsideTheContent(
        status: Dp,
        navigation: Dp,
    ) {
        val content = bandsFor(status, navigation)
        val slab = composeRule.onNodeWithTag(GAME_BOARD_SLAB_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val where = "with a ${status.value}dp status bar and a ${navigation.value}dp navigation bar hidden"
        assertWithMessage("whether the board slab $slab lies between the bands $content, $where")
            .that(content.contains(slab))
            .isTrue()
        val cells =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(BoardCellPositionKey), useUnmergedTree = true)
                .fetchSemanticsNodes()
        assertWithMessage("how many cells the board offers").that(cells).isNotEmpty()
        cells.forEach { cell ->
            assertWithMessage("whether ${describe(cell)} at ${cell.boundsInRoot} lies inside the slab $slab, $where")
                .that(slab.contains(cell.boundsInRoot))
                .isTrue()
        }
    }

    /** The window between the two bands, in px: below the top band and above the bottom band. */
    private fun bandsFor(
        status: Dp,
        navigation: Dp,
    ): Rect {
        val window = composeRule.onRoot().fetchSemanticsNode().size
        return with(composeRule.density) {
            Rect(0f, status.toPx(), window.width.toFloat(), window.height - navigation.toPx())
        }
    }

    private fun describe(node: SemanticsNode): String {
        val label =
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                ?: "node ${node.id}"
        return "\"$label\""
    }
}

/** `a` contains `b` when every edge of `b` is on or inside `a`'s. */
private fun Rect.contains(other: Rect): Boolean =
    other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

/** A node a finger or a screen reader can press: click, long click, or toggle. */
private val INTERACTIVE =
    hasClickAction() or
        SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick) or
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

/** At least the back control and the dock's four on gameplay, and the back and three actions on Results. */
private const val MIN_TARGETS = 4

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val BAND_TARGETS_ROBOLECTRIC_SDK = 34

/** §4.3.1's design baseline: a 360x640dp phone. */
private const val DESIGN_BASELINE_QUALIFIER = "w360dp-h640dp"

/** Criterion 30d's two windows: 24dp over three-button navigation, and a hole-punch bar over a gesture bar. */
private val INSET_PAIRS = listOf(24.dp to 48.dp, 54.dp to 24.dp)

/** A finished Classic board: Replay, Catalog and Home. */
private val CLASSIC_RESULTS =
    ResultsUiState(
        stars = 2,
        pegsRemaining = 2,
        elapsedSeconds = 75,
        score = 1280,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )
