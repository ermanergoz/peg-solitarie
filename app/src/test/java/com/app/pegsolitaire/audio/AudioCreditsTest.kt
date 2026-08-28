package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Guards the D-01 deliverable: every audio file committed to `app/src/main/res/raw/` must
 * carry an in-repo provenance row in `docs/audio/CREDITS.md` naming its original filename,
 * an HTTPS source and its licence.
 *
 * This is the T-07-01 mitigation that does not rot. The download-time checks (HTTPS fetch,
 * `unzip -l` inspection, `ffprobe` codec confirmation) all happened once, at curation time;
 * only this test keeps the *record* of them honest as the asset set changes. Adding a
 * twelfth sound without crediting it fails the build.
 *
 * Runner-free — no JUnit runner, no Robolectric, no Android types — because it is a plain
 * file scan over the working tree. It locates the repository root by walking up from the
 * process working directory to the directory holding `settings.gradle.kts`, rather than
 * assuming Gradle's test working directory, so it behaves the same from the IDE and CI.
 */
class AudioCreditsTest {
    private val repoRoot: File = findRepositoryRoot()

    private val soundFiles: List<File> =
        File(repoRoot, RAW_RESOURCE_DIR)
            .listFiles { file -> file.isFile && file.extension == AUDIO_EXTENSION }
            ?.sortedBy { it.name }
            .orEmpty()

    private val creditsLines: List<String> = File(repoRoot, CREDITS_PATH).readLines()

    @Test
    fun everyCommittedSoundHasACreditsRow() {
        assertThat(soundFiles).isNotEmpty()

        val uncredited = soundFiles.map { it.name }.filterNot { name -> tableRowFor(name) != null }

        assertThat(uncredited).isEmpty()
    }

    @Test
    fun everyCreditsRowNamesASourceAndLicence() {
        assertThat(soundFiles).isNotEmpty()

        soundFiles.forEach { file ->
            val row = tableRowFor(file.name)
            assertThat(row).isNotNull()

            val cells = requireNotNull(row).split(COLUMN_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            assertThat(cells.size).isAtLeast(REQUIRED_COLUMN_COUNT)
            assertThat(cells[SOURCE_COLUMN]).contains(HTTPS_PREFIX)
            assertThat(cells[LICENCE_COLUMN]).isNotEmpty()
        }
    }

    /**
     * The markdown table row crediting [fileName], or null if there is none. Restricted to
     * lines that actually start a table row so that prose elsewhere in CREDITS.md mentioning
     * a filename can never stand in for a real provenance row.
     */
    private fun tableRowFor(fileName: String): String? =
        creditsLines.firstOrNull { line ->
            line.trimStart().startsWith(COLUMN_SEPARATOR) && line.contains(fileName)
        }

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
        const val RAW_RESOURCE_DIR = "app/src/main/res/raw"
        const val CREDITS_PATH = "docs/audio/CREDITS.md"
        const val AUDIO_EXTENSION = "ogg"
        const val COLUMN_SEPARATOR = "|"
        const val HTTPS_PREFIX = "https://"
        const val REQUIRED_COLUMN_COUNT = 4
        const val SOURCE_COLUMN = 2
        const val LICENCE_COLUMN = 3
    }
}
