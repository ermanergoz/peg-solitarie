package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
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
     * Any `levelNumber < 1` is clamped to level 1 (T-03-01). Each level's own
     * reject-and-reseed loop ([generateForLevel]) reseeds with a fresh deterministic
     * salt ([stableSeed]) and reverse-walks a candidate, accepting the first that
     * clears every [passesQualityGates] gate — INCLUDING that its symmetry-canonical
     * key does not collide with the previous [GeneratorConstants.DEDUP_WINDOW]
     * levels — or, after [GeneratorConstants.MAX_ATTEMPTS], the best-scoring board
     * seen (deterministic bounded fallback, T-03-04). Generation always terminates,
     * so it can never hang on an untrusted level number.
     */
    fun generate(levelNumber: Int): GeneratedLevel {
        val target = levelNumber.coerceAtLeast(MIN_LEVEL)

        // Exact rolling dedup window: replay the deterministic sequence from level 1,
        // deduping each level against the ACTUAL accepted canonical keys of the
        // previous DEDUP_WINDOW levels. Because the anchor (level 1) and every step
        // are deterministic, generate(n-1) is a prefix of generate(n)'s pass — so the
        // window is self-consistent across independent calls and "no two boards in a
        // window share a canonical key" holds by construction (Pitfall 3). No
        // recursion; membership tests never feed a PRNG index (Pitfall 1).
        val recentKeys = ArrayDeque<Long>()
        var produced: GeneratedLevel? = null
        for (level in MIN_LEVEL..target) {
            val tier = DifficultyTiers.forLevel(level)
            produced = generateForLevel(level, tier, forbiddenKeys = recentKeys.toHashSet())
            recentKeys.addLast(canonicalKeyOf(produced.startingBoard))
            if (recentKeys.size > GeneratorConstants.DEDUP_WINDOW) recentKeys.removeFirst()
        }
        return requireNotNull(produced) { "reverse walk produced no board for level $target" }
    }

    /**
     * The bounded reject-and-reseed loop for a single [level]: reverse-walk a
     * candidate per attempt and return the first that clears [passesQualityGates]
     * (with [forbiddenKeys] applied to the dedup gate). On exhaustion accept the
     * best-scoring candidate seen — the deterministic fallback that guarantees
     * termination (T-03-04). Single-threaded and synchronous (Pitfall 1).
     */
    private fun generateForLevel(
        level: Int,
        tier: DifficultyTier,
        forbiddenKeys: Set<Long>,
    ): GeneratedLevel {
        var best: GeneratedLevel? = null
        var bestScore = FALLBACK_UNSET_SCORE
        var attempt = 0
        while (attempt < GeneratorConstants.MAX_ATTEMPTS) {
            val rng = Random(stableSeed(level, salt = attempt))
            val candidate = walkBack(level, tier, rng)
            if (passesQualityGates(candidate, tier, forbiddenKeys)) return candidate
            val score = candidate.solution.size
            if (score > bestScore) {
                best = candidate
                bestScore = score
            }
            attempt++
        }
        return requireNotNull(best) { "reverse walk produced no board for level $level" }
    }

    /**
     * The six quality gates (RESEARCH §Quality Gates, Pitfall 3), applied to a
     * finished reverse-walk [candidate]. All thresholds are tier knobs or scoped
     * [GeneratorConstants] — no inline literals (QUAL-03). Any failure means the
     * caller reseeds with the next deterministic salt.
     */
    private fun passesQualityGates(
        candidate: GeneratedLevel,
        tier: DifficultyTier,
        forbiddenKeys: Set<Long>,
    ): Boolean {
        val board = candidate.startingBoard
        val solution = candidate.solution
        return solution.size >= tier.minSolutionLength &&
            board.pegCount in tier.pegRange &&
            RulesEngine.legalMoves(board).size >= tier.minOpeningMoves &&
            !isForcedLine(board, solution) &&
            boundingBoxSpread(board) >= tier.minSpread &&
            canonicalKeyOf(board) !in forbiddenKeys
    }

    /**
     * Whether the opening is a fully forced line: EACH of the first
     * [GeneratorConstants.FORCED_LINE_PREFIX] recorded moves is the only legal move
     * on its replayed board. Such openings feel scripted (D-06). A single branching
     * position within the prefix clears the gate.
     */
    private fun isForcedLine(
        startingBoard: Board,
        solution: List<Move>,
    ): Boolean {
        val prefix = minOf(GeneratorConstants.FORCED_LINE_PREFIX, solution.size)
        var board = startingBoard
        for (index in 0 until prefix) {
            if (RulesEngine.legalMoves(board).size > 1) return false
            board = RulesEngine.applyMove(board, solution[index])
        }
        // An empty prefix (prefix == 0) is not a forced line; a fully-traversed
        // prefix with no branching position is.
        return prefix > 0
    }

    /**
     * The bounding-box spread of [board]'s pegs — the width plus height of their
     * axis-aligned bounding box. A clumped opening (all pegs in one small corner)
     * scores low; the [DifficultyTier.minSpread] gate rejects it.
     */
    private fun boundingBoxSpread(board: Board): Int {
        val pegs = board.spec.inPlay.filter { board.cellAt(it) == Cell.Peg }
        if (pegs.isEmpty()) return 0
        val minRow = pegs.minOf { it.row }
        val maxRow = pegs.maxOf { it.row }
        val minCol = pegs.minOf { it.col }
        val maxCol = pegs.maxOf { it.col }
        return (maxRow - minRow) + (maxCol - minCol)
    }

    /** The symmetry-canonical dedup key for [board] (lifted [BoardCanonical]). */
    private fun canonicalKeyOf(board: Board): Long =
        BoardCanonical.canonical(board.pegs, BoardCanonical.symmetryPermutations(board.spec))

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
        val footprint = pickFootprint(level, tier, rng)
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
     * Picks the footprint for one reverse-walk attempt: a canonical
     * [DifficultyTier.footprintPool] spec below [DifficultyTier.freeformIntroLevel]
     * (D-08), and at/above it a [FreeformFootprints.build] shape when one is
     * produced — otherwise falling back to the canonical pool (D-09). A freeform
     * shape that admits no reverse walk to the tier's length simply fails the
     * quality gates and reseeds. The PRNG index is drawn against the pool `List`
     * (Pitfall 1); freeform draws are deterministic in [rng].
     */
    private fun pickFootprint(
        level: Int,
        tier: DifficultyTier,
        rng: Random,
    ): BoardSpec {
        if (level >= tier.freeformIntroLevel) {
            val freeform = FreeformFootprints.build(rng, tier)
            if (freeform != null) return freeform
        }
        return tier.footprintPool[rng.nextInt(tier.footprintPool.size)]
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

    /** Sentinel "no candidate scored yet" for the bounded fallback (any real score is >= 0). */
    private const val FALLBACK_UNSET_SCORE = -1

    /** An orthogonal step vector used to enumerate candidate un-jumps. */
    private data class Direction(
        val rowDelta: Int,
        val colDelta: Int,
    )
}
