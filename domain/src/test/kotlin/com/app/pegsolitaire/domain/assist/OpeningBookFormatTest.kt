package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * The two halves of [OpeningBookFormat]'s contract: the encoder and the decoder are
 * exact inverses, and the decoder is **incapable of throwing** on any input string.
 *
 * The second half is the load-bearing one. A book resource ships inside the APK and is
 * read back at hint time; RESEARCH §Security requires that a truncated or corrupt
 * resource make the whole board's book unavailable — falling through to the `Solver` —
 * rather than crashing a hint or, worse, yielding a partial book. So every malformed
 * case here asserts a `null` RETURN, never a particular exception type: asserting an
 * exception would lock in exactly the behaviour that must not exist.
 *
 * The malformed corpus is generated from [CORPUS_SEED], and every mutation it applies
 * is *structurally* breaking (an extra separator, a missing separator, a non-hex digit,
 * a non-numeric coordinate), so asserting `null` for all of them is sound rather than
 * probabilistic. The separate binary-noise case cannot make that guarantee — a random
 * string could in principle be a legal entry — so it asserts the total contract
 * instead: every input is either rejected or decodes to something that re-encodes to
 * itself byte for byte.
 */
class OpeningBookFormatTest {
    @Test
    fun everyRepresentativeMaskAndMoveRoundTripsThroughTheDecoder() {
        representativeEntries().forEach { (pegs, move) ->
            val encoded = OpeningBookFormat.encodeEntry(pegs, move)
            assertWithMessage("entry '%s' must decode back to (%s, %s)", encoded, pegs, move)
                .that(OpeningBookFormat.decodeEntry(encoded))
                .isEqualTo(pegs to move)
        }
    }

    /**
     * The authoritative "no winning move" marker is a distinct decoded value from a
     * malformed line — one is `(mask, null)`, the other is `null` — because plan 08-09's
     * loader must treat a book hit recording "no winning move" as truth while treating a
     * malformed line as "no book at all".
     */
    @Test
    fun theAuthoritativeNoWinningMoveMarkerRoundTrips() {
        representativeMasks().forEach { pegs ->
            val encoded = OpeningBookFormat.encodeEntry(pegs, move = null)
            assertWithMessage("the no-winning-move marker '%s' must round-trip", encoded)
                .that(OpeningBookFormat.decodeEntry(encoded))
                .isEqualTo(pegs to null)
        }
    }

    /**
     * Wiegleb-45 occupies bits 0..44, so its full mask sets bit 44 — the bit position
     * plan 08-02 flagged (Pitfall 4) as the one a narrower encoding would silently drop.
     * The two top-bit masks prove the unsigned hex form really is unsigned.
     */
    @Test
    fun highBitMasksSurviveTheUnsignedHexEncoding() {
        val highBitMasks =
            listOf(
                fullMaskOf(Boards.Wiegleb45),
                1L shl WIEGLEB_45_TOP_BIT,
                Long.MIN_VALUE,
                -1L,
            )
        highBitMasks.forEach { pegs ->
            val encoded = OpeningBookFormat.encodeEntry(pegs, SYNTHETIC_MOVE)
            assertWithMessage("high-bit mask entry '%s' must round-trip", encoded)
                .that(OpeningBookFormat.decodeEntry(encoded))
                .isEqualTo(pegs to SYNTHETIC_MOVE)
        }
    }

    @Test
    fun structurallyMalformedLinesDecodeToNull() {
        namedMalformedLines().forEach { (reason, line) ->
            assertWithMessage("a line that is %s must decode to null, was '%s'", reason, line)
                .that(OpeningBookFormat.decodeEntry(line))
                .isNull()
        }
    }

    @Test
    fun decodingNeverThrowsOverASeededMalformedCorpus() {
        val corpus = malformedCorpus()
        assertWithMessage("the malformed corpus must be at least %s cases", MINIMUM_CORPUS_SIZE)
            .that(corpus.size)
            .isAtLeast(MINIMUM_CORPUS_SIZE)
        corpus.forEach { line ->
            assertWithMessage("malformed corpus entry '%s' must decode to null", line)
                .that(OpeningBookFormat.decodeEntry(line))
                .isNull()
        }
    }

    /**
     * Random binary noise, including control characters and lone surrogates. A random
     * string cannot be *proven* malformed, so this asserts the total contract: rejected,
     * or canonical. Either way the decoder returns rather than throwing.
     */
    @Test
    fun decodingNeverThrowsOnRandomBinaryNoise() {
        val random = Random(NOISE_SEED)
        repeat(NOISE_CASES) {
            val line = randomNoise(random)
            val decoded = OpeningBookFormat.decodeEntry(line)
            if (decoded != null) {
                assertWithMessage("noise '%s' decoded, so it must re-encode to itself", line)
                    .that(OpeningBookFormat.encodeEntry(decoded.first, decoded.second))
                    .isEqualTo(line)
            }
        }
    }

    @Test
    fun anEncodedEntryHoldsExactlyOneWhitespaceCharacter() {
        representativeEntries().forEach { (pegs, move) ->
            val encoded = OpeningBookFormat.encodeEntry(pegs, move)
            assertWithMessage("entry '%s' must hold exactly one whitespace separator", encoded)
                .that(encoded.count(Char::isWhitespace))
                .isEqualTo(1)
        }
    }

    @Test
    fun encodingIsStableSoRegeneratedBooksAreByteComparable() {
        representativeEntries().forEach { (pegs, move) ->
            assertThat(OpeningBookFormat.encodeEntry(pegs, move))
                .isEqualTo(OpeningBookFormat.encodeEntry(pegs, move))
        }
    }

    private fun representativeEntries(): List<Pair<Long, Move?>> {
        val moves = representativeMoves()
        return representativeMasks().flatMapIndexed { index, pegs ->
            listOf(pegs to null, pegs to moves[index % moves.size])
        }
    }

    /** Every shipped footprint's full mask and start position, plus the degenerate ends. */
    private fun representativeMasks(): List<Long> =
        listOf(0L, 1L) +
            Boards.CLASSIC_SPECS.map(::fullMaskOf) +
            CLASSIC_STARTS.map { it.pegs }

    /** Real legal moves off the shipped starts, plus a multi-digit synthetic coordinate. */
    private fun representativeMoves(): List<Move> =
        CLASSIC_STARTS.map { RulesEngine.legalMoves(it).first() } + SYNTHETIC_MOVE

    private fun fullMaskOf(spec: BoardSpec): Long = (0 until spec.size).fold(0L) { mask, bit -> mask or (1L shl bit) }

    private fun namedMalformedLines(): List<Pair<String, String>> =
        listOf(
            "empty" to "",
            "blank" to " ",
            "one token" to "1f",
            "one token with no mask" to "1,2-3,4-5,6",
            "three tokens" to "1f 1,2-3,4-5,6 extra",
            "three tokens via a doubled separator" to "1f  1,2-3,4-5,6",
            "a non-hex mask" to "1g 1,2-3,4-5,6",
            "an uppercase (non-canonical) mask" to "1F 1,2-3,4-5,6",
            "a signed mask" to "+1f 1,2-3,4-5,6",
            "an empty mask" to " 1,2-3,4-5,6",
            "a mask wider than 64 bits" to "1ffffffffffffffff 1,2-3,4-5,6",
            "two coordinate groups" to "1f 1,2-3,4",
            "four coordinate groups" to "1f 1,2-3,4-5,6-7,8",
            "a coordinate group with one field" to "1f 1-3,4-5,6",
            "a coordinate group with three fields" to "1f 1,2,9-3,4-5,6",
            "a non-numeric coordinate" to "1f 1,x-3,4-5,6",
            "an empty coordinate" to "1f 1,-3,4-5,6",
            "a negative coordinate" to "1f 1,2-3,4-5,-6",
            "a coordinate that overflows Int" to "1f 1,2-3,4-5,99999999999",
            "a whitespace-padded coordinate" to "1f 1, 2-3,4-5,6",
            "a bare separator with no mask" to "-",
            "a marker with no mask" to " -",
        )

    /**
     * Structurally-breaking mutations applied at every site of every valid base line.
     *
     * Every mutation is chosen so the result **cannot** be a legal entry, which is what
     * lets this corpus assert `null` outright instead of the weaker "did not throw":
     *  - inserting a space always raises the token count from two to three;
     *  - inserting or substituting a letter outside `[0-9a-f]` breaks the mask if it
     *    lands left of the separator, and breaks a coordinate field or a group boundary
     *    if it lands right of it;
     *  - duplicating a separator always changes a group or field count.
     *
     * Deletion and digit-duplication are deliberately NOT used: dropping or repeating a
     * digit can leave a perfectly legal line, which would make the `null` assertion
     * false. The [decodingNeverThrowsOnRandomBinaryNoise] case covers unstructured input.
     */
    private fun malformedCorpus(): List<String> {
        val random = Random(CORPUS_SEED)
        val bases = representativeEntries().map { (pegs, move) -> OpeningBookFormat.encodeEntry(pegs, move) }
        return bases
            .flatMap { base ->
                base.indices.flatMap { index ->
                    val head = base.substring(0, index)
                    val tail = base.substring(index)
                    val separatorDouble =
                        if (base[index] in SEPARATORS) head + base[index] + tail else null
                    listOfNotNull(
                        head + SPACE + tail,
                        head + NON_HEX_LETTER + tail,
                        head + NON_HEX_LETTER + base.substring(index + 1),
                        head + randomNonHexLetter(random) + tail,
                        separatorDouble,
                    )
                }
            }.distinct()
    }

    private fun randomNonHexLetter(random: Random): Char = NON_HEX_LETTERS[random.nextInt(NON_HEX_LETTERS.length)]

    private fun randomNoise(random: Random): String =
        buildString {
            repeat(random.nextInt(NOISE_MAX_LENGTH)) {
                append(random.nextInt(NOISE_CODE_POINTS).toChar())
            }
        }

    private companion object {
        /** The shipped Classic starts, the boards whose positions the book actually holds. */
        val CLASSIC_STARTS: List<Board> =
            listOf(
                Boards.english33Initial,
                Boards.european37Initial,
                Boards.asymmetric39Initial,
                Boards.diamond41Initial,
                Boards.wiegleb45Initial,
            )

        /**
         * Multi-digit coordinates no shipped footprint produces (all five are at most
         * 9 columns wide), included so the field encoding is not accidentally
         * single-digit-only.
         */
        val SYNTHETIC_MOVE =
            Move(
                from = Position(row = 10, col = 11),
                over = Position(row = 10, col = 12),
                to = Position(row = 10, col = 13),
            )

        /** Wiegleb-45's highest occupied bit index (45 holes -> bits 0..44). */
        const val WIEGLEB_45_TOP_BIT = 44

        /** Fixed so the malformed corpus is identical on every run and in CI. */
        const val CORPUS_SEED = 0x0B00_C0DEL

        /** Fixed so the binary-noise sweep is identical on every run and in CI. */
        const val NOISE_SEED = 0x0B00_1215L

        /** The plan's floor for the malformed corpus. */
        const val MINIMUM_CORPUS_SIZE = 500

        const val NOISE_CASES = 2_000
        const val NOISE_MAX_LENGTH = 40
        const val NOISE_CODE_POINTS = 0x0100
        const val SPACE = " "

        /** Letters outside `[0-9a-f]`, so inserting one breaks a mask and a coordinate alike. */
        const val NON_HEX_LETTERS = "ghijklmnopqrstuvwxyz"
        const val NON_HEX_LETTER = 'z'

        /** The three characters whose count the format's structure depends on. */
        const val SEPARATORS = " ,-"
    }
}
