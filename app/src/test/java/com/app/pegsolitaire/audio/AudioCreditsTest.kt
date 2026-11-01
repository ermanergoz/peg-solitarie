package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Guards the provenance record of every sound the app ships: each file committed to
 * `app/src/main/res/raw/` has exactly one row in the `## Files` table of
 * `docs/audio/CREDITS.md`, and that row proves where the file came from.
 *
 * This is the T-07-01 mitigation that does not rot. The download-time checks (HTTPS fetch,
 * `unzip -l` before extraction, codec confirmation) happened once, at curation time; only this
 * test keeps the *record* of them honest as the asset set changes.
 *
 * The contract is `docs/tech/audio-silent-mode.md` §2.3 (P5), which replaced the OGG-only
 * scan once the set became WAV and two cues became in-house recipes:
 *
 * - every file in `res/raw`, **whatever its extension**, has exactly one row, matched by an
 *   exact file-name cell rather than by "the first line containing the name";
 * - a **sourced** row (its source is an `https://` URL) carries a licence from an allowlist,
 *   today exactly `CC0 1.0`. A licence enters [ALLOWED_SOURCED_LICENCES] only by a reviewed
 *   edit of this test, checked against rules/ip-policy.md: CC-BY also needs the licences
 *   screen, and NC, ND or SA never enter;
 * - an **in-house** row (its source is a repo path) names `audio/recipes/<stem>.json`, the
 *   recipe the WAV is rendered from, and that recipe is committed;
 * - no recipe is an orphan, and when the table pins a `sha256` the committed bytes match it.
 *   PCM output is deterministic, so the hash pins exactly the bytes that were reviewed.
 *
 * Not asserted here, by design: the byte-identical re-render, which needs the studio's
 * `sfx-synth.sh` (outside this repo, not on CI) and stays in the sound-designer's review.
 *
 * Runner-free: no JUnit runner, no Robolectric, no Android types, a plain scan of the
 * working tree.
 */
class AudioCreditsTest {
    private val rawFiles: List<File> = AudioRepoFiles.rawFiles()

    private val table: CreditsTable = CreditsTable.parse(AudioRepoFiles.creditsLines())

    @Test
    fun theFilesTableHasEveryColumnTheCreditsContractReads() {
        assertThat(table.headers).containsAtLeast(FILE_COLUMN, ORIGINAL_COLUMN, SOURCE_COLUMN, LICENCE_COLUMN)
    }

    @Test
    fun everyRowNamesExactlyOneBacktickedFileName() {
        val malformed = table.rows.map { it.cell(FILE_COLUMN).orEmpty() }.filterNot(BACKTICKED_FILE_NAME::matches)

        assertThat(table.rows).isNotEmpty()
        assertThat(malformed).isEmpty()
    }

    @Test
    fun everyFileInResRawHasExactlyOneCreditsRow() {
        val named = table.rows.mapNotNull(::fileNameOf)
        val rowsPerFile = rawFiles.associate { file -> file.name to named.count { it == file.name } }

        assertThat(rawFiles).isNotEmpty()
        assertThat(rowsPerFile.filterValues { it != 1 }).isEmpty()
    }

    @Test
    fun everyCreditsRowNamesAFileThatIsCommitted() {
        val committed = rawFiles.map { it.name }.toSet()

        assertThat(table.rows.mapNotNull(::fileNameOf).filterNot { it in committed }).isEmpty()
    }

    @Test
    fun everyRowIsEitherSourcedOrMadeInHouse() {
        val neither = table.rows.filter { kindOf(it) == null }.map { it.cell(FILE_COLUMN) }

        assertThat(neither).isEmpty()
    }

    @Test
    fun everySourcedRowCarriesAnAllowListedLicenceAndItsOriginalFilename() {
        val sourced = table.rows.filter { kindOf(it) == RowKind.Sourced }

        assertThat(sourced).isNotEmpty()
        sourced.forEach { row ->
            val file = row.cell(FILE_COLUMN)
            assertWithMessage("licence of $file").that(row.cell(LICENCE_COLUMN)).isIn(ALLOWED_SOURCED_LICENCES)
            assertWithMessage("original filename of $file").that(row.cell(ORIGINAL_COLUMN).orEmpty()).isNotEmpty()
            if (AUTHOR_COLUMN in table.headers) {
                assertWithMessage("author of $file").that(row.cell(AUTHOR_COLUMN).orEmpty()).isNotEmpty()
            }
        }
    }

    @Test
    fun everyInHouseRowNamesItsOwnRecipeAndThatRecipeIsCommitted() {
        val inHouse = table.rows.filter { kindOf(it) == RowKind.InHouse }

        assertThat(inHouse).isNotEmpty()
        inHouse.forEach { row ->
            val file = fileNameOf(row).orEmpty()
            val recipe = recipePathFor(file)
            assertWithMessage("recipe named by the row of $file").that(sourceOf(row)).isEqualTo(recipe)
            assertWithMessage("$recipe is committed").that(AudioRepoFiles.fileAt(recipe).isFile).isTrue()
            assertWithMessage("licence of $file").that(row.cell(LICENCE_COLUMN).orEmpty()).isNotEmpty()
        }
    }

    @Test
    fun everyRecipeIsNamedByExactlyOneRow() {
        val named = table.rows.filter { kindOf(it) == RowKind.InHouse }.map(::sourceOf)
        val recipes = AudioRepoFiles.recipeFiles().map(AudioRepoFiles::repoPathOf)

        assertThat(recipes).isNotEmpty()
        assertThat(recipes.associateWith { recipe -> named.count { it == recipe } }.filterValues { it != 1 }).isEmpty()
    }

    @Test
    fun everyCommittedFileMatchesTheSha256ItsRowPins() {
        // The column is optional in the contract; when it is present, every cell in it binds.
        val pinned = table.rows.mapNotNull { row -> row.cell(SHA256_COLUMN)?.let { fileNameOf(row) to it } }

        pinned.forEach { (file, sha256) ->
            val committed = rawFiles.single { it.name == file }
            assertWithMessage("sha256 of $file").that(sha256Of(committed)).isEqualTo(sha256.lowercase())
        }
    }

    private enum class RowKind { Sourced, InHouse }

    /** The file a row credits, or `null` when its file cell is not exactly one backticked name. */
    private fun fileNameOf(row: CreditsRow): String? =
        BACKTICKED_FILE_NAME.matchEntire(row.cell(FILE_COLUMN).orEmpty())?.groupValues?.get(1)

    private fun sourceOf(row: CreditsRow): String = row.cell(SOURCE_COLUMN).orEmpty().removeSurrounding(BACKTICK)

    /** Sourced when the source is an `https://` URL, in-house when it is a repo path, else neither. */
    private fun kindOf(row: CreditsRow): RowKind? {
        val source = sourceOf(row)
        return when {
            HTTPS_SOURCE in source -> RowKind.Sourced
            REPO_PATH.matches(source) -> RowKind.InHouse
            else -> null
        }
    }

    /** `sfx_invalid.wav` → `audio/recipes/invalid.json`. */
    private fun recipePathFor(fileName: String): String =
        RECIPES_DIR_PREFIX + fileName.removePrefix(RAW_FILE_PREFIX).substringBeforeLast(EXTENSION_DOT) + RECIPE_SUFFIX

    private fun sha256Of(file: File): String =
        HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest(file.readBytes()))

    private companion object {
        const val FILE_COLUMN = "res/raw filename"
        const val ORIGINAL_COLUMN = "original filename"
        const val SOURCE_COLUMN = "source"
        const val LICENCE_COLUMN = "licence"
        const val AUTHOR_COLUMN = "author"
        const val SHA256_COLUMN = "sha256"

        /** The licences a sourced sound may carry. Widen only by a reviewed edit (rules/ip-policy.md). */
        val ALLOWED_SOURCED_LICENCES = setOf("CC0 1.0")

        /** A cell that is exactly one backticked file name and nothing else. */
        val BACKTICKED_FILE_NAME = Regex("^`([^`\\s]+)`$")

        /** A repo-relative path: no scheme, no spaces, no prose. */
        val REPO_PATH = Regex("^[A-Za-z0-9_][A-Za-z0-9_./-]*$")

        const val HTTPS_SOURCE = "https://"
        const val BACKTICK = "`"
        const val RECIPES_DIR_PREFIX = "audio/recipes/"
        const val RECIPE_SUFFIX = ".json"
        const val RAW_FILE_PREFIX = "sfx_"
        const val EXTENSION_DOT = '.'
        const val SHA_256 = "SHA-256"
    }
}
