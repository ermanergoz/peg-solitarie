package com.app.pegsolitaire.domain.generator

/**
 * The single definition of the valid Challenge level range, plus the clamp every
 * boundary applies to an untrusted level number (**D-15**).
 *
 * Before this object the lower bound was written out three times — a `private const`
 * in the data-layer `ChallengeLevelSource`, a companion `const` in `GameViewModel`, and
 * `LevelGenerator.MIN_LEVEL` — and closing WARN-03 would have added a fourth. That is a
 * plain violation of QUAL-03's no-magic-number rule ("extract to constants
 * files, split by scope"), and worse, three copies of a *security* bound can silently
 * drift apart. Every clamp site now calls [clamp] here.
 *
 * Scoped deliberately: this object owns the **range of valid level numbers**.
 * [GeneratorConstants] keeps owning the frozen seed math and the ceiling itself — [MAX]
 * delegates to [GeneratorConstants.MAX_CHALLENGE_LEVEL] rather than redeclaring it, so
 * the frozen-generator-contract file is not touched and there is still exactly one
 * ceiling value.
 *
 * **`DifficultyTiers.Curve.MIN_LEVEL` is deliberately NOT absorbed into this object.**
 * That constant lives inside the explicitly playtest-tunable `Curve` knob block, where
 * it is the origin of the difficulty ramp (`fromStart = level - MIN_LEVEL`) alongside
 * knobs like "levels between each +1 step". It is a *curve* parameter, not a statement
 * about which level numbers are legal, and the two are free to be tuned independently.
 * The duplication is intentional, not an oversight.
 *
 * Pure Kotlin, zero Android dependencies (QUAL-01).
 */
object ChallengeLevelBounds {
    /** The lowest valid Challenge level; anything below it clamps up to it (T-03-01). */
    const val MIN = 1

    /**
     * The highest valid Challenge level. Delegates to the frozen generator ceiling
     * ([GeneratorConstants.MAX_CHALLENGE_LEVEL]) so the bound is stated once — see that
     * constant's KDoc for why the ceiling exists (O(level) replay cost, CR-01).
     */
    const val MAX = GeneratorConstants.MAX_CHALLENGE_LEVEL

    /**
     * [levelNumber] coerced into `[MIN]..[MAX]`.
     *
     * A Challenge level number is **untrusted input**: it arrives as a typed nav route
     * argument, or is read back from stored state (a resume snapshot's provenance
     * stamp), either of which can be forged or simply stale, and neither is bounded by
     * its type — an `Int` route arg can carry `Int.MAX_VALUE` or `-5`.
     *
     * The policy is **fail-safe, not fail-closed** (T-06-22), matching the treatment
     * `GeneratorConstants.MAX_CHALLENGE_LEVEL` already documents and the resume codec
     * already applies to stored moves: an out-of-range value plays the nearest legal
     * level rather than crashing, hanging on an O(level) replay, or refusing to load.
     * Clamping (rather than rejecting) also keeps the level a result is *recorded*
     * under identical to the level that was actually *played* (WARN-03) — the two
     * cannot diverge when both read one clamped value.
     */
    fun clamp(levelNumber: Int): Int = levelNumber.coerceIn(MIN, MAX)
}
