---
phase: 08-assist-generator-quality-debt
plan: 06
subsystem: testing
tags: [kotlin, beam-search, gf4-invariant, diamond41, golden-fixtures, junit4, truth, authoring-tools]

# Dependency graph
requires:
  - phase: 08-assist-generator-quality-debt
    provides: "08-01 — BoardSpec.minimumPegs, Boards.Diamond41 target, BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR"
  - phase: 08-assist-generator-quality-debt
    provides: "08-02 — BoardCanonical.canonicalFast/chunkTables, the single canonicaliser AuthoringSolver already delegates to"
  - phase: 07-classic-catalog
    provides: "the five shipped Classic footprints and the recorded-solution fixture pipeline"
provides:
  - "BeamSearchAuthoring — a deterministic, mobility-ordered, width-parameterised beam search in the :domain test source set"
  - "domain/src/test/resources/golden/solutions/diamond41.txt — the committed 38-move line from the centre start to a two-peg dead end"
  - "DiamondFloorOptimalityTest — exhaustive GF(4) exclusion of all 41 single-peg positions, with positive and negative controls"
  - "SolutionTest.assertReplayReachesBoardTarget — target-aware replay asserting spec.minimumPegs across all five boards"
  - "SolutionFixtures.ALL (now five boards) and SolutionFixtures.ONE_PEG (the four one-peg boards)"
  - "DEBT-02 discharged: F = 2 is a proven fact, reachable by witness and optimal by invariant"
affects: [08-04-pagoda-generalisation, 08-05-bitboard-solver, 08-07-star-bands, 08-09-opening-book, 08-10-opening-book]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Authoring-time incomplete search + CI-time complete verification: the beam proposes a witness, RulesEngine.legalMoves re-validates every step on replay"
    - "Per-ply LinkedHashSet dedup over a deterministically ordered frontier as the byte-stability mechanism for a randomised search"
    - "Parent-pointer search nodes (pegs mask + link) instead of per-node path copies, so a level-synchronous beam is memory-feasible"
    - "Proof tests re-derive the invariant independently of the production code they justify, and carry both a positive and a negative control"
    - "Deliberate-break verification: a proof is only accepted once it has been shown to fail against the bugs it exists to catch"

key-files:
  created:
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/BeamSearchAuthoring.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DiamondFloorOptimalityTest.kt
    - domain/src/test/resources/golden/solutions/diamond41.txt
  modified:
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverBudgetTest.kt
    - domain/build.gradle.kts

key-decisions:
  - "WIDTH pinned at 60_000, not RESEARCH's 400_000: widths 2 000 / 10 000 / 400 000 were all measured to reach the floor through this repo's own RulesEngine, and a sub-minute regeneration is what makes byte-stability a guarantee people actually re-verify"
  - "SolutionFixtures gains a ONE_PEG subset so the one-peg-semantics suites (Pagoda, Solver) stay honest while ALL becomes the true five-board catalogue"
  - "Fixture targets are read from spec.minimumPegs at the point of use rather than duplicated onto Fixture, keeping one source of truth"
  - "The GF(4) signature is re-derived inside DiamondFloorOptimalityTest and never calls Pagoda, because a proof that restates the code it justifies proves nothing"
  - "The terminal reached — {(2,2),(5,5)} — is the horizontal mirror of RESEARCH's independent witness {(2,6),(5,3)}, which is a stronger cross-check than the literal residue test the plan suggested"

patterns-established:
  - "Incomplete-search-as-author, complete-check-as-gate: an incomplete search is sound when its output is a witness that a complete checker re-validates"
  - "Assert both what survived and what was examined, so a shrinking search space cannot make an emptiness assertion vacuously true"

requirements-completed: [DEBT-02]

# Metrics
duration: 26 min
completed: 2026-08-29
---

# Phase 8 Plan 06: Diamond-41 Fewest-Pegs Floor Proof Summary

**Diamond-41's floor is now proven to be exactly 2: a committed 38-move line replays through the unchanged `RulesEngine` to a two-peg dead end at (2,2)/(5,5), and all 41 single-peg positions are shown incompatible with the centre start's GF(4) colour signature.**

## Performance

- **Duration:** 26 min
- **Started:** 2026-08-28T23:34:23Z
- **Completed:** 2026-08-29T00:00:25Z
- **Tasks:** 3 (4 commits — Task 2 ran a full RED/GREEN cycle)
- **Files modified:** 10 (3 created, 7 modified)

## Accomplishments

- **The A1 gate is open.** RESEARCH's `F = 2` came from the researcher's own scratchpad harness. It is now a fact established inside this repository: the line was generated against the real `Boards.diamond41Initial` and every one of its 38 moves is re-validated against `RulesEngine.legalMoves` on the board it acts upon before being applied. Plan 08-07 may anchor star bands on it.
- **Both halves of D-05 are closed.** Reachability by the committed witness; optimality because `F - 1 = 1` is exactly what the colour invariant excludes, verified exhaustively — 0 of 41 single-peg positions survive.
- **The terminal independently corroborates RESEARCH.** The beam landed on `{(2,2),(5,5)}`, which is the reflection `(r, 8-c)` of RESEARCH's canonical witness `{(2,6),(5,3)}` — a different search, in different code, arriving at a symmetry image of the same answer.
- **The floor is a real floor, not a stopping point.** `RulesEngine.legalMoves` on the terminal is empty. (This is forced, and the test proves it rather than assuming it: a reachable 2-peg position with a legal move would yield a reachable 1-peg position, which the invariant forbids.)
- **A normal CI run still performs zero search.** `./gradlew :domain:test` is ~12 s and leaves `domain/src/test/resources` byte-identical; the beam and the 8 GB heap both live behind the existing `UPDATE_GOLDEN` conditional.
- **The last two stale "several pegs" surfaces are gone.** `grep -rn 'several pegs' domain/src app/src` returns nothing repo-wide, closing Pitfall 7 across all surfaces (08-01 took `Boards.kt`, this plan took the two test-source KDocs deferred to it).

## Task Commits

1. **Task 1: beam-search authoring engine and Diamond fixture recipe** — `a0eb7f0` (feat)
2. **Task 2 (RED): failing target-aware replay for the Diamond floor** — `b3d7832` (test)
3. **Task 2 (GREEN): commit the Diamond-41 floor line reaching exactly 2 pegs** — `c253adf` (feat)
4. **Task 3: prove one peg is unreachable from the centre start** — `f1808de` (test)

_No REFACTOR step was needed; the GREEN implementation is a generated data file._

## The generated line (recorded per the plan's request)

| Property | Value |
|----------|-------|
| Move count | **38** |
| Terminal peg count | **2** (`== Boards.Diamond41.minimumPegs == DIAMOND41_FEWEST_PEGS_FLOOR`) |
| Terminal peg cells | **(2,2)** and **(5,5)** |
| Terminal legal moves | **0** — a genuine dead end |
| Authoring wall-clock | **3 min 4 s** first run, **3 min 8 s** second run (whole `SolutionTest` regeneration: all five fixtures, of which the Diamond beam is ~40 s) |
| Byte-stability | Second run reproduced all five fixtures byte-identically (`git status --porcelain domain/src/test/resources` empty) |
| Beam parameters | `WIDTH = 60_000`, `BASE_SEED = 0x0D1A_4041` |

**Parity cross-check (computed independently of the Kotlin code, by replaying the committed file in Python):**

| Colouring | Start parity | Terminal parity | Moves | Flip |
|-----------|--------------|-----------------|-------|------|
| `(r+c) mod 3` | `[0,0,0]` | `[0,0,0]` | 38 | even → none |
| `(r-c) mod 3` | `[0,0,0]` | `[0,0,0]` | 38 | even → none |

The invariant is conserved exactly. Both terminal pegs share a residue class under each colouring — `(r+c) ≡ 1` and `(r-c) ≡ 0` — which is the condition that makes the GF(4) value zero, matching the start.

**Note on the plan's literal sanity check.** The plan asked that both terminal cells satisfy `(r+c) mod 3 == 2` and `(r-c) mod 3 == 2`. They do not — they sit in classes 1 and 0. That check transcribed the *specific residues of RESEARCH's representative* rather than the invariant itself. The conserved quantity is "both pegs in the **same** class under each colouring" (equivalently, GF(4) value 0, matching the start's `[0,0,0]`); which class is not fixed, because a symmetry transform permutes the classes. The terminal here is the horizontal mirror of RESEARCH's witness, so it is the *same* answer with permuted residues — corroboration, not a red flag. `DiamondFloorOptimalityTest.theCommittedTwoPegTerminalIsCompatibleWithTheCentreStart` asserts the correct, symmetry-robust form of this check in CI.

## Files Created/Modified

- `domain/src/test/kotlin/.../golden/solutions/BeamSearchAuthoring.kt` — **created.** `internal object` exposing `beamSolve(start, targetPegs, width, seed)`. Level-synchronous best-first search over `RulesEngine.legalMoves`/`applyMove`, per-ply dedup by `BoardCanonical.canonicalFast`, survivors ranked by mobility with the top half kept strictly and the remainder sampled by an in-place partial Fisher-Yates from one seeded `Random`. Nodes hold `(pegs, move, parent)` rather than path copies. Object KDoc states the incompleteness contract and enumerates every pinned ordering that makes byte-stability hold.
- `domain/src/test/resources/golden/solutions/diamond41.txt` — **created.** 38 lines in `BoardCodec.encodeMoves` format, generated under `UPDATE_GOLDEN=true`, never hand-edited.
- `domain/src/test/kotlin/.../solver/DiamondFloorOptimalityTest.kt` — **created.** 6 tests: the exhaustive 41-position exclusion (asserting both `0` survived and `41` examined), the all-even start signature, invariant conservation over 200 seeded random play-outs, the positive control, the negative control, and a boundary test showing the exclusion stops at one peg (two-peg finishes *are* admitted).
- `domain/src/test/kotlin/.../golden/solutions/AuthoringSolver.kt` — **modified.** `SolutionFixtures.ALL` gains `diamond41`; new `ONE_PEG` subset; `writeFixtures` solves each board to `fixture.spec.minimumPegs` via a new `solveToTarget` that prefers the DFS for one-peg targets and falls back to the beam; two stale KDocs corrected.
- `domain/src/test/kotlin/.../golden/solutions/SolutionTest.kt` — **modified.** `assertReplayReducesToOnePeg` → `assertReplayReachesBoardTarget` asserting `initial.spec.minimumPegs`; `SOLVED_PEG_COUNT` deleted; replay extracted to `replayFixture` so the dead-end test reuses the same step-validated fold; three new Diamond tests; class KDoc corrected.
- `domain/src/test/kotlin/.../solver/CanonicalTest.kt` — **modified.** Golden-prefix sweep narrowed to `ONE_PEG` in Task 1 and widened back to `ALL` in Task 2, so it now exercises real 41-hole positions (the Pitfall 4 bit-40 case).
- `domain/src/test/kotlin/.../solver/PagodaTest.kt`, `SolverTest.kt`, `SolverBudgetTest.kt` — **modified (one identifier each).** `SolutionFixtures.ALL` → `ONE_PEG`; see deviation 1.
- `domain/build.gradle.kts` — **modified.** `UPDATE_GOLDEN` heap `4g` → `8g`, inside the existing conditional, with the reason recorded.

## Decisions Made

- **`WIDTH = 60_000`, not RESEARCH's 400 000.** I measured three widths against `Boards.diamond41Initial` through this repo's own `RulesEngine`: 2 000 reaches the floor in ~1.5 s, 10 000 in ~6 s, 400 000 in ~277 s. Every one lands on a 38-move line at a dead end. 400 000 buys nothing for this board and makes a full regeneration ~7 minutes, which quietly turns "regenerates byte-identically" into a claim nobody re-checks. 60 000 is a 30× margin over the smallest measured success and keeps the Diamond beam at ~40 s. `width` remains a parameter, and the 8 GB heap is retained so a future board can be given a much larger beam without a build change.
- **`ONE_PEG` as a named subset rather than filtering at each call site.** Three suites in the solver package are specifications about *one-peg* solvability. Diamond-41 is correctly excluded by that question, so feeding it in would assert the opposite of what the invariant proves. A named list with a KDoc explaining why — and naming the plans that will retire it — beats four copies of `.filter { ... }`.
- **The optimality proof re-derives the invariant instead of calling `Pagoda`.** `Pagoda` is production code this phase modifies (plan 08-04 generalises it). A test that delegated to it would pass by construction and prove nothing about the mathematics.
- **The dead-end assertion is kept even though it is forced.** A reachable 2-peg position with a legal move would give a reachable 1-peg position, which the invariant forbids — so the dead end is a theorem, not a coincidence. It is still asserted, because "the fixture is a floor witness" and "the fixture stopped early" are indistinguishable without it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Adding `diamond41` to `SolutionFixtures.ALL` breaks four suites that iterate it**

- **Found during:** Task 1 (fixture-catalogue entry)
- **Issue:** The plan's `files_modified` covers only the fixture pipeline, but `SolutionFixtures.ALL` is iterated by `PagodaTest`, `SolverTest`, `SolverBudgetTest` and `CanonicalTest`. The first three are one-peg specifications: `Pagoda.mayBeSolvable` asks "can a single peg still be reached", and `Solver.firstWinningMove` searches for a one-peg line. Both correctly answer "no" for every Diamond-41 position, so adding the diamond would have made those suites assert the negation of the very invariant this plan proves. `CanonicalTest` additionally loads every fixture file, which does not exist yet at Task 1.
- **Fix:** Added `SolutionFixtures.ONE_PEG` (the subset whose `spec.minimumPegs` is the default 1) and pointed the three one-peg-semantics suites at it — a single identifier change in each, with a comment naming the plan that will widen it back (08-04 for the pruner, 08-05 for the solver). `CanonicalTest` was narrowed to `ONE_PEG` in Task 1 and widened back to `ALL` in Task 2 once `diamond41.txt` existed, so it now covers the 41-hole footprint.
- **Files modified:** `AuthoringSolver.kt`, `PagodaTest.kt`, `SolverTest.kt`, `SolverBudgetTest.kt`, `CanonicalTest.kt`
- **Verification:** `./gradlew :domain:test` green at every commit in this plan.
- **Committed in:** `a0eb7f0` (Task 1), widened in `c253adf` (Task 2)
- **Coordination note:** `PagodaTest.kt` is owned by sibling wave-2 plan **08-04**, and `SolverTest.kt`/`SolverBudgetTest.kt` by wave-3 plan **08-05**. Each change is one identifier plus a comment, so a merge conflict is trivial to resolve — and in both cases the correct resolution is the *other* plan's, which replaces `ONE_PEG` with the generalised per-target form.

**2. [Rule 3 - Blocking] `detekt` `ReturnCount` on `beamSolve`**

- **Found during:** Task 1 (lint gate)
- **Issue:** `beamSolve` had three returns (early target hit, goal found mid-ply, frontier exhausted); the project limit is two.
- **Fix:** Split the ply loop into a private `descend(...)`, which also gave the "an empty frontier means *this beam* found nothing, not that the target is unreachable" caveat a natural place to live.
- **Files modified:** `BeamSearchAuthoring.kt`
- **Verification:** `./gradlew ktlintCheck detekt` green.
- **Committed in:** `a0eb7f0`

**3. [Rule 3 - Blocking] Missing `local.properties` in the worktree**

- **Found during:** Plan-level verification
- **Issue:** `:app:testDebugUnitTest` cannot configure — "SDK location not found". `local.properties` is gitignored and absent from a fresh worktree (a known property of worktree execution, hit by 08-01 and 08-02 too).
- **Fix:** Copied the main checkout's `local.properties` into the worktree. **Not committed** — gitignored by design, per-machine.
- **Verification:** `./gradlew :app:testDebugUnitTest` green.
- **Committed in:** n/a (gitignored)

---

**Total deviations:** 3 auto-fixed (3 blocking)
**Impact on plan:** None on scope or deliverables. Deviation 1 is the only one that touches files this plan does not own, and it is one identifier per file with a comment pointing at the plan that supersedes it. No production (`domain/src/main`, `app/src/main`) file was modified by this plan at all.

## Issues Encountered

- **Task 3's RED gate passed on first run.** Task 3 is `tdd="true"`, but it adds no behaviour — it re-derives a mathematical property of `RulesEngine`'s jump rule that has always held. There is no implementation to write; the proof *is* the test. Per the fail-fast rule I did not proceed blind: instead of a RED commit I ran **deliberate-break verification**, the discipline plan 08-02 established for property nets. Forcing the forward projection off fails 2 of the 6 tests; counting holes instead of pegs fails 3. The net has teeth. See TDD Gate Compliance.
- **The `UPDATE_GOLDEN` run reports failures on the same pass that generates the fixture.** JUnit runs the replay tests before `regenerateSolutionFixturesWhenUpdateFlagSet` within one class, so the first authoring run legitimately shows the two Diamond tests failing while writing the file they need. A subsequent normal run is green. This is pre-existing pipeline behaviour, not a regression; the second authoring run (for byte-stability) was green end-to-end because the fixture already existed.

## TDD Gate Compliance

- **Task 2** — full RED → GREEN cycle observed. RED (`b3d7832`) failed with exactly the two fixture-dependent tests (`diamond41RecordedSolutionReducesToItsProvenTwoPegFloor`, `diamond41TerminalIsAGenuineDeadEnd`) while `diamond41TargetIsTheFrozenProvenFloor` passed, since 08-01 had already landed the constant. GREEN (`c253adf`) turned them green. No REFACTOR needed.
- **Task 3** — proof/lock task with no behaviour to add, so no RED phase is possible (see Issues Encountered). Committed as `test(08-06)` and validated by deliberate breaks instead.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test` (full suite) | PASS |
| `./gradlew :domain:test --tests '*SolutionTest*'` | PASS — five boards replay to their own targets |
| `./gradlew :domain:test --tests '*DiamondFloorOptimalityTest*'` | PASS — 6 tests |
| `./gradlew ktlintCheck detekt` | PASS |
| `./gradlew :app:testDebugUnitTest` | PASS |
| `git status --porcelain domain/src/test/resources` after a normal run | empty — CI does not regenerate |
| Second `UPDATE_GOLDEN=true` run | all five fixtures byte-identical |
| `grep -rn 'BeamSearchAuthoring' domain/src/main` | no matches |
| `grep -rn 'several pegs' domain/src app/src` | no matches |
| Malformed lines in `diamond41.txt` | 0 of 38 |
| `grep -c 'SOLVED_PEG_COUNT' SolutionTest.kt` | 0 |
| `Pagoda.` in executable lines of `DiamondFloorOptimalityTest.kt` | 0 |
| Deliberate break: forward projection removed | FAILS 2/6 tests |
| Deliberate break: holes counted instead of pegs | FAILS 3/6 tests |
| New `@Suppress` annotations | 0 |

## Success Criteria

- [x] `diamond41.txt` is committed, regenerates byte-identically, and replays step-by-step through the unchanged `RulesEngine` to a dead-end board holding exactly 2 pegs
- [x] All 41 single-peg Diamond-41 positions shown incompatible with the centre start's GF(4) signature, with positive and negative controls
- [x] The four existing fixtures still replay to 1 peg, asserted against `spec.minimumPegs` rather than a duplicated literal
- [x] A normal CI run performs zero search; the beam exists only in the test source set
- [x] No file in the repository still claims the Diamond floor is "several pegs"

## Threat Model Coverage

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-08-17 (tampered fixture) | mitigate | Mitigated — generated by tooling against the real `Boards.Diamond41`, never hand-written; every replayed step is re-validated against `RulesEngine.legalMoves` before it is applied; byte-stability verified by a second authoring run. |
| T-08-18 (beam reaches production) | mitigate | Mitigated — `internal object` in the `:domain` test source set, KDoc states it is incomplete and can never prove unsolvability, `grep -rn 'BeamSearchAuthoring' domain/src/main` returns nothing, `DomainPurityTest` remains the structural gate. |
| T-08-19 (proof restates the implementation) | mitigate | Mitigated — the GF(4) signature is re-derived in `DiamondFloorOptimalityTest`, which never calls `Pagoda`; positive and negative controls are present; the proof was verified to fail under two deliberate breaks. |
| T-08-20 (8 GB multi-minute search in CI) | mitigate | Mitigated — generation runs only under `UPDATE_GOLDEN=true`, the heap bump is inside that conditional, and a normal `:domain:test` run is ~12 s and leaves the fixture untouched. |
| T-08-SC (package-manager installs) | n/a | No packages installed. |

## Known Stubs

None. No placeholder values, TODO markers, or unwired data paths were introduced.

## Threat Flags

None. This plan modifies no file under `domain/src/main` or `app/src/main` — no new network, auth, file-access, or schema surface.

## User Setup Required

None — no external service configuration required. (Per-machine note, unchanged from 08-01/08-02: a fresh worktree needs its own gitignored `local.properties` with `sdk.dir` before `:app` tasks will configure.)

## Next Phase Readiness

- **Plan 08-07 (star bands) is unblocked.** This was the A1 gate. `F = 2` is now proven inside this repository, so Diamond's star bands may be anchored on `spec.minimumPegs` / `DIAMOND41_FEWEST_PEGS_FLOOR`. The escalation clause did **not** fire.
- **DEBT-02 is fully discharged.** 08-01 landed the frozen constant and the per-board target; this plan supplies the committed golden line proving reachability plus the exhaustive optimality check. The orchestrator may now mark DEBT-02 complete in `REQUIREMENTS.md`.
- **Deferred item closed.** `deferred-items.md`'s Pitfall 7 surfaces #2 and #3 are marked resolved with their commit hashes.
- **Follow-ups for the plans that own the affected suites (both trivial, both non-blocking):**
  - **08-04** — once `Pagoda` takes a per-board target, revert `PagodaTest`'s `SolutionFixtures.ONE_PEG` to `ALL`; the diamond should then be accepted rather than rejected.
  - **08-05** — once `Solver`'s goal test reads `spec.minimumPegs`, revert `SolverTest` and `SolverBudgetTest` to `ALL`. Note for `SolverBudgetTest`: adding Diamond-41 will change `maxObservedNodes`, so re-read Pitfall 2 before touching `HEADROOM_FACTOR` — the standing instruction is to raise `DEFAULT_MAX_NODES`, never to lower the assertion.
- **Available to 08-09/08-10 (opening book):** `BeamSearchAuthoring.beamSolve` is the general authoring engine for book lines, and the 8 GB `UPDATE_GOLDEN` heap is already sized for it.
- No blockers.

## Self-Check: PASSED

- All 6 key files verified present on disk (`BeamSearchAuthoring.kt`, `diamond41.txt`, `DiamondFloorOptimalityTest.kt`, `SolutionTest.kt`, `AuthoringSolver.kt`, `build.gradle.kts`).
- All 4 task commits verified in `git log` (`a0eb7f0`, `b3d7832`, `c253adf`, `f1808de`).
- No file deletions across the plan (`git diff --diff-filter=D --name-only 1e68cb5..HEAD` empty).
- No untracked files left behind; the scratch width probe was deleted before Task 1's commit and `local.properties` is gitignored by design.
- All task `<acceptance_criteria>` re-run and passing; the plan `<verification>` block re-run and green.
- `STATE.md` and `ROADMAP.md` untouched, per the parallel-execution contract.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-29*
