package com.app.pegsolitaire.locale

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer

/**
 * The two Fredoka files the app ships are the committed recipe's output, and the recipe changed
 * nothing English can see (docs/tech-design.md §11.1 Q-039 (1), (2)).
 *
 * `fonts/fredoka-tr/recipe.json` is the record: the upstream inputs and the shipped outputs by
 * sha256, and the five composites the build adds (Ğ ğ İ Ş ş). The hashes tie the shipped bytes to
 * the recipe and the recipe to Google Fonts' files; the rest compares upstream and shipped through
 * `java.awt`'s own reading of each file: the code points each can draw, each glyph's advance, and
 * each English string's advance with kerning on. `fonts/fredoka-tr/verify.py` makes the byte-level
 * comparison of the same contract at build and review time; it needs Python, so it is not here.
 */
class BundledFontRecipeTest {
    private val recipeDir = File(repositoryRoot, RECIPE_DIR)
    private val recipe: JsonObject by lazy {
        Json
            .parseToJsonElement(
                File(recipeDir, RECIPE_FILE).readText(),
            ).jsonObject
    }
    private val weights by lazy { recipe.getValue("weights").jsonArray.map { it.jsonObject } }
    private val composites: Set<Int> by lazy {
        recipe
            .getValue("glyphs")
            .jsonArray
            .map { codePoint(it.jsonObject.string("codepoint")) }
            .toSet()
    }

    @Test
    fun theShippedFredokaFilesAreTheRecipesOutputs() {
        assertThat(weights).isNotEmpty()
        weights.forEach { weight ->
            val shipped = File(repositoryRoot, weight.string("output"))
            assertWithMessage(shipped.name).that(sha256(shipped)).isEqualTo(weight.string("output_sha256"))
            assertWithMessage(
                shipped.name,
            ).that(shipped.length()).isEqualTo(
                weight
                    .getValue("output_bytes")
                    .jsonPrimitive.int
                    .toLong(),
            )
        }
    }

    @Test
    fun theRecipesInputsAreThePinnedUpstreamCopies() {
        val inputs = recipe.getValue("inputs").jsonArray.map { it.jsonObject }
        assertThat(inputs).isNotEmpty()
        inputs.forEach { input ->
            val file = File(recipeDir, input.string("file"))
            assertWithMessage(input.string("file")).that(sha256(file)).isEqualTo(input.string("sha256"))
        }
    }

    /** The licence the upstream files came with is the very notice the app ships (OFL §2). */
    @Test
    fun theUpstreamLicenceIsTheNoticeTheAppShips() {
        assertThat(File(recipeDir, UPSTREAM_OFL).readBytes()).isEqualTo(File(repositoryRoot, SHIPPED_OFL).readBytes())
    }

    @Test
    fun eachShippedFileDrawsTheUpstreamCodePointsAndExactlyTheRecipesCompositesBeside() {
        assertThat(composites).isNotEmpty()
        eachWeight { name, upstream, shipped ->
            val before = displayable(upstream)
            val after = displayable(shipped)
            assertWithMessage("$name: what the build added").that(after - before).containsExactlyElementsIn(composites)
            assertWithMessage("$name: what the build lost").that(before - after).isEmpty()
        }
    }

    @Test
    fun everyUpstreamCodePointKeepsItsAdvance() {
        eachWeight { name, upstream, shipped ->
            val moved = displayable(upstream).filter { advance(upstream, it) != advance(shipped, it) }
            assertWithMessage("$name: code points whose advance moved").that(moved).isEmpty()
        }
    }

    /**
     * Every base string the upstream file could draw lays out to the same advance, kerning and
     * ligatures on: what an English screen shows is unchanged to the unit. A string with a
     * character the upstream lacks could not have been drawn by it, so it has nothing to keep.
     */
    @Test
    fun everyEnglishStringKeepsItsKernedAdvance() {
        val english = StringResourceFile.read(File(mainResDir, BASE_STRINGS)).allValues.map { displayedText(it) }
        eachWeight { name, upstream, shipped ->
            val drawable =
                english.filter { text ->
                    text.isNotEmpty() &&
                        text.codePoints().allMatch { upstream.canDisplay(it) }
                }
            assertWithMessage("$name: strings compared").that(drawable.size).isAtLeast(english.size - 1)
            val moved = drawable.filter { kernedAdvance(upstream, it) != kernedAdvance(shipped, it) }
            assertWithMessage("$name: strings whose advance moved").that(moved).isEmpty()
        }
    }

    /**
     * Each composite advances and kerns as the letter it is built on (its Unicode decomposition's
     * base): next to every ASCII letter, on either side, `Ğy` lays out as `Gy` does.
     */
    @Test
    fun eachCompositeAdvancesAndKernsAsItsBaseLetter() {
        eachWeight { name, _, shipped ->
            composites.forEach { composite ->
                val letter = String(Character.toChars(composite))
                val base = Normalizer.normalize(letter, Normalizer.Form.NFD).substring(0, 1)
                val mismatches =
                    ASCII_LETTERS.filter { other ->
                        kernedAdvance(shipped, letter + other) != kernedAdvance(shipped, base + other) ||
                            kernedAdvance(shipped, other + letter) != kernedAdvance(shipped, other + base)
                    }
                assertWithMessage("$name, $letter against $base")
                    .that(advance(shipped, composite))
                    .isEqualTo(advance(shipped, base.codePointAt(0)))
                assertWithMessage("$name, $letter against $base: neighbours that kern differently")
                    .that(mismatches.toList())
                    .isEmpty()
            }
        }
    }

    private fun eachWeight(check: (name: String, upstream: Font, shipped: Font) -> Unit) {
        weights.forEach { weight ->
            val upstream = sized(File(recipeDir, weight.string("input")))
            val shipped = sized(File(repositoryRoot, weight.string("output")))
            check(weight.string("weight"), upstream, shipped)
        }
    }
}

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

private fun codePoint(written: String): Int = written.removePrefix(CODE_POINT_PREFIX).toInt(HEX_RADIX)

private fun sha256(file: File): String =
    MessageDigest.getInstance(SHA_256).digest(file.readBytes()).joinToString("") { byte ->
        (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
    }

private fun sized(file: File): Font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(PROBE_SIZE)

/** The code points of the Basic Multilingual Plane that [font]'s character map draws. */
private fun displayable(font: Font): Set<Int> = (0..BMP_LAST).filter { font.canDisplay(it) }.toSet()

/** A single glyph's advance, unhinted (fractional metrics), from the font's own metrics. */
private fun advance(
    font: Font,
    codePoint: Int,
): Float = font.createGlyphVector(RENDER_CONTEXT, String(Character.toChars(codePoint))).getGlyphMetrics(0).advance

/** [text]'s laid-out advance with kerning and ligatures on, as a shaper sets it. */
private fun kernedAdvance(
    font: Font,
    text: String,
): Float = TextLayout(text, font.deriveFont(SHAPING), RENDER_CONTEXT).advance

private const val RECIPE_DIR = "fonts/fredoka-tr"
private const val RECIPE_FILE = "recipe.json"
private const val UPSTREAM_OFL = "upstream/OFL.txt"
private const val SHIPPED_OFL = "app/src/main/assets/licenses/Fredoka-OFL.txt"
private const val BASE_STRINGS = "values/strings.xml"
private const val CODE_POINT_PREFIX = "U+"
private const val HEX_RADIX = 16
private const val BYTE_MASK = 0xff
private const val SHA_256 = "SHA-256"
private const val BMP_LAST = 0xFFFF

/** A size where one unit of the 1000-unit em is a whole unit of the measurement. */
private const val PROBE_SIZE = 1000f
private const val ASCII_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

/** No anti-aliasing, fractional metrics: advances as the font states them, never rounded. */
private val RENDER_CONTEXT = FontRenderContext(null, false, true)
private val SHAPING =
    mapOf(
        TextAttribute.KERNING to TextAttribute.KERNING_ON,
        TextAttribute.LIGATURES to TextAttribute.LIGATURES_ON,
    )
