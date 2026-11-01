package com.app.pegsolitaire.domain.scoring

/**
 * The stateless, pure-Kotlin authority for peg-solitaire scoring.
 *
 * Mirrors the `RulesEngine` shape: a top-level `object` of pure functions with
 * **zero `android.*` imports** (QUAL-01 — enforced by the domain-purity gate).
 * Stars (SCOR-01 / D-01) and score (SCOR-02 / D-03) are two independent axes:
 * stars depend solely on pegs remaining measured against the board's perfect
 * finish (which target HOLE a peg lands in is still ignored as a star condition,
 * D-02), while score rewards pegs cleared plus a time bonus that decays with
 * elapsed time and is floored at 0. Every tunable value lives in [ScoreParams];
 * the only literal here is the `maxOf(0, …)` time-bonus floor.
 *
 * Since DEBT-02 the 3-star boundary is per-board (D-03 / D-04): it arrives as a
 * `minimumPegs` peg COUNT taken from the finishing board's `BoardSpec`, defaulted
 * so that every existing caller and every one-peg board behaves exactly as before.
 * The signature takes a count rather than a board identity precisely to preserve
 * this object's statelessness — there is no `ScoreCalculator` ↔ level coupling,
 * and `:domain`'s scoring package still knows nothing about `ClassicLevelIds`.
 */
object ScoreCalculator {
    /**
     * Stars (1-3) for a finished board, from [pegsRemaining] measured against that
     * board's own perfect finish [minimumPegs] (D-01 / D-03 / D-04, DEBT-02).
     *
     * At or below [minimumPegs] → 3, within the [ScoreParams.TWO_STAR_BAND_WIDTH]
     * pegs above it → 2, otherwise the [ScoreParams.MIN_STARS] floor — never 0.
     * The band SHAPE is identical on every board; only the anchor moves. At the
     * default [ScoreParams.THREE_STAR_MAX_PEGS] target the result is byte-identical
     * to the pre-DEBT-02 global bands across the whole peg-count domain, so the four
     * Classic cross boards and every generated Challenge footprint are untouched.
     *
     * [minimumPegs] is a peg COUNT derived from the finishing board's
     * `BoardSpec.minimumPegs` — never a level id, and never looked up from one. That
     * is the class's standing "no `ScoreCalculator` ↔ level coupling" rule, and it is
     * load-bearing rather than stylistic: the resume path rebuilds a `BoardSpec`
     * structurally from a persisted blob, so a level-id lookup here would be silently
     * wrong for exactly the games that survived process death.
     *
     * Diamond-41 is the board this exists for: its floor is a proven 2
     * (`BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`), so a 2-peg finish there is a
     * perfect result and must read as 3 stars, not 2.
     */
    fun stars(
        pegsRemaining: Int,
        minimumPegs: Int = ScoreParams.THREE_STAR_MAX_PEGS,
    ): Int =
        when {
            pegsRemaining <= minimumPegs -> ScoreParams.THREE_STARS
            pegsRemaining <= minimumPegs + ScoreParams.TWO_STAR_BAND_WIDTH -> ScoreParams.TWO_STARS
            else -> ScoreParams.MIN_STARS
        }

    /**
     * The ranking score (D-03): [pegsCleared] × [ScoreParams.PEG_VALUE] plus a
     * time bonus that starts at [ScoreParams.TIME_BONUS_CAP] and loses
     * [ScoreParams.DECAY_PER_SECOND] per elapsed second, floored at 0 so the base
     * never drops below the peg term regardless of [elapsedSeconds]. The whole base
     * is then multiplied by [levelMultiplier] (D-11) — a pure difficulty/level factor
     * the caller (Challenge's `GameViewModel`) supplies. At the default
     * [ScoreParams.DEFAULT_LEVEL_MULTIPLIER] the result is byte-identical to the
     * Phase-2 formula, so Classic is untouched; this function stays free of any
     * level state (no `ScoreCalculator` ↔ level coupling).
     */
    fun score(
        pegsCleared: Int,
        elapsedSeconds: Int,
        levelMultiplier: Int = ScoreParams.DEFAULT_LEVEL_MULTIPLIER,
    ): Int =
        (
            pegsCleared * ScoreParams.PEG_VALUE +
                maxOf(0, ScoreParams.TIME_BONUS_CAP - elapsedSeconds * ScoreParams.DECAY_PER_SECOND)
        ) * levelMultiplier

    /**
     * Assembles a [ScoreResult] for the Results destination (02-07): computes
     * [stars] from [pegsRemaining] against the finishing board's [minimumPegs] target
     * (D-03 / D-04), and [score] from [pegsCleared]/[elapsedSeconds] (scaled by
     * [levelMultiplier], D-11), bundling the inputs alongside the derived values.
     *
     * [stars] is deliberately NOT re-based on the multiplier — a perfect solve stays
     * 3 stars whatever the level (D-11) — and symmetrically [score] is NOT re-based on
     * [minimumPegs]: stars and score remain two independent axes, so re-anchoring
     * Diamond-41's bands changes the star count a player sees without moving their
     * numeric score by a point.
     *
     * [stars] defaults to the per-board bands above, so every Classic caller is byte-identical.
     * The Challenge caller passes the stars `ChallengeClearRule.finish` graded instead (docs/gdd.md
     * R5): Challenge bands are absolute, one star per peg from three pegs down, and do not read
     * [minimumPegs]. An override changes the star count only; the score never reads it.
     */
    fun evaluate(
        pegsRemaining: Int,
        pegsCleared: Int,
        elapsedSeconds: Int,
        levelMultiplier: Int = ScoreParams.DEFAULT_LEVEL_MULTIPLIER,
        minimumPegs: Int = ScoreParams.THREE_STAR_MAX_PEGS,
        stars: Int = stars(pegsRemaining = pegsRemaining, minimumPegs = minimumPegs),
    ): ScoreResult =
        ScoreResult(
            stars = stars,
            score =
                score(
                    pegsCleared = pegsCleared,
                    elapsedSeconds = elapsedSeconds,
                    levelMultiplier = levelMultiplier,
                ),
            pegsRemaining = pegsRemaining,
            pegsCleared = pegsCleared,
            elapsedSeconds = elapsedSeconds,
        )
}
