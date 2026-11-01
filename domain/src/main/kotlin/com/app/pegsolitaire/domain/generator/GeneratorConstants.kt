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
     * 2). Bumped only when the generation algorithm changes. From the first store build of
     * this codebase onward a bump ships as a parallel generator, never an in-place mutation,
     * because every level a player has seen is a pure function of its number.
     *
     * **Version 2 (3.0.0; docs/gdd.md I7, GD-SB-12; tech-design §2.2):** the ladder's shift along
     * the difficulty curve (`DifficultyTiers`' level offset) changes the board every level deals.
     * It re-rolled in place, with the golden fixtures regenerated and the whole level space
     * re-fuzzed, because no store build of this codebase existed before it, so no player held a
     * level version 1 had to keep serving. The stamp on a stored resume snapshot is what says
     * which contract made its board.
     */
    const val GENERATOR_VERSION = 2

    /**
     * Bounded rejection-loop ceiling. `generate` retries a dead-ended reverse walk
     * with a fresh deterministic salt up to this many times, then accepts the best
     * board it reached — so generation can never hang on an untrusted level number
     * (T-03-01). The quality-gate reject-and-reseed loop in 03-02 reuses this bound.
     */
    const val MAX_ATTEMPTS = 1_000

    /**
     * Hard upper ceiling on the (untrusted) Challenge level number (CR-01, T-03-09).
     * `generate` replays the deterministic chain from level 1 to the target to build
     * the rolling dedup window, so the work is O(level); a forged nav arg (e.g.
     * `challengeLevel = 2_000_000_000`) would otherwise loop billions of times and
     * hang. The level is upper-clamped to this frozen ceiling so worst-case work is
     * bounded — a value above it plays the same board as the cap. Set well above any
     * realistically hand-reached level and above every golden-fixture level (<= 1000),
     * so the frozen determinism lock is untouched; product-tunable upward if
     * progression ever approaches it.
     */
    const val MAX_CHALLENGE_LEVEL = 10_000

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
     * Symmetry-normalized dedup window (03-02 quality gate, Pitfall 3): a starting
     * board is rejected if its symmetry-canonical `pegs` key
     * ([BoardCanonical.canonical]) matches that of any of the previous
     * [DEDUP_WINDOW] consecutive levels. "Unique in the player's sequence", NOT a
     * global uniqueness guarantee — the window is bounded so generation stays
     * O(window) and never scans the infinite level set. Playtest-tunable.
     */
    const val DEDUP_WINDOW = 8

    /**
     * Hard cap on a footprint's in-play cell count — the domain `Board` is a single
     * `Long` peg-bitmask, so no footprint (canonical or freeform) may exceed 64
     * cells (D-09, T-03-05). The freeform builder rejects any candidate above this.
     */
    const val MAX_IN_PLAY_CELLS = 64

    /**
     * The square bounding-box side length freeform shapes are grown within (D-09).
     * Sized so `dim * dim` == [MAX_IN_PLAY_CELLS] (8x8 == 64) — the shape can never
     * out-grow the bitmask cap, and the box stays proportionate to the cell count so
     * `BoardGeometry` renders it at a sensible scale (RESEARCH grid-dimension
     * caution). Playtest-tunable.
     */
    const val FREEFORM_BOUNDING_DIM = 8

    /**
     * Lower bound on a freeform shape's cell count, so a freeform board is never a
     * tiny sliver with no room to play. Playtest-tunable.
     */
    const val FREEFORM_MIN_CELLS = 12

    /**
     * Extra cells grown above the tier's upper peg bound, so a freeform footprint
     * has empty headroom for the reverse walk to un-jump into (a too-tight shape
     * would dead-end and reseed). Playtest-tunable.
     */
    const val FREEFORM_SIZE_HEADROOM = 6

    /**
     * How many candidate shapes the freeform builder grows before keeping the most
     * symmetric one (D-09 — "prefer symmetric/aesthetically-intentional shapes where
     * cheap"). Bounded so the build stays cheap. Playtest-tunable.
     */
    const val FREEFORM_SYMMETRY_CANDIDATES = 4
}
