package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * Correctness net for [BoardCanonical] — the single symmetry-canonicalisation
 * implementation behind the generator's dedup gate, the production [Solver]'s memo
 * key, and the authoring tools.
 *
 * **Why this file exists (RESEARCH Pitfall 4).** This logic previously existed as
 * three verbatim private copies with nothing pinning them together, and a chunked
 * variant of it silently dropped a high bit: handling only chunks 0-4 covers bits
 * 0-39, so bit 40 of the 41-hole Diamond and bits 40-44 of Wiegleb-45 vanished from
 * the canonical key. The search then memoised corrupted masks as real positions and
 * reported *reaching one peg on Diamond-41* — a terminal state the GF(4) colour
 * invariant proves impossible.
 *
 * The three properties below (peg-count preserving, idempotent, and identical to the
 * naive implementation) plus the explicit high-bit cases are what make it safe to
 * swap a solver's inner loop onto [BoardCanonical.canonicalFast].
 *
 * The symmetry-order test doubles as an independent cross-check of RESEARCH's
 * board-facts table: Asymmetric-39 has symmetry order 2, not 8, which is what its
 * node-budget headroom has to be sized against.
 */
class CanonicalTest {
    @Test
    fun canonicalisationPreservesThePegCountOnEveryFootprint() {
        forEachFootprintAndMask { name, _, tables, mask ->
            val canonicalMask = BoardCanonical.canonicalFast(mask, tables)
            assertWithMessage("$name: canonicalFast dropped pegs from ${mask.toHex()} -> ${canonicalMask.toHex()}")
                .that(canonicalMask.countOneBits())
                .isEqualTo(mask.countOneBits())
        }
    }

    @Test
    fun canonicalisationIsIdempotentSoAMemoKeyIsStable() {
        forEachFootprintAndMask { name, _, tables, mask ->
            val once = BoardCanonical.canonicalFast(mask, tables)
            val twice = BoardCanonical.canonicalFast(once, tables)
            assertWithMessage("$name: re-canonicalising ${once.toHex()} moved it to ${twice.toHex()}")
                .that(twice)
                .isEqualTo(once)
        }
    }

    @Test
    fun theFastPathIsExactlyTheNaivePath() {
        forEachFootprintAndMask { name, spec, tables, mask ->
            val fast = BoardCanonical.canonicalFast(mask, tables)
            val naive = BoardCanonical.canonical(mask, BoardCanonical.symmetryPermutations(spec))
            assertWithMessage(
                "$name: mask ${mask.toHex()} canonicalised to ${fast.toHex()} fast, ${naive.toHex()} naive",
            ).that(fast)
                .isEqualTo(naive)
        }
    }

    @Test
    fun theHighestBitsOfTheLargestFootprintsSurviveCanonicalisation() {
        // The exact Pitfall 4 regression: a five-chunk table covers bits 0-39 only, so
        // each of these single-peg masks canonicalises to 0 (peg count 0) instead of 1.
        val highBitCases =
            listOf(
                "Diamond41" to (Boards.Diamond41 to listOf(DIAMOND41_HIGH_BIT)),
                "Wiegleb45" to (Boards.Wiegleb45 to (DIAMOND41_HIGH_BIT until WIEGLEB45_SIZE).toList()),
            )
        highBitCases.forEach { (name, board) ->
            val (spec, bits) = board
            val tables = BoardCanonical.chunkTables(spec)
            bits.forEach { bit ->
                val mask = 1L shl bit
                val canonicalMask = BoardCanonical.canonicalFast(mask, tables)
                assertWithMessage("$name: bit $bit (${mask.toHex()}) canonicalised to ${canonicalMask.toHex()}")
                    .that(canonicalMask.countOneBits())
                    .isEqualTo(1)
            }
        }
    }

    @Test
    fun symmetryOrdersMatchTheMeasuredBoardFacts() {
        // Asymmetric-39's only non-identity symmetry is the anti-transpose; the other
        // four footprints carry the full dihedral group of their bounding box.
        assertThat(BoardCanonical.symmetryPermutations(Boards.Asymmetric39)).hasSize(ASYMMETRIC39_SYMMETRY_ORDER)
        classicSpecs
            .filterNot { (name, _) -> name == ASYMMETRIC39_NAME }
            .forEach { (name, spec) ->
                assertWithMessage("$name: unexpected symmetry order")
                    .that(BoardCanonical.symmetryPermutations(spec))
                    .hasSize(FULL_DIHEDRAL_ORDER)
            }
    }

    @Test
    fun everyGoldenSolutionPrefixCanonicalisesWithAPreservedPegCount() {
        // ONE_PEG for now: the diamond41 fixture this would also cover is committed in
        // the next task of this plan, at which point this widens back to ALL.
        SolutionFixtures.ONE_PEG.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            val tables = BoardCanonical.chunkTables(fixture.spec)
            for (prefixLength in 0..line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                val canonicalMask = BoardCanonical.canonicalFast(board.pegs, tables)
                assertWithMessage(
                    "${fixture.name}: prefix $prefixLength (${board.pegs.toHex()}) " +
                        "canonicalised to ${canonicalMask.toHex()}",
                ).that(canonicalMask.countOneBits())
                    .isEqualTo(board.pegs.countOneBits())
            }
        }
    }

    /**
     * Drives [assertion] over a seeded mask corpus on every shipped footprint, passing
     * the board name (for diagnosis), its spec, its cached chunk tables, and the mask.
     */
    private fun forEachFootprintAndMask(assertion: (String, BoardSpec, BoardCanonical.ChunkTables, Long) -> Unit) {
        classicSpecs.forEach { (name, spec) ->
            val tables = BoardCanonical.chunkTables(spec)
            val random = Random(MASK_CORPUS_SEED)
            val footprintMask = footprintMaskOf(spec)
            repeat(MASKS_PER_FOOTPRINT) {
                assertion(name, spec, tables, random.nextLong() and footprintMask)
            }
        }
    }

    /** Every bit a legal position mask of [spec] may set — bits `0 until spec.size`. */
    private fun footprintMaskOf(spec: BoardSpec): Long = (1L shl spec.size) - 1

    private fun Long.toHex(): String = "0x" + toULong().toString(HEX_RADIX)

    private companion object {
        /**
         * Fixed seed for the random mask corpus, so a failure is reproducible from the
         * message alone and the suite never flakes — the same determinism discipline as
         * `AuthoringSolver.BASE_SEED`.
         */
        const val MASK_CORPUS_SEED = 0x5150_2602L

        /** Masks sampled per footprint — dense enough to hit every chunk boundary. */
        const val MASKS_PER_FOOTPRINT = 2_000

        /** Highest bit index of the 41-hole Diamond — the bit a five-chunk table drops. */
        const val DIAMOND41_HIGH_BIT = 40

        /** Hole count of Wiegleb-45, so bits `40 until 45` are its dropped high band. */
        const val WIEGLEB45_SIZE = 45

        /** Asymmetric-39's symmetry group: identity plus the anti-transpose only. */
        const val ASYMMETRIC39_SYMMETRY_ORDER = 2

        /** The full dihedral group of a square bounding box. */
        const val FULL_DIHEDRAL_ORDER = 8

        const val ASYMMETRIC39_NAME = "Asymmetric39"

        const val HEX_RADIX = 16

        /**
         * The five shipped Classic footprints in `Boards.CLASSIC_SPECS` order, paired
         * with names because the catalog is unnamed and assertions here must say which
         * footprint failed.
         */
        val classicSpecs: List<Pair<String, BoardSpec>> =
            listOf(
                "English33" to Boards.English33,
                "European37" to Boards.European37,
                ASYMMETRIC39_NAME to Boards.Asymmetric39,
                "Diamond41" to Boards.Diamond41,
                "Wiegleb45" to Boards.Wiegleb45,
            )
    }
}
