---
phase: 03-challenge-generation-difficulty-curve
plan: 02
subsystem: domain-generator
tags: [generator, quality-gates, dedup, freeform, golden-master, determinism, pure-kotlin]

requires:
  - phase: 03-01
    provides: "LevelGenerator reverse-generation core, GeneratorConstants, DifficultyTier/DifficultyTiers"
  - phase: 01
    provides: "BoardCodec + GoldenMasterAssert golden-master harness; AuthoringSolver symmetry canonicalization; RulesEngine/Board"
provides:
  - "BoardCanonical: symmetry-canonical pegs key (symmetryPermutations/canonical) in main source"
  - "Six quality gates + deterministic reject-and-reseed + bounded best-scoring fallback in LevelGenerator"
  - "Exact rolling symmetry-normalized dedup window (Pitfall 3), self-consistent across independent generate() calls"
  - "FreeformFootprints.build: connected (<=64-cell) freeform BoardSpec builder gated by freeformIntroLevel"
  - "Five committed golden fixtures (levels 1/2/10/100/1000) freezing v1 generator output"
affects:
  - "03-03 (ChallengeLevelSource wiring consumes the frozen, gated generator)"
  - "Any future generator change (must bump GENERATOR_VERSION + regen fixtures — Pitfall 2)"

tech-stack:
  added: []
  patterns:
    - "Reject-and-reseed quality gates via deterministic salted sub-seeds (never breaks determinism)"
    - "Exact rolling dedup window anchored at level 1 so generate(n-1) is a deterministic prefix of generate(n)"
    - "Frontier-grown connected freeform footprints + independent flood-fill validation"
    - "Golden-master determinism lock (BoardCodec + GoldenMasterAssert reused verbatim)"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/FreeformFootprints.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/FreeformValidityTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeGeneratorGoldenTest.kt
    - domain/src/test/resources/golden/challenge_level_1.txt
    - domain/src/test/resources/golden/challenge_level_2.txt
    - domain/src/test/resources/golden/challenge_level_10.txt
    - domain/src/test/resources/golden/challenge_level_100.txt
    - domain/src/test/resources/golden/challenge_level_1000.txt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratorConstants.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTiers.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeSolvabilityTest.kt

key-decisions:
  - "Exact rolling dedup window anchored at level 1 (not a raw-predecessor lookup) so the 'no repeat within a window' guarantee is self-consistent across independent generate() calls"
  - "Cap the solution-length curve (SOLUTION_LENGTH_MAX = 30) so the minSolutionLength/pegRange gates stay satisfiable at high levels — a greedy random reverse walk cannot reliably near-fill a board (D-07 playtest tuning)"
  - "Regenerate-identical assertions compare footprint CONTENT (in-play positions), not identity, since freeform footprints are fresh BoardSpec instances per call"
  - "Freeform shapes grown by seeded frontier expansion (connected by construction) then re-validated by an INDEPENDENT flood-fill; most-symmetric-of-N candidates kept (D-09)"

patterns-established:
  - "Quality gates as pure predicate + deterministic salted reseed + bounded best-scoring fallback (T-03-04 never hangs)"
  - "Symmetry canonicalization lifted verbatim into main source (BoardCanonical) rather than re-derived"

requirements-completed: [CHAL-01, CHAL-02, CHAL-03, CHAL-04, QUAL-04]

duration: 15min
completed: 2026-07-01
---

# Phase 3 Plan 02: Quality Gates, Freeform Footprints & the Determinism Lock Summary

**Completed the v1 Challenge generator — six quality gates with deterministic reject-and-reseed, connected freeform footprints gated by intro level, and a bounded fallback — then FROZE the output with committed golden-master fixtures for levels 1/2/10/100/1000, CI-green with no update flag.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-07-01T18:49:17Z
- **Completed:** 2026-07-01T19:05:00Z (approx)
- **Tasks:** 3 (all TDD except the golden lock)
- **Files created/modified:** 14 (2 main + 3 test + 5 fixtures created; 3 main + 1 test modified)

## Accomplishments

- **Six quality gates** (min solution length, peg-density band, branching-factor floor, forced-line rejection, bounding-box spread, symmetry-normalized dedup) applied via `passesQualityGates`, with per-level reject-and-reseed on a deterministic salt and a best-scoring bounded fallback that guarantees termination (T-03-04 — no hang on any level number).
- **`BoardCanonical`** — the `symmetryPermutations`/`canonical` dihedral canonicalization lifted verbatim from the test-source `AuthoringSolver` into main source, powering the dedup key.
- **Exact rolling dedup window** anchored at level 1: because every step is deterministic, `generate(n-1)` is a prefix of `generate(n)`'s pass, so "no two boards within a `DEDUP_WINDOW` share a symmetry-canonical key" holds self-consistently across independent calls (Pitfall 3).
- **`FreeformFootprints.build`** — seeded frontier-grown connected blobs within an 8×8 (≤64-cell) bounding box, independently flood-fill-validated, most-symmetric-of-N kept (D-09); wired behind `pickFootprint` so canonical footprints serve below `freeformIntroLevel` and freeform at/above (with canonical fallback / reseed).
- **The determinism lock** — `ChallengeGeneratorGoldenTest` reuses `BoardCodec` + `GoldenMasterAssert` verbatim; five golden fixtures minted once and committed. Level 1000's fixture is a connected freeform board with a 30-move winning line (the solution-length cap); levels 1/2/10/100 are canonical. Green against committed fixtures with **no** `UPDATE_GOLDEN` flag.

## Task Commits

1. **Task 1: Quality gates + symmetry dedup + bounded reject-and-reseed** — `d5f1043` (test RED) → `e0ac27c` (feat GREEN)
2. **Task 2: Freeform footprint builder + connectivity/size validity** — `b3d79a7` (test RED) → `0a8a489` (feat GREEN)
3. **Task 3: Golden-master determinism lock + 5 fixtures** — `4dddcc7` (test)

## Files Created/Modified

- `generator/BoardCanonical.kt` — symmetry-canonical `pegs` key (lifted from `AuthoringSolver`).
- `generator/FreeformFootprints.kt` — connected ≤64-cell freeform `BoardSpec` builder + independent flood-fill.
- `generator/LevelGenerator.kt` — `passesQualityGates`, per-level reject-and-reseed, rolling dedup pass, `pickFootprint` freeform wiring.
- `generator/GeneratorConstants.kt` — `FORCED_LINE_PREFIX`, `DEDUP_WINDOW`, freeform consts (`MAX_IN_PLAY_CELLS`, `FREEFORM_BOUNDING_DIM`, `FREEFORM_MIN_CELLS`, `FREEFORM_SIZE_HEADROOM`, `FREEFORM_SYMMETRY_CANDIDATES`).
- `generator/DifficultyTiers.kt` — `SOLUTION_LENGTH_MAX` cap on the solution-length curve (deviation, below).
- `generator/QualityGateTest.kt`, `FreeformValidityTest.kt`, `ChallengeGeneratorGoldenTest.kt` — the three test specs.
- `generator/ChallengeSolvabilityTest.kt` — determinism assertion relaxed to content equality (deviation, below).
- `resources/golden/challenge_level_{1,2,10,100,1000}.txt` — the five frozen fixtures.

## Decisions Made

- **Rolling dedup anchored at level 1 (not raw-predecessor lookup).** An initial raw-predecessor window let `generate(23)` collide with the *actual* (itself-deduped) `generate(21)` board. Anchoring the deterministic rolling window at level 1 makes each predecessor's accepted key exactly the one the successor forbids — a by-construction guarantee. Cost is O(n) per call; 03-03 wiring can cache the sequence.
- **Freeform preferred at/above intro but with canonical fallback**, so a freeform shape that admits no reverse walk simply fails the gates and reseeds (never crashes) — verified for levels 40/41/100/500/1000.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Capped the solution-length curve so high-level gates are satisfiable**
- **Found during:** Task 1 (quality gates)
- **Issue:** The 03-01 curve grows `solutionLength` unboundedly (42 at level 1000). A greedy random reverse walk reliably fills the canonical pool to only ~30–38 un-jumps (near-full boards leave too few empty cells to un-jump into), so `minSolutionLength`/`pegRange` were unsatisfiable at high levels — the fallback returned sub-floor boards and `QualityGateTest` failed (pegCount 39 vs required 42–45).
- **Fix:** Added `Curve.SOLUTION_LENGTH_MAX = 30` and `coerceAtMost` in `DifficultyTiers.forLevel`. This is exactly the sanctioned D-07 knob tuning done before freeze; difficulty beyond the cap is carried by footprint size/shape and freeform variety (D-06). `DifficultyTierTest` stays green (still monotonic; level 1000 = 30 > level 1 = 3).
- **Files modified:** `generator/DifficultyTiers.kt`
- **Verification:** `./gradlew :domain:test` green (QualityGateTest + DifficultyTierTest).
- **Committed in:** `e0ac27c`

**2. [Rule 1 - Bug] Relaxed two regenerate-identical assertions to content equality**
- **Found during:** Task 2 (freeform)
- **Issue:** `ChallengeSolvabilityTest` and `QualityGateTest` asserted `spec isSameInstanceAs` — true only for the singleton canonical specs. Freeform footprints are fresh `BoardSpec` instances per call (and `BoardSpec` is not a data class), so identity fails though the CONTENT is fully deterministic.
- **Fix:** Compare `spec.inPlay` (List<Position>, structurally equal) plus `pegs` and `solution` — the actual CHAL-03 determinism contract. `ChallengeSolvabilityTest.kt` is a 03-01 file; touching it was required because introducing freeform changed the identity semantics its assertion relied on.
- **Files modified:** `generator/QualityGateTest.kt`, `generator/ChallengeSolvabilityTest.kt`
- **Verification:** `./gradlew :domain:test` green.
- **Committed in:** `0a8a489`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both were necessary for correctness; the curve cap is explicitly sanctioned D-07 tuning done before the version freeze. No scope creep — the frozen fixtures capture the tuned output.

## Issues Encountered

- **`ReturnCount` detekt violation** on `isForcedLine` (3 returns > limit 2) — refactored to two returns (`return prefix > 0`). Green afterward.

## Known Stubs

None. All gate thresholds and freeform tuning values are scoped, KDoc'd `const val`s in `GeneratorConstants`; the "placeholder" language in `DifficultyTiers`/`DifficultyTier` refers to playtest-tunable knob VALUES (D-07), not code stubs. The generator is fully wired against real `RulesEngine`/`Board`/`Boards`/`BoardCanonical`.

## Next Phase Readiness

- **The ship-blocking determinism lock is in place and CI-green** — 03-03 UI/data wiring (and any future knob tuning) may now proceed. Any generator change that alters a fixture must bump `GENERATOR_VERSION` and regen (Pitfall 2).
- **Performance note for 03-03:** `generate(n)` is O(n) (rolling dedup pass from level 1). The `ChallengeLevelSource`/`ChallengeProgress` wiring should cache or advance the sequence incrementally rather than call `generate(highLevel)` cold on every load.

## Self-Check: PASSED

- All 10 created files (2 main-source + 3 tests + 5 golden fixtures) verified present on disk.
- All 5 task commits (`d5f1043`, `e0ac27c`, `b3d79a7`, `0a8a489`, `4dddcc7`) verified in git log.
- Full wave gate `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` — BUILD SUCCESSFUL.
- Golden lock green against committed fixtures with NO `UPDATE_GOLDEN` flag; no `.actual` sidecars.

---
*Phase: 03-challenge-generation-difficulty-curve*
*Completed: 2026-07-01*
