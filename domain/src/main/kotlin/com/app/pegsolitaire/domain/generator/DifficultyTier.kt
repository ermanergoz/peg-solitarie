package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardSpec

/**
 * The resolved difficulty knobs for one Challenge level — a pure data record with
 * **fields only, no behaviour** (the `DifficultyTiers.forLevel` resolver owns the
 * curve). Modelled on the `ScoreParams` scoped-constant discipline: every knob is a
 * named, decision-tied value, never an inline literal.
 *
 * Per D-07 the *structure* is final but the *values* are playtest-tunable
 * placeholders — the curve is retuned before `GeneratorConstants.GENERATOR_VERSION`
 * is frozen. Solution length is the primary difficulty lever (D-06); footprint size
 * and freeform variety are secondary and grow later (D-05/D-08).
 *
 * @property solutionLength Target length of the recorded winning line — the PRIMARY
 *   difficulty lever (depth of planning, D-06).
 * @property minSolutionLength Gate floor: the shortest winning line that still
 *   passes quality gates (D-06). Enforced by the reject-and-reseed loop in 03-02.
 * @property footprintPool Eligible board footprints for this level — canonical
 *   `Boards.*` specs early, freeform shapes added at higher levels in 03-02 (D-08).
 * @property pegRange Acceptable starting peg-count band (density gate); a reverse
 *   walk of [solutionLength] deals `solutionLength + 1` pegs by construction.
 * @property minOpeningMoves Branching-factor floor: the opening must offer at least
 *   this many legal moves so it is not a single forced line (gate).
 * @property minSpread Spatial-spread floor so pegs are not all clumped (gate).
 * @property freeformIntroLevel The level at/above which freeform footprints become
 *   eligible; below it the pool is canonical-only (D-08).
 * @property scoreMultiplier The level-scaled numeric-score multiplier (D-11).
 */
data class DifficultyTier(
    val solutionLength: Int,
    val minSolutionLength: Int,
    val footprintPool: List<BoardSpec>,
    val pegRange: IntRange,
    val minOpeningMoves: Int,
    val minSpread: Int,
    val freeformIntroLevel: Int,
    val scoreMultiplier: Int,
)
