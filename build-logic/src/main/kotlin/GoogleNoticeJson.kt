import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** One entry of a `third_party_licenses.json`: a component [name] and its byte slice of the `.txt`. */
data class NoticeSlice(
    val name: String,
    val start: Long,
    val length: Long,
)

/** The JSON is not the flat shape [GoogleNoticeJson] reads; [message] says where and why. */
class GoogleNoticeJsonException(
    message: String,
) : RuntimeException(message)

/**
 * Reads the one shape a Google AAR's root `third_party_licenses.json` has (tech design §13.1
 * LN-2): a single object mapping each component name to `{"start": n, "length": n}`, the byte
 * slice of the sibling `third_party_licenses.txt` that holds that component's notice.
 *
 * **Strict on purpose.** Anything outside that shape — a nested object, a non-integer offset, a
 * duplicate name or field, an unknown field, trailing content, bytes that are not UTF-8 — is a
 * [GoogleNoticeJsonException], which `GoogleNoticeMerge` turns into a build failure naming the
 * AAR. A future SDK that changes the format must stop the build and be looked at, never be read
 * approximately into the notices the app ships.
 *
 * Gradle-free and dependency-free (LN-2: no new coordinate), so `GoogleNoticeJsonTest` proves it
 * the way `AdIdResolutionTest` proves its sibling. Negative integers parse: rejecting them is the
 * merge's job, because only the merge can name the AAR.
 */
object GoogleNoticeJson {
    fun parse(json: ByteArray): List<NoticeSlice> = Reader(decodeStrictUtf8(json)).document()

    private fun decodeStrictUtf8(bytes: ByteArray): String =
        try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            throw GoogleNoticeJsonException("not valid UTF-8 (${e.message})")
        }

    /** A single-pass recursive-descent reader over the decoded text; [pos] is the next character. */
    private class Reader(
        private val text: String,
    ) {
        private var pos = 0

        fun document(): List<NoticeSlice> {
            skipWhitespace()
            val slices = outerObject()
            skipWhitespace()
            if (pos != text.length) fail("unexpected content after the object")
            return slices
        }

        private fun outerObject(): List<NoticeSlice> {
            expect('{')
            skipWhitespace()
            val slices = mutableListOf<NoticeSlice>()
            val seen = HashSet<String>()
            if (peek() == '}') {
                pos++
                return slices
            }
            while (true) {
                skipWhitespace()
                val name = string()
                if (!seen.add(name)) fail("duplicate name \"$name\"")
                skipWhitespace()
                expect(':')
                skipWhitespace()
                slices += slice(name)
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    '}' -> return slices
                    else -> fail("expected ',' or '}' after the entry for \"$name\"", pos - 1)
                }
            }
        }

        private fun slice(name: String): NoticeSlice {
            expect('{')
            val fields = HashMap<String, Long>()
            while (true) {
                skipWhitespace()
                val field = string()
                if (field != START && field != LENGTH) fail("unknown field \"$field\" in \"$name\"")
                if (field in fields) fail("duplicate field \"$field\" in \"$name\"")
                skipWhitespace()
                expect(':')
                skipWhitespace()
                fields[field] = integer()
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    '}' -> break
                    else -> fail("expected ',' or '}' in \"$name\"", pos - 1)
                }
            }
            val start = fields[START] ?: fail("\"$name\" has no \"$START\"")
            val length = fields[LENGTH] ?: fail("\"$name\" has no \"$LENGTH\"")
            return NoticeSlice(name, start, length)
        }

        /** A JSON integer: `-?(0|[1-9][0-9]*)`, within `Long`; a fraction or exponent is rejected. */
        private fun integer(): Long {
            val begin = pos
            if (peek() == '-') pos++
            val firstDigit = pos
            while (pos < text.length && text[pos] in '0'..'9') pos++
            if (pos == firstDigit) fail("expected an integer", begin)
            if (text[firstDigit] == '0' && pos - firstDigit > 1) fail("an integer has a leading zero", begin)
            if (peek() == '.' || peek() == 'e' || peek() == 'E') fail("an offset must be an integer", begin)
            return text.substring(begin, pos).toLongOrNull() ?: fail("an integer is out of range", begin)
        }

        private fun string(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                if (pos >= text.length) fail("unterminated string")
                when (val char = text[pos++]) {
                    '"' -> break
                    '\\' -> out.append(escape())
                    else -> {
                        if (char < ' ') fail("a raw control character in a string", pos - 1)
                        out.append(char)
                    }
                }
            }
            requirePairedSurrogates(out)
            return out.toString()
        }

        private fun escape(): Char {
            if (pos >= text.length) fail("unterminated escape")
            return when (val char = text[pos++]) {
                '"' -> '"'
                '\\' -> '\\'
                '/' -> '/'
                'b' -> '\b'
                'f' -> FORM_FEED
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> unicodeEscape()
                else -> fail("unknown escape '\\$char'", pos - 1)
            }
        }

        private fun unicodeEscape(): Char {
            if (pos + HEX_DIGITS > text.length) fail("truncated \\u escape")
            val hex = text.substring(pos, pos + HEX_DIGITS)
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail("bad \\u escape \"$hex\"")
            pos += HEX_DIGITS
            return hex.toInt(HEX_RADIX).toChar()
        }

        /** A `\uD83D` with no low half (or the reverse) would not survive UTF-8 encoding intact. */
        private fun requirePairedSurrogates(value: CharSequence) {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                when {
                    char.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate() -> index += 2
                    char.isSurrogate() -> fail("an unpaired surrogate in a string")
                    else -> index++
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos] in WHITESPACE) pos++
        }

        private fun peek(): Char? = text.getOrNull(pos)

        private fun next(): Char? = text.getOrNull(pos++)

        private fun expect(char: Char) {
            if (peek() != char) fail("expected '$char'")
            pos++
        }

        private fun fail(
            reason: String,
            at: Int = pos,
        ): Nothing = throw GoogleNoticeJsonException("$reason at character $at")
    }

    private const val START = "start"
    private const val LENGTH = "length"
    private const val HEX_DIGITS = 4
    private const val HEX_RADIX = 16
    private const val FORM_FEED = '\u000C'

    /** JSON's four whitespace characters (RFC 8259 §2). */
    private const val WHITESPACE = " \t\n\r"
}
