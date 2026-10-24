package com.app.pegsolitaire.audio

/**
 * The `## Files` table of `docs/audio/CREDITS.md`, read the way the credits contract
 * (`docs/tech/audio-silent-mode.md` §2.3) says it must be read.
 *
 * - **Only the first table under `## Files` counts.** The history sections further down still
 *   name the retired OGG files of 2026-08, and a second table there must never stand in for a
 *   row of the real one.
 * - **Columns are found by their header text, never by position**, so the sound-designer may
 *   reorder the columns or add new ones without breaking the build, and a test that read cell 2
 *   as "the source" cannot silently start reading the licence.
 *
 * Header text is compared trimmed and lower-cased. An escaped pipe (`\|`) stays inside its
 * cell, as it does in rendered Markdown.
 *
 * Test-source-set only.
 */
internal class CreditsTable(
    val headers: List<String>,
    val rows: List<CreditsRow>,
) {
    companion object {
        /** The heading the table sits under; nothing before it is read. */
        private const val FILES_HEADING = "## Files"

        /** Any second-level heading: the end of the `## Files` section. */
        private const val SECTION_HEADING_PREFIX = "## "

        private const val TABLE_LINE_PREFIX = "|"

        /** A pipe that is a column boundary, i.e. not written as `\|`. */
        private val CELL_BOUNDARY = Regex("""(?<!\\)\|""")

        /** The header's underline row: dashes, optional alignment colons, pipes. */
        private val SEPARATOR_ROW = Regex("""^\|?(\s*:?-+:?\s*\|)*\s*:?-+:?\s*\|?$""")

        /**
         * Reads the first table under `## Files` out of [lines], the whole document.
         *
         * @throws IllegalStateException when the document has no `## Files` heading, or no
         *   table follows it before the next section begins.
         */
        fun parse(lines: List<String>): CreditsTable {
            val headingIndex = lines.indexOfFirst { it.trim() == FILES_HEADING }
            check(headingIndex >= 0) { "No '$FILES_HEADING' heading in the credits document" }
            val section =
                lines
                    .drop(headingIndex + 1)
                    .takeWhile { !it.trimStart().startsWith(SECTION_HEADING_PREFIX) }
            val tableStart = section.indexOfFirst(::isTableLine)
            check(tableStart >= 0) { "No table under '$FILES_HEADING'" }
            val table = section.drop(tableStart).takeWhile(::isTableLine)
            check(table.size >= 2 && SEPARATOR_ROW.matches(table[1].trim())) {
                "The table under '$FILES_HEADING' has no header row and separator"
            }
            val headers = cellsOf(table[0]).map { it.lowercase() }
            val rows =
                table.drop(2).map { line ->
                    CreditsRow(headers.zip(cellsOf(line)).toMap())
                }
            return CreditsTable(headers = headers, rows = rows)
        }

        private fun isTableLine(line: String): Boolean = line.trimStart().startsWith(TABLE_LINE_PREFIX)

        private fun cellsOf(line: String): List<String> =
            line
                .trim()
                .removePrefix(TABLE_LINE_PREFIX)
                .removeSuffix(TABLE_LINE_PREFIX)
                .split(CELL_BOUNDARY)
                .map { it.trim().replace("\\|", "|") }
    }
}

/** One data row of the table, its cells keyed by their column's header text. */
internal class CreditsRow(
    private val cellsByHeader: Map<String, String>,
) {
    /** The cell under [header], or `null` when the table has no such column. */
    fun cell(header: String): String? = cellsByHeader[header]
}
