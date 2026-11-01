package com.app.pegsolitaire.ui.game

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.PaletteScene
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.BoardMarkPaletteKey
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The live switch (docs/ux/colour-vision.md CV-AC03, CV-AC10): a board on screen redraws in the
 * chosen palette the moment the theme's palette changes — no new `setContent`, no restart — and
 * flipping back draws today's board again, pixel for pixel. The board-state node reports the
 * palette in use, and its other keys do not move.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LIVE_SWITCH_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ColorblindPaletteLiveSwitchTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun theBoardRedrawsInTheChosenPaletteWithoutLeavingTheScreen() {
        val scene = PaletteScene.midGame(Boards.english33Initial)
        composeRule.setContent {
            AppTheme(darkTheme = false, boardMarkPalette = palette.value) {
                GameScreen(
                    state = scene.state,
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
        val before = boardState()
        val standard = composeRule.renderedWindow()

        flip(BoardMarkPalette.Colorblind)
        val colorblind = composeRule.renderedWindow()
        val during = boardState()

        flip(BoardMarkPalette.Standard)
        val standardAgain = composeRule.renderedWindow()

        val slab = composeRule.onNodeWithTag(GAME_BOARD_SLAB_TEST_TAG).fetchSemanticsNode().boundsInRoot
        assertWithMessage(
            "board pixels the flip changed",
        ).that(differingPixels(standard, colorblind, slab)).isGreaterThan(0)
        assertWithMessage("board pixels left changed after flipping back")
            .that(differingPixels(standard, standardAgain, slab))
            .isEqualTo(0)
        assertThat(before.config[BoardMarkPaletteKey]).isEqualTo(BoardMarkPalette.Standard)
        assertThat(during.config[BoardMarkPaletteKey]).isEqualTo(BoardMarkPalette.Colorblind)
        assertThat(during.config[PegCountKey]).isEqualTo(before.config[PegCountKey])
        assertThat(during.config[LegalTargetCountKey]).isEqualTo(before.config[LegalTargetCountKey])
        assertThat(during.config[GameStatusKey]).isEqualTo(before.config[GameStatusKey])
    }

    private fun flip(to: BoardMarkPalette) {
        composeRule.runOnUiThread { palette.value = to }
        composeRule.waitForIdle()
    }

    private fun boardState(): SemanticsNode = composeRule.onNodeWithTag(GAME_STATE_TEST_TAG).fetchSemanticsNode()

    private fun differingPixels(
        first: Bitmap,
        second: Bitmap,
        box: Rect,
    ): Int {
        var differing = 0
        for (y in box.top.toInt() until box.bottom.toInt()) {
            for (x in box.left.toInt() until box.right.toInt()) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) differing++
            }
        }
        return differing
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val LIVE_SWITCH_ROBOLECTRIC_SDK = 34
