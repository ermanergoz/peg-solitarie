package com.app.pegsolitaire.ui.licenses

/**
 * Re-flows a licence text into its paragraphs (spec LS-07; tech design §3.6, §13.1 LN-4), so it
 * fits the screen at any font scale while no word is changed, added or removed.
 *
 * - `\r\n` and a lone `\r` are line breaks, like `\n`.
 * - A blank or whitespace-only line ends a paragraph; a run of them is one break.
 * - Each line is trimmed, and a paragraph's lines are joined with one space; spacing *inside* a
 *   line is kept as it is.
 * - Empty paragraphs are dropped.
 *
 * It changes whitespace only: with every whitespace run collapsed, the paragraphs joined by a space
 * read exactly as the raw text does (`LicenseTextReflowTest`). The asset itself is never touched.
 *
 * **One pass.** Cronet's notice is 785,183 B in 2,497 paragraphs and is re-flowed on the Default
 * dispatcher, so this walks the text once, indexing into it and appending each trimmed line to a
 * builder per paragraph — no `String +=`, no regex over the whole text.
 */
fun reflowLicenseText(raw: String): List<String> {
    val paragraphs = ArrayList<String>()
    val paragraph = StringBuilder()
    var lineStart = 0
    while (lineStart <= raw.length) {
        val lineEnd = raw.lineEndFrom(lineStart)
        val first = raw.skipWhitespaceForward(lineStart, lineEnd)
        val last = raw.skipWhitespaceBackward(lineEnd, first)
        if (first == last) {
            paragraph.flushInto(paragraphs)
        } else {
            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(raw, first, last)
        }
        lineStart = raw.nextLineStart(lineEnd)
    }
    paragraph.flushInto(paragraphs)
    return paragraphs
}

/** The index of the first `\n` or `\r` at or after [start], or the text's length. */
private fun String.lineEndFrom(start: Int): Int {
    var end = start
    while (end < length && this[end] != LF && this[end] != CR) end++
    return end
}

/** The first index in [from, until) that is not whitespace, or [until]. */
private fun String.skipWhitespaceForward(
    from: Int,
    until: Int,
): Int {
    var index = from
    while (index < until && this[index].isWhitespace()) index++
    return index
}

/** The end (exclusive) of the text in [downTo, from) once trailing whitespace is dropped. */
private fun String.skipWhitespaceBackward(
    from: Int,
    downTo: Int,
): Int {
    var index = from
    while (index > downTo && this[index - 1].isWhitespace()) index--
    return index
}

/**
 * Where the line after the break at [lineEnd] starts: past `\r\n` as one break, past any other
 * single break, and past the end (so the loop stops) when [lineEnd] is the text's end.
 */
private fun String.nextLineStart(lineEnd: Int): Int =
    when {
        lineEnd == length -> length + 1
        startsWith(CRLF, lineEnd) -> lineEnd + CRLF.length
        else -> lineEnd + 1
    }

/** Moves a non-empty paragraph into [paragraphs] and clears the builder for the next one. */
private fun StringBuilder.flushInto(paragraphs: MutableList<String>) {
    if (isEmpty()) return
    paragraphs += toString()
    setLength(0)
}

private const val LF = '\n'
private const val CR = '\r'
private const val CRLF = "\r\n"
