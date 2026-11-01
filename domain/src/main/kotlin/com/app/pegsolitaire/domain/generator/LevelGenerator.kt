package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
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
 *
 * Generator contract 2 (3.0.0; docs/gdd.md §7-§8, tech-design §7.1) adds the fail-proof gate
 * on the first levels (G6, [isFailProof]) and the even freeform/canonical draw ([pickFootprint],
 * G7); the curve's ladder cut and the one-peg pool live in [DifficultyTiers].
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
     * The level number is an untrusted nav arg, so it is clamped into
     * `[ChallengeLevelBounds.MIN, ChallengeLevelBounds.MAX]` via
     * [ChallengeLevelBounds.clamp] (T-03-01/T-03-09, D-15): anything `< 1` clamps up to
     * level 1 and anything above the ceiling clamps down to it. Because the replay chain
     * is O(level), that upper clamp is what bounds the
     * worst-case work — a forged huge level can no longer loop for billions of
     * iterations (CR-01). Each level's own reject-and-reseed loop ([generateForLevel])
     * reseeds with a fresh deterministic salt ([stableSeed]) and reverse-walks a
     * candidate, accepting the first that clears every [passesQualityGates] gate —
     * INCLUDING that its symmetry-canonical key does not collide with the previous
     * [GeneratorConstants.DEDUP_WINDOW] levels — or, after
     * [GeneratorConstants.MAX_ATTEMPTS], the best-scoring non-colliding board seen
     * (deterministic bounded fallback, T-03-04). The clamp bounds the chain length, so
     * generation is not just terminating but bounded in cost. The level is the last one
     * [generateLadder] yields for the same bound.
     */
    fun generate(levelNumber: Int): GeneratedLevel = generateLadder(levelNumber).last()

    /**
     * Every accepted level of the deterministic chain, `1..clamp(upTo)`, in order; O(upTo) in total.
     *
     * This is the chain [generate] has always walked to rebuild the exact dedup window, exposed so
     * a caller that needs every level up to N — the fuzz harness — walks it once instead of calling
     * [generate] N times, which would cost O(N²) (docs/tech-design.md §7.3 item 2). The frozen
     * generator contract is about OUTPUT, and this lifts the same loop with its body unchanged, so
     * each level it yields is the level [generate] returns for that number; the golden fixtures pin
     * the bytes. [upTo] is clamped exactly as [generate] clamps its level number, so the sequence is
     * never empty and never runs past [ChallengeLevelBounds.MAX]. Every iteration of the returned
     * sequence replays the chain from level 1.
     */
    fun generateLadder(upTo: Int): Sequence<GeneratedLevel> =
        sequence {
            val target = ChallengeLevelBounds.clamp(upTo)

            // Exact rolling dedup window: replay the deterministic sequence from level 1,
            // deduping each level against the ACTUAL accepted canonical keys of the
            // previous DEDUP_WINDOW levels. Because the anchor (level 1) and every step
            // are deterministic, generate(n-1) is a prefix of generate(n)'s pass — so the
            // window is self-consistent across independent calls and "no two boards in a
            // window share a canonical key" holds by construction (Pitfall 3). No
            // recursion; membership tests never feed a PRNG index (Pitfall 1).
            val recentKeys = ArrayDeque<Long>()
            for (level in ChallengeLevelBounds.MIN..target) {
                val tier = DifficultyTiers.forLevel(level)
                val produced = generateForLevel(level, tier, forbiddenKeys = recentKeys.toHashSet())
                recentKeys.addLast(canonicalKeyOf(produced.startingBoard))
                if (recentKeys.size > GeneratorConstants.DEDUP_WINDOW) recentKeys.removeFirst()
                yield(produced)
            }
        }

    /**
     * The bounded reject-and-reseed loop for a single [level]: reverse-walk a
     * candidate per attempt and return the first that clears [passesQualityGates]
     * (with [forbiddenKeys] applied to the dedup gate). On exhaustion accept the
     * best-scoring candidate that STILL clears the dedup gate, preferring it over the
     * overall best so the deterministic fallback can never silently emit a board that
     * collides on the symmetry-canonical key with a recent level (WR-01 — the
     * never-repeating core value). Only if EVERY candidate collided does it fall back
     * to the overall best, which still guarantees termination (T-03-04). Deterministic
     * (same seeds → same choice every run), single-threaded, and synchronous (Pitfall 1).
     */
    private fun generateForLevel(
        level: Int,
        tier: DifficultyTier,
        forbiddenKeys: Set<Long>,
    ): GeneratedLevel {
        var best: GeneratedLevel? = null
        var bestScore = FALLBACK_UNSET_SCORE
        var dedupBest: GeneratedLevel? = null
        var dedupBestScore = FALLBACK_UNSET_SCORE
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
            // Track the best candidate that at least clears the non-negotiable dedup
            // gate, so the fallback prefers a non-colliding board over a shorter-but-
            // colliding one (WR-01).
            if (canonicalKeyOf(candidate.startingBoard) !in forbiddenKeys && score > dedupBestScore) {
                dedupBest = candidate
                dedupBestScore = score
            }
            attempt++
        }
        return requireNotNull(dedupBest ?: best) { "reverse walk produced no board for level $level" }
    }

    /**
     * The quality gates (RESEARCH §Quality Gates, Pitfall 3; docs/gdd.md §8), applied to a
     * finished reverse-walk [candidate]. All thresholds are tier knobs or scoped
     * [GeneratorConstants] — no inline literals (QUAL-03). Any failure means the
     * caller reseeds with the next deterministic salt.
     *
     * A forced-line gate was removed (dead code, WR-02): it only ran once the
     * [DifficultyTier.minOpeningMoves] branching floor (>= 2) had already passed on
     * the opening board, so it could never fire — the branching floor already
     * guarantees the opening move is not the sole legal move.
     *
     * The fail-proof gate (G6) runs last: it is the only one that enumerates positions, and only
     * on a [DifficultyTier.failProof] level, so every cheaper gate short-circuits before it.
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
            boundingBoxSpread(board) >= tier.minSpread &&
            canonicalKeyOf(board) !in forbiddenKeys &&
            (!tier.failProof || isFailProof(board))
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
     * Picks the footprint for one reverse-walk attempt (gate G7, docs/gdd.md §7.3): a canonical
     * [DifficultyTier.footprintPool] spec below [DifficultyTier.freeformIntroLevel] (D-08); at or
     * above it, an even draw from [rng] — taken BEFORE any footprint is built, so both branches stay
     * deterministic — chooses a [FreeformFootprints.build] shape on
     * [GeneratorConstants.FREEFORM_DRAW] and the canonical pool otherwise, or when no freeform shape
     * is produced (D-09). Under contract 1 freeform was tried first at every attempt and always
     * produced, so the pool widening past the first footprint was unreachable. A shape that admits no
     * reverse walk to the tier's length simply fails the quality gates and reseeds. The PRNG index is
     * drawn against the pool `List` (Pitfall 1); freeform draws are deterministic in [rng].
     */
    private fun pickFootprint(
        level: Int,
        tier: DifficultyTier,
        rng: Random,
    ): BoardSpec {
        if (level >= tier.freeformIntroLevel &&
            rng.nextInt(GeneratorConstants.FREEFORM_SHARE_DENOMINATOR) == GeneratorConstants.FREEFORM_DRAW
        ) {
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

    /** Sentinel "no candidate scored yet" for the bounded fallback (any real score is >= 0). */
    private const val FALLBACK_UNSET_SCORE = -1

    /** An orthogonal step vector used to enumerate candidate un-jumps. */
    private data class Direction(
        val rowDelta: Int,
        val colDelta: Int,
    )
}

/**
 * Gate G6 (docs/gdd.md §7.4, GD-SB-10): whether no sequence of legal jumps from [start] can end
 * with more than [ChallengeClearRule.CLEAR_MAX_PEGS] pegs — so the level cannot be finished
 * uncleared, whatever the player taps. Called by `LevelGenerator.passesQualityGates` for a
 * [DifficultyTier.failProof] level. File-private rather than a member: it reads only the board,
 * and the generator object is at detekt's `TooManyFunctions` limit.
 *
 * A depth-first walk over every position reachable from [start], each visited once (keyed by its
 * peg mask, as a `Board` over one spec is), answering `false` at the first terminal position
 * above the clear threshold. The threshold is read from the clear rule itself, never copied: the
 * generator grades no finish, it only refuses a board that some finish would leave stuck. At the
 * six-peg openings this gate guards the walk is tiny — 9 to 35 positions on the shipped levels 1–4
 * of contract 2 — and the set is used for membership only, so the answer is the same whatever
 * order the walk takes (Pitfall 1).
 */
private fun isFailProof(start: Board): Boolean {
    val seen = hashSetOf(start.pegs)
    val pending = ArrayDeque(listOf(start))
    while (pending.isNotEmpty()) {
        val board = pending.removeLast()
        val moves = RulesEngine.legalMoves(board)
        if (moves.isEmpty() && board.pegCount > ChallengeClearRule.CLEAR_MAX_PEGS) return false
        moves
            .map { RulesEngine.applyMove(board, it) }
            .filter { seen.add(it.pegs) }
            .forEach(pending::addLast)
    }
    return true
}
