package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import kotlin.random.Random

/**
 * The pure-Kotlin, deterministic Challenge-level generator (the project's #1 risk).
 *
 * Levels are produced by **reverse-generation**: starting from a solved single-peg
 * board, the generator applies "un-jump" moves (each adds one peg) driven by a
 * level-seeded PRNG until it has dealt the tier's target solution length. The result
 * is **solvable by construction** — the recorded reverse walk, reversed, is a legal
 * forward winning line that folds the starting board back to one peg through the
 * unchanged `RulesEngine.applyMove` (a reverse move is just an ordinary [Move]
 * applied backwards, so no `ReverseMove` type is needed).
 *
 * Determinism (CHAL-03) rests on four disciplines (RESEARCH §Determinism, Pitfall
 * 1): a portable [stableSeed] bit-mix, candidate enumeration sorted into a `List`
 * before every PRNG draw (never a `Set`/`Map`), the specified-algorithm
 * `kotlin.random.Random(Long)`, and single-threaded synchronous generation (no
 * coroutines in the loop). Zero Android dependencies (QUAL-01).
 *
 * Quality gates, freeform footprints, and the full reject-and-reseed policy arrive
 * in 03-02; this plan ships the determinism *mechanism* and a bounded internal
 * retry that escapes an empty-candidate dead-end deterministically.
 */
object LevelGenerator {
    /** The four orthogonal step vectors, in the same fixed order `RulesEngine` uses. */
    private val REVERSE_DIRECTIONS: List<Direction> =
        listOf(
            Direction(rowDelta = 0, colDelta = -1),
            Direction(rowDelta = 0, colDelta = 1),
            Direction(rowDelta = -1, colDelta = 0),
            Direction(rowDelta = 1, colDelta = 0),
        )

    /** A jump spans two cells along one axis; the un-jumped peg lands the midpoint. */
    private const val JUMP_SPAN = 2

    /**
     * Generates the deterministic, solvable-by-construction level for [levelNumber].
     *
     * Any `levelNumber < 1` is clamped to level 1 (T-03-01). The reverse walk is
     * retried with a fresh deterministic salt if it dead-ends before reaching the
     * tier's [DifficultyTier.solutionLength]; after [GeneratorConstants.MAX_ATTEMPTS]
     * the best board reached is accepted, so generation always terminates.
     */
    fun generate(levelNumber: Int): GeneratedLevel {
        val level = levelNumber.coerceAtLeast(MIN_LEVEL)
        val tier = DifficultyTiers.forLevel(level)

        var best: GeneratedLevel? = null
        var attempt = 0
        while (attempt < GeneratorConstants.MAX_ATTEMPTS) {
            val rng = Random(stableSeed(level, salt = attempt))
            val candidate = walkBack(level, tier, rng)
            if (candidate.solution.size >= tier.solutionLength) return candidate
            if (best == null || candidate.solution.size > best.solution.size) best = candidate
            attempt++
        }
        return requireNotNull(best) { "reverse walk produced no board for level $level" }
    }

    /**
     * A portable splitmix64 finalizer over the level number and rejection [salt].
     * Pure arithmetic only, so the seed is identical on the JVM and ART — using only
     * [GeneratorConstants] values, never inline literals. The exact mix is part of
     * the frozen generator contract (changing it rewrites every level).
     */
    fun stableSeed(
        levelNumber: Int,
        salt: Int,
    ): Long {
        var z =
            (levelNumber.toLong() shl GeneratorConstants.SALT_SHIFT) xor
                (salt.toLong() + GeneratorConstants.GOLDEN_GAMMA)
        z = (z xor (z ushr GeneratorConstants.MIX_SHIFT_1)) * GeneratorConstants.MIX_1
        z = (z xor (z ushr GeneratorConstants.MIX_SHIFT_2)) * GeneratorConstants.MIX_2
        return z xor (z ushr GeneratorConstants.MIX_SHIFT_3)
    }

    /**
     * Runs one seeded reverse walk: pick a footprint and a single-peg target, then
     * un-jump until the target solution length is reached or no candidate remains.
     */
    private fun walkBack(
        level: Int,
        tier: DifficultyTier,
        rng: Random,
    ): GeneratedLevel {
        val footprint = tier.footprintPool[rng.nextInt(tier.footprintPool.size)]
        val target = footprint.inPlay[rng.nextInt(footprint.inPlay.size)]
        var board = Board(spec = footprint, pegs = 0L).withPeg(target)

        val reverse = ArrayList<Move>(tier.solutionLength)
        while (reverse.size < tier.solutionLength) {
            val candidates = legalReverseMoves(board)
            if (candidates.isEmpty()) break
            val move = candidates[rng.nextInt(candidates.size)]
            board = applyReverse(board, move)
            reverse += move
        }

        return GeneratedLevel(
            levelNumber = level,
            startingBoard = board,
            solution = reverse.asReversed().toList(),
            generatorVersion = GeneratorConstants.GENERATOR_VERSION,
        )
    }

    /**
     * All reverse-legal "un-jump" moves on [board], sorted with the **identical**
     * comparator `RulesEngine.legalMoves` uses so the PRNG index maps to the same
     * move on every run and device (the determinism linchpin, Pitfall 1). The result
     * is a sorted `List` — a PRNG index is only ever drawn against this list, never a
     * `Set`/`Map`.
     *
     * Treating a [Move] `m` as a forward move, `m` is reverse-legal on [board] iff
     * the board currently looks like `m`'s *result*: `to = Peg`, `over = Empty`,
     * `from = Empty`, all three in-spec.
     */
    private fun legalReverseMoves(board: Board): List<Move> =
        buildList {
            for (to in board.spec.inPlay) {
                if (board.cellAt(to) != Cell.Peg) continue
                for (direction in REVERSE_DIRECTIONS) {
                    val over = Position(row = to.row + direction.rowDelta, col = to.col + direction.colDelta)
                    val from =
                        Position(
                            row = to.row + JUMP_SPAN * direction.rowDelta,
                            col = to.col + JUMP_SPAN * direction.colDelta,
                        )
                    if (from in board.spec &&
                        board.cellAt(over) == Cell.Empty &&
                        board.cellAt(from) == Cell.Empty
                    ) {
                        add(Move(from = from, over = over, to = to))
                    }
                }
            }
        }.sortedWith(
            compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }),
        )

    /**
     * Applies the inverse of [move] to [board] — the exact mirror of
     * `RulesEngine.applyMove`: the jumped peg is cleared at `to` and pegs are
     * restored at `over` and `from` (net +1 peg). Copy-based, never mutates.
     */
    private fun applyReverse(
        board: Board,
        move: Move,
    ): Board =
        board
            .withoutPeg(move.to)
            .withPeg(move.over)
            .withPeg(move.from)

    /** The lowest valid level; anything below clamps up to it (T-03-01). */
    private const val MIN_LEVEL = 1

    /** An orthogonal step vector used to enumerate candidate un-jumps. */
    private data class Direction(
        val rowDelta: Int,
        val colDelta: Int,
    )
}
