package com.app.pegsolitaire.ui.game

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.withTabularFigures
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.ceil

/**
 * [TimerDigits] in the shipped face: every digit sits in a cell as wide as the widest digit of the
 * face the timer's role is set in, and the cells follow that face if it changes after the first
 * layout.
 *
 * ## The cells are Fredoka Medium's (qa-engineer F-C)
 *
 * The timer's role, `headlineSmall`, is set in Fredoka Medium, whose digits are proportional: "1"
 * is 388 units per 1000 em and "2", the widest, 587. Cells measured in any other face, the
 * platform's say, would be narrower than Fredoka's "2", so its wider digits would outgrow them and
 * the pill would swing as the clock ticks, on exactly the devices where the face renders. Two
 * clocks that measure alike cannot see that. So the cells are held to Fredoka's own widest digit,
 * typed from the face file, never measured by the code under test.
 *
 * ## A face that changes after the first layout
 *
 * The faces are bundled and load before the first frame, so on a device the timer's first frame is
 * already in its final face. The cells must still follow a face, or a font scale, that changes
 * after the first layout: a width remembered in composition would keep the old face's forever.
 * So the same clock is also set twice, side by side: once in a Fredoka held back until after the
 * first frame, and once in the identical Fredoka loaded up front, both from the file the app ships.
 * Before the late face arrives, the two clocks differ, which proves the late one really started in
 * another face. Once it has arrived and the clock is laid out again, the two must be the same width
 * at every reading.
 *
 * ## The frame after the face lands
 *
 * On a device, the frame after a face lands lays its text out again. Robolectric does not: here
 * not even a plain `Text` in a late face ever changes after the face arrives. The host therefore
 * reads the face's resolution state itself ([lateFaceLanding]). That read recomposes it when the
 * face lands, and the pass that follows lays the text out again, standing in for the device's
 * frame. What stays under test is this layout's own part: once laid out again, the clock's cells
 * are the late face's, not a width kept from the face before it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TIMER_ROBOLECTRIC_SDK], qualifiers = TIMER_WINDOW_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimerDigitsTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Qa-engineer F-C, closed (tech-lead font ruling, T3): in the app's own type scale, every
     * digit of the clock sits in a cell exactly as wide as Fredoka Medium's widest digit. That is
     * "2", 587 units per 1000 em: 234.8px at 100sp on xxxhdpi, which a text layout rounds up to a
     * whole 235px.
     *
     * Each cell is read off the rendered clock. The cells tile the clock from its left edge, the
     * colon's cell is the colon itself, and each digit is centred in its own cell. So every digit
     * of `mm` and of `ss` shares one cell edge with the clock or with the colon, and its cell is
     * its own width plus twice its distance from that edge. Centring on whole pixels leaves that
     * reading a pixel short or long when a digit and its cell differ by an odd number of pixels,
     * hence the ±1px.
     *
     * The first assertion proves that the platform face's widest digit is at least 3px away from
     * Fredoka's at this size, so cells measured in it could not pass.
     */
    @Test
    @Config(qualifiers = CELL_WINDOW_QUALIFIER)
    fun everyDigitSitsInACellAsWideAsFredokaMediumsWidestDigit() {
        val elapsed = mutableStateOf(READINGS_SECONDS.first())
        lateinit var style: TextStyle
        lateinit var measurer: TextMeasurer
        composeRule.setContent {
            AppTheme {
                style =
                    MaterialTheme.typography.headlineSmall
                        .withTabularFigures()
                        .copy(fontSize = CELL_PROBE_SP.sp)
                measurer = rememberTextMeasurer()
                TimerDigits(
                    time = GameTimerFormat.format(elapsed.value),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(CELL_CLOCK),
                )
            }
        }
        composeRule.waitForIdle()

        val fontPx = with(composeRule.density) { CELL_PROBE_SP.sp.toPx() }
        val fredokaCellPx = FREDOKA_MEDIUM_WIDEST_DIGIT_PER_MILLE * fontPx / PER_MILLE
        val platformCellPx =
            DECIMAL_DIGITS.maxOf { digit ->
                measurer.measure(digit.toString(), style.copy(fontFamily = FontFamily.Default)).size.width
            }
        assertWithMessage("the platform face's widest digit against Fredoka Medium's, in px")
            .that(abs(platformCellPx - fredokaCellPx))
            .isAtLeast(MIN_PLATFORM_CELL_GAP_PX)

        READINGS_SECONDS.forEach { seconds ->
            val time = GameTimerFormat.format(seconds)
            composeRule.runOnUiThread { elapsed.value = seconds }
            composeRule.waitForIdle()
            digitCellsPx(CELL_CLOCK, time).forEach { (index, cellPx) ->
                assertWithMessage("the cell of the digit at $index in $time, in px")
                    .that(cellPx)
                    .isWithin(CELL_TOLERANCE_PX)
                    .of(ceil(fredokaCellPx))
            }
        }
    }

    @Test
    fun aClockWhoseFaceArrivesAfterTheFirstFrameTakesThatFacesCells() {
        val arrival = CompletableDeferred<Unit>()
        val lateFace = FontFamily(ShippedFredoka(FontLoadingStrategy.Async, arrival))
        val presentFace = FontFamily(ShippedFredoka(FontLoadingStrategy.Blocking, arrival = null))
        val elapsed = mutableStateOf(READINGS_SECONDS.first())
        composeRule.setContent {
            AppTheme {
                lateFaceLanding(lateFace)
                val style = MaterialTheme.typography.headlineSmall.withTabularFigures()
                val color = MaterialTheme.colorScheme.onSurface
                val time = GameTimerFormat.format(elapsed.value)
                Column {
                    TimerDigits(time, style.copy(fontFamily = lateFace), color, Modifier.testTag(LATE_CLOCK))
                    TimerDigits(time, style.copy(fontFamily = presentFace), color, Modifier.testTag(PRESENT_CLOCK))
                }
            }
        }
        composeRule.waitForIdle()

        assertWithMessage("the late clock before its face arrives, against the present one, in dp")
            .that(widthOf(LATE_CLOCK))
            .isNotWithin(WIDTH_TOLERANCE_DP)
            .of(widthOf(PRESENT_CLOCK))

        composeRule.runOnUiThread { arrival.complete(Unit) }
        composeRule.waitForIdle()

        READINGS_SECONDS.forEach { seconds ->
            composeRule.runOnUiThread { elapsed.value = seconds }
            composeRule.waitForIdle()
            assertWithMessage("the late clock at ${GameTimerFormat.format(seconds)}, against the present one, in dp")
                .that(widthOf(LATE_CLOCK))
                .isWithin(WIDTH_TOLERANCE_DP)
                .of(widthOf(PRESENT_CLOCK))
        }
    }

    /**
     * Reads which typeface [face] resolves to at the Medium weight, so the calling composition is
     * recomposed when a late face lands (see the class KDoc for why the host must).
     */
    @Composable
    private fun lateFaceLanding(face: FontFamily): Any =
        LocalFontFamilyResolver.current.resolve(face, FontWeight.Medium).value

    private fun widthOf(tag: String): Float {
        val bounds = composeRule.onNodeWithTag(tag).getBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    /**
     * The cell of every digit in the clock tagged [tag], which shows [time], in px, keyed by the
     * digit's index in [time]. A digit's cell is its own width plus twice its distance from the
     * cell edge it shares with the clock or with the colon (see
     * [everyDigitSitsInACellAsWideAsFredokaMediumsWidestDigit]).
     */
    private fun digitCellsPx(
        tag: String,
        time: String,
    ): Map<Int, Float> {
        val clock = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val characters = clock.children.map { it.positionInRoot.x to it.positionInRoot.x + it.size.width }
        check(characters.size == time.length) { "the clock shows $time in ${characters.size} cells" }
        val clockLeft = clock.positionInRoot.x
        val clockRight = clockLeft + clock.size.width
        return time.indices.filter { time[it].isDigit() }.associateWith { index ->
            val (left, right) = characters[index]
            val leftEdge =
                when {
                    index == 0 -> clockLeft
                    time[index - 1] == COLON -> characters[index - 1].second
                    else -> null
                }
            val rightEdge =
                when {
                    index == time.lastIndex -> clockRight
                    time[index + 1] == COLON -> characters[index + 1].first
                    else -> null
                }
            when {
                leftEdge != null -> (right - left) + 2 * (left - leftEdge)
                rightEdge != null -> (right - left) + 2 * (rightEdge - right)
                else -> error("the digit at $index in $time shares no cell edge with the clock or the colon")
            }
        }
    }
}

/**
 * Fredoka Medium, the timer's face, from the file the app ships, loaded [loadingStrategy]: up
 * front, or late, once [arrival] completes, as a face that changes after the first layout does.
 */
private class ShippedFredoka(
    loadingStrategy: FontLoadingStrategy,
    val arrival: CompletableDeferred<Unit>?,
) : AndroidFont(loadingStrategy, ShippedFredokaLoader, FontVariation.Settings()) {
    override val weight: FontWeight = FontWeight.Medium
    override val style: FontStyle = FontStyle.Normal
}

private object ShippedFredokaLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(
        context: Context,
        font: AndroidFont,
    ): Typeface = fredokaMedium(context)

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
    ): Typeface {
        (font as ShippedFredoka).arrival?.await()
        return fredokaMedium(context)
    }
}

/** The same typeface whenever it is loaded: only the timing differs between the two clocks. */
private fun fredokaMedium(context: Context): Typeface =
    checkNotNull(ResourcesCompat.getFont(context, R.font.fredoka_medium)) { "the bundled Fredoka Medium did not load" }

/**
 * 00:00, 08:08, 11:11, 59:59 (criterion 18d's readings) and 22:22, which sets Fredoka's widest
 * digit, "2", throughout, and so is the reading that overflows cells measured in another face.
 */
private val READINGS_SECONDS = listOf(0, 488, 671, 3599, 1342)

/** Criterion 18d's ±0.5dp. */
private const val WIDTH_TOLERANCE_DP = 0.5f

private const val LATE_CLOCK = "late_clock"
private const val PRESENT_CLOCK = "present_clock"
private const val CELL_CLOCK = "cell_clock"

/** The ten digits; the widest of them sets the cell. */
private const val DECIMAL_DIGITS = "0123456789"

/** The clock's separator, whose cell is itself. */
private const val COLON = ':'

/** Fredoka Medium's widest digit, "2", in units per 1000 em (HarfBuzz on `res/font/fredoka_medium.ttf`). */
private const val FREDOKA_MEDIUM_WIDEST_DIGIT_PER_MILLE = 587f

/** The unit of [FREDOKA_MEDIUM_WIDEST_DIGIT_PER_MILLE]. */
private const val PER_MILLE = 1000f

/** The cell test's type size: 100sp, 400px on xxxhdpi, where one pixel is a quarter of a percent of a digit. */
private const val CELL_PROBE_SP = 100

/** The ruling's ±1px, which here is the pixel whole-pixel centring leaves unresolved. */
private const val CELL_TOLERANCE_PX = 1f

/** The platform's widest digit must be at least this far from Fredoka's for the cell test to tell them apart. */
private const val MIN_PLATFORM_CELL_GAP_PX = 3f

/**
 * The design baseline's width at a phone's density. At the default density, hinting rounds every
 * digit of both faces to 14px, so a clock measured in the fallback and one measured in Fredoka are
 * the same width there, and the late-face test could not tell them apart.
 */
private const val TIMER_WINDOW_QUALIFIER = "w360dp-h640dp-xxhdpi"

/** The cell test's window: 1440px wide on xxxhdpi, room for a clock set at 100sp (about 1032px). */
private const val CELL_WINDOW_QUALIFIER = "w360dp-h640dp-xxxhdpi"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TIMER_ROBOLECTRIC_SDK = 34
