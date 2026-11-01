import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The flat shape of a Google AAR's `third_party_licenses.json` (tech design §13.1 LN-2):
 * `{"<name>": {"start": n, "length": n}, …}` — strings with the standard JSON escapes,
 * integers and whitespace, and nothing else.
 *
 * Every rejection here is a build failure further up (`GoogleNoticeMerge` names the AAR), so
 * each case below is a shape a future SDK could ship that must stop the build rather than be
 * read approximately. The parser is the primary reader because it is pure and testable
 * (LN-2's permitted alternative, Gradle's bundled `JsonSlurper`, is not used).
 */
class GoogleNoticeJsonTest {
    @Test
    fun parsesTheFlatShapeWhateverTheFieldOrder() {
        val slices = parse("""{"Alpha": {"length": 12, "start": 3}, "Beta": {"start": 15, "length": 0}}""")

        assertThat(slices)
            .containsExactly(NoticeSlice("Alpha", 3, 12), NoticeSlice("Beta", 15, 0))
            .inOrder()
    }

    @Test
    fun anEmptyObjectHasNoEntries() {
        assertThat(parse("{}")).isEmpty()
    }

    @Test
    fun whitespaceIsAllowedWhereverJsonAllowsIt() {
        val slices = parse(" \t\r\n{ \"A\" :\n{ \"start\" : 1 ,\r\n \"length\" : 2 } \t} \n")

        assertThat(slices).containsExactly(NoticeSlice("A", 1, 2))
    }

    @Test
    fun everyStandardEscapeDecodesToItsCharacter() {
        val slices = parse("""{"q\"b\\s\/b\bf\fn\nr\rt\tué": {"start": 0, "length": 0}}""")

        assertThat(slices.single().name).isEqualTo("q\"b\\s/b\bf\u000Cn\nr\rt\tué")
    }

    @Test
    fun aSurrogatePairEscapeDecodesToOneCodePoint() {
        val slices = parse("""{"😀": {"start": 0, "length": 0}}""")

        assertThat(slices.single().name).isEqualTo("😀")
    }

    @Test
    fun aLoneSurrogateEscapeIsRejected() {
        assertRejected("""{"\ud83d": {"start": 0, "length": 0}}""")
    }

    @Test
    fun aNegativeIntegerParsesSoTheMergeCanRejectItByName() {
        assertThat(parse("""{"A": {"start": -1, "length": 2}}""").single().start).isEqualTo(-1L)
    }

    @Test
    fun aNestedObjectIsRejected() {
        assertRejected("""{"A": {"start": {"x": 1}, "length": 2}}""")
        assertRejected("""{"A": {"start": 1, "length": 2, "more": {}}}""")
        assertRejected("""{"A": {"B": {"start": 1, "length": 2}}}""")
    }

    @Test
    fun aNonIntegerOffsetIsRejected() {
        assertRejected("""{"A": {"start": 1.5, "length": 2}}""")
        assertRejected("""{"A": {"start": 1e3, "length": 2}}""")
        assertRejected("""{"A": {"start": "1", "length": 2}}""")
        assertRejected("""{"A": {"start": true, "length": 2}}""")
        assertRejected("""{"A": {"start": null, "length": 2}}""")
        assertRejected("""{"A": {"start": 01, "length": 2}}""")
        assertRejected("""{"A": {"start": 99999999999999999999, "length": 2}}""")
    }

    @Test
    fun aDuplicateNameIsRejected() {
        assertRejected("""{"A": {"start": 0, "length": 1}, "A": {"start": 1, "length": 1}}""")
    }

    @Test
    fun aDuplicateOrMissingOrUnknownFieldIsRejected() {
        assertRejected("""{"A": {"start": 0, "start": 1, "length": 1}}""")
        assertRejected("""{"A": {"start": 0}}""")
        assertRejected("""{"A": {"start": 0, "length": 1, "url": 2}}""")
        assertRejected("""{"A": {}}""")
    }

    @Test
    fun anythingButOneFlatObjectIsRejected() {
        assertRejected("")
        assertRejected("[]")
        assertRejected("""{"A": {"start": 0, "length": 1}} {}""")
        assertRejected("""{"A": {"start": 0, "length": 1},}""")
        assertRejected("""{"A": {"start": 0, "length": 1}""")
        assertRejected("""{A: {"start": 0, "length": 1}}""")
        assertRejected("""{"A" {"start": 0, "length": 1}}""")
    }

    @Test
    fun aRawControlCharacterOrAnUnknownEscapeInANameIsRejected() {
        assertRejected("{\"A\tB\": {\"start\": 0, \"length\": 1}}")
        assertRejected("""{"A\x": {"start": 0, "length": 1}}""")
        assertRejected("""{"A\u00g1": {"start": 0, "length": 1}}""")
        assertRejected("""{"A""")
    }

    /**
     * A truncated two-byte sequence (0xC3 then a quote) inside an otherwise valid document, so
     * only the strict decode can reject it: a lenient decode would read the name as "A�"
     * and the document would parse.
     */
    @Test
    fun bytesThatAreNotUtf8AreRejected() {
        val prefix = "{\"A".toByteArray(Charsets.UTF_8)
        val suffix = "\": {\"start\": 0, \"length\": 1}}".toByteArray(Charsets.UTF_8)
        val invalid = prefix + byteArrayOf(TRUNCATED_LEAD_BYTE) + suffix

        assertThrows(GoogleNoticeJsonException::class.java) { GoogleNoticeJson.parse(invalid) }
    }

    private fun parse(json: String): List<NoticeSlice> = GoogleNoticeJson.parse(json.toByteArray(Charsets.UTF_8))

    private fun assertRejected(json: String) {
        assertThrows(GoogleNoticeJsonException::class.java) { parse(json) }
    }

    private companion object {
        /** The lead byte of a two-byte UTF-8 sequence, here left without its continuation byte. */
        const val TRUNCATED_LEAD_BYTE: Byte = 0xC3.toByte()
    }
}
