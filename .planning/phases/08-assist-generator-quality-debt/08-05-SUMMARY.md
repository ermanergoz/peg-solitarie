---
phase: 08-assist-generator-quality-debt
plan: 05
subsystem: solver
tags: [kotlin, bitboard, solver, restart-policy, heavy-tailed-search, pagoda, junit4, truth]

# Dependency graph
requires:
  - phase: 08-04
    provides: "SolverTables (needMask/toMask/xorMask/moveOrder/classPlus/classMinus/chunkTables) and a target-aware Pagoda"
  - phase: 08-01
    provides: "BoardSpec.minimumPegs plus its `minimumPegs in 1..size` construction guard"
  - phase: 08-02
    provides: "BoardCanonical.canonicalFast/chunkTables — the memo key"
  - phase: 08-06
    provides: "SolutionFixtures.ALL/ONE_PEG and the committed diamond41.txt floor line"
provides:
  - "An allocation-free bitboard search: three mask operations per node, no RulesEngine call, no Board copy, no per-node List<Move> sort"
  - "A per-board goal test (`pegCount <= spec.minimumPegs`) replacing the hardcoded SOLVED_PEG_COUNT"
  - "A working restart policy: per-attempt node caps that escalate geometrically, with maxNodes as the cumulative ceiling"
  - "SolverBudget.perAttemptMaxNodes and Solver.MAX_CAP_ESCALATIONS — the restart-frequency and memory knobs, both measurement-documented"
  - "Pagoda.mayBeSolvable(pegs, pegCount, target, tables) — a per-node entry point with no cache lookup or lock"
  - "SolverTest: European-37 and Asymmetric-39 full starts proven inside the unchanged 1.5s deadline (DEBT-01), plus determinism and restart-reachability"
  - "SolverBudgetTest re-anchored on the widened five-board corpus, DEFAULT_MAX_NODES held at 40M"

affects: [08-07-star-bands, 08-08-opening-book, 08-09-opening-book, 08-10-opening-book]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Geometric restart schedule for heavy-tailed search: cap the attempt, then grow the cap — a flat cap can never solve instances whose solution lies past it"
    - "Bound memory by the per-attempt cap, not the cumulative ceiling, when the memo is discarded between attempts"
    - "A second entry point onto one implementation (Pagoda's mask overload) rather than a second implementation, when a hot path must skip a cache lookup"
    - "Deliberate-break RED: run new tests against the previous implementation restored via `git show`, rather than skipping the RED gate"

key-files:
  created: []
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverBudget.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverBudgetTest.kt

key-decisions:
  - "The per-attempt cap ESCALATES geometrically rather than staying flat: a flat 200k cap regressed Wiegleb-45's mid-line position, which needs one 830k-node dive it completes in ~269ms"
  - "Escalation is bounded at 4 doublings, because the memo is per-attempt and an unbounded escalation to the 40M ceiling is a 1 GB allocation — observed as a real OutOfMemoryError"
  - "Pagoda gains a tables-taking overload instead of the parity test being duplicated into the search loop"
  - "Per-attempt static move ordering (one seeded bounded-displacement shuffle) replaces the per-node randomised sort — the allocation was the point"
  - "Diamond-41's full start is deliberately NOT asserted: measured right ON the 1.5s deadline, so pinning it would be a flaky test rather than a contract"
  - "DEFAULT_MAX_NODES held at 40M — re-measured maximum is ~48k, leaving ~836x headroom, so Pitfall 2's raise-never-lower instruction did not fire"

patterns-established:
  - "Restart schedules must have unbounded-in-principle run lengths (Luby/Sinclair/Zuckerman); a bounded-run-length schedule is incomplete by construction"
  - "State which resource each budget field actually bounds, separately, when they are not the same resource"

requirements-completed: [DEBT-01]

# Metrics
duration: 19 min
completed: 2026-08-29
---

# Phase 8 Plan 05: Bitboard Solver and Restart Policy Summary

**The hint search now runs allocation-free over `SolverTables` against each board's own `spec.minimumPegs` target, with per-attempt node caps that escalate geometrically — European-37's full start goes from "no winning move" to a real move in ~84ms, and Asymmetric-39's in under 1ms, against the untouched 1.5s deadline.**

## Performance

- **Duration:** 19 min
- **Started:** 2026-08-29T00:04:37Z
- **Completed:** 2026-08-29T00:23:06Z
- **Tasks:** 3 (4 commits — Task 3's fix was committed separately from its tests)
- **Files modified:** 5 (0 created, 5 modified)

## Accomplishments

- **DEBT-01's two solvable boards are fixed, and proven fixed.** European-37 and Asymmetric-39 full starts folded to "no winning move" before this plan. They now return a legal winning move in ~84ms and <1ms respectively against a 1500ms ceiling — an 18x and >100x margin, so `SolverTest`'s new cases assert a capability envelope rather than a stopwatch reading. Both were verified RED against the previous implementation before being accepted.
- **The Diamond-41 hazard 08-04 handed forward is closed.** Since 08-04 landed, `Pagoda` correctly admitted Diamond-41 but `Solver` still hunted a GF(4)-impossible one-peg terminal until the deadline fired — "slow and wrong", in 08-04's words, with no CI test exposing it. The goal test now reads `spec.minimumPegs`, and every Diamond position after the opening resolves in tens of milliseconds (mid-line ~29ms, endgame tail ≤1ms).
- **The restart loop is genuinely reachable, and there is a test that fails if it stops being.** `MAX_ATTEMPTS = 40` was unreachable dead code in production: every attempt was handed the full 40M budget, so the deadline always fired inside attempt 0. `aPositionAbandonedByItsFirstAttemptIsStillDecidedByARestart` drives the corpus at a deliberately tiny per-attempt cap and asserts positions resolve *having exceeded that cap* — which only a later attempt can do.
- **A flat cap was measured to be wrong, and the schedule was corrected.** The plan specified a flat per-attempt cap. That regressed Wiegleb-45's mid-line position, which needs a single 830 574-node dive it completes in ~269ms: under a flat 200k cap none of the 40 orderings reach it, so the solver burned ~2.1s to answer "no winning move" about a position it can decide well inside the deadline. Geometric escalation keeps the cheap early restarts *and* reaches the deep dives (see Deviations).
- **A real `OutOfMemoryError` was found and fixed.** Unbounded escalation reaches the 40M cumulative ceiling, and a 40M-entry `VisitedSet` is a 1 GB `long[]` (~1.5 GB while doubling). Because the memo is per-attempt, memory is bounded by the *largest attempt*, not the total — so the escalation is now capped at 4 doublings. The KDocs no longer claim `maxNodes` bounds memory; they say plainly that it bounds time and `perAttemptMaxNodes` bounds memory.
- **The node-headroom contract was re-anchored with its measurement recorded, not silently edited.** Re-measured maximum `Solver.lastVisited` over the widened five-board corpus is **47 794** (english33's full start), leaving **~836x** headroom against the unchanged 40 000 000 ceiling. `HEADROOM_FACTOR` stays `4L`; `DEFAULT_MAX_NODES` was neither raised nor lowered.
- **08-06's `ONE_PEG` narrowing is retired in this plan's files.** `SolverTest` and `SolverBudgetTest` iterate `SolutionFixtures.ALL` again, and the win-preserving assertion reads `spec.minimumPegs` instead of a duplicated one-peg literal — the end-state 08-06's summary specified.

## Task Commits

1. **Task 1: per-attempt node cap on `SolverBudget`** — `eb8c84a` (feat)
2. **Task 2: bitboard search on a per-board goal with a working restart loop** — `8ec3bfa` (perf)
3. **Task 3 (fix): bound the escalation so peak memory stays bounded** — `00e2638` (fix)
4. **Task 3 (test): prove the envelope moved, re-anchor node headroom** — `6d0765a` (test)

## Measurements

**Full-footprint starts, driven through the production envelope (nanotime `shouldCancel` closed over the 1500ms `DEFAULT_DEADLINE`):**

| Board | Wall clock | `lastVisited` | Asserted in CI? |
|-------|-----------|---------------|-----------------|
| English-33 | 133–342 ms (varies with JIT warm-up position) | 47 794 | yes — control |
| European-37 | 82–123 ms | 130 894 | **yes — DEBT-01** |
| Asymmetric-39 | <1 ms | 1 010 | **yes — DEBT-01** |
| Diamond-41 | ~1181 ms in one run, deadline miss in another | 332 225 | **no — on the deadline, see below** |
| Wiegleb-45 | not attempted | — | no — outside by ~4 orders of magnitude |

**Mid-line positions (half the committed line replayed), no caller deadline:**

| Board | Wall clock |
|-------|-----------|
| English-33 | 16 ms |
| European-37 | 33 ms |
| Asymmetric-39 | 86 ms |
| Diamond-41 | 29 ms |
| Wiegleb-45 | 724–927 ms |

**Node headroom (`SolverBudgetTest` corpus, widened to all five boards):**

| Quantity | Value |
|----------|-------|
| Maximum observed `Solver.lastVisited` | **47 794** (english33@start) |
| `DEFAULT_MAX_NODES` | 40 000 000 (unchanged) |
| Observed headroom | **~836x** against a required 4x |

## Files Created/Modified

- `domain/src/main/kotlin/.../solver/Solver.kt` — **modified (rewritten search).** `firstWinningMove`'s goal test becomes `board.pegCount <= board.spec.minimumPegs`; `SOLVED_PEG_COUNT` deleted. `WinningLineSearch` recurses on `(pegs: Long, pegCount: Int, depth: Int)` over `SolverTables`; `search`/`expand`/`isLegal`/`pollCancellation`/`reorderedForThisAttempt` are each small enough that detekt is satisfied without any `@Suppress`. `searchWithRestarts` gives each attempt its own cap, escalating by `ATTEMPT_CAP_GROWTH = 2L` up to `MAX_CAP_ESCALATIONS = 4` doublings, with `budget.maxNodes` as the cumulative terminator. `centroidOf`/`inwardness`/`distanceSquared` deleted — that logic now lives once, in `SolverTables.moveOrder`.
- `domain/src/main/kotlin/.../solver/SolverBudget.kt` — **modified.** New `perAttemptMaxNodes` field (last, defaulted) and `DEFAULT_PER_ATTEMPT_MAX_NODES = 200_000L`. `DEFAULT_DEADLINE` unchanged at `1_500.milliseconds`, its KDoc now naming D-07. `DEFAULT_MAX_NODES` unchanged at `40_000_000L`, its KDoc now resolving Pitfall 2 explicitly.
- `domain/src/main/kotlin/.../solver/Pagoda.kt` — **modified (additive).** New `internal mayBeSolvable(pegs, pegCount, target, tables)`; the `Board` overload became a thin delegation to it. One implementation, two entry points.
- `domain/src/test/kotlin/.../solver/SolverTest.kt` — **modified.** 3 tests to 8. Three full-start cases, a determinism case, a restart-reachability case; the mid-game sweep widened to `ALL` and made target-aware. Class KDoc records both deliberate omissions.
- `domain/src/test/kotlin/.../solver/SolverBudgetTest.kt` — **modified.** Corpus widened to `ALL`; class KDoc restates what `lastVisited` counts after the rewrite, records the re-measured maximum and re-states Pitfall 2's standing instruction.

## Decisions Made

- **The escalation is geometric, not flat.** See Deviation 1 — this is the substantive decision of the plan and it is measurement-driven.
- **Escalation bounded at 4 doublings.** The largest attempt is 16x `perAttemptMaxNodes` (3.2M positions, ~67 MB memo) — above the deepest dive the corpus needs (~830k) and above what 1.5s can afford at the measured node rate, so the bound is free in production while restoring a real memory guarantee.
- **`Pagoda` gained an overload rather than the search gaining a copy of the parity test.** The hot loop must not pay `SolverTables.forSpec`'s `List<Position>` cache lookup and lock per node, but duplicating the invariant would leave two places to fix it. An `internal` entry point taking already-resolved tables gets both.
- **Per-attempt static ordering, not per-node randomisation.** One seeded bounded-displacement shuffle of `tables.moveOrder` per attempt. The property preserved is "each restart explores a genuinely different near-inward ordering"; because the base order is inwardness-sorted, a displacement bounded to `ORDER_JITTER_WINDOW = 8` can only exchange near-equal-inwardness moves, which is exactly what `inwardness + random * INWARD_JITTER` did — minus the per-node allocation that was the whole problem.
- **Diamond-41's full start is not asserted.** Measured at ~1181ms in one run and a deadline miss in another on the same machine, because `Pagoda`'s prune is provably vacuous at a two-peg target (08-04). Asserting it would be a coin-flip test. Recorded in `SolverTest`'s KDoc as evidence that the opening book is load-bearing for the diamond, not an optimisation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] A flat per-attempt cap makes the solver lie about Wiegleb-45's mid-line position**

- **Found during:** Task 2 (verification — `SolverTest.returnsALegalWinPreservingMoveFromASolvableMidGameState` failed)
- **Issue:** The plan specifies "each attempt is constructed with `budget.perAttemptMaxNodes` as its node cap". Implemented literally, Wiegleb-45's mid-line position (23 pegs) regressed from a returned move to `null`. Measured cause: that position needs a single **830 574**-node dive, which it completes in **269ms**. Under a flat 200k cap no ordering can reach it, so the solver spent **~2 116ms** across all 40 attempts and answered "no winning move" — worse than the shipped behaviour in both truthfulness *and* latency, and a direct violation of the plan's own must-have "the solver still returns `null` only for positions it genuinely could not solve". This is the classic Luby/Sinclair/Zuckerman result: a restart schedule whose run lengths are bounded can never solve instances whose solution lies past the bound.
- **Fix:** `perAttemptMaxNodes` becomes the FIRST attempt's cap, and each subsequent attempt doubles it (`ATTEMPT_CAP_GROWTH = 2L`). The plan's intent — cheap early restarts so one unlucky ordering cannot consume the deadline — is fully preserved, since attempts 0-2 are still 200k/400k/800k. Measured after the fix: Wiegleb-45 mid-line resolves in ~724-927ms, and European-37's and Asymmetric-39's starts are unaffected (they decide in attempts 0-1).
- **Files modified:** `Solver.kt`
- **Verification:** `SolverTest` and the full `:domain:test` suite green; per-cap sweep recorded above.
- **Committed in:** `8ec3bfa` (Task 2)

**2. [Rule 2 - Missing Critical] Unbounded cap escalation exhausts the heap**

- **Found during:** Task 3 (measuring the restart-reachability corpus)
- **Issue:** With escalation unbounded, a Diamond-41 search called without a caller deadline escalates to `budget.maxNodes = 40M` and dies with `java.lang.OutOfMemoryError` in `VisitedSet.grow`. A 40M-entry memo needs a 134M-slot `long[]` at the 0.5 load factor — **1 GB**, and ~1.5 GB while the old and new arrays coexist during a grow. `SolverBudget`'s KDoc claimed `maxNodes` "makes the WORST case bounded in memory as well as time", which was simply false — and threat T-08-14's disposition depends on it being true. It only became *reachable* now, because Diamond-41 is searched at all for the first time.
- **Fix:** `MAX_CAP_ESCALATIONS = 4` bounds the largest attempt at 16x `perAttemptMaxNodes`. The memo is per-attempt and discarded between restarts, so this is what actually bounds peak heap. Both KDocs corrected to state which resource each field bounds.
- **Files modified:** `Solver.kt`, `SolverBudget.kt`
- **Verification:** the OOM reproducer (Diamond-41 at a tiny cap with no deadline) now terminates cleanly; full `:domain:test` and `:app:testDebugUnitTest` green.
- **Committed in:** `00e2638`

**3. [Rule 3 - Blocking] The per-node parity test needs `Pagoda` to accept a mask**

- **Found during:** Task 2
- **Issue:** The plan requires the loop to "fold the popcounts inline… and avoid per-node `List`-keyed memo lookups" (08-04's hand-off). `Pagoda.mayBeSolvable(board)` takes a `Board` and calls `SolverTables.forSpec`, whose key is a `List<Position>` behind a lock — exactly the per-node cost the tables exist to remove. Inlining the popcounts into `Solver.kt` would have created a second copy of the colour-parity invariant, against CLAUDE.md and against 08-04's "single definition" discipline. `Pagoda.kt` is not in this plan's `files_modified`.
- **Fix:** Added `internal fun mayBeSolvable(pegs, pegCount, target, tables)` to `Pagoda` and made the `Board` overload delegate to it. Additive: no existing caller or test changed, and both entry points run the identical `terminalParityFitsTarget` pair. Verified no sibling wave-3 plan (08-07) touches `Pagoda.kt`, so there is no merge exposure.
- **Files modified:** `Pagoda.kt`
- **Verification:** `PagodaTest` (7 tests, owned by 08-04) green unchanged; `ktlintCheck detekt` green.
- **Committed in:** `8ec3bfa` (Task 2)

**4. [Rule 3 - Blocking] `detekt` `LoopWithTooManyJumpStatements` on the move loop**

- **Found during:** Task 2 (lint gate)
- **Issue:** The natural transcription of RESEARCH's hot-loop sketch has one `continue` and two `break`s; the project limit is one jump.
- **Fix:** Moved the exit conditions into the `while` guard (`index < order.size && !solved && !cancelled`) and extracted `isLegal` and `pollCancellation`. Zero jump statements, and the cancel-poll idiom `(nodeCounter++ and SolverBudget.CANCEL_MASK) == 0L` survives verbatim. No `@Suppress` added.
- **Files modified:** `Solver.kt`
- **Verification:** `./gradlew ktlintCheck detekt` green.
- **Committed in:** `8ec3bfa` (Task 2)

**5. [Rule 3 - Blocking] Missing `local.properties` in the worktree**

- **Found during:** Task 1 verification (`ktlintCheck detekt` spans `:app`)
- **Issue:** `local.properties` is gitignored, so a fresh worktree cannot configure `:app` — "SDK location not found". The same environment property plans 08-01, 08-02, 08-04 and 08-06 hit.
- **Fix:** Copied the main checkout's `local.properties` into the worktree. **Not committed** — gitignored by design and per-machine.
- **Verification:** `./gradlew ktlintCheck detekt` and `./gradlew :app:testDebugUnitTest` both configure and pass.
- **Committed in:** n/a (gitignored)

---

**Total deviations:** 5 auto-fixed (1 bug, 1 missing critical, 3 blocking).
**Impact on plan:** Deviations 1 and 2 are the substantive ones and both make the plan's own stated must-haves true rather than changing them — a flat cap violated "returns `null` only for positions it genuinely could not solve", and an unbounded escalation violated the memory half of threat T-08-14. Neither touches D-07: the deadline is still 1.5s and still not per-board. No scope creep; the only file touched outside `files_modified` is `Pagoda.kt`, additively, for the reason the plan itself and 08-04's hand-off both anticipated.

## Issues Encountered

- **`git checkout --` during the RED gate silently discarded an uncommitted production fix.** The RED demonstration restored the pre-Task-2 `Solver.kt`/`Pagoda.kt` via `git show HEAD~1:…`, then reverted with `git checkout -- <files>`. That reverts to the last COMMIT, so the not-yet-committed `MAX_CAP_ESCALATIONS` fix in `Solver.kt` was lost. Caught immediately by a `grep -c MAX_CAP_ESCALATIONS` check (returned `0`) before any further work, and re-applied. Worth recording as a hazard: when a deliberate-break/RED experiment overwrites files, commit or otherwise preserve any uncommitted work in those files first.
- **Diamond-41's full start is genuinely non-deterministic in outcome, though the search is deterministic.** The engine is deterministic given a fixed input, but `shouldCancel` is wall-clock, so a position sitting exactly on the deadline resolves or does not depending on machine load. This is not a solver defect; it is what "on the deadline" means, and it is why the opening book exists.

## TDD Gate Compliance

Task 3 is `tdd="true"` and ran a genuine RED → GREEN cycle.

- **RED:** the new tests were written, then run against the previous implementation, restored into the working tree with `git show HEAD~1:…`. **5 of 9 tests failed** — `resolvesTheEuropean37FullStartInsideTheProductionDeadline`, `resolvesTheAsymmetric39FullStartInsideTheProductionDeadline`, `aPositionAbandonedByItsFirstAttemptIsStillDecidedByARestart`, `returnsALegalWinPreservingMoveFromASolvableMidGameState` (widened to `ALL`) and `SolverBudgetTest.everyTractableSolvablePositionReturnsAMoveWithinBudget` (widened to `ALL`). These are genuine behavioural failures, not compile errors.
- **Control:** `resolvesTheEnglish33FullStartInsideTheProductionDeadline`, `returnsTheSameMoveWhenTheSameBoardIsQueriedTwice`, `returnsNullOnAGenuinelyUnsolvablePosition` and `returnsNullWhenAlreadySolved` passed on BOTH sides — correct, since English-33 was already inside the deadline and the `null` contracts are unchanged. A net that failed everything would have proven only that the file did not compile.
- **GREEN:** `6d0765a` with the rewritten solver restored — all 9 green.
- No REFACTOR step was needed.

Tasks 1 and 2 are `type="auto"` and carry no TDD gate.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test --rerun-tasks` (full suite) | PASS |
| `./gradlew :domain:test --tests '*SolverTest*'` | PASS — 8 tests (was 3) |
| `./gradlew :domain:test --tests '*SolverBudgetTest*'` | PASS — corpus widened to all five boards |
| `./gradlew :domain:test --tests '*PagodaTest*' --tests '*SolverTablesTest*' --tests '*CanonicalTest*' --tests '*SolutionTest*'` | PASS — no regression on 08-02/08-04/08-06 output |
| `./gradlew :domain:test --tests '*GetHintUseCaseTest*' --tests '*ChallengeSolvabilityTest*'` | PASS |
| `./gradlew :app:testDebugUnitTest --rerun-tasks` | PASS — incl. `GameViewModelTest`, hint orchestration untouched |
| `./gradlew ktlintCheck detekt` | PASS |
| `grep -c 'DEFAULT_DEADLINE = 1_500.milliseconds' SolverBudget.kt` | `1` — D-07 honoured literally |
| `grep -c 'DEFAULT_MAX_NODES = 40_000_000L' SolverBudget.kt` | `1` — safety ceiling not lowered |
| `grep -c 'HEADROOM_FACTOR = 4L' SolverBudgetTest.kt` | `1` — factor not lowered |
| `grep -c 'SOLVED_PEG_COUNT' Solver.kt` | `0` |
| `Solver.kt` contains `board.pegCount <= board.spec.minimumPegs`, `SolverTables.forSpec`, `perAttemptMaxNodes`, `SolverBudget.CANCEL_MASK` | yes |
| `RulesEngine.applyMove`/`legalMoves` in executable lines of `Solver.kt` | `0` |
| `grep -c '@Suppress' Solver.kt` | `0` |
| RED verification (tests vs. pre-rewrite solver) | FAILS 5/9, with 4 controls passing on both sides |
| File deletions across the plan (`git diff --diff-filter=D 00c6bb6..HEAD`) | none |
| Untracked files left behind | none (scratch probe deleted; `local.properties` gitignored) |

## Success Criteria

- [x] `Solver.firstWinningMove` returns a legal winning move for the European-37 and Asymmetric-39 full starts within the unchanged 1.5s deadline
- [x] The hot loop performs no `RulesEngine.legalMoves`/`applyMove` call and no per-node allocation
- [x] Each attempt is capped, restarts are demonstrably reachable (asserted), and the proven-exhausted early exit still ends the search immediately
- [x] The 1.5s deadline is unchanged and not per-board; the node safety ceiling was held, never lowered, with its measurement re-documented
- [x] Wiegleb-45's residual gap is recorded in code (`Solver`'s class KDoc and `SolverTest`'s class KDoc), not hidden — and Diamond-41's newly-discovered on-the-deadline status is recorded alongside it

## Threat Model Coverage

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-08-13 (forged sub-target makes `pegCount <= minimumPegs` trivially true) | mitigate | Mitigated — `BoardSpec` validates `minimumPegs in 1..size` at construction (08-01) and `SnapshotMapper` fail-closes a forged blob (08-07). `Solver`'s class KDoc names both dependencies explicitly so neither can be removed casually. |
| T-08-14 (unbounded solver work on a crafted or freeform board) | mitigate | Mitigated, and **strengthened beyond the plan** — both original bounds still hold (the 1.5s `shouldCancel` deadline and `budget.maxNodes` as the cumulative ceiling), and the previously-false memory claim is now true: `MAX_CAP_ESCALATIONS` caps the largest single attempt's memo at 16x `perAttemptMaxNodes`. See Deviation 2. |
| T-08-15 (a rewritten loop reporting a winning line that does not exist) | mitigate | Mitigated — tables proven set-equal to `RulesEngine` in 08-04, canonicalisation proven bit-count-preserving in 08-02, every returned move asserted to be a member of `RulesEngine.legalMoves` in `SolverTest`, and `GetHintUseCase` re-validates independently at the boundary (T-04-CT). |
| T-08-16 (silently editing `DEFAULT_MAX_NODES` or `HEADROOM_FACTOR` to make a test pass) | mitigate | Mitigated — neither constant changed; the re-measured maximum (47 794, ~836x headroom) is recorded in `SolverBudgetTest`'s KDoc, in `DEFAULT_MAX_NODES`'s KDoc and in this summary. |
| T-08-SC (package-manager installs) | n/a | No packages installed. |

## Known Stubs

None. No placeholder values, TODO/FIXME markers or unwired components were introduced.

## Threat Flags

None. This plan touches only the solver's internals and its tests — no new network, auth, file-access or schema surface.

## User Setup Required

None — no external service configuration required. (Per-machine note, unchanged from 08-01/08-02/08-04/08-06: a fresh worktree needs its own gitignored `local.properties` with `sdk.dir` before any `:app` task will configure, and `ktlintCheck`/`detekt` span `:app`.)

## Next Phase Readiness

**DEBT-01's solver half is delivered.** The two boards solver strengthening could fix are fixed and gated by tests. The residual gap is exactly what RESEARCH predicted, and it is now two boards rather than one:

1. **Wiegleb-45** — unchanged and unchangeable by solver strength. Outside the 1.5s deadline by ~4 orders of magnitude.
2. **Diamond-41 — newly measured, and new information for the book plans.** Its full start is right ON the deadline (~1181ms in one run, a miss in another), because `Pagoda` is provably vacuous at a two-peg target. 08-04 flagged the book as "load-bearing rather than optional" for the diamond; this plan supplies the number that proves it. **Plans 08-08/08-09/08-10 should treat Diamond-41's opening as a required book entry, not an optional one** — its mid-game and endgame are cheap, so a shallow book covering the opening plies is sufficient.

**For 08-09/08-10's `DepthSweepTest`:** `SolverTest`'s class KDoc explicitly names `DepthSweepTest`/`OpeningBookTest` as where Wiegleb-45's truthfulness is proven, so that test's existence is now a documented commitment rather than an inference.

**Node-budget trend for the next reader:** maximum observed `Solver.lastVisited` over the tractable corpus dropped to **47 794** with ~836x headroom. If the book plans widen this corpus, re-measure before touching `DEFAULT_MAX_NODES` — Pitfall 2's raise-never-lower instruction still binds.

**Merge note for the orchestrator:** `SolverTest.kt` and `SolverBudgetTest.kt` were changed by 08-06 (`ALL` → `ONE_PEG`, one identifier each) and are re-generalised back to `ALL` here, which 08-06's summary identifies as the correct end state. If a conflict surfaces, this plan's version is the resolution.

No blockers.

## Self-Check: PASSED

- All 5 modified files verified present on disk (`Solver.kt`, `SolverBudget.kt`, `Pagoda.kt`, `SolverTest.kt`, `SolverBudgetTest.kt`).
- All 4 task commits verified in `git log` (`eb8c84a`, `8ec3bfa`, `00e2638`, `6d0765a`).
- Every task `<acceptance_criteria>` re-run and passing; the plan-level `<verification>` block re-run and green.
- No file deletions introduced across the plan.
- No untracked files left behind; the scratch probe was deleted and `local.properties` is gitignored by design.
- No stubs, placeholders or TODO markers introduced; no `@Suppress` added.
- No threat flags: no new network, auth, file-access or schema surface.
- `STATE.md` and `ROADMAP.md` untouched, per the parallel-execution contract.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-29*
