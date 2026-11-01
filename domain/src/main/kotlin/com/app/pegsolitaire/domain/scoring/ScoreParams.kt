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
     * The DEFAULT "perfect finish" peg target (D-01), used by [ScoreCalculator.stars]
     * and [ScoreCalculator.evaluate] when the caller has no board in hand. At or below
     * this count the board scores 3 stars; the boundary is 1 peg (a textbook perfect
     * solve), so `stars(0)` and `stars(1)` both yield 3.
     *
     * Since DEBT-02 the 3-star boundary is no longer globally fixed: it is the
     * FINISHING BOARD's own `BoardSpec.minimumPegs`, and this constant is only the
     * fallback for boards that reduce to a single peg. It is equal by construction to
     * `BoardConstants.MINIMUM_PEGS_DEFAULT` — the same number viewed from the scoring
     * side rather than the model side. (`:domain`'s scoring package deliberately does
     * not import the model package for this: the two are independently-motivated
     * constants that happen to coincide, and a test asserts they still do.)
     */
    const val THREE_STAR_MAX_PEGS = 1

    /**
     * How many pegs WIDE the 2-star band is, measured above the board's perfect
     * finish (D-04).
     *
     * This is the band SHAPE, and it is global: re-anchoring a board changes where
     * the 2-star band SITS, never how wide it is. English-33's perfect finish is 1 peg
     * so its 2-star band is 2-3; Diamond-41's proven floor is 2 pegs
     * (`BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`) so its band is 3-4. Same width,
     * different anchor.
     */
    const val TWO_STAR_BAND_WIDTH = 2

    /**
     * Max pegs remaining for a 2-star result on a board whose perfect finish is the
     * default one peg (D-01).
     *
     * DERIVED from [THREE_STAR_MAX_PEGS] + [TWO_STAR_BAND_WIDTH] rather than written
     * as a standalone `3`, so the boundary and its anchor cannot drift apart: moving
     * either the anchor or the width now moves this value with it. [ScoreCalculator]
     * computes the same sum against the board's own target instead of reading this,
     * so this constant exists for callers reasoning about the legacy global bands.
     */
    const val TWO_STAR_MAX_PEGS = THREE_STAR_MAX_PEGS + TWO_STAR_BAND_WIDTH

    /**
     * The star floor (D-01). More than [TWO_STAR_BAND_WIDTH] pegs above the board's
     * perfect finish still earns this many stars — never 0 (D-01 overrides spec §7 to
     * avoid punishing an honest finish with a blank result).
     */
    const val MIN_STARS = 1

    /** Stars awarded in the top band (at or below the board's perfect finish, D-01/D-04). */
    const val THREE_STARS = 3

    /** Stars awarded in the middle band (the [TWO_STAR_BAND_WIDTH] pegs above it, D-01/D-04). */
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
