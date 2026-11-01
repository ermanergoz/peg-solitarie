package com.app.pegsolitaire.locale

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.awt.Font
import java.io.File
import java.util.Locale

/**
 * Turkish text in the body face draws in the body face (docs/ux/localisation-tr.md D-13, §7.3, C-2):
 * every Nunito file in `res/font` maps every Turkish letter, and every character the Turkish copy
 * uses, to a glyph of its own, so no letter of a Turkish word falls back to the platform's font.
 * Read from each file's own character map through [Font.canDisplay], with no Android in between.
 *
 * Fredoka is not held to this, because it cannot pass: no Fredoka release maps Ğ ğ İ Ş ş (Google
 * Fonts v17, static and variable, the google/fonts repository and the designer's upstream at
 * 35c584f, all checked on 2026-11-01), so a Turkish heading set in Fredoka draws those five letters
 * in the platform font. That is a decision for the tech-lead and the ux-designer
 * (`.planning/reports/turkish-locale/android-game-dev.md`); the Fredoka files join this test
 * when it is made.
 */
class BundledFontGlyphCoverageTest {
    private val nunitoFaces: List<File> by lazy {
        File(mainResDir, FONT_DIR)
            .listFiles { file -> file.name.startsWith(NUNITO_PREFIX) && file.extension == TRUETYPE_EXTENSION }
            .orEmpty()
            .sortedBy { it.name }
    }

    @Test
    fun everyNunitoFaceDrawsEveryTurkishLetter() {
        assertThat(nunitoFaces.map { it.name }).containsExactly(*NUNITO_FILES)
        nunitoFaces.forEach { face ->
            assertWithMessage(face.name).that(undrawable(face, TURKISH_LETTERS.codePoints().toArray())).isEmpty()
        }
    }

    @Test
    fun everyNunitoFaceDrawsEveryCharacterOfTheTurkishCopy() {
        val copy = StringResourceFile.read(File(mainResDir, TURKISH_STRINGS)).allValues.joinToString(separator = "")
        val drawn =
            copy
                .filterNot { it in ANDROID_ESCAPES || it.isWhitespace() }
                .codePoints()
                .distinct()
                .toArray()

        nunitoFaces.forEach { face ->
            assertWithMessage(face.name).that(undrawable(face, drawn)).isEmpty()
        }
    }

    /** The code points of [codePoints] that [face]'s character map has no glyph for, as text. */
    private fun undrawable(
        face: File,
        codePoints: IntArray,
    ): List<String> {
        val font = Font.createFont(Font.TRUETYPE_FONT, face)
        return codePoints
            .filterNot { font.canDisplay(it) }
            .map { String.format(Locale.ROOT, CODE_POINT_FORMAT, it, String(Character.toChars(it))) }
    }
}

private const val FONT_DIR = "font"
private const val NUNITO_PREFIX = "nunito_"
private const val TRUETYPE_EXTENSION = "ttf"
private val NUNITO_FILES = arrayOf("nunito_bold.ttf", "nunito_regular.ttf", "nunito_semibold.ttf")
private const val TURKISH_STRINGS = "values-tr/strings.xml"

/**
 * Spec §7.3's thirteen: the twelve letters of the Turkish alphabet beyond ASCII (Ç ç Ğ ğ İ ı Ö ö
 * Ş ş Ü ü) and the â of "hâlâ" in the Undecided notice.
 */
private const val TURKISH_LETTERS = "ÇçĞğİıÖöŞşÜüâ"

/** How a missing glyph is reported: its code point and the character itself. */
private const val CODE_POINT_FORMAT = "U+%04X %s"

/** Characters a resource value carries for aapt2 that are never drawn: the escape and the quote. */
private const val ANDROID_ESCAPES = "\\\""
