package com.app.pegsolitaire.ui.licenses

import com.app.pegsolitaire.data.licenses.BundledLicenseText
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * `reflowLicenseText` (spec LS-07, LS-AC05; tech design §3.6): a licence text re-flows to fit any
 * screen and font scale, and is never edited. Hard line breaks inside a paragraph become single
 * spaces, blank or whitespace-only lines separate paragraphs, `\r\n` and `\r` count as line breaks,
 * and nothing but whitespace changes — the property the curated-text test proves on all nine
 * committed files, the two CRLF font notices included.
 */
class LicenseTextReflowTest {
    @Test
    fun joinsHardWrappedLinesWithinAParagraph() {
        assertThat(reflowLicenseText("Copyright 2008 Google Inc.\nAll rights reserved.\n"))
            .containsExactly("Copyright 2008 Google Inc. All rights reserved.")
    }

    @Test
    fun separatesParagraphsOnBlankLines() {
        assertThat(reflowLicenseText("First line\nstill first.\n\nSecond.\n"))
            .containsExactly("First line still first.", "Second.")
            .inOrder()
    }

    @Test
    fun dropsWhitespaceOnlyParagraphsAndTreatsARunOfBlankLinesAsOneBreak() {
        assertThat(reflowLicenseText("\n \n\t\nOnly one.\n   \n\n")).containsExactly("Only one.")
        assertThat(reflowLicenseText("A\n\n\n \nB")).containsExactly("A", "B").inOrder()
    }

    @Test
    fun normalisesCarriageReturns() {
        assertThat(reflowLicenseText("Line one\r\nline two\r\rNext\rparagraph"))
            .containsExactly("Line one line two", "Next paragraph")
            .inOrder()
    }

    @Test
    fun trimsEachLineButKeepsTheSpacingInsideIt() {
        assertThat(reflowLicenseText("   TERMS AND CONDITIONS\n  1.  Definitions.  \n"))
            .containsExactly("TERMS AND CONDITIONS 1.  Definitions.")
    }

    @Test
    fun anEmptyTextHasNoParagraphs() {
        assertThat(reflowLicenseText("")).isEmpty()
        assertThat(reflowLicenseText("\r\n\n  \n")).isEmpty()
    }

    @Test
    fun reflowChangesWhitespaceOnlyForEveryCuratedText() {
        val assets = findAssetsRoot()

        BundledLicenseText.entries.forEach { text ->
            val raw = File(assets, text.assetPath).readText(Charsets.UTF_8)

            val paragraphs = reflowLicenseText(raw)

            assertWithMessage(text.assetPath).that(paragraphs).isNotEmpty()
            assertWithMessage(text.assetPath)
                .that(collapseWhitespace(paragraphs.joinToString(" ")))
                .isEqualTo(collapseWhitespace(raw))
        }
    }

    /**
     * Cronet's notice is 785,183 B in 2,497 paragraphs and is re-flowed on the Default dispatcher,
     * so the re-flow is one pass with a builder per paragraph. A synthetic text of that shape comes
     * back as exactly its paragraphs, first and last intact.
     */
    @Test
    fun reflowsATwoThousandFiveHundredParagraphTextIntoExactlyItsParagraphs() {
        val raw =
            buildString {
                repeat(PARAGRAPH_COUNT) { index ->
                    repeat(LINES_PER_PARAGRAPH) { line ->
                        append("Paragraph $index line $line of a long notice text.\n")
                    }
                    append('\n')
                }
            }

        val paragraphs = reflowLicenseText(raw)

        assertThat(raw.length).isAtLeast(SYNTHETIC_MINIMUM_CHARS)
        assertThat(paragraphs).hasSize(PARAGRAPH_COUNT)
        assertThat(paragraphs.first()).startsWith("Paragraph 0 line 0 of a long notice text. Paragraph 0 line 1 ")
        assertThat(paragraphs.last())
            .endsWith("Paragraph ${PARAGRAPH_COUNT - 1} line ${LINES_PER_PARAGRAPH - 1} of a long notice text.")
    }

    /** [text] with every run of whitespace (Kotlin's `isWhitespace`) made one space, and trimmed. */
    private fun collapseWhitespace(text: String): String =
        buildString {
            var pendingSpace = false
            text.forEach { char ->
                if (char.isWhitespace()) {
                    pendingSpace = isNotEmpty()
                } else {
                    if (pendingSpace) append(' ')
                    pendingSpace = false
                    append(char)
                }
            }
        }

    /** `app/src/main/assets`, found by walking up to the directory that holds `settings.gradle.kts`. */
    private fun findAssetsRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, ROOT_MARKER).isFile) return File(candidate, ASSETS_DIR)
            candidate = candidate.parentFile
        }
        error("Could not locate the repository root from ${File(".").absolutePath}.")
    }

    private companion object {
        const val ROOT_MARKER = "settings.gradle.kts"
        const val ASSETS_DIR = "app/src/main/assets"
        const val PARAGRAPH_COUNT = 2_500
        const val LINES_PER_PARAGRAPH = 8
        const val SYNTHETIC_MINIMUM_CHARS = 800_000
    }
}
