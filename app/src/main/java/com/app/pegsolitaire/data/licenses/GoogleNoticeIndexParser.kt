package com.app.pegsolitaire.data.licenses

/**
 * Parses the build's `licenses/google/index.tsv` (tech design §13.1 LN-3, LN-4), whole or not at
 * all: any malformed line makes the result `null`, the screens' error state, because a list that
 * silently dropped a line would hide a notice the app owes the player.
 *
 * Each line is exactly four tab-separated fields `key name stem sources`, none empty; `key` and
 * `stem` are sixteen lowercase hex digits; `sources` is comma-separated with no empty id; keys are
 * unique. The file ends with one `\n` (optional here) and holds no `\r` and no blank line — the
 * build writes neither. An empty file is an empty list, not `null`: whether no notices is an error
 * is the screen's decision (`GoogleNoticesViewModel` makes it one).
 */
fun parseGoogleNoticeIndex(raw: String): List<GoogleNotice>? {
    if (raw.isEmpty()) return emptyList()
    val notices = raw.removeSuffix(LINE_END).split(LINE_END).map(::parseIndexLine)
    val wellFormed =
        CARRIAGE_RETURN !in raw &&
            notices.none { it == null } &&
            notices.distinctBy { it?.key }.size == notices.size
    return if (wellFormed) notices.filterNotNull() else null
}

/** One `key\tname\tstem\tsources` line, or `null` when it breaks any rule of the format. */
private fun parseIndexLine(line: String): GoogleNotice? {
    val fields = line.split(FIELD_SEPARATOR)
    if (fields.size != FIELD_COUNT || fields.any { it.isEmpty() }) return null
    val sources = fields[SOURCES_FIELD].split(SOURCE_SEPARATOR)
    val valid =
        GoogleNoticeAssets.isHexId(fields[KEY_FIELD]) &&
            GoogleNoticeAssets.isHexId(fields[STEM_FIELD]) &&
            sources.none { it.isEmpty() }
    return if (valid) {
        GoogleNotice(key = fields[KEY_FIELD], name = fields[NAME_FIELD], sources = sources, stem = fields[STEM_FIELD])
    } else {
        null
    }
}

private const val LINE_END = "\n"
private const val CARRIAGE_RETURN = '\r'
private const val FIELD_SEPARATOR = "\t"
private const val SOURCE_SEPARATOR = ","
private const val FIELD_COUNT = 4
private const val KEY_FIELD = 0
private const val NAME_FIELD = 1
private const val STEM_FIELD = 2
private const val SOURCES_FIELD = 3
