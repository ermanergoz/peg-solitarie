package com.app.pegsolitaire.domain.scoring

/**
 * The stateless, pure-Kotlin authority for peg-solitaire scoring.
 *
 * Mirrors the `RulesEngine` shape: a top-level `object` of pure functions with
 * **zero `android.*` imports** (QUAL-01 — enforced by the domain-purity gate).
 * Stars (SCOR-01 / D-01) and score (SCOR-02 / D-03) are two independent axes:
 * stars depend solely on pegs remaining (the board's target/center hole is
 * ignored as a star condition, D-02), while score rewards pegs cleared plus a
 * time bonus that decays with elapsed time and is floored at 0. Every tunable
 * value lives in [ScoreParams]; the only literal here is the `maxOf(0, …)`
 * time-bonus floor.
 */
object ScoreCalculator {
    /**
     * Stars (1-3) for a finished board, from [pegsRemaining] alone (D-01).
     * At or below [ScoreParams.THREE_STAR_MAX_PEGS] → 3, within the 2-3 band
     * (`<= `[ScoreParams.TWO_STAR_MAX_PEGS]) → 2, otherwise the
     * [ScoreParams.MIN_STARS] floor — never 0.
     */
    fun stars(pegsRemaining: Int): Int =
        when {
            pegsRemaining <= ScoreParams.THREE_STAR_MAX_PEGS -> ScoreParams.THREE_STARS
            pegsRemaining <= ScoreParams.TWO_STAR_MAX_PEGS -> ScoreParams.TWO_STARS
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
     * [stars] from [pegsRemaining] and [score] from [pegsCleared]/[elapsedSeconds]
     * (scaled by [levelMultiplier], D-11), bundling the inputs alongside the derived
     * values. [stars] is deliberately NOT re-based on the multiplier — a 1-peg solve
     * stays 3 stars whatever the level (D-11).
     */
    fun evaluate(
        pegsRemaining: Int,
        pegsCleared: Int,
        elapsedSeconds: Int,
        levelMultiplier: Int = ScoreParams.DEFAULT_LEVEL_MULTIPLIER,
    ): ScoreResult =
        ScoreResult(
            stars = stars(pegsRemaining),
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
