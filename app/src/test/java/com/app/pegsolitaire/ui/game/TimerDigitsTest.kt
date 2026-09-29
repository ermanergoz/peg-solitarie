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
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.app.pegsolitaire.testing.shippedFaceFile
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.withTabularFigures
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * [TimerDigits] on a phone, where the timer's face arrives late.
 *
 * Fredoka is a downloadable Google Font: the first frame is drawn in the platform fallback, and
 * the real face lands a moment later. The digit cells are only as good as the face they were
 * measured in. If they kept the fallback's width, Fredoka's digits would sit in cells of the wrong
 * width, and part of the jitter criterion 18d forbids would come back on exactly the devices where
 * the face resolves. The emulator never shows this, because the face never resolves there.
 *
 * So the same clock is set twice, side by side: once in a Fredoka held back until after the first
 * frame, and once in the identical Fredoka loaded up front. Before the late face arrives, the two
 * clocks differ, which proves the late one really started in the fallback. Once it has arrived and
 * the clock is laid out again, the two must be the same width at every reading.
 *
 * ## The frame after the face lands
 *
 * On a device, the frame after a face lands lays its text out again. Robolectric does not: here
 * not even a plain `Text` in a late face ever changes after the face arrives. The host therefore
 * reads the face's resolution state itself ([lateFaceLanding]). That read recomposes it when the
 * face lands, and the pass that follows lays the text out again, standing in for the device's
 * frame. What stays under test is this layout's own part: once laid out again, the clock's cells
 * are the late face's, not a width kept from the fallback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TIMER_ROBOLECTRIC_SDK], qualifiers = TIMER_WINDOW_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimerDigitsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val fontFolder = TemporaryFolder()

    @Test
    fun aClockWhoseFaceArrivesAfterTheFirstFrameTakesThatFacesCells() {
        val file = fontFolder.shippedFaceFile()
        val arrival = CompletableDeferred<Unit>()
        val lateFace = FontFamily(FileFace(file, FontLoadingStrategy.Async, arrival))
        val presentFace = FontFamily(FileFace(file, FontLoadingStrategy.Blocking, arrival = null))
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
}

/**
 * The Fredoka file at the Medium weight the timer's role asks for, loaded [loadingStrategy]:
 * up front, or late, once [arrival] completes, as a downloadable face lands after the first frame.
 */
private class FileFace(
    val file: File,
    loadingStrategy: FontLoadingStrategy,
    val arrival: CompletableDeferred<Unit>?,
) : AndroidFont(loadingStrategy, FileFaceLoader, FontVariation.Settings()) {
    override val weight: FontWeight = FontWeight.Medium
    override val style: FontStyle = FontStyle.Normal

    /** The same typeface whenever it is loaded: only the timing differs between the two clocks. */
    fun typeface(): Typeface = Typeface.Builder(file).setFontVariationSettings(MEDIUM_WEIGHT_AXIS).build()
}

private object FileFaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(
        context: Context,
        font: AndroidFont,
    ): Typeface = (font as FileFace).typeface()

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
    ): Typeface {
        val face = font as FileFace
        face.arrival?.await()
        return face.typeface()
    }
}

/** Fredoka is a variable face; Medium is 500 on its weight axis. */
private const val MEDIUM_WEIGHT_AXIS = "'wght' 500"

/**
 * 00:00, 08:08, 11:11, 59:59 (criterion 18d's readings) and 22:22, which sets Fredoka's widest
 * digit, "2", throughout, and so is the reading that overflows cells measured in the fallback.
 */
private val READINGS_SECONDS = listOf(0, 488, 671, 3599, 1342)

/** Criterion 18d's ±0.5dp. */
private const val WIDTH_TOLERANCE_DP = 0.5f

private const val LATE_CLOCK = "late_clock"
private const val PRESENT_CLOCK = "present_clock"

/**
 * The design baseline's width at a phone's density. At the default density, hinting rounds every
 * digit of both faces to 14px, so a clock measured in the fallback and one measured in Fredoka are
 * the same width there, and this test could not tell them apart.
 */
private const val TIMER_WINDOW_QUALIFIER = "w360dp-h640dp-xxhdpi"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TIMER_ROBOLECTRIC_SDK = 34
