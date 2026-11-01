package com.app.pegsolitaire.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import com.app.pegsolitaire.R
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The app's type scale is set in the two bundled families, at the weights DESIGN-SYSTEM.md
 * §Typography gives each role (tech-lead font ruling, T1).
 *
 * ## What this guards
 *
 * For a month every role carried a `null` font family. `AppTypography` was declared above the two
 * families in the same file, so it was built while both were still `null`, and every role fell back
 * to the platform's Roboto on every device (qa-engineer F-A). Nothing failed and nothing looked
 * broken: the only symptom was the wrong face. This test reads the scale itself, with no rendering:
 * each role's family must be the bundled family of its group, each font in it a font file packaged
 * in the app and loaded before the first frame, and each weight a role asks for present in its
 * family exactly, so no role is drawn in a neighbouring or synthesised weight.
 *
 * The expected fonts are typed here from the ruling's table, not read from [AppFonts], so a font
 * swapped for another inside [AppFonts] fails too.
 */
class AppTypographyTest {
    @Test
    fun everyRoleIsSetInTheBundledFamilyOfItsGroup() {
        TYPE_ROLES.forEach { role ->
            val family = role.style(AppTypography).fontFamily
            assertWithMessage("${role.name}'s font family").that(family).isNotNull()
            assertWithMessage("${role.name}'s font family").that(family).isSameInstanceAs(appFamily(role.face.family))
        }
    }

    @Test
    fun everyRoleAsksForTheWeightTheDesignSystemGivesIt() {
        TYPE_ROLES.forEach { role ->
            assertWithMessage("${role.name}'s font weight")
                .that(role.style(AppTypography).fontWeight)
                .isEqualTo(role.face.weight)
        }
    }

    @Test
    fun everyWeightARoleAsksForIsInItsFamilyExactly() {
        TYPE_ROLES.forEach { role ->
            val style = role.style(AppTypography)
            val fonts = (style.fontFamily as? FontListFontFamily)?.fonts.orEmpty()
            assertWithMessage("the weights in ${role.name}'s family")
                .that(fonts.filter { it.style == FontStyle.Normal }.map { it.weight })
                .contains(style.fontWeight)
        }
    }

    @Test
    fun everyBundledFontIsAFontFileInTheAppLoadedBeforeTheFirstFrame() {
        TypeFamily.entries.forEach { family ->
            assertWithMessage("the fonts of $family")
                .that((appFamily(family) as? FontListFontFamily)?.fonts.orEmpty().map { it.described() })
                .containsExactlyElementsIn(FONT_FILES.getValue(family))
        }
    }

    private fun appFamily(family: TypeFamily): FontFamily =
        when (family) {
            TypeFamily.Fredoka -> AppFonts.Fredoka
            TypeFamily.Nunito -> AppFonts.Nunito
        }

    /**
     * What a font is, as far as this test cares: which packaged file, at which weight and style,
     * loaded how. A font that is not a packaged file has no resource id.
     */
    private data class FontFile(
        val resId: Int?,
        val weight: FontWeight,
        val style: FontStyle,
        val loading: FontLoadingStrategy,
    )

    private fun Font.described(): FontFile = FontFile((this as? ResourceFont)?.resId, weight, style, loadingStrategy)

    private companion object {
        /** The ruling's D3 table: the two bundled Fredoka weights and the three Nunito ones. */
        val FONT_FILES =
            mapOf(
                TypeFamily.Fredoka to
                    listOf(
                        bundled(R.font.fredoka_medium, FontWeight.Medium),
                        bundled(R.font.fredoka_semibold, FontWeight.SemiBold),
                    ),
                TypeFamily.Nunito to
                    listOf(
                        bundled(R.font.nunito_regular, FontWeight.Normal),
                        bundled(R.font.nunito_semibold, FontWeight.SemiBold),
                        bundled(R.font.nunito_bold, FontWeight.Bold),
                    ),
            )

        fun bundled(
            resId: Int,
            weight: FontWeight,
        ) = FontFile(resId, weight, FontStyle.Normal, FontLoadingStrategy.Blocking)
    }
}
