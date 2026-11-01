package com.app.pegsolitaire.locale

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.ResourceFont
import com.app.pegsolitaire.ui.theme.AppTypography
import com.app.pegsolitaire.ui.theme.TYPE_ROLES
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.awt.Font
import java.io.File
import java.util.Locale

/**
 * Every font file the type scale draws holds every character every shipped language puts on screen
 * (docs/tech-design.md §11.1 Q-039 (2); docs/ux/localisation-tr.md D-13, D-17, §7.3, C-2), so no
 * letter of a word falls back to the platform's font, whatever the role or the language.
 *
 * Nothing here is typed but the role list and the thirteen letters: the languages come from the
 * resource folders as `AppLanguagesTest` reads them; each language's characters from its own
 * `strings.xml` plus the base file's untranslated keys, which a device in that language also shows;
 * each role's file from `AppTypography` itself, its `ResourceFont` turned into a file through
 * `getResourceEntryName`. Each character is held to the file's own character map through
 * [Font.canDisplay], with no Android shaping in between, because Android chooses a font per
 * character from its character map, before any shaper runs. There is no allowance list: a character
 * a face lacks is fixed in the copy or in the face, never excepted here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [GLYPH_COVERAGE_SDK])
class BundledFontGlyphCoverageTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val fonts = mutableMapOf<File, Font>()

    @Test
    fun everyRoleDrawsEveryCharacterOfEveryShippedLanguageFromItsOwnFile() {
        val languages = shippedLanguageFolders()
        assertThat(languages.keys).contains(TURKISH)

        val gaps =
            languages.flatMap { (language, folder) ->
                val drawn = charactersOf(language, folder)
                TYPE_ROLES.mapNotNull { role ->
                    val file = fileOf(role.style(AppTypography))
                    val missing = drawn.filterNot { font(file).canDisplay(it) }
                    missing.takeIf { it.isNotEmpty() }?.let {
                        "$language, ${role.name}, ${file.name}: ${described(
                            it,
                        )}"
                    }
                }
            }

        assertThat(gaps).isEmpty()
    }

    /** One font directory, so a role's file cannot change with the language (no `font-tr/`). */
    @Test
    fun resFontIsTheOnlyFontResourceDirectory() {
        val fontDirectories = mainResDir.listFiles { dir -> dir.isDirectory && dir.name.startsWith(FONT_DIR) }.orEmpty()

        assertThat(fontDirectories.map { it.name }).containsExactly(FONT_DIR)
    }

    /**
     * What a device in [language] can draw from the app: its own values, the base file's untranslated
     * keys (a proper name reads the same in every language), and for Turkish the alphabet as a floor.
     */
    private fun charactersOf(
        language: String,
        folder: File,
    ): Set<Int> {
        val strings = StringResourceFile.read(File(folder, STRINGS_FILE))
        val base = StringResourceFile.read(File(mainResDir, BASE_STRINGS))
        val untranslated =
            base.strings.filterKeys { it in base.untranslatable }.values +
                base.plurals
                    .filterKeys { it in base.untranslatable }
                    .values
                    .flatMap { it.values }
        val values = if (language == BASE_LANGUAGE) strings.allValues else strings.allValues + untranslated
        val floor = if (language == TURKISH) TURKISH_LETTERS.codePoints().toArray().toSet() else emptySet()
        assertWithMessage("$language draws nothing").that(values).isNotEmpty()
        return values.flatMap { drawnCodePoints(it) }.toSet() + floor
    }

    /** The file a role is drawn from: its family's font at its weight, as a `res/font` file. */
    private fun fileOf(style: TextStyle): File {
        val family = style.fontFamily as FontListFontFamily
        val resource = family.fonts.filterIsInstance<ResourceFont>().single { it.weight == style.fontWeight }
        val entry = app.resources.getResourceEntryName(resource.resId)
        return File(mainResDir, FONT_DIR).listFiles { file -> file.nameWithoutExtension == entry }.orEmpty().single()
    }

    private fun font(file: File): Font = fonts.getOrPut(file) { Font.createFont(Font.TRUETYPE_FONT, file) }

    private fun described(codePoints: List<Int>): String =
        codePoints.sorted().joinToString {
            String.format(
                Locale.ROOT,
                CODE_POINT_FORMAT,
                it,
                String(Character.toChars(it)),
            )
        }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val GLYPH_COVERAGE_SDK = 34

private const val FONT_DIR = "font"
private const val STRINGS_FILE = "strings.xml"
private const val BASE_STRINGS = "values/strings.xml"
private const val TURKISH = "tr"

/**
 * Spec §7.3's thirteen: the twelve letters of the Turkish alphabet beyond ASCII (Ç ç Ğ ğ İ ı Ö ö
 * Ş ş Ü ü) and the â of "hâlâ" in the Undecided notice. A floor: Turkish copy may use any of them.
 */
private const val TURKISH_LETTERS = "ÇçĞğİıÖöŞşÜüâ"

/** How a missing glyph is reported: its code point and the character itself. */
private const val CODE_POINT_FORMAT = "U+%04X %s"
