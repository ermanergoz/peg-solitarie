---
phase: 04-assist-hint-undo
plan: 03
subsystem: ui
tags: [hint, solver, usecase, viewmodel, mvvm, stateflow, coroutines, purity, hilt]

# Dependency graph
requires:
  - phase: 04-assist-hint-undo
    provides: "04-01 AdGateway/AdReason seam (reused for AdReason.HINT); 04-02 Solver.firstWinningMove + SolverBudget.DEFAULT"
  - phase: 03-challenge-generation-difficulty-curve
    provides: "Level.solution recorded winning line threaded through LevelRepository.loadChallenge"
  - phase: 01-foundation
    provides: "RulesEngine.legalMoves/applyMove move authority; GameViewModel state authority; DomainPurityTest gate"
provides:
  - "GetHintUseCase — pure :domain recorded-line-first -> budgeted Solver -> null hint decision (ASST-01/ASST-03)"
  - "GameViewModel.onHintRequested — off-thread, cancellable, grant-gated hint orchestration writing hintSource/hintTarget/solverThinking/noWinningMove"
  - "GameUiState hint fields (hintSource, hintTarget, solverThinking, noWinningMove) for 04-03b to render and 04-04's no-win banner"
  - "onRecordedLine deviation tracking (recomputed on undo) — never blind-indexes solution[moveCount]"
affects: [04-03b-hint-rendering, 04-04, 04-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure use case (plain class, no Android/coroutines) calling the domain Solver/RulesEngine; cancellation crosses as a plain shouldCancel lambda"
    - "Off-thread cancellable VM job: cancel prior hintJob, raise thinking flag synchronously, withContext(defaultDispatcher) with a deadline-realised shouldCancel"
    - "Recorded-line-first hint: serve the recorded winning move instantly on-line; Solver fallback only after deviation / for Classic (D-03)"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCaseTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/di/DomainModule.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "GetHintUseCase is a plain :domain class (no Android, no coroutines); it references object Solver + RulesEngine directly and takes shouldCancel as a plain lambda — DomainPurityTest stays green."
  - "Recorded branch is guarded by BOTH a range check (List.getOrNull) AND a legality check (move in RulesEngine.legalMoves(board)); a stale/illegal recorded index falls through to the Solver, never returned blind (Pitfall 5 / T-04-CT)."
  - "onRecordedLine is a private VM boolean, flipped false permanently on the first deviating move and recomputed on undo by prefix-comparing moveHistory against solution; solution is handed to the use case only while true. Exposed via an internal isOnRecordedLine() seam purely for the VM test."
  - "onHintRequested raises solverThinking synchronously before launching, so the D-10 thinking state is visible immediately; the launched job requests AdReason.HINT (reusing the 04-01 seam), runs the use case in withContext(defaultDispatcher), and folds a null result to noWinningMove (D-04)."
  - "Deadline realised in the VM as timeSource.markNow() + SolverBudget.DEFAULT.deadline, polled by the Solver via shouldCancel = { !isActive || deadline.hasPassedNow() } — the pure-domain boundary never sees a coroutine type."

patterns-established:
  - "Pure hint decision + off-thread cancellable VM orchestration: recorded-line-first, Solver-fallback, thinking flag, no-win fold."
  - "Cancellation cooperation without overlapping searches: cancel the prior job before each new request; proven by a gated AdGateway spy (only one job proceeds past the grant)."

requirements-completed: [ASST-01, ASST-03]

# Metrics
duration: 40min
completed: 2026-07-08
---

# Phase 4 Plan 03: Hint Logic (GetHintUseCase + VM Orchestration) Summary

**Delivered the Hint decision end-to-end minus pixels: a pure `GetHintUseCase` that returns the recorded winning move while on the Challenge line (no Solver) and defers to the budgeted 04-02 Solver otherwise, plus `GameViewModel.onHintRequested` that runs it off `defaultDispatcher` on a cancellable, grant-gated job and writes `hintSource`/`hintTarget`/`solverThinking`/`noWinningMove` for 04-03b to render.**

## Performance

- **Duration:** ~40 min
- **Completed:** 2026-07-08
- **Tasks:** 2 (both `tdd`)
- **Files:** 7 (2 created, 5 modified)

## Accomplishments

- `GetHintUseCase` (pure `:domain`): returns `solution[movesPlayed]` instantly when a non-null recorded line covers the ply AND that move is currently legal; otherwise runs `Solver.firstWinningMove(board, SolverBudget.DEFAULT, shouldCancel)`. A `null` Solver result folds to `null` (the no-win state, D-04). No Android, no coroutines — `DomainPurityTest` green.
- `GameViewModel.onHintRequested`: cancels any prior `hintJob`, raises `solverThinking` synchronously, requests `AdReason.HINT` through the 04-01 `AdGateway` seam, then resolves the move in `withContext(defaultDispatcher)` with the deadline realised as `shouldCancel = { !isActive || deadline.hasPassedNow() }`. A found move sets `hintSource`/`hintTarget`; a `null` sets `noWinningMove`; either clears `solverThinking`.
- Recorded-line tracking (`onRecordedLine`): captured `solution` in the async Challenge `init` load; flips permanently `false` on the first deviating move in `performJump`; recomputed on `onUndo` by prefix-comparing `moveHistory` against `solution` (so undo can walk back onto the line). The VM never blind-indexes `solution[moveCount]`.
- Hint highlight + no-win state cleared on every jump and undo; restart resets `onRecordedLine = true` and cancels the hint job.
- `GameUiState` gained `hintSource`/`hintTarget`/`solverThinking`/`noWinningMove` (defaults in both `initial()` and `loading()` factories, documented in KDoc); `DomainModule` provides `GetHintUseCase`.

## Task Commits

1. **Task 1 (RED): failing GetHintUseCase spec** — `50d487b` (test)
2. **Task 1 (GREEN): pure GetHintUseCase recorded-line-first -> Solver** — `86f8dec` (feat)
3. **Task 2: VM hint orchestration + state fields + tests** — `cc4e6e1` (feat)

## Solver Performance Envelope (carried from 04-02)

The recorded-line path (Challenge, on-line) always resolves instantly with no Solver call. The Solver fallback (Classic hints, post-deviation hints) is bounded by 04-02's documented envelope: endgame/easy positions decide within the 1.5s deadline; hard early positions on the larger Classic boards exceed it and fold to `noWinningMove` (D-04). This is the designed degradation — the VM treats every `null` (unsolvable / budget-missed / cancelled) as the no-win state and never crashes or emits an illegal move. Acceptability to be confirmed at the 04-05 playtest.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] detekt `MaxLineLength` on a test `Move(...)` literal (Task 1)**
- **Issue:** the fabricated illegal `Move` in `GetHintUseCaseTest` exceeded the line-length limit.
- **Fix:** wrapped the constructor call across lines. **Committed in:** `86f8dec`.

**2. [Rule 3 - Blocking] detekt `LargeClass` on `GameViewModelTest` (Task 2)**
- **Issue:** the eight new hint tests + `GatedAdGateway` pushed the single VM behavior spec over detekt's `LargeClass` threshold.
- **Fix:** added `@Suppress("LargeClass")` with a rationale (one file exhaustively documents the VM contract) — no threshold change to the shared config. **Committed in:** `cc4e6e1`.

**3. [Rule 3 - Blocking] detekt `LongParameterList` guard on the 9-arg VM constructor (Task 2)**
- **Issue:** adding `getHintUseCase` made `GameViewModel`'s `@Inject` constructor 9 params.
- **Fix:** extended the existing class-level `@Suppress` to `@Suppress("TooManyFunctions", "LongParameterList")` (the VM is the injected UI event hub). **Committed in:** `cc4e6e1`.

**4. [Rule 3 - Blocking] ktlint chain-wrap + coroutines opt-in warnings in the test (Task 2)**
- **Issue:** ktlint required the wrapped `RulesEngine.legalMoves(...)` chain to break before the first `.`; the recompiled file surfaced `ExperimentalCoroutinesApi` opt-in warnings for `runCurrent`/`advanceUntilIdle`/`advanceTimeBy`.
- **Fix:** broke the chain onto its own line and added a class-level `@OptIn(ExperimentalCoroutinesApi::class)`. **Committed in:** `cc4e6e1`.

**Total deviations:** 4 (all Rule 3 - blocking lint/style gate fixes; no behavior change to plan intent).

## TDD Gate Compliance

- **Task 1** followed a clean RED (`50d487b` `test(...)`) → GREEN (`86f8dec` `feat(...)`) split: the spec failed to compile (unresolved `GetHintUseCase`) before the implementation landed.
- **Task 2** (`tdd="true"`) landed its tests and implementation together in a single `feat(...)` commit (`cc4e6e1`). The RED→GREEN discipline held during execution — the new VM tests drove the `onHintRequested`/recorded-line design and the full targeted suite is green — but there is no standalone preceding `test(...)` commit for Task 2. Noted for gate-sequence transparency (same pattern as 04-01 Task 2).

## Notes on the 04-03b handoff (not a stub)

`hintSource`/`hintTarget`/`solverThinking`/`noWinningMove` are correctly written by the VM but not yet rendered — the `BoardCanvas` highlight, `HintControl` button + thinking spinner, D-02 "Show possible moves" rename, and `GameScreen`/`GameRoute` wiring ship in the separate **04-03b** wave (which reads exactly these fields). This is the planned logic/pixels split, not an unwired stub: the state is real and test-verified.

## Verification

- `./gradlew :domain:test --tests '*GetHintUseCaseTest*' :app:testDebugUnitTest --tests '*GameViewModelTest*' --tests '*DomainPurityTest*' --tests '*GameFlowTest*' :domain:ktlintCheck :domain:detekt :app:ktlintCheck :app:detekt` — BUILD SUCCESSFUL.
- New VM tests prove: recorded-line hint returns the recorded from/to (no Solver latency); Classic hint sets `solverThinking` synchronously then a legal highlight; grant requested with `AdReason.HINT`; a denied grant does not proceed and clears thinking; an unsolvable position sets `noWinningMove`; a second hint request cancels the prior search (only one job proceeds past the gate); deviation flips `onRecordedLine`; undo recomputes it and clears the hint; a jump clears the highlight.

## Self-Check: PASSED

- `domain/.../usecase/GetHintUseCase.kt` and `GetHintUseCaseTest.kt` exist on disk — verified.
- Commits `50d487b`, `86f8dec`, `cc4e6e1` present in `git log` — verified.
- `grep` acceptance: `withContext(defaultDispatcher)` and `requestRewarded(AdReason.HINT)` present in `GameViewModel.kt`; `hintSource`/`hintTarget`/`solverThinking`/`noWinningMove` present in `GameUiState.kt`; no `kotlinx.coroutines`/`android`/`androidx` in `GetHintUseCase.kt`.
- All targeted tests + ktlint + detekt green for `:domain` and `:app`.

---
*Phase: 04-assist-hint-undo*
*Completed: 2026-07-08*
