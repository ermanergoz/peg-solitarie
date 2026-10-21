package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * How [CreditsTable] reads `docs/audio/CREDITS.md`, pinned on fixtures rather than on the
 * real file.
 *
 * The real file passes a position-based reader too, as long as nobody touches its column
 * order. These fixtures are the cases it would pass for the wrong reason: the credits contract
 * (`docs/tech/audio-silent-mode.md` §2.3) lets the sound-designer reorder and add columns, and
 * says only the first table under `## Files` counts, because the history sections below it
 * still name the retired OGG files.
 */
class CreditsTableTest {
    @Test
    fun columnsAreFoundByTheirHeaderTextWhateverOrderTheyAreIn() {
        val table =
            CreditsTable.parse(
                listOf(
                    "## Files",
                    "",
                    "| licence | sha256 | res/raw filename | source |",
                    "|---|---|---|---|",
                    "| CC0 1.0 | abc123 | `sfx_a.wav` | https://example.org/a |",
                ),
            )

        val row = table.rows.single()
        assertThat(row.cell("res/raw filename")).isEqualTo("`sfx_a.wav`")
        assertThat(row.cell("source")).isEqualTo("https://example.org/a")
        assertThat(row.cell("licence")).isEqualTo("CC0 1.0")
        assertThat(row.cell("author")).isNull()
    }

    @Test
    fun onlyTheFirstTableUnderTheFilesHeadingIsRead() {
        val table =
            CreditsTable.parse(
                listOf(
                    "# Credits",
                    "| res/raw filename | source |",
                    "|---|---|",
                    "| `sfx_retired_before.wav` | https://example.org/before |",
                    "## Files",
                    "One row per file.",
                    "",
                    "| res/raw filename | source |",
                    "|------------------|--------|",
                    "| `sfx_a.wav` | https://example.org/a |",
                    "| `sfx_b.wav` | audio/recipes/b.json |",
                    "",
                    "| res/raw filename | source |",
                    "|---|---|",
                    "| `sfx_retired_history.wav` | https://example.org/old |",
                    "## History",
                    "| res/raw filename | source |",
                    "|---|---|",
                    "| `sfx_retired_older.wav` | https://example.org/older |",
                ),
            )

        assertThat(table.rows.map { it.cell("res/raw filename") })
            .containsExactly("`sfx_a.wav`", "`sfx_b.wav`")
            .inOrder()
    }

    @Test
    fun anEscapedPipeStaysInsideItsCell() {
        val table =
            CreditsTable.parse(
                listOf(
                    "## Files",
                    "| res/raw filename | original filename | source |",
                    "|---|---|---|",
                    "| `sfx_a.wav` | take 1 \\| take 2 | https://example.org/a |",
                ),
            )

        assertThat(table.rows.single().cell("original filename")).isEqualTo("take 1 | take 2")
        assertThat(table.rows.single().cell("source")).isEqualTo("https://example.org/a")
    }

    @Test
    fun aDocumentWithNoTableUnderTheFilesHeadingIsRejected() {
        assertThrows(IllegalStateException::class.java) {
            CreditsTable.parse(
                listOf(
                    "## Files",
                    "Nothing here yet.",
                    "## History",
                    "| res/raw filename | source |",
                    "|---|---|",
                    "| `sfx_retired_older.wav` | https://example.org/older |",
                ),
            )
        }
    }
}
