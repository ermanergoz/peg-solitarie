package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.LightColors
import com.app.pegsolitaire.ui.theme.boardMarksFor
import com.app.pegsolitaire.ui.theme.goldGradientFill
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The star in each palette (docs/ux/colour-vision.md §3.4, CV-10, CV-13a; CV-AC05, CV-13).
 *
 * Every frame holds today's earned star — gold-filled, drawn exactly as the Results row and the
 * catalogue pips drew it before the option — beside [StarGlyph]'s, so "the standard earned star is
 * today's pixels" is a comparison inside one frame, not against a stored golden. The unearned star
 * is the hollow star in both palettes (CV-13a), so it is read for its band and its open centre.
 * Sampled on the 24-unit glyph: its centre (12, 12) lies in the fill, and (12, 4.5) lies on the
 * hollow outline's band below the top tip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [STAR_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StarGlyphTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theStandardEarnedStarIsTodaysPixelsAndItsUnearnedStarIsHollow() {
        show(BoardMarkPalette.Standard)
        val frame = composeRule.renderedWindow()
        val marks = boardMarksFor(BoardMarkPalette.Standard, darkTheme = false)

        assertWithMessage("earned").that(differing(frame, TODAY_EARNED, GLYPH_EARNED)).isEqualTo(0)
        assertWithMessage("the unearned outline below the top tip")
            .that(isColour(frame.at(GLYPH_UNEARNED, CENTRE_UNITS, OUTLINE_UNITS), marks.unearnedStarOutline))
            .isTrue()
        assertWithMessage("hollow: the background shows at the unearned star's centre")
            .that(isColour(frame.at(GLYPH_UNEARNED, CENTRE_UNITS, CENTRE_UNITS), LightColors.background))
            .isTrue()
    }

    @Test
    fun aColourBlindEarnedStarIsAmberFilledAndOutlined() {
        show(BoardMarkPalette.Colorblind)
        val frame = composeRule.renderedWindow()
        val marks = boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = false)

        assertWithMessage("the fill at the centre is amber, between the two stops")
            .that(isAmber(frame.at(GLYPH_EARNED, CENTRE_UNITS, CENTRE_UNITS)))
            .isTrue()
        assertWithMessage("the outline below the top tip")
            .that(isColour(frame.at(GLYPH_EARNED, CENTRE_UNITS, OUTLINE_UNITS), checkNotNull(marks.earnedStarOutline)))
            .isTrue()
    }

    @Test
    fun aColourBlindUnearnedStarIsAHollowOnSurfaceVariantOutline() {
        show(BoardMarkPalette.Colorblind)
        val frame = composeRule.renderedWindow()
        val marks = boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = false)

        assertWithMessage("the outline below the top tip")
            .that(isColour(frame.at(GLYPH_UNEARNED, CENTRE_UNITS, OUTLINE_UNITS), marks.unearnedStarOutline))
            .isTrue()
        assertWithMessage("hollow: the background shows at the centre")
            .that(isColour(frame.at(GLYPH_UNEARNED, CENTRE_UNITS, CENTRE_UNITS), LightColors.background))
            .isTrue()
    }

    /** CV-AC10: the glyph is decorative and adds nothing to the semantics tree. */
    @Test
    fun theStarGlyphCarriesNoDescription() {
        show(BoardMarkPalette.Colorblind)

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(0)
    }

    private fun show(palette: BoardMarkPalette) {
        composeRule.setContent {
            AppTheme(darkTheme = false, boardMarkPalette = palette) {
                Row(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.testTag(TODAY_EARNED).goldGradientFill().size(STAR_DP.dp),
                    )
                    Box(Modifier.testTag(GLYPH_EARNED)) { StarGlyph(earned = true, size = STAR_DP.dp) }
                    Box(Modifier.testTag(GLYPH_UNEARNED)) { StarGlyph(earned = false, size = STAR_DP.dp) }
                }
            }
        }
    }

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** The pixel at ([x], [y]) in the glyph's 24-unit viewport. */
    private fun Bitmap.at(
        tag: String,
        x: Float,
        y: Float,
    ): Int {
        val box = bounds(tag)
        return getPixel(
            (box.left + box.width * x / VIEWPORT_UNITS).roundToInt(),
            (box.top + box.height * y / VIEWPORT_UNITS).roundToInt(),
        )
    }

    private fun differing(
        frame: Bitmap,
        today: String,
        glyph: String,
    ): Int {
        val a = bounds(today)
        val b = bounds(glyph)
        assertThat(b.size).isEqualTo(a.size)
        var count = 0
        for (dy in 0 until a.height.roundToInt()) {
            for (dx in 0 until a.width.roundToInt()) {
                val left = frame.getPixel(a.left.roundToInt() + dx, a.top.roundToInt() + dy)
                val right = frame.getPixel(b.left.roundToInt() + dx, b.top.roundToInt() + dy)
                if (left != right) count++
            }
        }
        return count
    }

    /** Between `#C05A08` and `#B45309` in every channel, give or take the tolerance. */
    private fun isAmber(pixel: Int): Boolean {
        val red = (pixel shr RED_SHIFT) and BYTE
        val green = (pixel shr GREEN_SHIFT) and BYTE
        val blue = pixel and BYTE
        return red in AMBER_RED && green in AMBER_GREEN && blue in AMBER_BLUE
    }
}

private const val TODAY_EARNED = "today_earned"
private const val GLYPH_EARNED = "glyph_earned"
private const val GLYPH_UNEARNED = "glyph_unearned"
private const val STAR_DP = 48
private const val VIEWPORT_UNITS = 24f
private const val CENTRE_UNITS = 12f
private const val OUTLINE_UNITS = 4.5f
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BYTE = 0xFF
private val AMBER_RED = (0xB4 - CHANNEL_TOLERANCE)..(0xC0 + CHANNEL_TOLERANCE)
private val AMBER_GREEN = (0x53 - CHANNEL_TOLERANCE)..(0x5A + CHANNEL_TOLERANCE)
private val AMBER_BLUE = (0x08 - CHANNEL_TOLERANCE)..(0x09 + CHANNEL_TOLERANCE)

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val STAR_ROBOLECTRIC_SDK = 34
