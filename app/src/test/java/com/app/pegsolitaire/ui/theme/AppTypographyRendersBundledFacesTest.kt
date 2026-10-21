package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Every role of the type scale renders in its bundled face, and not in the platform's (tech-lead
 * font ruling, T2).
 *
 * `AppTypographyTest` reads the scale; this one draws it. Inside [AppTheme], each role is set at
 * 100sp with no tracking and three probe strings are measured, under NATIVE graphics so the real
 * text stack lays them out from the packaged font files. Each width must match the width HarfBuzz
 * gives that string in the role's face, and at least one probe must differ from the same probe in
 * [FontFamily.Default] by a clear margin. A role that falls back to the platform face, or is set in
 * a neighbouring bundled face, fails here and is named.
 *
 * At 100sp on mdpi one em is 100px, so a width of `n` units per 1000 em is `n / 10` px. The probes
 * are set with linear metrics ([TextMotion.Animated]), as HarfBuzz's are. The default,
 * [TextMotion.Static], hints each glyph's advance to a whole pixel, and over thirteen glyphs those
 * roundings add up to more than the ±1px the widths are held to. The face is the same either way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FACES_ROBOLECTRIC_SDK], qualifiers = FACES_DENSITY_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppTypographyRendersBundledFacesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val expect: Expect = Expect.create()

    @Test
    fun everyRoleRendersInItsBundledFaceAndNotInThePlatformFace() {
        lateinit var typography: Typography
        lateinit var measurer: TextMeasurer
        composeRule.setContent {
            AppTheme {
                typography = MaterialTheme.typography
                measurer = rememberTextMeasurer()
            }
        }
        composeRule.waitForIdle()

        TYPE_ROLES.forEach { role ->
            val style =
                role.style(typography).copy(
                    fontSize = PROBE_SP.sp,
                    letterSpacing = 0.sp,
                    textMotion = TextMotion.Animated,
                )
            val platform = style.copy(fontFamily = FontFamily.Default)
            val platformDifferences =
                PROBE_TEXTS.mapIndexed { index, probe ->
                    val width = measurer.measure(probe, style).size.width
                    expect
                        .withMessage("${role.name}: \"$probe\" in px, against ${role.face}")
                        .that(width.toFloat())
                        .isWithin(WIDTH_TOLERANCE_PX)
                        .of(role.face.probeWidthsPerMille[index] * PROBE_SP / PER_MILLE)
                    abs(width - measurer.measure(probe, platform).size.width)
                }
            expect
                .withMessage("${role.name}: its widest difference from the platform face, in px")
                .that(platformDifferences.max())
                .isAtLeast(MIN_PLATFORM_DIFFERENCE_PX)
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val FACES_ROBOLECTRIC_SDK = 34

/** One px per sp, so the oracle's units convert by a plain scale. */
private const val FACES_DENSITY_QUALIFIER = "mdpi"

/** The probe size: at 100sp one em is 100px on mdpi. */
private const val PROBE_SP = 100

/** The oracle's unit: widths are given per 1000 em. */
private const val PER_MILLE = 1000f

/** The ruling's ±1px, which also covers the layout rounding a width up to a whole pixel. */
private const val WIDTH_TOLERANCE_PX = 1f

/** A role in its bundled face differs from the platform face by at least this on some probe. */
private const val MIN_PLATFORM_DIFFERENCE_PX = 8
