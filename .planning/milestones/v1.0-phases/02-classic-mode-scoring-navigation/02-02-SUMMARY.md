---
phase: 02-classic-mode-scoring-navigation
plan: 02
subsystem: domain
tags: [peg-solitaire, board-catalog, golden-master, solver, kotlin, jvm-test]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: "Board/BoardSpec/Boards Long-bitmask model, RulesEngine, BoardCodec/GoldenMasterAssert harness"
provides:
  - "Four new Classic board footprints (European-37, Asymmetric-39, Diamond-41, Wiegleb-45) as pure-domain BoardSpec + initial Board values"
  - "Committed recorded-solution fixtures proving English-33, European-37, Asymmetric-39 and Wiegleb-45 solvable to one peg (D-19)"
  - "AuthoringSolver (test-only) that generates winning lines; RecordedSolutions decoder; SolutionTest replay + footprint sanity"
affects: [classic-catalog, level-repository, board-geometry, phase-3-generator]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scoped per-board layout objects in Boards.kt (QUAL-03) — derive in-play footprint from named const/val rules"
    - "Recorded-solution golden fixture (D-19): commit a winning move-list, replay through RulesEngine to pegCount==1 — no runtime Solver"
    - "Test-only authoring solver: symmetry-canonical memo + inward heuristic; confined to :domain test source set (DomainPurityTest unaffected)"

key-files:
  created:
    - "domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt"
    - "domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/RecordedSolutions.kt"
    - "domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt"
    - "domain/src/test/resources/golden/solutions/{english33,european37,asymmetric39,wiegleb45}.txt"
  modified:
    - "domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt"
    - "domain/build.gradle.kts"

key-decisions:
  - "Four boards added square-grid; RulesEngine unchanged (all orthogonal). European-37 & Diamond-41 start OFF-CENTER; Asymmetric-39 & Wiegleb-45 center-empty."
  - "Diamond-41's D-19 recorded-solution proof is DEFERRED — its 41-hole space (4 solvable starts, none center-finishing) exceeds feasible DFS search; needs pagoda pruning."
  - "Authoring solver replaced the planned plain DFS+visited-set with a symmetry-canonical memo + inward heuristic to make the 45-hole Wiegleb cross tractable."

patterns-established:
  - "Per-board layout object + BoardSpec + initial in Boards.kt (self-pattern from English33Layout)"
  - "Symmetry-canonical (dihedral) memo key for peg-solitaire search — sound for first-solution search, ~8x reduction on D4-symmetric boards"

requirements-completed: [CLAS-02]

# Metrics
duration: 188min
completed: 2026-06-29
---

# Phase 2 Plan 02: Classic Board Catalog + Solvability Fixtures Summary

**Added the four new square-grid Classic boards (European-37, Asymmetric-39, Diamond-41, Wiegleb-45) to the pure-domain catalog and proved English-33, European-37, Asymmetric-39 and the 45-hole Wiegleb cross solvable to one peg via committed recorded-solution fixtures replayed through the unchanged RulesEngine (D-19); Diamond-41's proof is deferred.**

## Performance

- **Duration:** ~188 min (dominated by solver iteration on the two 9x9-class boards)
- **Started:** 2026-06-28T22:45:00Z
- **Completed:** 2026-06-29T01:53:29Z
- **Tasks:** 2 (Task 1 fully; Task 2 with a scoped deviation — see below)
- **Files modified:** 6 (1 created package of 3 test sources + 4 fixtures, 2 modified)

## Accomplishments
- Four new `private object <Name>Layout` footprints + `BoardSpec`/initial `Board` values in `Boards.kt`, all `<= 64` holes, correct hole counts (37/39/41/45) and start vacancies; English-33 and `RulesEngine` untouched.
- A test-only `AuthoringSolver` that **generates** winning lines (in our exact `(row,col)`/`Move` encoding — Pitfall 7) and never ships in `:domain` main (`DomainPurityTest` stays green).
- Committed recorded-solution fixtures for **4 boards**; `SolutionTest` replays each through `RulesEngine` (re-validating every step via `legalMoves`) down to exactly one peg, plus footprint/peg-count sanity for **all** new boards.
- Reproducible fixtures: regeneration under `UPDATE_GOLDEN=true` yields byte-identical files (deterministic seeds).

## Task Commits

1. **Task 1: Add the four new board footprints to Boards.kt** - `240f64b` (feat)
2. **Task 2: Authoring solver + committed fixtures + replay SolutionTest (D-19)** - `97e1daf` (test)

## Files Created/Modified
- `domain/src/main/kotlin/.../model/Boards.kt` - +4 layout objects, specs, and initials (European37/Asymmetric39/Diamond41/Wiegleb45)
- `domain/src/test/.../golden/solutions/AuthoringSolver.kt` - test-only generator (symmetry-canonical memo + inward heuristic + jittered restarts)
- `domain/src/test/.../golden/solutions/RecordedSolutions.kt` - decodes committed fixtures back to `List<Move>`
- `domain/src/test/.../golden/solutions/SolutionTest.kt` - 4 replay proofs + footprint/peg-count sanity for all boards
- `domain/src/test/resources/golden/solutions/{english33,european37,asymmetric39,wiegleb45}.txt` - committed winning move-lists
- `domain/build.gradle.kts` - opt-in larger test heap, gated on `UPDATE_GOLDEN` (authoring only; CI/replay stays default)

## Decisions Made
- **Solver redesign:** The plan specified a "plain DFS with a visited `HashSet<Long>`". That is correct for the 33-39 hole boards but exhausts the heap and time on the 9x9 boards. The shipped authoring solver uses (a) a memory-lean primitive `long[]` memo keyed on the **symmetry-canonical** bitmask (~8x reduction, sound for first-solution search) and (b) an **inward** move ordering with jittered restarts (collapse cross arms toward center — the textbook cross-solving strategy). This cracked the 45-hole Wiegleb central game in ~4.5M positions.
- **Off-center starts:** European-37 starts `(3,2)` and Diamond-41 `(4,2)` (their centers are provably unsolvable); the fixtures lock European-37's off-center start by construction.

## Deviations from Plan

### 1. [Rule 4 - Scope/architectural] Diamond-41 recorded-solution proof DEFERRED
- **Found during:** Task 2 (solvability fixtures)
- **Issue:** The 41-hole diamond has only four single-vacancy solvable starts and, by parity, **none finishes at the center**, so the inward heuristic can never align with a solution, and an exhaustive search of its symmetry-reduced space (>360M canonical positions, raw >2.9B) exceeds a practical heap. No correct, feasible technique in the implemented solver (DFS + symmetry + heuristic) produces a Diamond-41 winning line; this genuinely requires **pagoda-function pruning**, which is a substantial solver feature beyond this plan's scope.
- **Resolution:** Diamond-41 still ships in `Boards.kt` with its documented off-center `(4,2)` start (geometry, hole count `41`, and 40-peg initial are all verified by `SolutionTest` sanity checks). Its D-19 recorded-solution fixture and replay test are omitted and clearly documented as deferred in `AuthoringSolver`/`SolutionFixtures`/`SolutionTest`.
- **Impact:** 4 of 5 non-trivial Classic boards have committed solvability proofs. Wiegleb-45 (the harder 9x9 board) **is** proven, so the catalog's largest board is locked. The literature (gibell.net / Bell-Beasley) asserts an off-center diamond start is solvable; `(4,2)`'s specific solvability remains unverified pending a pagoda-based solver.
- **Follow-up:** A future task should add pagoda pruning to the authoring solver to generate and commit the Diamond-41 fixture (and verify/replace the `(4,2)` start if needed), completing D-19 for all five boards.

**Total deviations:** 1 (scoped deferral of one board's solvability fixture).
**Impact on plan:** Catalog geometry complete for all 5 boards; solvability proven for 4/5. No scope creep; the gap is a documented, bounded follow-up.

## Issues Encountered
- The 9x9 boards (Wiegleb-45, Diamond-41) have reachable state spaces far larger than the 33-39 hole boards (raw billions of positions), defeating naive DFS. Resolved for Wiegleb via symmetry-canonical memoization + an inward (cross-collapse) heuristic with jittered restarts. An initially-tried "stranded-peg" prune was found **unsound** (a peg with no neighbour can regain one via a later landing) and removed before it could corrupt any fixture — the replay-through-RulesEngine design (Pitfall 7) would have caught a bad line, but the prune was caught at authoring time.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- The 5-board square-grid Classic catalog (D-18) exists in pure domain and is ready for the `LevelRepository`/catalog UI (CLAS-01) and per-board `BoardGeometry` grid-dimension derivation (dims 7/8/9).
- 4/5 boards have committed solvability proofs; Phase-3's generator reuses the same recorded-solution replay contract.
- **Open item:** Diamond-41 D-19 fixture deferred — add pagoda pruning to the authoring solver to close it.

## Self-Check: PASSED

All created files verified on disk (Boards.kt, AuthoringSolver.kt, RecordedSolutions.kt, SolutionTest.kt, 4 fixtures, SUMMARY) and both task commits present (`240f64b`, `97e1daf`). Full gate green: `:domain:test` (4 replay proofs + sanity), `:app:testDebugUnitTest --tests *DomainPurityTest*`, `ktlintCheck`, `detekt`.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-29*
