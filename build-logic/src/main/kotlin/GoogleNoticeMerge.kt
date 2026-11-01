import java.security.MessageDigest

/**
 * One AAR's two root notice entries, as bytes: [json] is `third_party_licenses.json`, [txt] is
 * `third_party_licenses.txt`, each `null` when the AAR has no such entry. [artifactId] is what
 * the index's `<sources>` column shows and what every failure names.
 */
class AarNotices(
    val artifactId: String,
    val json: ByteArray?,
    val txt: ByteArray?,
)

/**
 * One line of `licenses/google/index.tsv` (tech design §13.1 LN-3): a distinct (name, text) pair,
 * the AARs that carry it, the text's [stem] and the pair's [key].
 */
data class NoticeIndexLine(
    val key: String,
    val name: String,
    val stem: String,
    val sources: List<String>,
)

/**
 * The merge's whole output: the sorted index [lines] and one text per stem in [texts], each the
 * exact bytes of its source slice. The task writes [indexTsv] to `licenses/google/index.tsv` and
 * each text to `licenses/google/<stem>.txt`.
 */
class MergedNotices(
    val lines: List<NoticeIndexLine>,
    val texts: Map<String, ByteArray>,
) {
    /**
     * LN-3's file: UTF-8 without a byte-order mark, one `key\tname\tstem\tsources` line per pair,
     * `sources` comma-joined with no spaces, every line ending in one `\n` and no blank line after
     * the last. Empty when there are no lines.
     */
    fun indexTsv(): ByteArray =
        lines
            .joinToString(separator = "") { line ->
                "${line.key}\t${line.name}\t${line.stem}\t${line.sources.joinToString(SOURCE_SEPARATOR)}\n"
            }.toByteArray(Charsets.UTF_8)

    private companion object {
        const val SOURCE_SEPARATOR = ","
    }
}

/** An AAR's notices cannot be merged; the build fails naming [artifactId] and the [reason]. */
class GoogleNoticeMergeException(
    val artifactId: String,
    reason: String,
) : RuntimeException("$artifactId: $reason")

/**
 * Merges the open-source notices every Google AAR ships at its root — and AGP never packages —
 * into one index and one file per distinct text (tech design §13.1 LN-2, LN-3).
 *
 * - **Per AAR.** No `third_party_licenses.json` → the AAR contributes nothing, whatever else it
 *   holds. A JSON without its `.txt`, a JSON that is not the flat shape [GoogleNoticeJson] reads,
 *   a slice outside the text, and an empty name or one holding a tab or line break (it would break
 *   the TSV) each fail, naming the AAR.
 * - **Bytes, never decoded.** A slice is `txt[start, start + length)` copied as bytes: its stem and
 *   the file written for it are those bytes, so a text in any encoding round-trips unchanged.
 * - **Stem and key.** The stem is the first 16 lowercase hex digits of SHA-256 over the slice; the
 *   key the first 16 of SHA-256 over the name's UTF-8 bytes, a 0x00 byte, and the stem.
 * - **Merge.** One line per distinct (name, stem): the same name with two texts is two lines, and
 *   `sources` is the sorted, de-duplicated artifactIds that carry the pair.
 * - **Order.** By the name folded with Kotlin's locale-independent `lowercase()`, then the stem,
 *   then the exact name — so the build machine's locale and the AARs' order change no byte.
 *
 * Gradle-free on purpose, like `AdIdResolution`: `GoogleNoticeMergeTest` proves every rule above,
 * and `GenerateGoogleNoticesTask` only reads the AARs and writes what this returns.
 */
object GoogleNoticeMerge {
    fun merge(aars: List<AarNotices>): MergedNotices {
        val sourcesByPair = HashMap<Pair<String, String>, MutableSet<String>>()
        val texts = HashMap<String, ByteArray>()
        aars.forEach { aar ->
            slicesOf(aar).forEach { (name, text) ->
                val stem = hexPrefix(text)
                texts[stem] = text
                sourcesByPair.getOrPut(name to stem) { HashSet() } += aar.artifactId
            }
        }
        val lines =
            sourcesByPair
                .map { (pair, sources) ->
                    val (name, stem) = pair
                    NoticeIndexLine(
                        key = hexPrefix(name.toByteArray(Charsets.UTF_8) + KEY_SEPARATOR + stem.toByteArray(Charsets.UTF_8)),
                        name = name,
                        stem = stem,
                        sources = sources.sorted(),
                    )
                }.sortedWith(compareBy<NoticeIndexLine>({ it.name.lowercase() }, { it.stem }, { it.name }))
        return MergedNotices(lines, texts)
    }

    /** Every (name, slice bytes) the AAR's notices hold, validated; empty for an AAR without the JSON. */
    private fun slicesOf(aar: AarNotices): List<Pair<String, ByteArray>> {
        val json = aar.json ?: return emptyList()
        val txt = aar.txt ?: fail(aar, "has $JSON_ENTRY but no $TXT_ENTRY")
        val slices =
            try {
                GoogleNoticeJson.parse(json)
            } catch (e: GoogleNoticeJsonException) {
                fail(aar, "$JSON_ENTRY does not parse: ${e.message}")
            }
        return slices.map { slice ->
            val name = slice.name
            if (name.isEmpty()) fail(aar, "$JSON_ENTRY has an empty name")
            if (name.any { it in FORBIDDEN_NAME_CHARACTERS }) {
                fail(aar, "the name \"${printable(name)}\" contains a tab or a line break")
            }
            // Written as `length > size - start` rather than `start + length > size`, so two huge
            // values cannot wrap round to a negative sum and pass.
            val inside =
                slice.start >= 0 &&
                    slice.length >= 0 &&
                    slice.start <= txt.size &&
                    slice.length <= txt.size - slice.start
            if (!inside) {
                fail(
                    aar,
                    "the slice of \"$name\" (start ${slice.start}, length ${slice.length}) " +
                        "runs past the end of $TXT_ENTRY (${txt.size} bytes)",
                )
            }
            val start = slice.start.toInt()
            name to txt.copyOfRange(start, start + slice.length.toInt())
        }
    }

    /** The first 16 lowercase hex digits of the SHA-256 of [bytes] (LN-3's stem and key). */
    private fun hexPrefix(bytes: ByteArray): String =
        MessageDigest
            .getInstance(SHA_256)
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it) }
            .take(PREFIX_HEX_DIGITS)

    private fun printable(name: String): String =
        name.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun fail(
        aar: AarNotices,
        reason: String,
    ): Nothing = throw GoogleNoticeMergeException(aar.artifactId, reason)

    private const val JSON_ENTRY = "third_party_licenses.json"
    private const val TXT_ENTRY = "third_party_licenses.txt"
    private const val SHA_256 = "SHA-256"
    private const val PREFIX_HEX_DIGITS = 16
    private const val FORBIDDEN_NAME_CHARACTERS = "\t\n\r"
    private val KEY_SEPARATOR = byteArrayOf(0)
}
