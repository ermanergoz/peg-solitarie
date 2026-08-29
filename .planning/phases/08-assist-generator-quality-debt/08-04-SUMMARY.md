---
phase: 08-assist-generator-quality-debt
plan: 04
subsystem: solver
tags: [kotlin, bitboard, solver, pagoda, colour-parity, gf4, precomputed-tables, junit4, truth]

# Dependency graph
requires:
  - phase: 08-01
    provides: BoardSpec.minimumPegs (the per-board target), BoardSpec.footprint (the structural cache key), Boards.CLASSIC_SPECS
  - phase: 08-02
    provides: BoardCanonical.chunkTables / canonicalFast — the single canonicalisation the tables reuse instead of adding a fourth copy
  - phase: 04-solver-assist
    provides: Pagoda, Solver/WinningLineSearch and its inwardness heuristic
provides:
  - "SolverTables — per-footprint needMask/toMask/xorMask, a precomputed inwardness moveOrder, the six colour-class masks and the footprint's canonicalisation tables"
  - "A bounded (6-footprint), access-ordered, structurally-keyed table cache with a referential fast path for the per-node call"
  - "A target-aware, popcount-based Pagoda.mayBeSolvable that reads BoardSpec.minimumPegs"
  - "SolverTablesTest — table-vs-RulesEngine agreement proof over a seeded reachable corpus on all five footprints"
  - "PagodaTest extended from 2 to 7 tests, including the honest statement that the prune is vacuous above one peg"

affects: [08-05-bitboard-solver, 08-06-diamond-floor-fixture, 08-08-opening-book, 08-09-opening-book, 08-10-opening-book, star-bands]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Precompute FROM the authority, never alongside it: jump triples are enumerated by asking RulesEngine, and set-equality against it is the gate"
    - "Single-slot referential fast path in front of a structurally-keyed memo, so a per-node call does not pay an O(size) List.hashCode"
    - "Assert a weakened invariant's exact weakness as a test, with its proof, rather than leaving it implicit"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverTables.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTablesTest.kt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt

key-decisions:
  - "Jump triples are enumerated as the union of RulesEngine.legalMoves over 'full board minus one hole', once per hole — a full board has no legal move, and this keeps RulesEngine the sole definition of legality"
  - "SolverTables.forSpec carries a single-slot referential fast path: Pagoda calls it once per search node, and a List<Position> hashCode is O(footprint size)"
  - "The colour-class masks are keyed on the footprint only and carry no target, so the Diamond footprint with minimumPegs=1 correctly shares tables with the shipped minimumPegs=2 spec"
  - "MAX_CACHED_FOOTPRINTS is internal, not private, so the cache-eviction test churns past the real bound instead of restating it as a literal"
  - "The plan's 'synthetic Diamond position rejected at T=2' test is mathematically impossible and was replaced by a proof of vacuity plus a genuine rejection at T=1 (see Deviations)"

patterns-established:
  - "Deliberate-break verification, continued from 08-02: the Pagoda net was shown to fail 3 of 7 tests against a degenerated condition before being accepted"
  - "State a weakened invariant's exact new strength as an asserted fact with its proof in KDoc, so a future reader does not hunt a bug that is not there"

requirements-completed: [DEBT-01, DEBT-02]

# Metrics
duration: 12 min
completed: 2026-08-29
---

# Phase 8 Plan 04: Solver Tables and a Target-Aware Colour-Parity Condition Summary

**`SolverTables` gives every shipped footprint precomputed `needMask`/`toMask`/`xorMask`, inwardness ordering, colour-class and canonicalisation tables — proven set-equal to `RulesEngine` over a seeded corpus — and `Pagoda.mayBeSolvable` now reads `BoardSpec.minimumPegs` via six popcounts, so Diamond-41 is no longer rejected out of hand.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-08-28T23:33:20Z
- **Completed:** 2026-08-28T23:46:02Z
- **Tasks:** 3
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments

- **A node is now three mask operations and zero allocation.** `needMask`/`toMask`/`xorMask` replace the `List<Move>` of data-class objects, the per-move `Random.nextDouble()` sort and the `Board` data-class copy that RESEARCH measured as a ~55x pure constant factor (~0.27M vs ~15M nodes/s). Plan 08-05 rewrites the inner loop on top of them.
- **The tables cannot drift from the rules, because they are built from them.** Jump triples are enumerated by asking `RulesEngine.legalMoves` for the board that is full except one hole, once per hole. `SolverTablesTest` then proves, over 500 reachable positions per footprint on all five shipped boards, that the table-derived legal-move set *equals* `RulesEngine.legalMoves` and that `pegs xor xorMask[m]` *equals* `RulesEngine.applyMove(...).pegs`. That is threat T-08-09 closed by construction plus proof, not by convention.
- **Diamond-41's hint is no longer structurally incapable of telling the truth.** `Pagoda.mayBeSolvable` asserted a one-peg terminal, so it returned `false` for *every* Diamond-41 position and `Solver.firstWinningMove` answered `null` in O(cells) with zero search. It now reads `board.spec.minimumPegs` and admits `Boards.diamond41Initial`.
- **The parity test is six popcounts, not a 41-cell `Math.floorMod` loop** — and it allocates nothing, where the old path allocated a `BooleanArray(3)` per call on the hottest path in the search.
- **The generalisation's true cost is measured, asserted and named, not glossed.** The prune does not merely weaken at a two-peg target; it becomes provably vacuous. That is stated with its proof in `Pagoda`'s KDoc and pinned as a test (see Deviations), because it is what makes the opening book load-bearing rather than optional.
- **The property net was verified to catch the bug it exists to catch.** Degenerating the condition to "always admit" fails 3 of `PagodaTest`'s 7 tests — the same deliberate-break discipline plan 08-02 established.

## Task Commits

1. **Task 1 (RED): failing spec for the precomputed solver tables** — `df84e59` (test)
2. **Task 1 (GREEN): SolverTables + `Pagoda.COLORING_MODULUS` promoted to internal** — `19a24ee` (feat)
3. **Task 2 (RED): failing spec for a target-aware colour-parity condition** — `afea4c4` (test)
4. **Task 2 (GREEN): generalise the condition to the per-board target** — `1fc353c` (feat)
5. **Task 3: extend the Pagoda property net for target-aware behaviour** — `968e04e` (test)

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverTables.kt` — **created.** `internal class SolverTables` exposing `needMask`, `toMask`, `xorMask`, `moveOrder`, `classPlus`, `classMinus`, `chunkTables` and `moveAt(index)`, with `forSpec` memoised on `BoardSpec.footprint` in an access-ordered `LinkedHashMap` bounded by `MAX_CACHED_FOOTPRINTS = 6` via `removeEldestEntry`. Each table's construction is its own small private top-level function; no `@Suppress` anywhere.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTablesTest.kt` — **created.** 8 tests: the two `RulesEngine` agreement proofs (the load-bearing ones, with board name and mask in hex in every message), the jump-triple mask shape, `needMask and toMask == 0`, the colour-class partition, `moveOrder` permutation, the structural-footprint cache hit, and bounded-cache eviction.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt` — **modified.** Target-aware, popcount-based condition; `SINGLE_PEG_ODD_CLASSES` deleted with no second code path for T = 1; `COLORING_MODULUS` promoted to `internal`; KDoc rewritten to name Pitfall 1, quantify the weakening, restate one-directionality and board-agnosticism, and name the opening book as the T-08-11 mitigation.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt` — **modified.** 2 tests to 7; the two pre-existing one-directionality tests untouched.

## Decisions Made

- **Enumerate jump triples via "full board minus one hole", per hole.** The plan says to ask `RulesEngine` for "the board's move set on a fully-populated footprint", but a fully-populated board has *no* legal move — a jump needs an empty destination. The board that is full except one hole offers exactly the jumps landing in that hole, so the union over every hole is the complete jump set, still obtained entirely from `RulesEngine`. Re-sorted into `RulesEngine`'s own `(from, to)` order so move indices are reproducible across runs.
- **A single-slot referential fast path in `forSpec`.** `Pagoda` calls `forSpec` once per search node and the cache key is a `List<Position>` whose `hashCode` is O(footprint size) — paying 45 element hashes plus an O(45) `equals` per node would have handed back the allocation win these tables exist to deliver. A search hammers one spec instance, so an identity check short-circuits it. Correctness does not depend on the slot: the tables are a pure function of the footprint, so a stale-but-equal hit is the same object the map would return.
- **The tables carry no target.** `minimumPegs` is read from `board.spec` at call time, not baked into the tables. That is what lets the Diamond footprint with `minimumPegs = 1` share the cached tables with the shipped `minimumPegs = 2` spec and still get a different verdict — which is precisely what `rejectsTheDiamondStartWhenTheFootprintCarriesAOnePegTarget` proves.
- **`MAX_CACHED_FOOTPRINTS` is `internal`, not `private`.** The eviction test churns past the *real* bound rather than restating `6` as a test literal that could silently drift.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Task 3's "synthetic Diamond-41 position rejected at T = 2" test is mathematically impossible**

- **Found during:** Task 3 (extending the Pagoda property net), while deriving the construction the plan asked to hand-build
- **Issue:** The plan's behavior bullet asks for "a synthetic Diamond-41 position whose forward-projected odd-class count exceeds 2 [and] is REJECTED — proving the generalised condition still prunes something rather than degenerating to 'always true'". No such position exists. The number of odd colour classes `k` satisfies `k ≡ P (mod 2)` because the three class counts sum to the peg count `P`. The forward projection flips every class iff `P - T` is odd, which is exactly when `P ≢ T`, so the projected count always ends up congruent to `T` mod 2. The parity half of the test is therefore *automatic* and the only surviving constraint is `oddClasses <= T`. With three classes and `T = 2`, the projected count is even and so is 0 or 2 — always `<= 2`. **At a two-peg target the condition admits every position, without exception.** RESEARCH's Pitfall 1 called this "nearly vacuous"; it is exactly vacuous. Writing the test as specified would have required either a fabricated assertion or a false claim in a KDoc.
- **Fix:** Split the intent into two honest tests that between them are strictly stronger than the impossible one. (a) `theConditionIsProvablyVacuousAtTheDiamondsTwoPegTarget` asserts every position in the seeded 200-position Diamond corpus is admitted, with the full parity proof in its KDoc, so the fact is pinned and a future reader cannot mistake it for a regression. (b) `rejectsASyntheticPositionWhoseProjectedParityNeedsAllThreeClasses` supplies the "still prunes something" proof the plan wanted, at the only target where the invariant can prune: pegs hand-placed at `(4,1)` and `(4,4)` on the Diamond footprint carrying `minimumPegs = 1`. Both sit in the same `(r+c) mod 3` class and the same `(r-c) mod 3` class, giving a base vector of weight 0; one forced move flips all three classes to weight 3, which no single peg can account for, so it is rejected. It is genuinely dead as well as formally excluded — the test asserts `RulesEngine.legalMoves` is empty, so the two pegs cannot jump at all. The plan's acceptance criterion ("contains a test asserting a synthetic position IS rejected under the generalised condition") is satisfied literally.
- **Files modified:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt`, `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt` (KDoc states the vacuity rather than only "weaker")
- **Verification:** All 7 `PagodaTest` tests pass. Deliberate break — replacing the condition body with `oddClasses <= COLORING_MODULUS` (i.e. always true) — fails exactly `rejectsAKnownDeadPosition`, `rejectsASyntheticPositionWhoseProjectedParityNeedsAllThreeClasses` and `rejectsTheDiamondStartWhenTheFootprintCarriesAOnePegTarget`, then restored to green.
- **Committed in:** `968e04e` (Task 3) and `1fc353c` (the KDoc half)

**2. [Rule 3 - Blocking] `Pagoda.COLORING_MODULUS` was `private`**

- **Found during:** Task 1 (building the colour-class masks)
- **Issue:** `SolverTables` must build exactly `COLORING_MODULUS` class masks, but the constant was `private` inside `object Pagoda`. Declaring a second `3` would violate CLAUDE.md's no-magic-numbers rule; the plan anticipated this ("promote it to `internal` if needed").
- **Fix:** Promoted to `internal const val` with KDoc naming why.
- **Files modified:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt`
- **Verification:** `ktlintCheck detekt` green; `SolverTables` builds `LongArray(Pagoda.COLORING_MODULUS)`.
- **Committed in:** `19a24ee` (Task 1 GREEN commit)

**3. [Rule 3 - Blocking] Task 2's `<files>` list omits its own RED test**

- **Found during:** Task 2
- **Issue:** Task 2 is `tdd="true"` but lists only `Pagoda.kt`, so there is no file in scope to write a failing test into. The RED gate cannot be satisfied without touching a test file.
- **Fix:** The one-assertion RED test (`admitsTheDiamondStartNowThatItsTargetIsTwoPegs`) was added to `PagodaTest.kt` — a file Task 3 owns and extends — and committed as its own `test(08-04)` commit before the implementation. Task 3's remaining four tests followed in its own commit, and the two pre-existing tests are untouched exactly as Task 3 requires.
- **Files modified:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt`
- **Verification:** RED observed — the new test failed at `PagodaTest.kt:32` with the two existing tests still passing; GREEN after `1fc353c`.
- **Committed in:** `afea4c4`

**4. [Rule 3 - Blocking] Missing `local.properties` in the worktree**

- **Found during:** Task 1 verification (`ktlintCheck detekt` spans `:app`)
- **Issue:** `local.properties` is gitignored, so a fresh worktree cannot configure `:app` — "SDK location not found". Same environment property plans 08-01 and 08-02 hit.
- **Fix:** Copied the main checkout's `local.properties` into the worktree. **Not committed** — gitignored by design and per-machine.
- **Verification:** `./gradlew ktlintCheck detekt` and `./gradlew :app:testDebugUnitTest` both configure and pass.
- **Committed in:** n/a (gitignored)

---

**Total deviations:** 4 auto-fixed (1 bug, 3 blocking).
**Impact on plan:** Deviation 1 is the substantive one — the plan asked for a test that mathematics forbids, and the honest replacement changes what the phase *knows* about its own Diamond hint path (see Next Phase Readiness). Deviations 2-4 are mechanical and were anticipated by the plan or by sibling plans. No scope creep: every file touched is in the plan's `files_modified` list.

## Issues Encountered

**Pagoda now runs on the search's hottest path with a memo lookup in it.** `Pagoda.mayBeSolvable` is called once per node by `Solver`, and the generalised version calls `SolverTables.forSpec`, whose cache key is a `List<Position>`. A naive implementation would have paid an O(footprint size) `hashCode` plus `equals` per node — quietly worse than the 41-cell `Math.floorMod` loop it replaced. Resolved with the documented single-slot referential fast path; the remaining per-node cost is an uncontended `@Synchronized` block plus six popcounts, against the old path's `BooleanArray(3)` allocation plus 33-45 `floorMod` calls. `SolverBudgetTest`'s wall-clock guards (2 s per solve) and the full `:domain:test` runtime (~14 s cold) are unchanged.

## TDD Gate Compliance

- **Task 1** — full RED to GREEN cycle. RED (`df84e59`) failed at `:domain:compileTestKotlin` with `Unresolved reference 'SolverTables'`; GREEN (`19a24ee`) turned all 8 tests green. No REFACTOR step needed.
- **Task 2** — full RED to GREEN cycle. RED (`afea4c4`) failed with a Truth assertion at `PagodaTest.kt:32` (a genuine behavioural failure, not a compile error) while both pre-existing tests stayed green; GREEN (`1fc353c`). No REFACTOR step needed.
- **Task 3** — property-net extension over Task 2's output, so no independent RED phase is possible; committed as a single `test(08-04)`. Its rigour comes from the deliberate-break verification instead, which is the stronger check for a net of this kind.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test` (full suite, `--rerun-tasks`) | PASS — 112 tests across 18 classes, 0 failures |
| `./gradlew :domain:test --tests '*SolverTablesTest*'` | PASS — 8 tests |
| `./gradlew :domain:test --tests '*PagodaTest*'` | PASS — 7 tests (was 2) |
| `./gradlew :domain:test --tests '*CanonicalTest*'` | PASS — 6 tests |
| `./gradlew :domain:test --tests '*SolverTest*' --tests '*SolutionTest*'` | PASS — no regression on the four one-peg boards |
| `./gradlew :app:testDebugUnitTest` | PASS |
| `./gradlew ktlintCheck detekt` | PASS |
| `grep -rn 'fun symmetryPermutations' domain/src app/src` | exactly 1 (`BoardCanonical.kt:36`) — no fourth copy |
| `grep -c '@Suppress' SolverTables.kt` | `0` |
| `grep -c 'SINGLE_PEG_ODD_CLASSES' Pagoda.kt` | `0` |
| `grep -c 'Math.floorMod' Pagoda.kt` | `0` |
| `Pagoda.kt` contains `board.spec.minimumPegs` / `countOneBits()` / `Pitfall 1` | yes (lines 78, 99, 29) |
| `SolverTables.kt` contains `MAX_CACHED_FOOTPRINTS`, `removeEldestEntry`, `BoardCanonical` | yes |
| Deliberate break (condition degenerated to always-admit) | `PagodaTest` FAILS 3/7, restored to green |
| File deletions across the plan (`git diff --diff-filter=D 1e68cb5..HEAD`) | none |

## Success Criteria

- [x] `SolverTables.forSpec(spec)` yields move/ordering/colour-class/canonicalisation tables that provably agree with `RulesEngine` on every shipped footprint, memoised on the structural footprint key under a hard cache bound
- [x] `Pagoda.mayBeSolvable` reads `spec.minimumPegs`, is computed with popcounts, is bit-for-bit unchanged for one-peg boards, and admits `Boards.diamond41Initial`
- [x] The condition still rejects genuinely-excluded positions, proven by a hand-constructed synthetic case and by the Diamond-footprint-with-target-1 case — with the honest caveat that it can only prune at a one-peg target (Deviation 1)

## Threat Model Coverage

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-08-09 (tables diverging from `RulesEngine`) | mitigate | Mitigated — tables enumerated FROM `RulesEngine`; `SolverTablesTest` asserts move-set equality and `xor`-vs-`applyMove` mask equality over 500 reachable positions on each of the five footprints. |
| T-08-10 (generalised `Pagoda` returning a false reject) | mitigate | Mitigated — `neverRejectsASolvablePosition` still runs every prefix of every committed winning line, untouched; the Diamond corpus adds 200 positions, all admitted. Extended to the Diamond floor line by plan 08-06. |
| T-08-11 (weakened prune launching a real search on every Diamond hint) | mitigate | **Partially mitigated, and worse than the plan assumed** — the prune is vacuous, not merely weak, at `minimumPegs > 1`. The node cap and deadline both still apply and `hintJob?.cancel()` still keeps one search in flight, but the opening book (08-08..08-10) is now unambiguously load-bearing rather than an optimisation. See Next Phase Readiness. |
| T-08-12 (unbounded `SolverTables` cache on Challenge footprints) | mitigate | Mitigated — access-ordered `LinkedHashMap` bounded by `MAX_CACHED_FOOTPRINTS = 6` with `removeEldestEntry`, gated by an eviction test that churns past the real bound. |
| T-08-SC (package-manager installs) | n/a | No packages installed. |

## Known Stubs

None. No placeholder values, TODO/FIXME markers or unwired components were introduced.

## User Setup Required

None — no external service configuration required. (Per-machine note, unchanged from 08-01/08-02: a fresh worktree needs its own gitignored `local.properties` with `sdk.dir` before any `:app` task will configure, and `ktlintCheck`/`detekt` span `:app`.)

## Next Phase Readiness

**Plan 08-05 (bitboard solver rewrite) has everything it needs** — `SolverTables.forSpec` delivers the exact table shapes RESEARCH Pattern 3 sketches, and `SolverTablesTest` is the gate that licenses swapping the inner loop onto them.

**Two things 08-05 must handle, both consequences of this plan:**

1. **`Solver.SOLVED_PEG_COUNT` is still a hardcoded `1`.** Until 08-05 changes the goal test to `board.spec.minimumPegs`, a Diamond-41 hint is in the worst possible state: `Pagoda` now admits it (correctly), so the search runs — but it searches for a one-peg terminal the GF(4) invariant proves unreachable, with a vacuous prune, until the 1.5 s deadline fires. Before this plan the same hint was instant and wrong; after 08-05 it will be correct; **in between it is slow and wrong.** Nothing in `:domain` or `:app` tests exercises `Solver` on Diamond-41 so the suites are green either way, but the phase must not ship at this intermediate point. This is the single most important hand-off in this summary.
2. **The per-node `Pagoda.mayBeSolvable` call should disappear.** The bitboard loop can fold the six popcounts inline against `classPlus`/`classMinus` it already holds, which removes the per-node `forSpec` lookup and its lock entirely.

**Plans 08-08 / 08-09 / 08-10 (opening book) are now load-bearing for Diamond-41, not merely a speed-up.** The colour-parity guard provably cannot prune anything at a two-peg target, so the book is the only mechanism that keeps a Diamond hint cheap and truthful.

**Plan 08-06 (Diamond floor fixture)** should extend `neverRejectsASolvablePosition` over the committed `diamond41.txt` prefixes — every prefix of the floor line must be admitted, which is the one-directionality proof for the generalised condition on the board that motivated it.

**Minor, tracked elsewhere:** `CanonicalTest.classicSpecs` still declares its own five-footprint list; 08-02's summary flagged the post-merge swap to `Boards.CLASSIC_SPECS` (the `BoardCanonical.footprintKey` half is already done). Left alone here — out of this plan's scope.

No blockers.

## Self-Check: PASSED

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverTables.kt` — FOUND
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTablesTest.kt` — FOUND
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt` — FOUND
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt` — FOUND
- Commits `df84e59`, `19a24ee`, `afea4c4`, `1fc353c`, `968e04e` — all FOUND in `git log`
- Every task `<acceptance_criteria>` re-run and passing; the plan-level `<verification>` commands re-run and green
- No file deletions introduced across the plan
- No stubs, placeholders or TODO markers introduced
- No threat flags: no new network, auth, file-access or schema surface
- STATE.md and ROADMAP.md untouched (parallel worktree mode — the orchestrator owns those writes)

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-29*
