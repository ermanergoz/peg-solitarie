---
phase: 08-assist-generator-quality-debt
plan: 10
subsystem: domain
tags: [opening-book, debt-01, d-08, hint-pipeline, depth-sweep, gradle-task, kotlin]

# Dependency graph
requires:
  - phase: 08-09
    provides: OpeningBook.lookup's three-outcome contract (Hit / authoritative NoWinningMove / null MISS) and the footprint-keyed CATALOG carrying each board's committed depth
  - phase: 08-08
    provides: the five committed book resources, their self-declaring headers (entries / proven-dead / DOCUMENTED GAP), and the developer-approved gap dispositions
  - phase: 08-05
    provides: the restart-schedule Solver and its measured envelope — the fallback this plan wires the book in front of
  - phase: 08-03
    provides: the phase 3 and phase 4 validation contracts naming ./gradlew :domain:test the per-commit gate at seconds latency
provides:
  - A book-aware GetHintUseCase resolving recorded line, then book, then solver, behind ONE legality guard
  - An authoritative "no winning move" that short-circuits the solver, distinct from a miss that does not
  - DepthSweepTest — the exhaustive, deadline-faithful D-08 assertion over all five shipped boards, gated out of the per-commit run
  - A :domain:depthSweep Gradle task wired into check
  - TestFlags — the consolidated env-or-system-property gate, replacing three copies
affects: [08-11]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Candidate list + ONE shared guard: multiple stored-move sources are checked in priority order through a single RulesEngine.legalMoves membership test, so there are not N guards that can drift"
    - "Slow-test gating in TWO independent places — a dedicated Test task that sets the property AND an in-body flag read — so neither half can silently disable the other"
    - "A sweep that pins its outcome counts to the artifacts' OWN declared headers by equality, catching a gap that widens or narrows"
    - "Test-flag reads centralised once the third copy appeared, rather than at the fourth"

key-files:
  created:
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/TestFlags.kt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCaseTest.kt
    - domain/build.gradle.kts
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/GoldenMasterAssert.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/OpeningBookAuthoringTest.kt

key-decisions:
  - "DEBT-01's literal depth-4-everywhere claim was NOT asserted, because the committed artifacts do not support it; the sweep asserts the truthfulness property they do support, and asserts the full universal claim exactly where the books are complete"
  - "Fallback counts are pinned to each book's documented gap by EQUALITY rather than a bound, so a gap that narrows without a header diff also fails"
  - "The sweep drives GetHintUseCase, never Solver directly — D-08's claim is about what a player receives"
  - "The RUN_SWEEP gate is duplicated deliberately (Gradle task + in-body read) so a gated test cannot quietly never run"
  - "The env-or-property flag read was consolidated into TestFlags rather than copied a fourth time"

patterns-established:
  - "Solver-never-ran proof: shouldCancel returning true would force the Solver to null, so a non-null result with zero polls is a proof the solver branch was never entered"
  - "Anti-vacuity in three directions: examined count, classification totals, and a BFS cross-checked against an externally measured canonical table"

requirements-completed: [DEBT-01, DEBT-02]

# Metrics
duration: 41 min
completed: 2026-08-30
---

# Phase 8 Plan 10: Book-Aware Hint Pipeline and the D-08 Depth Sweep Summary

**`GetHintUseCase` now resolves recorded line → opening book → solver behind a single legality guard, with an authoritative "no winning move" that short-circuits the search; and an exhaustive, deadline-faithful sweep over all five shipped boards proves the pipeline never claims a move — or a dead position — it did not prove.**

## Performance

- **Duration:** 41 min
- **Started:** 2026-08-29T20:32:00Z
- **Completed:** 2026-08-29T21:14:47Z
- **Tasks:** 3 (Task 1 TDD RED/GREEN; Tasks 2 and 3 spec + gate)
- **Files created:** 2 | **modified:** 6

## Accomplishments

- **The DEBT-01 headline defect is gone.** Wiegleb-45's start — provably winnable, its one-peg line a committed fixture — used to produce the "no winning move" rescue banner because RESEARCH measured **0 of 76** of its depth-≤4 positions resolving inside the 1.5 s deadline even at ~15M nodes/s. It is now answered from the book with the solver never polled.
- **The full D-08 universal claim holds, exhaustively, on every board whose book is complete.** Across English-33, European-37 and Asymmetric-39, **not one** position in the entire depth-4 frontier reaches the solver. Pre-book, those same frontiers left 57, 26 and 4 positions unresolved and cost 158.6 s / 73.2 s / 10.9 s to sweep.
- **English-33's four "unresolved" positions turned out to be four *correct* answers.** They are the book's authoritative proven-dead entries — "no winning move" was right all along, and is now served as a proof in microseconds instead of inferred from a timeout.
- **One legality guard, not two.** Both stored-move paths (recorded line and book) pass through a single `RulesEngine.legalMoves` membership test; the acceptance criterion pins the call count at exactly 1 so the guard cannot be bypassed for the book path.
- **The per-commit gate stayed fast.** `./gradlew :domain:test` runs **167 tests in 31 s clean** (16.2 s of test time), with `DepthSweepTest` a **784 ms** no-op. The 11-minute sweep lives in `check`.

## Task Commits

1. **Task 1 (RED): failing test for the book-served Wiegleb-45 hint** — `0d34456` (test)
2. **Task 1 (GREEN): book wired between the recorded line and the solver** — `5e4bd35` (feat)
3. **Task 2: the pipeline serves the book without running the solver** — `49e566f` (test)
4. **Task 3: the D-08 sweep, in `check` but not per commit** — `ed95368` (test)

**Plan metadata:** this commit (docs).

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt` — three-path resolution; a candidate list checked in priority order through one guard; an authoritative short-circuit split into a named private function so the miss/proof distinction is visible in the code's shape.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCaseTest.kt` — 5 new tests (11 total, 82 ms): all five starts, the whole Diamond floor line, European-37's exhaustive depth-4 frontier, and the off-book miss that must still reach the solver.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt` — the D-08 sweep.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/TestFlags.kt` — the consolidated flag gate.
- `domain/build.gradle.kts` — the `depthSweep` task and its `check` dependency.
- `GoldenMasterAssert.kt`, `SolutionTest.kt`, `OpeningBookAuthoringTest.kt` — routed through `TestFlags`.

## The measured sweep (the headline result)

`./gradlew :domain:depthSweep`, each board at the depth its OWN committed book covers, inside the unchanged 1.5 s deadline, driving the real `GetHintUseCase`:

| Board | k | examined | canonical | book-served | authoritative dead | solver fallback | of which answered | max/call |
|-------|---|----------|-----------|-------------|--------------------|-----------------|-------------------|----------|
| English-33 | 4 | 373 | 51 | 369 | 4 | **0** | — | 12 ms |
| European-37 | 4 | 609 | 312 | 609 | 0 | **0** | — | 2 ms |
| Asymmetric-39 | 4 | 461 | 233 | 461 | 0 | **0** | — | 1 ms |
| Diamond-41 | 4 | 785 | 107 | 345 | 0 | 440 | **0** | 1512 ms |
| Wiegleb-45 | 3 | 101 | 16 | 97 | 0 | 4 | **0** | 1500 ms |

**Sweep wall-clock: 666.5 s (11 min 7 s).** Against RESEARCH's pre-book measurement of the same four boards it swept (~357 s, 87 positions unresolved): those four now cost **single-digit milliseconds apiece and leave nothing unresolved**. The entire 11 minutes is Diamond-41 — a board RESEARCH never swept — and it is exactly its 440 documented-gap positions each burning the full deadline.

**A finding worth carrying forward:** `of which answered = 0`. On every one of Diamond-41's 440 gap positions and Wiegleb-45's 4, the solver fallback returns `null` after the full 1.5 s. So the documented gap is not merely a book gap — it is a **player-visible** gap: those positions still show a 1.5 s spinner ending in the rescue banner. That was previously an inference; it is now a measurement.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :domain:test` (clean, `--rerun-tasks`) | **BUILD SUCCESSFUL in 31 s** — 167 tests, seconds-scale contract honoured |
| `./gradlew :domain:test --tests '*DepthSweepTest*'` | **BUILD SUCCESSFUL in 784 ms** (flag unset → body skipped; criterion was < 5 s) |
| `./gradlew :domain:depthSweep` | **BUILD SUCCESSFUL**, 666.5 s, non-zero examined counts on all five boards |
| `./gradlew check` | **BUILD SUCCESSFUL in 11 m 7 s, exit 0** |
| `./gradlew check --dry-run \| grep depthSweep` | `:domain:depthSweep` present in the task graph |
| `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | **BUILD SUCCESSFUL**, `GameViewModel` unchanged |
| `./gradlew ktlintCheck detekt` | **BUILD SUCCESSFUL** |
| `RulesEngine.legalMoves(` count in `GetHintUseCase.kt` (excl. comments) | **1** |
| `grep -c 'suspend\|android\.'` in `GetHintUseCase.kt` | **0** |
| Files above `:domain` changed | **0** |

The domain purity gate lives in `:app` (`app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt`), per plan 08-09's carried-forward note, and ran as part of `./gradlew check`.

## Decisions Made

1. **DEBT-01's literal claim was not asserted, because it is false of the shipped artifacts.** D-08 as written demands every position within four moves of European-37, Asymmetric-39 **and Wiegleb-45** resolve inside the deadline. Wiegleb-45's book ships at depth **3** (plan 08-08's checkpoint rescope, with the resource renamed `wiegleb45-k3.txt` precisely so it cannot misrepresent itself) with 4 of 140 in-remit positions an approved gap; Diamond-41 is missing 440 of 818, also approved; and English-33 ships four positions that are *authoritatively dead*, where "returns a non-null move" is false **by design**. Writing the literal claim would have committed a test that asserts something known to be untrue. The sweep instead asserts the property the artifacts genuinely support — *the pipeline never claims a move, or a dead position, it did not prove* — **and** asserts the full universal claim exactly where the books are complete. This follows the same rescope logic the developer approved at 08-08's checkpoint, and is the third consecutive plan in which the honest property proved stronger than the planned one.
2. **Gap counts pinned by equality, not by a bound.** `solver-fallback == DOCUMENTED_GAP[board]` rather than `<=`. A gap that *narrows* without its header changing is as much a drift between artifact and self-description as one that widens, and the equality catches both.
3. **The sweep drives `GetHintUseCase`, never `Solver`.** D-08's claim is about what a player receives, and the book is now part of that answer; a sweep against the raw solver would assert something the product does not do — and would still fail on Wiegleb-45 exactly as RESEARCH measured.
4. **The `RUN_SWEEP` gate is deliberately doubled.** The `depthSweep` task sets the system property *and* the test body reads it. Either alone would work; together, neither half can silently disable the other — a gated test that quietly never runs is indistinguishable from a passing one.
5. **Diamond-41's 11-minute cost was accepted rather than sampled away.** Shortening the deadline for gap positions would not have weakened any assertion (their only assertion is legality, which is budget-independent), but it would have hidden the finding that those positions genuinely exhaust the shipped envelope. The cost is confined to `check`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 — Bug] Task 3's behaviour bullet asserts a claim that is false of the shipped artifacts**
- **Found during:** Task 3
- **Issue:** The bullet requires that for every position within `BOOK_DEPTH` of European-37, Asymmetric-39 **and Wiegleb-45**, `GetHintUseCase` return a **non-null** move within the deadline. Wiegleb-45's book covers depth 3, not 4, and carries a 4-position approved gap on which the solver provably cannot answer (measured this plan: all 4 return `null` after the full 1.5 s). Written literally, the test fails on the first run by design — and asserting it would misrepresent the artifacts.
- **Fix:** Each board is swept at the depth its own committed book covers (read from `OpeningBook.CATALOG`, not assumed), and the assertion is the truthfulness property plus an exact per-board gap equality. The full non-null universal claim is still asserted, unweakened, on the three boards whose books are complete.
- **Files modified:** `DepthSweepTest.kt`
- **Verification:** Sweep green with counts matching every book header exactly.
- **Committed in:** `ed95368`

**2. [Rule 1 — Bug] Task 3's "non-null for every position" would also have failed on English-33**
- **Found during:** Task 3
- **Issue:** English-33 ships **four** authoritative proven-dead entries. A blanket non-null assertion would fail there too — on the one board with a complete, zero-gap book — for positions where "no winning move" is the *correct* answer.
- **Fix:** The sweep classifies into book-served / authoritative-dead / solver-fallback and pins the authoritative count to each file's own `proven-dead=N` header (English-33 → 4, all others → 0).
- **Files modified:** `DepthSweepTest.kt`
- **Verification:** `authoritative-dead=4` measured for English-33, 0 elsewhere.
- **Committed in:** `ed95368`

**3. [Rule 2 — Missing Critical] The plan's anti-vacuity criterion was self-referential as first written**
- **Found during:** Task 3
- **Issue:** My first draft asserted `examined == frontier.size` where both came from the same enumeration — precisely the vacuous self-check threat T-08-39 exists to prevent. A truncated BFS would have satisfied it.
- **Fix:** Three independent guards: a loop-body counter against the enumeration size, a classification-total identity, and the frontier's canonical size cross-checked against RESEARCH's independently measured depth table (51 / 312 / 233 / 107 at K=4, 16 for Wiegleb at K=3) — numbers the file cannot derive from what it is verifying. The same external cross-check (312) was added to Task 2's European-37 test, whose `examined > 0` was likewise too weak.
- **Files modified:** `DepthSweepTest.kt`, `GetHintUseCaseTest.kt`
- **Verification:** All five canonical counts matched on the nose.
- **Committed in:** `49e566f`, `ed95368`

**4. [Rule 2 — Missing Critical] A recorded move that failed the guard would have skipped the book**
- **Found during:** Task 1
- **Issue:** A draft that resolved the book only when no recorded move existed would, for an *illegal* recorded move, fall straight past the book to the solver — breaking the plan's own stated "recorded line, then book, then solver" order in exactly the stale-data case the guard exists for.
- **Fix:** Candidates are collected in priority order and the first one the board accepts wins, so a rejected recorded move falls through to the book rather than past it. `listOfNotNull` collapses to empty on a Classic miss, so the guard's predicate never runs and no legality set is built when there is nothing to check.
- **Files modified:** `GetHintUseCase.kt`
- **Verification:** `anIllegalRecordedMoveIsRejectedByTheLegalityGuardAndTheSolverIsUsed` still green.
- **Committed in:** `5e4bd35`

**5. [Rule 3 — Blocking] `MaxLineLength` detekt failure**
- **Found during:** Task 2
- **Issue:** The canonical cross-check line exceeded the configured maximum.
- **Fix:** Wrapped the assignment.
- **Files modified:** `GetHintUseCaseTest.kt`
- **Verification:** `./gradlew ktlintCheck detekt` green.
- **Committed in:** `49e566f`

---

**Total deviations:** 5 auto-fixed (2 bugs in the plan's assertions about the shipped artifacts, 2 missing-critical strengthenings, 1 blocking lint)
**Impact on plan:** No scope creep. Deviations 1 and 2 share 08-09's root cause — the plan was written against an idealised 08-08 (uniform depth 4, no gaps, no proven-dead markers) and 08-08 shipped something more honest. Each was corrected toward the property the artifacts actually support, and every corrected assertion is at least as strong as the one it replaced.

## Threat Model Coverage

| Threat ID | Disposition | How it is now discharged |
|-----------|-------------|--------------------------|
| T-04-CT (carried) | mitigate | One `RulesEngine.legalMoves` call serves both stored-move paths (call count pinned at 1 by an acceptance criterion). Legality asserted on every non-null result in Task 2 and on all 2 329 swept positions in Task 3. |
| T-08-36 | mitigate | Three distinct branches; `anOffBookClassicPositionStillReachesTheSolver` asserts the miss path polls the solver, with the position's MISS status asserted as a precondition rather than assumed. |
| T-08-37 | mitigate | `BookAnswer.NoWinningMove` returns `null` from a named private function without entering the solver. Measured: English-33's 4 dead positions answer in the same microseconds as a hit. |
| T-08-38 | mitigate | Doubly gated (task property + in-body read); `:domain:test --tests '*DepthSweepTest*'` measured at **784 ms** against a < 5 s criterion, and the full `:domain:test` at 31 s clean. |
| T-08-39 | mitigate | Three independent anti-vacuity guards including an externally sourced canonical table; per-board counts printed on every run. |
| T-08-SC | n/a | No packages installed. |

## Issues Encountered

- **The plan's D-08 assertions had drifted from the artifacts, in the same way 08-09's had.** Resolved identically: assert what the files declare about themselves. Documented as deviations 1–2 and decision 1 rather than silently narrowed.
- **`check` is now an 11-minute gate.** Entirely Diamond-41's documented gap. The per-commit gate is untouched, which is what plan 08-03's contracts bind, but this is a real cost and is called out below.
- **No blockers, no checkpoints.** The plan was fully autonomous as declared.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**Ready for plan 08-11** (release-APK resource survival), with four things it should know:

1. **The book is now on the production hint path**, so 08-11's R8 / resource-shrinking proof is load-bearing rather than precautionary. If the resources do not survive into the release APK, every board silently degrades to solver-only — which is measurably the pre-book behaviour, including Wiegleb-45's untruthful banner. `OpeningBook` fails closed, so this failure produces **no error at all**.
2. **`:domain:depthSweep` is the regression detector.** If 08-11 changes packaging, re-running the sweep is the end-to-end proof that lookups still resolve.
3. **`check` costs ~11 minutes now.** If that proves too heavy in practice, the honest lever is closing Diamond-41's 440-position gap (which removes the cost *and* the player-visible gap), not weakening the sweep.
4. **The residual, stated plainly:** DEBT-01 as literally written is still not met on Diamond-41 and Wiegleb-45, and this plan does not change that. What is now machine-checked on every `check` is the property the artifacts support — **the hint never claims a move it did not prove, never claims "no winning move" it did not prove, and a book miss falls to the solver whose answer is never dressed up as a proof.** The 444 gap positions across those two boards are measured, enumerated, and pinned by equality, so any change to them is a visible diff.

## Self-Check: PASSED

- Both created files exist on disk and are tracked by git; all six modified files present.
- All four `08-10` task commits verified in `git log`: `0d34456`, `5e4bd35`, `49e566f`, `ed95368`.
- `git status --porcelain` empty apart from the gitignored `local.properties` (confirmed via `git check-ignore`; never staged).
- Every plan-level `<verification>` command re-run and recorded in the Verification table above; `./gradlew check` exit 0.
- No modification to `.planning/STATE.md` or `.planning/ROADMAP.md`.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-30*
