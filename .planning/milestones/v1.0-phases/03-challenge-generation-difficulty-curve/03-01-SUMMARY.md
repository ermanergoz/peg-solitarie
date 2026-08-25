---
phase: 03-challenge-generation-difficulty-curve
plan: 01
subsystem: domain-generator
tags: [generator, determinism, difficulty-curve, reverse-generation, pure-kotlin]
requires:
  - "domain: RulesEngine (legalMoves/applyMove sorted enumeration)"
  - "domain: Board (immutable Long bitmask, withPeg/withoutPeg)"
  - "domain: BoardSpec/Boards (canonical footprints)"
provides:
  - "LevelGenerator.generate(levelNumber): deterministic, solvable-by-construction GeneratedLevel"
  - "GeneratedLevel value type (levelNumber/startingBoard/solution/generatorVersion)"
  - "GeneratorConstants (frozen GENERATOR_VERSION, splitmix64 seed constants, MAX_ATTEMPTS)"
  - "DifficultyTier data-driven knob record + DifficultyTiers.forLevel continuous mapping"
affects:
  - "03-02 (quality gates + freeform reuse this generator core + MAX_ATTEMPTS)"
  - "03-03 (data-layer Level carries solution/generatorVersion)"
  - "Phase 4 (hints consume solution); Phase 5 (resume consumes generatorVersion)"
tech-stack:
  added: []
  patterns:
    - "Reverse-generation (un-jump from solved 1-peg board) — solvable by construction"
    - "splitmix64 portable stableSeed; PRNG index only against a sorted List (Pitfall 1)"
    - "Continuous integer-division difficulty growth (no stepped when(range), D-05)"
    - "Scoped const-val knobs, zero inline literals (QUAL-03)"
key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratedLevel.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratorConstants.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTier.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTiers.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeSolvabilityTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTierTest.kt
  modified: []
decisions:
  - "Difficulty curve is a continuous integer-division growth mapping (A4/D-05), not stepped tiers"
  - "Knob VALUES are playtest-tunable placeholders in a scoped Curve object (D-07); structure is final"
  - "Reverse moves are ordinary forward Moves applied backwards — no ReverseMove type (RESEARCH A2)"
  - "Bounded internal retry (MAX_ATTEMPTS) accepts best board on dead-end so generate always terminates (T-03-01)"
metrics:
  duration_min: 16
  tasks: 3
  files: 7
  completed: 2026-07-01
---

# Phase 3 Plan 01: Deterministic Reverse-Generation Core Summary

Pure-`:domain` deterministic reverse-generation `LevelGenerator` with a data-driven continuous difficulty curve, locking solvable-by-construction and byte-stable-on-regeneration as output-agnostic JVM invariants.

## What Was Built

- **`LevelGenerator` (reverse-generation core)** — `generate(levelNumber)` reverse-walks from a solved single-peg board, applying seeded "un-jump" moves until the tier's target solution length is dealt. Solvable by construction: the recorded `solution` (reverse walk, reversed) replays legally through the unchanged `RulesEngine.applyMove` back to one peg. `stableSeed` is a portable splitmix64 finalizer over `(levelNumber, salt)` using only `GeneratorConstants` values; `legalReverseMoves` mirrors `RulesEngine.legalMoves`' `compareBy` comparator exactly and the PRNG index is only ever drawn against that sorted `List` (Pitfall 1). Single-threaded, synchronous, zero Android deps.
- **`GeneratedLevel`** — immutable value type carrying `levelNumber`, `startingBoard`, `solution: List<Move>`, `generatorVersion` (the last two are downstream seams for Phase 4 hints / Phase 5 resume, carried now per RESEARCH Open Q1).
- **`GeneratorConstants`** — frozen-contract scoped consts: `GENERATOR_VERSION = 1`, splitmix64 seed constants (`GOLDEN_GAMMA`/`MIX_1`/`MIX_2` in the codebase's two's-complement full-width-hex form, plus the shift distances), and `MAX_ATTEMPTS` (declared here, reused by 03-02).
- **`DifficultyTier` + `DifficultyTiers.forLevel`** — a fields-only knob record plus a **continuous** monotonic resolver (integer-division growth, not a stepped `when(range)`, per D-05/A4). Solution length is the primary lever and rises first and steadily (D-06); the footprint pool widens later and more slowly; freeform is gated by `freeformIntroLevel = 40`. Every growth rate/offset/cap is a named `const val` in a scoped `Curve` object — playtest-tunable placeholders (D-07), structure final.
- **Invariant tests** — `ChallengeSolvabilityTest` (fold-to-one-peg with each step asserted legal, regenerate-twice-identical, clamp `<1`, in-spec positions, version stamp) and `DifficultyTierTest` (monotonicity, bounded continuity, primary-lever-before-footprint, freeform gating, clamp). Both are output-agnostic property assertions, so they survive the 03-02 gate/freeform additions.

## Task Commits

| Task | Name | Commit | Type |
| ---- | ---- | ------ | ---- |
| 1 | Generator value types, seed constants, difficulty tiers | `b021183` | feat |
| 2 | LevelGenerator reverse-generation core (RED test) | `cb862c6` | test |
| 2 | LevelGenerator reverse-generation core (GREEN impl) | `cc27019` | feat |
| 3 | Difficulty-tier monotonicity & continuity test | `7ebed5e` | test |

## Key Decisions

- **Continuous curve, not stepped tiers (A4/D-05):** `forLevel` grows each knob by `base + (level-1)/stepLevels`, giving bounded consecutive deltas (≤1 for solution length) — a gentle continuous climb with no hard tier walls. No `when(range)` lookup anywhere.
- **Reverse move = forward `Move` applied backwards (RESEARCH A2):** no `ReverseMove` type; `applyReverse` is the exact mirror of `applyMove`, so the winning line is trivially replayable.
- **Bounded deterministic termination (T-03-01):** the reverse walk retries with a fresh salt on an empty-candidate dead-end up to `MAX_ATTEMPTS`, then accepts the longest board reached — so an untrusted `levelNumber` can never hang generation. Solvability holds regardless of early break (still folds to one peg by construction).

## Deviations from Plan

None — plan executed exactly as written. Task 3's tier structure was front-loaded into Task 1 (per the plan's own file layout), so Task 3 was a test-only lock over an already-present resolver; committed as a `test(...)` commit accordingly.

## Verification

- `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` — green (CHAL-02/CHAL-03).
- `./gradlew :domain:test --tests '*DifficultyTierTest*'` — green (CHAL-04).
- `./gradlew :app:testDebugUnitTest --tests '*DomainPurityTest*'` — green (generator is pure `:domain`, zero `android.*`).
- Wave gate `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` — **BUILD SUCCESSFUL** (detekt `MagicNumber` gate green; no inline literals in generator logic).

## Known Stubs

None. All generator logic is fully wired against real `RulesEngine`/`Board`/`Boards` types. (Freeform footprints and quality gates are intentionally deferred to 03-02 per plan scope, not stubbed here — the footprint pool is canonical-only and the reverse walk is complete.)

## Self-Check: PASSED

- All 7 created files verified present on disk.
- All 4 task commits (`b021183`, `cb862c6`, `cc27019`, `7ebed5e`) verified in git log.
