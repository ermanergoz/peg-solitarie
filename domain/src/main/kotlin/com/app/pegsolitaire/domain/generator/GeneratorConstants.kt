package com.app.pegsolitaire.domain.generator

/**
 * Scoped, frozen constants for the deterministic Challenge generator.
 *
 * Split by scope (QUAL-03's no-magic-number rule): every value is a KDoc'd
 * `const val` rather than a literal inlined in [LevelGenerator], mirroring the
 * `ScoreParams` precedent. These values — the [GENERATOR_VERSION], the splitmix64
 * seed-mix constants, and [MAX_ATTEMPTS] — are part of the **frozen generator
 * contract**: changing any of them rewrites every level, so they must never be
 * mutated in place after release (Pitfall 2). Difficulty *knobs* live separately in
 * `DifficultyTiers` and are playtest-tunable before freeze (D-07); the seed math
 * here is not.
 */
object GeneratorConstants {
    /**
     * The generator-contract version stamped onto every `GeneratedLevel` (Pitfall
     * 2). Bumped only when the generation algorithm changes; a bump ships as a
     * parallel generator, never an in-place mutation of v1.
     */
    const val GENERATOR_VERSION = 1

    /**
     * Bounded rejection-loop ceiling. `generate` retries a dead-ended reverse walk
     * with a fresh deterministic salt up to this many times, then accepts the best
     * board it reached — so generation can never hang on an untrusted level number
     * (T-03-01). The quality-gate reject-and-reseed loop in 03-02 reuses this bound.
     */
    const val MAX_ATTEMPTS = 1_000

    /**
     * Left-shift applied to the level number before mixing in the salt, so the
     * level number and the per-attempt salt occupy disjoint bit ranges of the raw
     * seed (a 32-bit salt below a level number shifted up 32 bits).
     */
    const val SALT_SHIFT = 32

    /**
     * The golden-ratio increment (2^64 / φ) added into the raw seed, the standard
     * splitmix64 stream constant. Expressed as the two's-complement negative of
     * `0x9E3779B97F4A7C15` (the codebase's existing full-width-hex convention).
     */
    const val GOLDEN_GAMMA = -0x61c8864680b583ebL

    /** First splitmix64 finalizer multiplier (`0xBF58476D1CE4E5B9`, two's-complement form). */
    const val MIX_1 = -0x40a7b892e31b1a47L

    /** Second splitmix64 finalizer multiplier (`0x94D049BB133111EB`, two's-complement form). */
    const val MIX_2 = -0x6b2fb644ecceee15L

    /** First splitmix64 xor-shift distance. */
    const val MIX_SHIFT_1 = 30

    /** Second splitmix64 xor-shift distance. */
    const val MIX_SHIFT_2 = 27

    /** Final splitmix64 xor-shift distance. */
    const val MIX_SHIFT_3 = 31

    /**
     * Forced-line rejection prefix length K (03-02 quality gate, Pitfall 3). A
     * starting board is rejected if EACH of the first K moves of its recorded
     * solution is the *only* legal move — a fully forced opening feels scripted
     * (D-06). Playtest-tunable; the branching floor already guarantees the opening
     * move itself is never forced.
     */
    const val FORCED_LINE_PREFIX = 3

    /**
     * Symmetry-normalized dedup window (03-02 quality gate, Pitfall 3): a starting
     * board is rejected if its symmetry-canonical `pegs` key
     * ([BoardCanonical.canonical]) matches that of any of the previous
     * [DEDUP_WINDOW] consecutive levels. "Unique in the player's sequence", NOT a
     * global uniqueness guarantee — the window is bounded so generation stays
     * O(window) and never scans the infinite level set. Playtest-tunable.
     */
    const val DEDUP_WINDOW = 8
}
