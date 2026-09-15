package com.app.pegsolitaire.domain.model

/**
 * Scoped constants for the board-model layer: the per-board "perfect finish" peg
 * targets a [BoardSpec] can carry.
 *
 * Split by scope (QUAL-03's no-magic-number rule): every value is a KDoc'd
 * `const val` naming the decision that fixes it, mirroring the `ScoreParams` and
 * `GeneratorConstants` precedents rather than inlining a literal at the
 * construction site. Scope is deliberately narrow — these are properties of a
 * board FOOTPRINT, never of a level id, so `:domain` never learns about
 * `ClassicLevelIds`.
 */
object BoardConstants {
    /**
     * The "perfect finish" peg count for every board whose footprint reduces to a
     * single peg (D-01). The four Classic cross boards — English-33, European-37,
     * Asymmetric-39, Wiegleb-45 — each have a committed one-peg solution fixture
     * (D-19), and every generated Challenge footprint is solvable-by-construction
     * to one peg because it is built by a reverse walk from a single peg. This is
     * therefore the [BoardSpec.minimumPegs] default.
     */
    const val MINIMUM_PEGS_DEFAULT = 1

    /**
     * Diamond-41's proven fewest-pegs floor F (DEBT-02, D-05).
     *
     * **Reachability** (D-05a): a two-peg terminal position is reached from the
     * canonical centre start by the committed golden solution line, replayed move
     * by move through the unchanged `RulesEngine` in `SolutionTest`.
     *
     * **Optimality** (D-05b): F - 1 = 1, and the GF(4) colour invariant (the sum of
     * `omega^((r+c) mod 3)` and of `omega^((r-c) mod 3)` over occupied cells, both
     * conserved by every jump) admits no single-peg target from the centre start —
     * verified exhaustively over all 41 single-peg positions, zero of which satisfy
     * the invariant the centre start pins. Two is therefore exactly the floor.
     *
     * This value is **FROZEN** by those two proofs. It must never be edited to make
     * a failing test pass: a failure means one of the proofs broke, and D-05's
     * escalation clause fires instead.
     */
    const val DIAMOND41_FEWEST_PEGS_FLOOR = 2

    /**
     * The largest [BoardSpec.minimumPegs] any shipped board declares — the upper bound
     * for clamping an *untrusted* minimum-peg floor that arrives from outside the
     * board model (a nav argument, a persisted blob).
     *
     * Every Classic cross board and every generated Challenge footprint sits at
     * [MINIMUM_PEGS_DEFAULT]; Diamond-41 alone sits at
     * [DIAMOND41_FEWEST_PEGS_FLOOR], which is therefore the ceiling. Clamping a
     * forged floor to this value is what keeps "did this play solve the board?"
     * (`pegsRemaining <= minimumPegs`) from being widened by a route argument: a
     * forged `41` would otherwise declare a barely-played board solved. Clamping
     * against the play's own `pegsRemaining` would have exactly that effect and is
     * the trap this constant exists to avoid.
     *
     * It is derived from the shipped roster, so a future board with a higher proven
     * floor must raise it here rather than at any clamp site.
     */
    const val HIGHEST_MINIMUM_PEGS = DIAMOND41_FEWEST_PEGS_FLOOR
}
