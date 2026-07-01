package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards

/**
 * Resolves a [DifficultyTier] for any Challenge level number via a **continuous**
 * growth mapping (D-05 — "smooth, no hard tier walls, no early plateau"), NOT a
 * stepped `when(range)` lookup.
 *
 * Each knob is a monotonic function of the (clamped) level number: solution length
 * rises first and steadily (the primary lever, D-06), while the footprint pool
 * widens later and more slowly (D-05/D-08). Every growth rate, offset, and cap is a
 * named [Curve] constant — the values are playtest-tunable placeholders (D-07); the
 * structure is final. Because each knob grows by integer division of the level
 * number, consecutive-level deltas are bounded (at most one step), giving the
 * gentle continuous climb D-05 asks for.
 */
object DifficultyTiers {
    /**
     * The [DifficultyTier] for [levelNumber]. Any `levelNumber < 1` is clamped to
     * level 1 (T-03-01: an untrusted nav-arg level must never crash), so
     * `forLevel(0)` and `forLevel(-5)` both return the level-1 tier.
     */
    fun forLevel(levelNumber: Int): DifficultyTier {
        val level = levelNumber.coerceAtLeast(Curve.MIN_LEVEL)
        val fromStart = level - Curve.MIN_LEVEL

        val solutionLength =
            (Curve.SOLUTION_LENGTH_BASE + fromStart / Curve.SOLUTION_LENGTH_STEP_LEVELS)
                .coerceAtMost(Curve.SOLUTION_LENGTH_MAX)
        val minSolutionLength =
            maxOf(Curve.SOLUTION_LENGTH_BASE, solutionLength - Curve.MIN_SOLUTION_LENGTH_SLACK)

        val footprintCount =
            (Curve.FOOTPRINT_POOL_BASE + fromStart / Curve.FOOTPRINT_STEP_LEVELS)
                .coerceAtMost(CanonicalFootprints.ORDERED_BY_SIZE.size)
        val footprintPool = CanonicalFootprints.ORDERED_BY_SIZE.take(footprintCount)

        val startingPegs = solutionLength + Curve.STARTING_PEG_OFFSET
        val pegFloor = minSolutionLength + Curve.STARTING_PEG_OFFSET
        val pegRange = pegFloor..(startingPegs + Curve.PEG_RANGE_UPPER_SLACK)

        val scoreMultiplier =
            Curve.SCORE_MULTIPLIER_BASE + fromStart / Curve.SCORE_MULTIPLIER_STEP_LEVELS

        return DifficultyTier(
            solutionLength = solutionLength,
            minSolutionLength = minSolutionLength,
            footprintPool = footprintPool,
            pegRange = pegRange,
            minOpeningMoves = Curve.MIN_OPENING_MOVES,
            minSpread = Curve.MIN_SPREAD,
            freeformIntroLevel = Curve.FREEFORM_INTRO_LEVEL,
            scoreMultiplier = scoreMultiplier,
        )
    }

    /**
     * The canonical footprints eligible before freeform is introduced, ordered by
     * ascending hole count so widening the pool adds progressively bigger boards
     * (D-05/D-08). Freeform shapes are appended at higher levels in 03-02.
     */
    private object CanonicalFootprints {
        val ORDERED_BY_SIZE: List<BoardSpec> =
            listOf(
                Boards.English33,
                Boards.European37,
                Boards.Asymmetric39,
                Boards.Diamond41,
                Boards.Wiegleb45,
            )
    }

    /**
     * The scoped, playtest-tunable curve constants (D-07 — VALUES are placeholders,
     * structure is final). No literal leaks into the `forLevel` logic; every growth
     * rate and offset is a KDoc'd `const val` here (QUAL-03).
     */
    private object Curve {
        /** The lowest valid level; anything below clamps up to it (T-03-01). */
        const val MIN_LEVEL = 1

        /** Solution length dealt at level 1 — a gentle on-ramp (D-05). */
        const val SOLUTION_LENGTH_BASE = 3

        /** Levels between each +1 step of the target solution length (primary lever, D-06). */
        const val SOLUTION_LENGTH_STEP_LEVELS = 25

        /**
         * Ceiling on the target solution length (D-07 playtest cap). A greedy random
         * reverse walk reliably fills the canonical pool to ~30 un-jumps but not far
         * beyond (near-full boards leave too few empty cells to un-jump into), so an
         * uncapped target would make the `minSolutionLength`/`pegRange` gates
         * unsatisfiable at very high levels. Past this cap the difficulty lever hands
         * off to footprint size/shape and freeform variety (D-06 — "trickier before
         * bigger", then bigger). Playtest-tunable before the version is frozen.
         */
        const val SOLUTION_LENGTH_MAX = 30

        /** How far below the target the gate floor sits (a short winning line still passes). */
        const val MIN_SOLUTION_LENGTH_SLACK = 1

        /** Footprints in the pool at level 1 — the smallest canonical board only (D-05). */
        const val FOOTPRINT_POOL_BASE = 1

        /**
         * Levels between each widening of the footprint pool. Larger than
         * [SOLUTION_LENGTH_STEP_LEVELS] so board size grows *later and more slowly*
         * than solution length (D-06 — trickier before bigger).
         */
        const val FOOTPRINT_STEP_LEVELS = 50

        /** Extra pegs the density band tolerates above the by-construction starting count. */
        const val PEG_RANGE_UPPER_SLACK = 2

        /** A reverse walk of length L deals `L + 1` pegs (the target peg plus L un-jumps). */
        const val STARTING_PEG_OFFSET = 1

        /** Branching-factor floor: the opening must offer at least this many legal moves (gate). */
        const val MIN_OPENING_MOVES = 2

        /** Spatial-spread floor so the opening pegs are not clumped in one corner (gate). */
        const val MIN_SPREAD = 2

        /**
         * The level at/above which freeform footprints become eligible (D-08). Sits
         * well above the ~1-10 gentle on-ramp band so early levels stay on familiar
         * canonical shapes. Freeform generation itself arrives in 03-02.
         */
        const val FREEFORM_INTRO_LEVEL = 40

        /** The level-scaled score multiplier at level 1 (D-11). */
        const val SCORE_MULTIPLIER_BASE = 1

        /** Levels between each +1 step of the score multiplier (D-11). */
        const val SCORE_MULTIPLIER_STEP_LEVELS = 20
    }
}
