package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.game.recipe
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The colour-blind marks add no motion (docs/ux/colour-vision.md §3.6; CV-AC09): with the animator
 * scale at 0 the board is still from one frame to the next, and at scale 1 only the existing
 * selection pulse moves — the halo with it — while the dashed hint rings never do.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [MOTION_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardMarkMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val scene = PaletteScene.midGame(Boards.english33Initial)

    @Test
    fun withAnimationsOffAColourBlindBoardIsStillFromOneFrameToTheNext() {
        show(animatorScale = 0f)
        val first = frameAfter(SETTLE_FRAMES)
        val second = frameAfter(1)

        assertWithMessage("pixels that changed in one frame with animations off")
            .that(differingPixels(first, second, canvas()))
            .isEqualTo(0)
    }

    @Test
    fun withAnimationsOnOnlyTheSelectionBreathesAndTheHintRingsStayStill() {
        show(animatorScale = 1f)
        val first = frameAfter(SETTLE_FRAMES)
        val second = frameAfter(PULSE_STEP_FRAMES)

        assertWithMessage("the held peg's pulse, its ring and halo with it")
            .that(differingPixels(first, second, cellBox(scene.selection.from)))
            .isGreaterThan(0)
        assertWithMessage("the dashed hint source ring")
            .that(differingPixels(first, second, cellBox(scene.hint.from)))
            .isEqualTo(0)
        assertWithMessage("the dashed hint target ring")
            .that(differingPixels(first, second, cellBox(scene.hint.to)))
            .isEqualTo(0)
    }

    private fun show(animatorScale: Float) {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            animatorScale,
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme(darkTheme = false, boardMarkPalette = BoardMarkPalette.Colorblind) {
                Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    BoardCanvas(
                        state = scene.state,
                        onCellTap = {},
                        onAnimationFinished = {},
                        modifier = Modifier.size(CANVAS_DP.dp),
                    )
                }
            }
        }
    }

    private fun frameAfter(frames: Int): Bitmap {
        repeat(frames) {
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeByFrame()
        }
        return composeRule.renderedWindow()
    }

    private fun canvas(): Rect = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().boundsInRoot

    /** [cell]'s square on the canvas, in window pixels. */
    private fun cellBox(cell: Position): Rect {
        val canvas = canvas()
        val geometry =
            BoardGeometry.forCanvas(
                scene.state.board.spec,
                Size(canvas.width, canvas.height),
                BoardMarkPalette.Colorblind.recipe,
            )
        val center = geometry.center(cell)
        val half = geometry.cellSize / 2
        return Rect(
            left = canvas.left + center.x - half,
            top = canvas.top + center.y - half,
            right = canvas.left + center.x + half,
            bottom = canvas.top + center.y + half,
        )
    }

    private fun differingPixels(
        first: Bitmap,
        second: Bitmap,
        box: Rect,
    ): Int {
        var differing = 0
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) differing++
            }
        }
        return differing
    }
}

/** A board of 350 px at density 1: 50 px cells on English-33. */
private const val CANVAS_DP = 350

/** Long enough for the legal-target marks' pop-in spring to come to rest. */
private const val SETTLE_FRAMES = 90

/** About a tenth of a second: a visible step of the 900 ms selection pulse. */
private const val PULSE_STEP_FRAMES = 6

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val MOTION_ROBOLECTRIC_SDK = 34
