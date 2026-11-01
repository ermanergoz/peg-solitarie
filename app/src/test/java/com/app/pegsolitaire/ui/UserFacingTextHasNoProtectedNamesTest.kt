package com.app.pegsolitaire.ui

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * BD-01's guard (spec LS-AC04, LS-AC17; brief §4.3): no text the studio ships to the player names
 * a protected product, brand or character. It scans the `strings.xml` of every `src/main/res`
 * directory whose name starts with `values`, and every file under `src/main/assets/` — the licence
 * texts the Licenses screens show — for each word in [PROTECTED_WORDS] as a whole word, in any case.
 *
 * Written on `feature/licences-screen` because LS-AC17 presupposes it runs on this branch's Check;
 * `feature/candy-backdrop` owns the word list from here and may widen it. The Google notices the
 * build generates live under `build/`, outside this scan by construction: they are Google's texts,
 * reproduced verbatim and never edited (spec §3.2, "Never listed").
 *
 * Runner-free, like `AudioCreditsTest`: a plain file scan over the working tree, rooted by walking
 * up to the directory that holds `settings.gradle.kts`.
 */
class UserFacingTextHasNoProtectedNamesTest {
    private val appMain = File(findRepositoryRoot(), APP_MAIN_DIR)

    @Test
    fun noStringResourceNamesAProtectedWord() {
        val stringFiles =
            File(appMain, RES_DIR)
                .listFiles { dir -> dir.isDirectory && dir.name.startsWith(VALUES_DIR_PREFIX) }
                .orEmpty()
                .map { File(it, STRINGS_FILE) }
                .filter { it.isFile }

        assertThat(stringFiles).isNotEmpty()
        assertThat(stringFiles.flatMap { offences(it) }).isEmpty()
    }

    @Test
    fun noCuratedLicenceTextNamesAProtectedWord() {
        val assetFiles =
            File(appMain, ASSETS_DIR)
                .walkTopDown()
                .filter { it.isFile }
                .toList()

        assertThat(assetFiles).isNotEmpty()
        assertThat(assetFiles.flatMap { offences(it) }).isEmpty()
    }

    /** The scan itself: a whole word in any case is found; the same letters inside a word are not. */
    @Test
    fun theScanFindsAWholeWordInAnyCaseButNotTheLettersInsideAnotherWord() {
        assertWithMessage("whole word, mixed case")
            .that(protectedWordsIn("A Sweet treat"))
            .containsExactly("sweet")
        assertWithMessage("inside another word")
            .that(protectedWordsIn("making a sodality"))
            .isEmpty()
        assertWithMessage("next to punctuation")
            .that(protectedWordsIn("(KING)"))
            .containsExactly("king")
    }

    /** `path:line: word` for each protected word found in [file]. */
    private fun offences(file: File): List<String> {
        val where = file.relativeTo(appMain).invariantSeparatorsPath
        return file.readLines(Charsets.UTF_8).flatMapIndexed { index, line ->
            protectedWordsIn(line).map { word -> "$where:${index + 1}: $word" }
        }
    }

    private fun protectedWordsIn(text: String): List<String> =
        PROTECTED_WORD_PATTERN.findAll(text).map { it.value.lowercase() }.toList()

    private fun findRepositoryRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, ROOT_MARKER).isFile) return candidate
            candidate = candidate.parentFile
        }
        error(
            "Could not locate the repository root: no ancestor of ${File(".").absolutePath} " +
                "contains $ROOT_MARKER.",
        )
    }

    private companion object {
        const val ROOT_MARKER = "settings.gradle.kts"
        const val APP_MAIN_DIR = "app/src/main"
        const val RES_DIR = "res"
        const val ASSETS_DIR = "assets"
        const val VALUES_DIR_PREFIX = "values"
        const val STRINGS_FILE = "strings.xml"

        /**
         * The candy-backdrop verdict's twelve (`tech-lead-architecture.md`, Checks, 2026-10-17)
         * and A.4's four. The candy branch owns this list and may widen it; it is never narrowed
         * to make a scan pass.
         */
        val PROTECTED_WORDS =
            listOf(
                "candy",
                "candies",
                "crush",
                "saga",
                "candyland",
                "tiffi",
                "toffee",
                "yeti",
                "odus",
                "king",
                "activision",
                "microsoft",
                "jelly",
                "soda",
                "sugar",
                "sweet",
            )

        val PROTECTED_WORD_PATTERN =
            Regex(
                PROTECTED_WORDS.joinToString(separator = "|", prefix = "\\b(?:", postfix = ")\\b"),
                RegexOption.IGNORE_CASE,
            )
    }
}
