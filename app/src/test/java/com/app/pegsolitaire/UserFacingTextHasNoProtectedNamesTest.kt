package com.app.pegsolitaire

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * BD-01 as a test (the tech-lead's D9 and A10): no user-facing string or bundled asset names King,
 * its games or its callouts, nor A.4's four words (research-analyst.md §3a, A.4). It scans every
 * `values*` `strings.xml` and everything under `src/main/assets` on every Check, so a later string
 * is held to the verdict as well. Whole words, any casing.
 */
class UserFacingTextHasNoProtectedNamesTest {
    @Test
    fun theScannerFindsEveryProtectedWordAndCalloutInAFixture() {
        // Every protected word in a sentence, cased as a player might type it; the scanner must find each.
        val fixture = ProtectedNames.words.joinToString(", ") { it.uppercase() } + ". Tasty! Divine!"
        assertThat(ProtectedNames.hits(fixture)).containsAtLeastElementsIn(ProtectedNames.words)
        assertThat(ProtectedNames.hits(fixture)).containsAtLeast("tasty!", "divine!")
        assertThat(ProtectedNames.hits("Peg Solitaire: kingdom-free, candied? No: crushed ice.")).isEmpty()
    }

    @Test
    fun noUserFacingStringOrAssetNamesAProtectedWord() {
        val files = userFacingFiles()
        assertWithMessage("files scanned").that(files).isNotEmpty()
        files.forEach { file ->
            assertWithMessage(file.relativeTo(root).path).that(ProtectedNames.hits(file.readText())).isEmpty()
        }
    }

    private val root: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }.first { File(it, ROOT_MARKER).isFile }
    }

    private fun userFacingFiles(): List<File> {
        val res = File(root, RES_DIR).listFiles { f -> f.isDirectory && f.name.startsWith(VALUES_PREFIX) }.orEmpty()
        val strings = res.map { File(it, STRINGS_FILE) }.filter { it.isFile }
        val assets = File(root, ASSETS_DIR).walkTopDown().filter { it.isFile }.toList()
        return (strings + assets).sortedBy { it.path }
    }

    private companion object {
        const val ROOT_MARKER = "settings.gradle.kts"
        const val RES_DIR = "app/src/main/res"
        const val ASSETS_DIR = "app/src/main/assets"
        const val VALUES_PREFIX = "values"
        const val STRINGS_FILE = "strings.xml"
    }
}

/**
 * The verdict's never-write list (§3a) plus A.4's words, and the §3a callouts. They are stored
 * reversed so that this file, which must name them, adds none of them to the diff that BD-01's own
 * grep reads (the backdrop spec §11: it prints zero lines).
 */
private object ProtectedNames {
    val words: List<String> =
        listOf(
            "ydnac",
            "seidnac",
            "hsurc",
            "agas",
            "dnalydnac",
            "iffit",
            "eeffot",
            "itey",
            "sudo",
            "gnik",
            "noisivitca",
            "tfosorcim",
            "yllej",
            "ados",
            "ragus",
            "teews",
        ).map { it.reversed() }
    private val callouts: List<String> = listOf(words.last(), "tasty", "delicious", "divine")
    private val wordPattern = Regex("""\b(${words.joinToString("|")})\b""", RegexOption.IGNORE_CASE)
    private val calloutPattern = Regex("""\b(${callouts.joinToString("|")})!""", RegexOption.IGNORE_CASE)

    fun hits(text: String): List<String> =
        (wordPattern.findAll(text) + calloutPattern.findAll(text)).map { it.value.lowercase() }.toList()
}
