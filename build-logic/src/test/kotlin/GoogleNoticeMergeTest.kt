import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

/**
 * LN-7 item 1 (tech design §13.1): the Gradle-free merge of every Google AAR's
 * `third_party_licenses.json`/`.txt` pair into one sorted, deduplicated index and one file
 * per distinct text.
 *
 * The fixtures are built in the layout Google ships — `\n\n<name>:\n\n<text>\n\n` runs, each
 * JSON entry pointing at its text's byte slice — so the offsets are real byte offsets into a
 * real-shaped file. Every expected stem and key below is a literal derived by hand with
 * `shasum -a 256`, never by the code under test.
 */
class GoogleNoticeMergeTest {
    @Test
    fun theStemAndTheKeyAreTheDocumentedSha256Prefixes() {
        val merged = GoogleNoticeMerge.merge(listOf(aar(ADS, ALPHA to TEXT_ALPHA)))

        val line = merged.lines.single()
        assertThat(line.stem).isEqualTo(STEM_ALPHA)
        assertThat(line.key).isEqualTo(KEY_ALPHA_ALPHA)
        assertThat(line.name).isEqualTo(ALPHA)
        assertThat(line.sources).containsExactly(ADS)
    }

    @Test
    fun twoAarsSharingAPairGiveOneLineWithBothSources() {
        val merged =
            GoogleNoticeMerge.merge(
                listOf(aar(UMP, ALPHA to TEXT_ALPHA), aar(ADS, ALPHA to TEXT_ALPHA)),
            )

        val line = merged.lines.single()
        assertThat(line.sources).containsExactly(ADS, UMP).inOrder()
        assertThat(merged.texts.keys).containsExactly(STEM_ALPHA)
    }

    @Test
    fun oneNameWithTwoTextsGivesTwoLines() {
        val merged =
            GoogleNoticeMerge.merge(
                listOf(aar(ADS, ALPHA to TEXT_ALPHA), aar(UMP, ALPHA to TEXT_BETA)),
            )

        assertThat(merged.lines.map { it.key }).containsExactly(KEY_ALPHA_ALPHA, KEY_ALPHA_BETA)
        assertThat(merged.lines.map { it.sources }).containsExactly(listOf(ADS), listOf(UMP))
        assertThat(merged.texts.keys).containsExactly(STEM_ALPHA, STEM_BETA)
    }

    @Test
    fun permutedInputsGiveByteIdenticalOutput() {
        val first = aar(ADS, ALPHA to TEXT_ALPHA, BETA to TEXT_BETA)
        val second = aar(UMP, BETA to TEXT_BETA, GAMMA to TEXT_ALPHA)
        val third = aar(GAMES, ALPHA to TEXT_BETA)

        val forward = GoogleNoticeMerge.merge(listOf(first, second, third))
        val backward = GoogleNoticeMerge.merge(listOf(third, second, first))

        assertThat(backward.indexTsv()).isEqualTo(forward.indexTsv())
        assertThat(backward.texts.keys).isEqualTo(forward.texts.keys)
    }

    @Test
    fun eachStemFileEqualsItsSourceSliceByteForByte() {
        // Not valid UTF-8 on purpose: the slice is copied as bytes and never decoded, so a text
        // in any encoding round-trips unchanged.
        val undecodable = byteArrayOf(0x41, 0xFF.toByte(), 0xFE.toByte(), 0x0A)
        val merged =
            GoogleNoticeMerge.merge(
                listOf(aarOfBytes(ADS, ALPHA to TEXT_ALPHA.toByteArray(Charsets.UTF_8), BETA to undecodable)),
            )

        assertThat(merged.texts[STEM_ALPHA]).isEqualTo(TEXT_ALPHA.toByteArray(Charsets.UTF_8))
        assertThat(merged.texts[STEM_UNDECODABLE]).isEqualTo(undecodable)
    }

    @Test
    fun aSliceRunningPastTheTextFailsNamingTheAar() {
        val txt = "\n\nAlpha:\n\nshort".toByteArray(Charsets.UTF_8)
        val json = """{"Alpha": {"start": 10, "length": 6}}""".toByteArray(Charsets.UTF_8)

        val failure = mergeFailure(AarNotices(ADS, json, txt))

        assertThat(failure.artifactId).isEqualTo(ADS)
        assertThat(failure).hasMessageThat().contains(ADS)
    }

    @Test
    fun aNegativeOffsetOrLengthFailsNamingTheAar() {
        val txt = "\n\nAlpha:\n\ntext".toByteArray(Charsets.UTF_8)

        val negativeStart = """{"Alpha": {"start": -1, "length": 2}}""".toByteArray(Charsets.UTF_8)
        val negativeLength = """{"Alpha": {"start": 10, "length": -2}}""".toByteArray(Charsets.UTF_8)

        assertThat(mergeFailure(AarNotices(ADS, negativeStart, txt)).artifactId).isEqualTo(ADS)
        assertThat(mergeFailure(AarNotices(UMP, negativeLength, txt)).artifactId).isEqualTo(UMP)
    }

    /** A start past the end of the text fails before any length arithmetic is done. */
    @Test
    fun aSliceStartingPastTheTextFailsNamingTheAar() {
        val txt = "\n\nAlpha:\n\ntext".toByteArray(Charsets.UTF_8)
        val json = """{"Alpha": {"start": ${Long.MAX_VALUE}, "length": 1}}""".toByteArray(Charsets.UTF_8)

        assertThat(mergeFailure(AarNotices(ADS, json, txt)).artifactId).isEqualTo(ADS)
    }

    /**
     * A small start and a huge length: `start + length` wraps round to a negative `Long`, so a
     * naive `start + length <= size` would pass it and `copyOfRange` would throw without naming the
     * AAR. The guard `length <= size - start` must reject it (review loop 1, F2).
     */
    @Test
    fun aSliceWhoseEndOverflowsFailsNamingTheAar() {
        val txt = "\n\nAlpha:\n\ntext".toByteArray(Charsets.UTF_8)
        val json = """{"Alpha": {"start": 1, "length": ${Long.MAX_VALUE}}}""".toByteArray(Charsets.UTF_8)

        assertThat(mergeFailure(AarNotices(ADS, json, txt)).artifactId).isEqualTo(ADS)
    }

    @Test
    fun unparseableJsonFailsNamingTheAar() {
        val failure =
            mergeFailure(
                AarNotices(GAMES, "{not json".toByteArray(Charsets.UTF_8), "text".toByteArray(Charsets.UTF_8)),
            )

        assertThat(failure.artifactId).isEqualTo(GAMES)
        assertThat(failure).hasMessageThat().contains(GAMES)
    }

    @Test
    fun aNameWithATabOrLineBreakFailsNamingTheAar() {
        listOf("Tab\there", "Line\nbreak", "Carriage\rreturn").forEach { name ->
            val failure = mergeFailure(aar(UMP, name to TEXT_ALPHA))

            assertThat(failure.artifactId).isEqualTo(UMP)
        }
    }

    @Test
    fun anEmptyNameFailsNamingTheAar() {
        assertThat(mergeFailure(aar(ADS, "" to TEXT_ALPHA)).artifactId).isEqualTo(ADS)
    }

    @Test
    fun jsonWithoutItsTextFailsNamingTheAar() {
        val json = """{"Alpha": {"start": 0, "length": 1}}""".toByteArray(Charsets.UTF_8)

        val failure = mergeFailure(AarNotices(GAMES, json, null))

        assertThat(failure.artifactId).isEqualTo(GAMES)
    }

    @Test
    fun anAarWithoutTheJsonContributesNothing() {
        val merged =
            GoogleNoticeMerge.merge(
                listOf(
                    AarNotices("androidx-core", json = null, txt = "stray text".toByteArray(Charsets.UTF_8)),
                    AarNotices("androidx-activity", json = null, txt = null),
                    aar(ADS, ALPHA to TEXT_ALPHA),
                ),
            )

        assertThat(merged.lines.single().sources).containsExactly(ADS)
        assertThat(merged.texts.keys).containsExactly(STEM_ALPHA)
    }

    @Test
    fun noAarsGiveAnEmptyIndex() {
        val merged = GoogleNoticeMerge.merge(emptyList())

        assertThat(merged.lines).isEmpty()
        assertThat(merged.indexTsv()).isEmpty()
    }

    /**
     * LN-3's order: case folds first ("apple" before "Banana"), then the stem breaks a tie on
     * the folded name, then the exact name breaks a tie between two spellings of one name that
     * share a text, so the output is stable whatever order the AARs arrive in.
     */
    @Test
    fun linesSortByLowercasedNameThenStemThenName() {
        val merged =
            GoogleNoticeMerge.merge(
                listOf(
                    aar(ADS, "Banana" to TEXT_ALPHA, "apple" to TEXT_ALPHA, "Alpha" to TEXT_BETA),
                    aar(UMP, "alpha" to TEXT_ALPHA, "Alpha" to TEXT_ALPHA, "ALPHA" to TEXT_ALPHA),
                ),
            )

        // STEM_ALPHA (677b…) sorts before STEM_BETA (d7e8…).
        assertThat(merged.lines.map { it.name to it.stem })
            .containsExactly(
                "ALPHA" to STEM_ALPHA,
                "Alpha" to STEM_ALPHA,
                "alpha" to STEM_ALPHA,
                "Alpha" to STEM_BETA,
                "apple" to STEM_ALPHA,
                "Banana" to STEM_ALPHA,
            ).inOrder()
    }

    /**
     * The build machine's locale must not change the bytes. Under a Turkish default locale a
     * locale-sensitive lowercase turns "Ia" into "ıa" (dotless ı, U+0131), which sorts after
     * "ib"; the locale-independent fold keeps "ia" before "ib".
     */
    @Test
    fun theOrderDoesNotDependOnTheDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            val merged = GoogleNoticeMerge.merge(listOf(aar(ADS, "ib" to TEXT_ALPHA, "Ia" to TEXT_ALPHA)))

            assertThat(merged.lines.map { it.name }).containsExactly("Ia", "ib").inOrder()
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun theIndexEndsEachLineWithOneNewlineAndHasNoTrailingBlankLine() {
        val merged =
            GoogleNoticeMerge.merge(
                listOf(aar(ADS, ALPHA to TEXT_ALPHA), aar(UMP, ALPHA to TEXT_ALPHA), aar(GAMES, ALPHA to TEXT_BETA)),
            )

        val expected =
            "$KEY_ALPHA_ALPHA\t$ALPHA\t$STEM_ALPHA\t$ADS,$UMP\n" +
                "$KEY_ALPHA_BETA\t$ALPHA\t$STEM_BETA\t$GAMES\n"
        assertThat(merged.indexTsv()).isEqualTo(expected.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun theIndexIsUtf8WithoutAByteOrderMark() {
        val name = "Café"
        val merged = GoogleNoticeMerge.merge(listOf(aar(ADS, name to TEXT_ALPHA)))

        val bytes = merged.indexTsv()
        assertThat(bytes.take(BOM.size)).isNotEqualTo(BOM.toList())
        assertThat(String(bytes, Charsets.UTF_8).split('\t')[1]).isEqualTo(name)
    }

    private fun mergeFailure(vararg aars: AarNotices): GoogleNoticeMergeException =
        assertThrows(GoogleNoticeMergeException::class.java) { GoogleNoticeMerge.merge(aars.toList()) }

    /** An AAR whose notices file holds [entries] in Google's layout, the JSON pointing at each text. */
    private fun aar(
        artifactId: String,
        vararg entries: Pair<String, String>,
    ): AarNotices = aarOfBytes(artifactId, *entries.map { (name, text) -> name to text.toByteArray(Charsets.UTF_8) }.toTypedArray())

    private fun aarOfBytes(
        artifactId: String,
        vararg entries: Pair<String, ByteArray>,
    ): AarNotices {
        var txt = ByteArray(0)
        val fields = mutableListOf<String>()
        entries.forEach { (name, text) ->
            txt += "\n\n$name:\n\n".toByteArray(Charsets.UTF_8)
            fields += "${jsonString(name)}: {\"start\": ${txt.size}, \"length\": ${text.size}}"
            txt += text
        }
        val json = fields.joinToString(prefix = "{", postfix = "}", separator = ", ")
        return AarNotices(artifactId, json.toByteArray(Charsets.UTF_8), txt)
    }

    /** A JSON string literal for [value], escaping what the fixtures need (quote, backslash, controls). */
    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { char ->
                when {
                    char == '"' -> append("\\\"")
                    char == '\\' -> append("\\\\")
                    char < ' ' -> append("\\u%04x".format(char.code))
                    else -> append(char)
                }
            }
            append('"')
        }

    private companion object {
        const val ADS = "ads-mobile-sdk"
        const val UMP = "user-messaging-platform"
        const val GAMES = "play-services-games-v2"

        const val ALPHA = "Alpha"
        const val BETA = "Beta"
        const val GAMMA = "Gamma"

        const val TEXT_ALPHA = "Alpha licence text.\n"
        const val TEXT_BETA = "Beta licence text.\n"

        /** `printf 'Alpha licence text.\n' | shasum -a 256 | cut -c1-16` */
        const val STEM_ALPHA = "677b56244522f04a"

        /** `printf 'Beta licence text.\n' | shasum -a 256 | cut -c1-16` */
        const val STEM_BETA = "d7e8056129ffead1"

        /** `printf 'A\xff\xfe\n' | shasum -a 256 | cut -c1-16` */
        const val STEM_UNDECODABLE = "3af5bc88f5256847"

        /** `printf 'Alpha\0677b56244522f04a' | shasum -a 256 | cut -c1-16` */
        const val KEY_ALPHA_ALPHA = "a4625c230ea75f8a"

        /** `printf 'Alpha\0d7e8056129ffead1' | shasum -a 256 | cut -c1-16` */
        const val KEY_ALPHA_BETA = "2cd9d0c59a742f9c"

        val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
