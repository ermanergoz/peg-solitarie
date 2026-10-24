package com.app.pegsolitaire.audio

import java.io.File

/**
 * The committed audio files the runner-free audio tests scan: every file in `res/raw`, the
 * in-house recipes and `docs/audio/CREDITS.md`.
 *
 * The repository root is found by walking up from the process working directory to the
 * directory holding `settings.gradle.kts`, rather than assuming Gradle's test working
 * directory, so the scans behave the same from the IDE and from CI.
 *
 * Test-source-set only.
 */
internal object AudioRepoFiles {
    private const val ROOT_MARKER = "settings.gradle.kts"
    private const val RAW_RESOURCE_DIR = "app/src/main/res/raw"
    private const val RECIPES_DIR = "audio/recipes"
    private const val CREDITS_PATH = "docs/audio/CREDITS.md"
    private const val RECIPE_EXTENSION = "json"

    /** The repository root: the nearest ancestor of the working directory holding [ROOT_MARKER]. */
    val root: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, ROOT_MARKER).isFile }
            ?: error("No ancestor of ${File(".").absolutePath} contains $ROOT_MARKER")
    }

    /** Every file committed to `res/raw`, whatever its extension, sorted by name. */
    fun rawFiles(): List<File> = filesIn(RAW_RESOURCE_DIR)

    /** Every in-house recipe, `audio/recipes/<name>.json`, sorted by name. */
    fun recipeFiles(): List<File> = filesIn(RECIPES_DIR).filter { it.extension == RECIPE_EXTENSION }

    /** The lines of `docs/audio/CREDITS.md`. */
    fun creditsLines(): List<String> = File(root, CREDITS_PATH).readLines()

    /** [file]'s path relative to the repository root, with forward slashes. */
    fun repoPathOf(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    /** The file at the repository-relative [path]. */
    fun fileAt(path: String): File = File(root, path)

    private fun filesIn(dir: String): List<File> =
        File(root, dir)
            .listFiles { file -> file.isFile }
            ?.sortedBy { it.name }
            .orEmpty()
}
