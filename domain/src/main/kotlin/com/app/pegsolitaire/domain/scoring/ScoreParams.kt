package com.app.pegsolitaire.domain.scoring

/**
 * Scoped tunable constants for the peg-solitaire scoring model.
 *
 * Split by scope (QUAL-03's no-magic-number rule): every value is a KDoc'd
 * `const val` tied to the design decision that fixes it, rather than a literal
 * inlined in [ScoreCalculator]. Stars and score are two independent axes
 * (D-01 vs. D-03) — a slow perfect solve is still 3 stars.
 */
object ScoreParams {
    /**
     * Max pegs remaining for a 3-star result (D-01). At or below this count the
     * board scores 3 stars; the boundary is 1 peg (a textbook perfect solve),
     * so `stars(0)` and `stars(1)` both yield 3.
     */
    const val THREE_STAR_MAX_PEGS = 1

    /**
     * Max pegs remaining for a 2-star result (D-01). Pegs remaining in the 2-3
     * band score 2 stars.
     */
    const val TWO_STAR_MAX_PEGS = 3

    /**
     * The star floor (D-01). More than [TWO_STAR_MAX_PEGS] pegs remaining still
     * earns this many stars — never 0 (D-01 overrides spec §7 to avoid punishing
     * an honest finish with a blank result).
     */
    const val MIN_STARS = 1

    /** Stars awarded in the top band (`<= `[THREE_STAR_MAX_PEGS] pegs, D-01). */
    const val THREE_STARS = 3

    /** Stars awarded in the middle band (2-3 pegs, D-01). */
    const val TWO_STARS = 2

    /** Points awarded per peg cleared (D-03 — the peg-base term of the score). */
    const val PEG_VALUE = 100

    /**
     * The maximum time bonus, awarded at `elapsedSeconds == 0` (D-03). The bonus
     * decays linearly from this cap and is floored at 0.
     */
    const val TIME_BONUS_CAP = 5000

    /**
     * Points the time bonus loses per elapsed second (D-03). The bonus reaches 0
     * at `TIME_BONUS_CAP / DECAY_PER_SECOND` = 500 seconds and never goes negative.
     */
    const val DECAY_PER_SECOND = 10

    /**
     * The neutral level/difficulty multiplier (D-11). At this default the numeric
     * score is byte-identical to the base Phase-2 formula, so Classic (which never
     * passes a multiplier) is untouched. Challenge callers pass a difficulty-scaled
     * multiplier `> 1` to make climbing the ladder score more — stars are NOT re-based.
     */
    const val DEFAULT_LEVEL_MULTIPLIER = 1
}
