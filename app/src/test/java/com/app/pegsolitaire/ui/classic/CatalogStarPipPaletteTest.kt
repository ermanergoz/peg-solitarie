package com.app.pegsolitaire.ui.classic

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.STAR_CENTRE_UNITS
import com.app.pegsolitaire.ui.components.isBetweenStops
import com.app.pegsolitaire.ui.components.isColour
import com.app.pegsolitaire.ui.components.starPixel
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.app.pegsolitaire.ui.theme.boardMarksFor
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What a catalogue card's star pips draw in each palette (docs/ux/colour-vision.md §3.4, §12;
 * CV-AC05 as amended 2026-10-24, CV-AC10; code-auditor F-2).
 *
 * The real `ClassicCatalogScreen` with one card on which two of three stars are earned, rendered
 * under the standard palette and then under the colour-blind one **in the same run**. In both
 * palettes and both themes an unearned pip is the hollow star, so its centre shows the card it
 * sits on (`surfaceContainerHigh`), and an earned pip's centre shows the palette's fill. The pip
 * row's spoken count does not move with the palette.
 *
 * At 16dp the hollow band is about 1dp wide, so only centres are read here; the band's colour is
 * read on the Results row's 48dp stars (`ResultsStarPaletteTest`) and on the glyph itself
 * (`StarGlyphTest`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CATALOG_PALETTE_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CatalogStarPipPaletteTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    private val description: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.catalog_stars_earned, EARNED, TOTAL_STARS)

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun aCatalogueCardsPipsDrawTheActivePaletteInTheLightTheme() = assertThePipsDrawBothPalettes(dark = false)

    @Test
    fun aCatalogueCardsPipsDrawTheActivePaletteInTheDarkTheme() = assertThePipsDrawBothPalettes(dark = true)

    private fun assertThePipsDrawBothPalettes(dark: Boolean) {
        var pipPx = 0f
        var gapPx = 0f
        composeRule.setContent {
            with(LocalDensity.current) {
                pipPx = dimensionResource(R.dimen.catalog_star_pip_size).toPx()
                gapPx = dimensionResource(R.dimen.spacing_xs).toPx()
            }
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
                ClassicCatalogScreen(state = ONE_CARD_TWO_STARS, onSelect = {}, onNavigateBack = {})
            }
        }
        val standard = frameIn(BoardMarkPalette.Standard)
        val spokenUnderStandard = pipRow().config[SemanticsProperties.ContentDescription]
        val colorblind = frameIn(BoardMarkPalette.Colorblind)
        val spokenUnderColorblind = pipRow().config[SemanticsProperties.ContentDescription]
        val pips = pipBoxes(pipRow().boundsInRoot, pipPx, gapPx)

        assertWithMessage("the pip row's description across the flip")
            .that(spokenUnderColorblind)
            .isEqualTo(spokenUnderStandard)
        assertThePips(colorblind, BoardMarkPalette.Colorblind, dark, pips)
        assertThePips(standard, BoardMarkPalette.Standard, dark, pips)
    }

    /** The frame drawn with [shown] in effect. */
    private fun frameIn(shown: BoardMarkPalette): Bitmap {
        composeRule.runOnUiThread { palette.value = shown }
        composeRule.waitForIdle()
        return composeRule.renderedWindow()
    }

    /** The pip row's own node; the card merges its children, so the unmerged tree holds it. */
    private fun pipRow(): SemanticsNode =
        composeRule.onNodeWithContentDescription(description, useUnmergedTree = true).fetchSemanticsNode()

    private fun assertThePips(
        frame: Bitmap,
        shown: BoardMarkPalette,
        dark: Boolean,
        pips: List<Rect>,
    ) {
        val marks = boardMarksFor(shown, dark)
        val card = (if (dark) DarkColors else LightColors).surfaceContainerHigh
        pips.forEachIndexed { index, box ->
            val centre = frame.starPixel(box, STAR_CENTRE_UNITS, STAR_CENTRE_UNITS)
            if (index < EARNED) {
                assertWithMessage("$shown: earned pip $index's centre shows its fill")
                    .that(isBetweenStops(centre, marks.earnedStarFill))
                    .isTrue()
            } else {
                assertWithMessage("$shown: the unearned pip's centre shows the card beneath it")
                    .that(isColour(centre, card))
                    .isTrue()
            }
        }
    }

    /** The three pips of [row], left to right: square, [gapPx] apart. */
    private fun pipBoxes(
        row: Rect,
        pipPx: Float,
        gapPx: Float,
    ): List<Rect> {
        assertWithMessage("the pip row's width, as its pips and gaps add up")
            .that(pipPx * TOTAL_STARS + gapPx * (TOTAL_STARS - 1))
            .isWithin(ONE_PIXEL)
            .of(row.width)
        return List(TOTAL_STARS) { index ->
            val left = row.left + index * (pipPx + gapPx)
            Rect(left, row.top, left + pipPx, row.top + pipPx)
        }
    }
}

/** A one-card catalogue: English-33, two of three stars earned. */
private val ONE_CARD_TWO_STARS =
    ClassicCatalogUiState(
        boards =
            listOf(
                ClassicBoardUiState(
                    descriptor =
                        ClassicLevelDescriptor(
                            id = ClassicLevelIds.ENGLISH33,
                            displayName = "English",
                            holeCount = 33,
                            previewBoard = Boards.english33Initial,
                        ),
                    earnedStars = EARNED,
                ),
            ),
    )

private const val EARNED = 2
private const val TOTAL_STARS = 3

/** Layout positions are whole pixels at xxhdpi; the row's measured width may round by one. */
private const val ONE_PIXEL = 1f

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val CATALOG_PALETTE_ROBOLECTRIC_SDK = 34
