package com.app.pegsolitaire.domain.solver

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
 * Correctness net for [SolverTables] — the per-footprint precomputed move, ordering,
 * colour-class and canonicalisation tables the bitboard solver runs on.
 *
 * **The load-bearing claim.** [RulesEngine] is and remains the single authority on
 * legality; [SolverTables] is a *precomputation of* those rules, never a second
 * definition of them. If the two ever drift, the solver explores moves gameplay
 * would reject and a hint highlights an illegal move (threat T-08-09). The two
 * agreement tests below are what forbid that: over a seeded corpus of reachable
 * positions on every shipped footprint, the table-derived legal-move set must equal
 * [RulesEngine.legalMoves] exactly, and `pegs xor xorMask[m]` must equal
 * [RulesEngine.applyMove]'s resulting mask exactly.
 *
 * Magic numbers (corpus sizes, walk depths, strip lengths) are intentional in test
 * code — they ARE the specification of the sampled corpus.
 */
class SolverTablesTest {
    @Test
    fun tableDerivedLegalMovesAgreeWithTheRulesEngineOnEveryFootprint() {
        forEachFootprintAndPosition { name, tables, board ->
            val fromTables = tableDerivedMoves(tables, board.pegs)
            val fromRules = RulesEngine.legalMoves(board).toSet()
            assertWithMessage("$name: legal-move disagreement at pegs=${board.pegs.toHex()}")
                .that(fromTables)
                .isEqualTo(fromRules)
        }
    }

    @Test
    fun applyingAMoveByXorAgreesWithTheRulesEngineOnEveryFootprint() {
        forEachFootprintAndPosition { name, tables, board ->
            for (index in tables.needMask.indices) {
                if (!isPlayable(tables, board.pegs, index)) continue
                val byXor = board.pegs xor tables.xorMask[index]
                val byRules = RulesEngine.applyMove(board, tables.moveAt(index)).pegs
                assertWithMessage(
                    "$name: applying ${tables.moveAt(index)} to ${board.pegs.toHex()} gave " +
                        "${byXor.toHex()} by xor, ${byRules.toHex()} by RulesEngine",
                ).that(byXor)
                    .isEqualTo(byRules)
            }
        }
    }

    @Test
    fun everyMoveMaskCoversExactlyTheThreeCellsOfItsJump() {
        namedClassicSpecs().forEach { (name, spec) ->
            val tables = SolverTables.forSpec(spec)
            for (index in tables.xorMask.indices) {
                assertWithMessage("$name: move $index mask ${tables.xorMask[index].toHex()} is not a jump triple")
                    .that(tables.xorMask[index].countOneBits())
                    .isEqualTo(JUMP_CELL_COUNT)
            }
        }
    }

    @Test
    fun theDestinationIsNeverOneOfTheTwoRequiredPegs() {
        namedClassicSpecs().forEach { (name, spec) ->
            val tables = SolverTables.forSpec(spec)
            for (index in tables.needMask.indices) {
                assertWithMessage("$name: move $index overlaps need=${tables.needMask[index].toHex()} with its target")
                    .that(tables.needMask[index] and tables.toMask[index])
                    .isEqualTo(0L)
            }
        }
    }

    @Test
    fun theColourClassMasksPartitionTheFootprint() {
        namedClassicSpecs().forEach { (name, spec) ->
            val tables = SolverTables.forSpec(spec)
            assertPartitions("$name plus", tables.classPlus, footprintMaskOf(spec))
            assertPartitions("$name minus", tables.classMinus, footprintMaskOf(spec))
        }
    }

    @Test
    fun moveOrderIsAPermutationOfEveryMoveIndex() {
        namedClassicSpecs().forEach { (name, spec) ->
            val tables = SolverTables.forSpec(spec)
            assertWithMessage("$name: moveOrder is not a permutation of 0 until ${tables.needMask.size}")
                .that(tables.moveOrder.toList().sorted())
                .isEqualTo(tables.needMask.indices.toList())
        }
    }

    @Test
    fun structurallyEqualSpecsShareOneTableSet() {
        val original = Boards.English33
        // A spec rebuilt from a resume blob is a different instance carrying the same
        // positions in a different order — it must still hit the cached tables.
        val rebuilt = BoardSpec(inPlay = original.inPlay.reversed(), target = original.target)
        assertThat(rebuilt).isNotSameInstanceAs(original)
        assertThat(rebuilt.footprint).isEqualTo(original.footprint)
        assertThat(SolverTables.forSpec(rebuilt)).isSameInstanceAs(SolverTables.forSpec(original))
    }

    @Test
    fun theTableCacheStaysBoundedAsFootprintsChurn() {
        val evicted = stripSpec(CHURN_FIRST_LENGTH)
        val evictedTables = SolverTables.forSpec(evicted)
        // Challenge generates a FRESH footprint per level; the cache must shed the
        // eldest rather than grow without bound at ~100 KB per entry (threat T-08-12).
        for (offset in 1..SolverTables.MAX_CACHED_FOOTPRINTS) {
            SolverTables.forSpec(stripSpec(CHURN_FIRST_LENGTH + offset))
        }
        assertThat(SolverTables.forSpec(evicted)).isNotSameInstanceAs(evictedTables)
    }

    /** Whether move [index] is playable on [pegs] purely by mask arithmetic. */
    private fun isPlayable(
        tables: SolverTables,
        pegs: Long,
        index: Int,
    ): Boolean = pegs and tables.needMask[index] == tables.needMask[index] && pegs and tables.toMask[index] == 0L

    private fun tableDerivedMoves(
        tables: SolverTables,
        pegs: Long,
    ): Set<Move> =
        tables.needMask.indices
            .filter { index -> isPlayable(tables, pegs, index) }
            .map { index -> tables.moveAt(index) }
            .toSet()

    private fun assertPartitions(
        label: String,
        classMasks: LongArray,
        footprintMask: Long,
    ) {
        var union = 0L
        classMasks.forEach { mask ->
            assertWithMessage("$label: colour classes overlap at ${(union and mask).toHex()}")
                .that(union and mask)
                .isEqualTo(0L)
            union = union or mask
        }
        assertWithMessage("$label: classes cover ${union.toHex()}, footprint is ${footprintMask.toHex()}")
            .that(union)
            .isEqualTo(footprintMask)
    }

    /** Drives [assertion] over the seeded reachable corpus of every shipped footprint. */
    private fun forEachFootprintAndPosition(assertion: (String, SolverTables, Board) -> Unit) {
        namedClassicSpecs().forEach { (name, spec) ->
            val tables = SolverTables.forSpec(spec)
            reachableCorpus(spec).forEach { board -> assertion(name, tables, board) }
        }
    }

    /**
     * At least [POSITIONS_PER_FOOTPRINT] genuinely reachable positions of [spec],
     * collected by replaying random legal-move prefixes from its single-vacancy start
     * under the fixed [CORPUS_SEED] — so a failure is reproducible from the message
     * alone and the suite never flakes.
     */
    private fun reachableCorpus(spec: BoardSpec): List<Board> {
        val random = Random(CORPUS_SEED)
        val corpus = mutableListOf<Board>()
        while (corpus.size < POSITIONS_PER_FOOTPRINT) {
            var board = startBoard(spec)
            var ply = 0
            while (ply < WALK_DEPTH) {
                corpus += board
                val moves = RulesEngine.legalMoves(board)
                if (moves.isEmpty()) break
                board = RulesEngine.applyMove(board, moves[random.nextInt(moves.size)])
                ply++
            }
        }
        return corpus
    }

    /** Every hole filled except the spec's start vacancy (its `target`, for all five Classic boards). */
    private fun startBoard(spec: BoardSpec): Board {
        val full = spec.inPlay.fold(Board(spec = spec, pegs = 0L)) { board, position -> board.withPeg(position) }
        val vacancy = requireNotNull(spec.target) { "every Classic spec carries its start vacancy as target" }
        return full.withoutPeg(vacancy)
    }

    /** A 1xN strip footprint — a cheap source of pairwise-distinct footprints for the cache test. */
    private fun stripSpec(length: Int): BoardSpec =
        BoardSpec(
            inPlay = (0 until length).map { column -> Position(row = 0, col = column) },
            target = Position(row = 0, col = 0),
        )

    private fun footprintMaskOf(spec: BoardSpec): Long = (1L shl spec.size) - 1

    /**
     * The shipped Classic footprints paired with a name derived from their hole count,
     * so an assertion message says which footprint failed and the list cannot drift out
     * of sync with [Boards.CLASSIC_SPECS].
     */
    private fun namedClassicSpecs(): List<Pair<String, BoardSpec>> =
        Boards.CLASSIC_SPECS.map { spec -> "footprint-${spec.size}" to spec }

    private fun Long.toHex(): String = "0x" + toULong().toString(HEX_RADIX)

    private companion object {
        /** Fixed seed for the reachable-position corpus — reproducible failures, no flake. */
        const val CORPUS_SEED = 0x5150_2602L

        /** Reachable positions sampled per footprint (the plan's floor is 500). */
        const val POSITIONS_PER_FOOTPRINT = 500

        /** Plies walked per random line before restarting from the board's start. */
        const val WALK_DEPTH = 25

        /** A jump touches exactly three cells: `from`, `over`, `to`. */
        const val JUMP_CELL_COUNT = 3

        /** Length of the first strip footprint in the cache-eviction churn. */
        const val CHURN_FIRST_LENGTH = 5

        const val HEX_RADIX = 16
    }
}
