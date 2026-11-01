package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.game.GAME_BOARD_SLAB_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_STATE_TEST_TAG
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.recipe
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.BoardMarkPaletteKey
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.app.pegsolitaire.ui.theme.boardMarksFor
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
 * What the board draws in each palette (docs/ux/colour-vision.md §3.4; CV-AC05, CV-13).
 *
 * One `GameScreen`, mid-game with a held peg, Show-moves on and a revealed hint, rendered under the
 * standard palette and then under the colour-blind one **in the same run** — no stored golden
 * (tech design §8.5). Outside the board slab the two frames are the same pixels: the palette is a
 * look of the board's marks and nothing else. Inside, the colour-blind frame draws §3.4: the halo
 * inside the selection ring, the opaque legal-target mark, and dashed opaque hint rings whose
 * pattern starts at a quadrant point — a dash's middle is amber, the gap after it and the gap
 * before it are the slab.
 *
 * Animations are off (the animator scale at 0, which `rememberAnimationsEnabled` reads), so the
 * selection does not breathe and both frames are still.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PALETTE_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardCanvasPaletteTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun anEnglishBoardDrawsSection34InTheLightTheme() = assertTheBoardDrawsBothPalettes(englishScene(), dark = false)

    @Test
    fun anEnglishBoardDrawsSection34InTheDarkTheme() = assertTheBoardDrawsBothPalettes(englishScene(), dark = true)

    /** The smallest cells the app draws: nine across, on a 360×640dp window. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun aWieglebBoardOnTheNarrowestWindowDrawsSection34() =
        assertTheBoardDrawsBothPalettes(PaletteScene.midGame(Boards.wiegleb45Initial), dark = false)

    private fun englishScene(): PaletteScene = PaletteScene.midGame(Boards.english33Initial)

    private fun assertTheBoardDrawsBothPalettes(
        scene: PaletteScene,
        dark: Boolean,
    ) {
        var haloWidthPx = 0f
        var ringWidthPx = 0f
        composeRule.setContent {
            with(LocalDensity.current) {
                haloWidthPx = dimensionResource(R.dimen.peg_selection_halo_width).toPx()
                ringWidthPx = dimensionResource(R.dimen.peg_selected_ring_width).toPx()
            }
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) { Game(scene) }
        }
        val standard = frameIn(BoardMarkPalette.Standard)
        val colorblind = frameIn(BoardMarkPalette.Colorblind)
        val slab = composeRule.onNodeWithTag(GAME_BOARD_SLAB_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val canvas = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().boundsInRoot

        assertWithMessage("pixels outside the board slab that the palette changed")
            .that(differingPixelsOutside(slab, standard, colorblind))
            .isEqualTo(0)

        val samples = Samples(scene, canvas, dark, haloWidthPx, ringWidthPx)
        samples.assertColourBlindMarks(colorblind)
        samples.assertStandardMarksAreNotTheColourBlindOnes(standard)
    }

    /** The frame drawn with [shown] in effect, after the board-state node reports it. */
    private fun frameIn(shown: BoardMarkPalette): Bitmap {
        composeRule.runOnUiThread { palette.value = shown }
        composeRule.waitForIdle()
        assertThat(
            composeRule.onNodeWithTag(GAME_STATE_TEST_TAG).fetchSemanticsNode().config[BoardMarkPaletteKey],
        ).isEqualTo(shown)
        return composeRule.renderedWindow()
    }

    /** The gameplay screen over [scene], every callback inert. */
    @Composable
    private fun Game(scene: PaletteScene) {
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

    private fun differingPixelsOutside(
        slab: Rect,
        first: Bitmap,
        second: Bitmap,
    ): Int {
        var differing = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                val inside = x >= slab.left && x < slab.right && y >= slab.top && y < slab.bottom
                if (!inside && first.getPixel(x, y) != second.getPixel(x, y)) differing++
            }
        }
        return differing
    }

    /** Where §3.4's marks are, in [canvas]'s window pixels, and what each must read in each palette. */
    private class Samples(
        private val scene: PaletteScene,
        private val canvas: Rect,
        dark: Boolean,
        private val haloWidthPx: Float,
        private val ringWidthPx: Float,
    ) {
        private val scheme = if (dark) DarkColors else LightColors
        private val marks = boardMarksFor(BoardMarkPalette.Colorblind, dark)
        private val geometry =
            BoardGeometry.forCanvas(
                scene.state.board.spec,
                Size(canvas.width, canvas.height),
                BoardMarkPalette.Colorblind.recipe,
            )

        private val haloRadius = geometry.selectedRingRadius - ringWidthPx / 2 - haloWidthPx / 2
        private val selection get() = geometry.center(scene.selection.from)
        private val sourceAway = quadrantAwayFrom(scene.hint.from, scene.hint.to)
        private val targetAway = quadrantAwayFrom(scene.hint.to, scene.hint.from)

        fun assertColourBlindMarks(frame: Bitmap) {
            val halo = frame.pixelAt(canvas, selection, haloRadius, SHADED_SIDE_DEGREES)
            val ring = frame.pixelAt(canvas, selection, geometry.selectedRingRadius, SHADED_SIDE_DEGREES)
            val dot = frame.pixelAt(canvas, geometry.center(scene.selection.to), 0f, 0f)
            assertWithMessage("the halo inside the selection ring")
                .that(isColour(halo, checkNotNull(marks.selectionHalo)))
                .isTrue()
            assertWithMessage("the selection ring at 0.42c").that(isColour(ring, marks.selectionRing)).isTrue()
            assertWithMessage("an opaque legal-target mark").that(isColour(dot, marks.legalTarget)).isTrue()
            assertDashed(frame, "source", geometry.center(scene.hint.from), geometry.hintSourceRingRadius, sourceAway)
            assertDashed(frame, "target", geometry.center(scene.hint.to), geometry.hintTargetRingRadius, targetAway)
        }

        /** CV-13's other half: today's marks are not §3.4's — no halo, a translucent dot, solid rings. */
        fun assertStandardMarksAreNotTheColourBlindOnes(frame: Bitmap) {
            val halo = frame.pixelAt(canvas, selection, haloRadius, SHADED_SIDE_DEGREES)
            val dot = frame.pixelAt(canvas, geometry.center(scene.selection.to), 0f, 0f)
            assertWithMessage("no halo in the standard palette")
                .that(isColour(halo, checkNotNull(marks.selectionHalo)))
                .isFalse()
            assertWithMessage("a translucent legal-target mark in the standard palette")
                .that(isColour(dot, marks.legalTarget))
                .isFalse()
            assertWithMessage("no gap in the standard palette's solid source ring")
                .that(
                    isColour(
                        frame.pixelAt(
                            canvas,
                            geometry.center(scene.hint.from),
                            geometry.hintSourceRingRadius,
                            sourceAway + MID_GAP_DEGREES,
                        ),
                        scheme.surfaceContainerHigh,
                    ),
                ).isFalse()
        }

        private fun assertDashed(
            frame: Bitmap,
            ring: String,
            center: Offset,
            radius: Float,
            away: Float,
        ) {
            val slab: Color = scheme.surfaceContainerHigh
            assertWithMessage("the hint $ring ring's dash after the quadrant point at $away°")
                .that(isColour(frame.pixelAt(canvas, center, radius, away + HALF_DASH_DEGREES), marks.hintRing))
                .isTrue()
            assertWithMessage("the gap after that dash on the hint $ring ring")
                .that(isColour(frame.pixelAt(canvas, center, radius, away + MID_GAP_DEGREES), slab))
                .isTrue()
            assertWithMessage("the gap before the quadrant point on the hint $ring ring")
                .that(isColour(frame.pixelAt(canvas, center, radius, away - HALF_DASH_DEGREES), slab))
                .isTrue()
        }
    }
}

/** The down-right side of a peg, where its gradient is darkest and today's ring merged into it (§2). */
private const val SHADED_SIDE_DEGREES = 45f

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val PALETTE_ROBOLECTRIC_SDK = 34
