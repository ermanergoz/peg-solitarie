---
phase: 04-assist-hint-undo
plan: 02
subsystem: domain
tags: [solver, pagoda, dfs, symmetry, budget, purity, junit, truth]

# Dependency graph
requires:
  - phase: 02-classic-mode
    provides: AuthoringSolver (test-only port source), RecordedSolutions corpus, SolutionFixtures (English-33/European-37/Asymmetric-39/Wiegleb-45), BoardCanonical symmetry
  - phase: 01-foundation
    provides: Board bitmask + RulesEngine.legalMoves/applyMove move authority, DomainPurityTest gate
provides:
  - "Production Solver.firstWinningMove(board, budget, shouldCancel) — pure :domain, arbitrary-start first-winning-move engine (ASST-01/ASST-03)"
  - "SolverBudget(maxNodes, deadline) value + DEFAULT (40M-node safety ceiling / 1.5s deadline / 4096-node cancel poll)"
  - "Pagoda.mayBeSolvable — sound mod-3 colour-parity necessary-condition fast-reject"
provides-caveat:
  - "PERFORMANCE ENVELOPE: the Solver reliably decides ENDGAME + easy positions within the 1.5s deadline; hard EARLY positions on the larger Classic boards (and a few english33 mid-line positions) exceed it and fold to the D-04 'no winning move' state. See Known Limitation."
affects: [04-03-hint-logic, 04-03b-hint-rendering, 04-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Port of a test-only authoring search into production :domain main source (cannot import test classes — copied VisitedSet/symmetry/inwardness)"
    - "Memoised symmetry-canonical DFS + O(cells) pagoda necessary-condition prune + deterministic jittered restarts under a node budget and cooperative shouldCancel"
    - "Coroutine-free cancellation: deadline enters as a plain shouldCancel: () -> Boolean; kotlin.time.Duration carried as data only (domain purity)"
    - "CI-fast measurement test: assert the achievable guarantee (correctness + endgame-tail completeness + node headroom) with a hard per-call wall-clock guard, not universal completeness"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverBudget.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverBudgetTest.kt
  modified: []

key-decisions:
  - "SolverBudget.DEFAULT_MAX_NODES kept at the measured 40M as a MEMORY/TIME SAFETY CEILING, not the operative limit: at ~0.27M nodes/s the 1.5s DEFAULT_DEADLINE reaches only ~0.4M nodes, so the deadline governs production and the node cap is a backstop. (A prior in-flight edit had lowered it to 6M, which cut solvable boards short — reverted per the test's own 'raise, never lower' rule.)"
  - "Pagoda implemented as the mod-3 colour-parity (GF(4)-style) necessary condition on both (row+col) and (row-col) colourings: each orthogonal jump flips all three colour parities, so the forced terminal parity of a reachable one-peg board has exactly one odd class; a position whose forced terminal parity is not weight-1 is provably dead. Sound one-directional cut, board-agnostic."
  - "Deterministic jittered restarts (fixed seeds, MAX_ATTEMPTS=40) added so an 'unlucky' inward move ordering on a large footprint can be escaped while keeping the engine deterministic (same board -> same move) and pure (kotlin.random.Random is Android-free). Restarts trigger only when an attempt is CUT SHORT by the node cap; a full within-budget exhaustion still proves null immediately."
  - "SolverBudgetTest right-sized for CI: the original 'every solvable prefix of every board decides within budget' is NOT achievable at a usable deadline (see Known Limitation) and ran 20-60+ min. Replaced with a fast, honest gate — unsolvable pagoda-passing samples -> null, every board's endgame tail + the reference board's start decide within a 2s per-call guard, and DEFAULT_MAX_NODES keeps 4x headroom over the largest observed tractable search."

patterns-established:
  - "Domain search engine: memoised symmetry-canonical DFS + pagoda prune + node budget + coroutine-free shouldCancel deadline hook."
  - "Right-sized measurement test: assert the achievable guarantee with a per-call wall-clock guard; document the capability envelope rather than asserting an unreachable universal property."

requirements-completed: [ASST-01, ASST-03]
requirements-caveat: "ASST-01/ASST-03 solver engine delivered; the deviation-hint's usable coverage is bounded by the performance envelope below — flagged for the 04-05 playtest and phase verification."

# Metrics
duration: 180min
completed: 2026-07-08
---

# Phase 4 Plan 02: Production Budgeted Solver + Pagoda Fast-Reject Summary

**A pure-`:domain` winning-move engine — memoised symmetry-canonical DFS with a mod-3 colour-parity pagoda fast-reject, deterministic jittered restarts, a node budget, and a coroutine-free cancellation hook — powering the Challenge deviation hint and every Classic hint (ASST-01/ASST-03). Ships with a documented performance envelope.**

## Performance
- **Duration:** ~180 min (resumed a stalled plan; substantial solver diagnosis + right-sizing)
- **Completed:** 2026-07-08
- **Tasks:** 3
- **Files:** 6 created (3 main, 3 test), 0 modified

## Accomplishments
- `Solver.firstWinningMove` returns a legal first move of a winning line from an arbitrary mid-game board, or `null` when the position is unsolvable / budget-missed / cancelled (folded per D-04), using the same `RulesEngine` move authority as gameplay (so a hint is always a legal, reachable move).
- `Pagoda.mayBeSolvable` upgraded from an accept-all scaffold to a real, sound mod-3 colour-parity necessary-condition fast-reject that collapses provably-dead positions to `null` in O(cells).
- `SolverBudget` value with a measured 40M-node safety ceiling, a 1.5s deadline (enforced by the caller via `shouldCancel`), and a 4096-node cancel poll — all documented, no magic numbers.
- Domain purity preserved (no android/androidx/coroutines in the solver) — `DomainPurityTest` green.

## Task Commits
1. **Task 1: Failing Solver/Pagoda/Budget scaffolds (RED)** — `cfe250e` (test) — prior executor
2. **Task 2: Port AuthoringSolver into budgeted cancellable Solver (GREEN)** — `8c70584` (feat) — prior executor
3. **Task 3: Pagoda parity fast-reject + jittered restarts + CI-fast budget test** — `6b8ceb7` (feat)

_The plan is `tdd`-style; Tasks 1-2 (RED/GREEN) were committed by a prior executor. Task 3 (the pagoda implementation, restart hardening, and the budget/measurement test) was completed here._

## ⚠ Known Limitation — Solver Performance Envelope (IMPORTANT for 04-03 / 04-05)

Measured facts (JVM, this machine, ~0.27M nodes/s), production 1.5s deadline:

| Position | Nodes | Time | Decides in 1.5s? |
|----------|-------|------|------------------|
| english33 start (32 pegs) | 21k | 0.1s | ✅ |
| english33 most mid-line prefixes | 60k–425k | <1.4s | ✅ (mostly) |
| **english33 prefix5 (27 pegs)** | **2.2M** | **8.9s** | ❌ |
| european37 start (36 pegs) | 2.2M | 7.7s | ❌ |
| asymmetric39 / wiegleb45 starts (38/44 pegs) | >2M | >8s | ❌ |
| any board endgame tail (≤~12 pegs) | small | <1s | ✅ |

**Implication:** the hint's canonical-recorded-line path (04-03) always works while the player is *on* the recorded line. The **Solver fallback** (used after a deviation) reliably returns a move within the deadline for **endgame and easy positions**, but on harder early positions of the larger Classic boards — and a few english33 mid positions — it exceeds 1.5s and D-04 folds that to the **"no winning move — undo or restart"** state. This is a *designed* degradation (D-04), but it means Success Criterion 2 ("after deviation the Solver still returns a legal win-preserving move") holds only within this envelope, not universally.

**Why it can't be trivially fixed:** jittered deterministic restarts do **not** reliably shortcut these positions (different seeds explore comparable node counts), and a smaller per-attempt node cap makes it *worse* (abandons positions that a single order would have solved). Genuinely widening coverage needs a stronger solver — e.g. weighted pagoda-function pruning or a better heuristic/algorithm — which is open-ended and out of scope for this plan. **Recommend surfacing at the 04-05 playtest and phase verification** to decide whether the current envelope is acceptable for Slice 1 or warrants a follow-up solver-improvement task.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `DEFAULT_MAX_NODES` had been lowered to 6M by a prior in-flight edit, cutting solvable boards short**
- **Issue:** the uncommitted working tree shipped `DEFAULT_MAX_NODES = 6_000_000`; a solvable prefix exceeded it and returned `null`, failing `SolverBudgetTest` (line 39). The test explicitly says "RAISE this value — never lower the assertion."
- **Fix:** restored the measured `40_000_000`; clarified in KDoc that it is a memory/time ceiling and the 1.5s deadline is the operative production limit.
- **Committed in:** `6b8ceb7`.

**2. [Rule 3 - Blocking] detekt `ComplexCondition` (4 > 3) on the in-search pagoda prune**
- **Issue:** `if (!cancelled && withinBudget && Pagoda.mayBeSolvable(current) && seen.add(...))` is a 4-term condition; detekt max is 3.
- **Fix:** hoisted the first three terms into a `val explorable = ...` local; the `if` is now two terms. Short-circuit and side-effect ordering preserved.
- **Committed in:** `6b8ceb7`.

**3. [Rule 3 - Blocking] `:domain:test` OOM at the 40M budget on the default heap**
- **Issue:** a prior edit granted a 3g heap only under a `SOLVER_MEASURE` env, so normal `:domain:test` OOM'd running the 40M-node search — which is what forced the (wrong) 6M downgrade.
- **Fix:** made obsolete by right-sizing the test (Deviation 4) so no CI solve approaches 40M; reverted `domain/build.gradle.kts` to its original (no heap hack, only the `UPDATE_GOLDEN` branch).
- **Committed in:** `6b8ceb7` (test), build file reverted (unchanged from HEAD).

**4. [Rule 2 - Plan-defect] `SolverBudgetTest` as written was unrunnable in CI (20-60+ min) and asserted an unachievable universal property**
- **Issue:** enumerating every prefix of every solvable line at the 40M budget takes tens of minutes, and the D-04 "every solvable board decides within budget" property is not achievable within a usable deadline (see Known Limitation). The planner never validated this test (prior executors stalled on it).
- **Fix (user-approved "right-size for CI"):** rewrote the test to assert the *achievable, honest* guarantee — correctness on unsolvable pagoda-passing samples, plus every board's endgame tail (and the reference board's start) deciding within a 2s per-call wall-clock guard, with 4x node headroom. The full early-position envelope is documented in the test KDoc and here, and deferred to the 04-03 ViewModel integration + the 04-05 playtest.
- **Committed in:** `6b8ceb7`.

**Total deviations:** 4 (3 blocking auto-fixes correcting a prior stalled executor's in-flight state; 1 plan-defect right-sizing, user-approved).

## Issues Encountered
- The prior two executors repeatedly stalled because the harness auto-backgrounds long Gradle runs and their turn ended awaiting a completion signal. Resolved by the orchestrator running verifications directly (re-invoked on completion) and completing the plan.
- Deep solver diagnosis was required (bounded measurement probes) to distinguish a budget-tuning bug from a genuine capability limit — the latter is now documented rather than papered over.

## User Setup Required
None.

## Next Phase Readiness
- 04-03 can call `Solver.firstWinningMove(board, SolverBudget.DEFAULT, shouldCancel)` off-thread (Dispatchers.Default) with the deadline realised as `shouldCancel { elapsed > DEFAULT_DEADLINE }`. It MUST treat `null` as the "no winning move" state (already the D-04 design).
- 04-03 should try the recorded canonical line first (instant), Solver only on deviation — consistent with the performance envelope above.
- The envelope (large-board early-position deviation hints fold to "no winning move") should be verified as acceptable at the 04-05 on-device playtest.

## Self-Check: PASSED
- All 6 files exist on disk (3 main solver files, 3 test files) — verified.
- Task commits present: `cfe250e`, `8c70584`, `6b8ceb7` — verified via `git log`.
- `./gradlew :domain:test :app:testDebugUnitTest --tests '*DomainPurityTest*' :domain:ktlintCheck :domain:detekt` — BUILD SUCCESSFUL (68 domain tests incl. right-sized SolverBudgetTest, domain purity, ktlint, detekt all green), ~14s.
- No scratch/diagnostic files left in the tree (verified).

---
*Phase: 04-assist-hint-undo*
*Completed: 2026-07-08*
