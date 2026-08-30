---
phase: 08-assist-generator-quality-debt
plan: 12
subsystem: ui
tags: [kotlin, coroutines, mvvm, stateflow, robolectric, mockk, turbine-free, hint, solver]

# Dependency graph
requires:
  - phase: 08-assist-generator-quality-debt
    provides: "GetHintUseCase's single legality guard, the committed opening book and the bitboard solver (08-01..08-10) — the :domain half of DEBT-01's truthfulness claim, which this plan extends to the UI layer"
provides:
  - "Hint-search cancellation at every board-mutation site (performJump, onUndo, resetToInitial) via a single cancelHintSearch() idiom"
  - "Main-thread capture of the three hint inputs (board, recorded line, move count) before the dispatcher hop"
  - "A board-identity guard that discards a hint result computed for a superseded board"
  - "GameViewModelHintStalenessTest — a gated-dispatcher spec that reproduces the CR-01 race deterministically in virtual time"
affects: [09-ads-admob, 13-cloud-save, any phase touching GameViewModel hint or undo flow]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Gated CoroutineDispatcher test harness: park a background hop to hold an async operation in flight deterministically, with no sleeping and no flake"
    - "Cancel-then-clear contract: a site that cancels a job also owns clearing the UI flag that job would have cleared"
    - "Capture-before-hop: read mutable ViewModel state on the main thread and pass values across the dispatcher boundary, never read it on the far side"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt

key-decisions:
  - "cancelHintSearch() is called from onHintRequested too, making its occurrence count 5 rather than the plan's stated 4 — the plan's two acceptance criteria contradicted each other and the 'one cancellation idiom' invariant is the load-bearing one"
  - "The unwinnable fixture is two mutually unreachable 1x3 components, not the plan's 1x5 pegs-at-{0,1,4} strip: that strip goes Stuck on its only jump, so stuck=true would raise showRescueBanner by itself and the test could never distinguish a stale banner from an honest one"
  - "Every gate drain is followed by runCurrent(), never advanceUntilIdle() — these fixtures stay Playing on purpose, so the eager per-second ticker never lets virtual time reach idle"
  - "Each test body runs inside a try/finally that always clears the ViewModelStore; without it a FAILING assertion leaves the ticker live and runTest's own drain hangs forever instead of reporting the failure"
  - "freezeAndFinishIfTerminal was extracted from performJump purely to stay under detekt's LongMethod ceiling after the two added lines; behaviour is byte-for-byte the same logic"

patterns-established:
  - "GatedDispatcher: a CoroutineDispatcher that queues instead of dispatching, exposing pending/releaseAll — reusable for any 'mutate state while an async job is in flight' race"
  - "A stale-result guard pairs with cancellation rather than replacing it: cancellation removes the writer, the guard catches whatever still arrives"

requirements-completed: [DEBT-01]

# Metrics
duration: ~95min
completed: 2026-08-30
---

# Phase 8 Plan 12: Stale-Hint Race Gap Closure Summary

**Hint searches are now cancelled by any board mutation and their results discarded unless the board they were computed for is still on screen — closing CR-01, the one FAILED truth in `08-VERIFICATION.md`.**

## Performance

- **Duration:** ~95 min (including two hung-test diagnoses via `jstack`)
- **Tasks:** 2 (both TDD: RED then GREEN)
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments

- The UI-layer half of DEBT-01's "the hint system tells the truth" is now machine-checked. Verification's failed truth — *"Every hint the game shows is a legal move on the board the player is actually looking at"* — is pinned by a committed spec that demonstrably failed before the fix and passes after it.
- Both `missing:` items from `08-VERIFICATION.md` are delivered: (1) cancellation at `performJump` and `onUndo`, (2) main-thread capture of board/solution/movesPlayed plus a board-identity re-check before the result is applied.
- No stuck-spinner regression: the cancelling sites clear `solverThinking`, and a dedicated test re-requests a hint after a cancelled search and asserts it resolves to a legal move.
- Zero `:domain` files touched, no dependency added, no accepted override revisited.

## Task Commits

1. **Task 1: Write the failing stale-hint spec (RED)** — `5c299eb` (test)
2. **Task 2: Cancel and board-guard the hint search in GameViewModel (GREEN)** — `b6e6704` (fix)

## Files Created/Modified

- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt` (created, 506 lines) — five specs plus a file-private `GatedDispatcher`, `HintLevelRepository`, `HintSettings` and `HintAdGateway`
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` (modified, +131/−55) — `cancelHintSearch()`, the four call sites, `solverThinking` clears, main-thread input capture, the board-identity guard, and the `freezeAndFinishIfTerminal` extraction

## Evidence

### RED — the four tests that failed at HEAD, with the assertion each failed on

Run: `./gradlew :app:testDebugUnitTest --tests '*GameViewModelHintStalenessTest*'` → **BUILD FAILED**, `5 tests completed, 4 failed`.

Assertion messages taken verbatim from `app/build/test-results/testDebugUnitTest/TEST-com.app.pegsolitaire.ui.game.GameViewModelHintStalenessTest.xml`:

| Test | Failed assertion | Message at HEAD |
|------|------------------|-----------------|
| `aJumpDuringAnInFlightSearchNeverAppliesTheStaleHint` | `assertThat(state.hintSource).isNull()` | `expected: null` / `but was : Position(row=0, col=2)` |
| `anUndoDuringAnInFlightSearchNeverAppliesTheStaleHint` | `assertThat(state.hintSource).isNull()` | `expected: null` / `but was : Position(row=0, col=0)` |
| `aStaleNullResultNeverRaisesTheRescueBannerOnANewBoard` | `assertThat(state.noWinningMove).isFalse()` | `expected to be false` |
| `anUndisturbedHintStillResolvesAndTheSearchSeesTheBoardAsOfTheRequest` | `assertThat(searchedBoards).doesNotContain(afterJump)` | `expected not to contain: Board(spec=…@3c36b649, pegs=12)` / `but was : [Board(spec=…@3d30e64b, pegs=11), Board(spec=…@3c36b649, pegs=12)]` |

The fifth test, `aJumpDuringAnInFlightSearchLeavesTheHintControlUsable`, **passed at HEAD** and is stated as such rather than dressed up. It is a *regression guard*, not a race reproduction: at HEAD the uncancelled job runs to completion and clears `solverThinking` itself, so the control was never stuck. It exists because the fix introduces the risk it guards (threat T-08-12-02) — cancelling the job kills that clear, and `HintControl` renders `enabled = false` while the flag is true. The plan's `<acceptance_criteria>` anticipated exactly this by requiring "at least 4 of the 5 tests failing"; the `<action>` prose's "tests 1-4 fail" was one test optimistic.

The `doesNotContain` message is the clearest single artifact of the bug: `pegs=11` is the board at request time (cols 0,1,3), `pegs=12` the board after the jump (cols 2,3). At HEAD the background block recorded **both**, proving it read `_uiState.value.board` on the wrong side of the dispatcher hop.

### GREEN — the same five tests after the fix

Run: `./gradlew :app:testDebugUnitTest --tests '*GameViewModelHintStalenessTest*' --tests '*GameViewModelTest*' --tests '*GameFlowTest*' --tests '*HintControlTest*' --tests '*NoWinBannerTest*' --tests '*UndoControlTest*'` → **BUILD SUCCESSFUL**. The JUnit XML for the new class contains **5 `<testcase>` elements and 0 `<failure>` elements**.

Full gate: `./gradlew :app:testDebugUnitTest :domain:test ktlintCheck detekt` → **BUILD SUCCESSFUL in 1m 11s**.

### No assertion was weakened between RED and GREEN

`git diff --stat HEAD -- app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt` at the point of the GREEN commit produced **empty output** — the spec file is byte-identical to the one committed in `5c299eb`. `git status --short` before the GREEN commit listed **only** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`.

### The `withContext(defaultDispatcher)` block, post-fix

```kotlin
withContext(defaultDispatcher) {
    getHintUseCase(
        board = boardForHint,
        solution = solutionForHint,
        movesPlayed = movesPlayedForHint,
        shouldCancel = { !isActive || deadline.hasPassedNow() },
    )
}
```

Four identifiers cross the boundary and all four are immutable locals captured on the main thread immediately above (`boardForHint`, `solutionForHint`, `movesPlayedForHint`, `deadline`). **No reference to `_uiState`, `moveHistory` or `onRecordedLine` remains inside the block.** `shouldCancel` is unchanged from before the plan.

### Grep counts from both tasks' acceptance criteria

Task 1 — `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt`:

| Criterion | Required | Actual |
|-----------|----------|--------|
| `grep -c '@Test'` | 5 | **5** ✓ |
| `grep -c 'class GatedDispatcher'` | 1 | **1** ✓ |
| non-comment `grep -c 'defaultDispatcher = '` | ≥1 | **1** ✓ |
| non-comment `grep -c 'pending'` | ≥4 | **8** ✓ |
| non-comment `grep -c 'withPeg(Position(row = 0, col = [0-9])'` | 0 | **0** ✓ |
| `<verify>` exits non-zero at HEAD, ≥4 of 5 failing | yes | **exit 1, 4 failed** ✓ |
| `ktlintCheck detekt` clean before the fix landed | SUCCESS | **BUILD SUCCESSFUL** ✓ |
| `git diff --name-only` lists only the new test file | yes | **yes** ✓ |

Task 2 — `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` (all counted on non-comment lines):

| Criterion | Required | Actual |
|-----------|----------|--------|
| `grep -c 'cancelHintSearch'` | 4 | **5** — see Deviation 1 |
| `grep -c 'hintJob?.cancel()'` | 1 | **1** ✓ |
| `grep -c 'solverThinking = false'` | ≥5 | **6** ✓ |
| `grep -c 'boardForHint'` | ≥3 | **3** ✓ |
| `grep -c 'it.board != boardForHint'` | 1 | **1** ✓ |
| `git status --short` shows only `GameViewModel.kt`, nothing under `domain/` | yes | **yes** ✓ |

Verification step 5 (the CR-01 spot-check that failed in `08-VERIFICATION.md`) now shows cancellation reachable from all four sites, not only the last two:

```
267:        private var hintJob: Job? = null
287:        private fun cancelHintSearch() {
288:            hintJob?.cancel()
289:            hintJob = null
627:            cancelHintSearch()      <- performJump
828:            cancelHintSearch()      <- onUndo
906:            cancelHintSearch()      <- onHintRequested
968:            cancelHintSearch()      <- resetToInitial
```

## Decisions Made

- **`cancelHintSearch()` is the single cancellation idiom, including in `onHintRequested`.** The plan's two acceptance criteria could not both hold (see Deviation 1); the one that survives is "cancellation lives in one place", because two idioms that can drift is the defect class CR-01 belongs to.
- **The unwinnable fixture became two detached 1x3 components.** The plan's 1x5 strip with pegs at {0,1,4} has exactly one legal jump, and that jump strands {2,4} with no move left — so `performJump` sets `stuck = true` and `showRescueBanner` is true *by construction*, independent of any stale search. The test would have failed even against a correct fix, and could never have distinguished a stale banner from an honest Stuck. Four pegs in two mutually unreachable components can only ever reduce to two, so the hint is an honest `null`, while the second component keeps the post-jump board `Playing`.
- **`runCurrent()` everywhere, never `advanceUntilIdle()`.** Also the tighter assertion: the `withContext` resumption is scheduled at the current virtual instant, so no time needs to pass for a result to land.
- **The gate is asserted before every mutation.** Each test checks `gate.pending == 1` before touching the board, so "the search really was in flight" is proven rather than assumed — without it, a refactor that made the hint resolve synchronously would leave all five tests passing vacuously.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Resolved a self-contradiction in Task 2's acceptance criteria**
- **Found during:** Task 2
- **Issue:** Criterion A requires `grep -c 'cancelHintSearch'` to return exactly `4` ("declaration + `performJump` + `onUndo` + `resetToInitial`"). Criterion B requires `grep -c 'hintJob?.cancel()'` to return `1` **and** that "`onHintRequested` starts its new request by calling `cancelHintSearch()` rather than cancelling inline". Both cannot hold: honouring B makes the `cancelHintSearch` count 5; honouring A leaves an inline `hintJob?.cancel()` in `onHintRequested` and makes B's count 2.
- **Fix:** Honoured criterion B. `onHintRequested` calls `cancelHintSearch()`, so `cancelHintSearch` appears **5** times and `hintJob?.cancel()` appears **1** time. The semantic invariant the plan's `<action>` step 1 states in prose — "one cancellation idiom rather than two that can drift" — is satisfied exactly; only the literal count differs.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`
- **Verification:** Both greps re-run and recorded in the table above.
- **Committed in:** `b6e6704`

**2. [Rule 3 - Blocking] Extracted `freezeAndFinishIfTerminal` from `performJump` to clear the detekt gate**
- **Found during:** Task 2
- **Issue:** The two lines this plan adds to `performJump` (`cancelHintSearch()` and `solverThinking = false,`) pushed it from 59 to 61 body lines. `./gradlew detekt` failed: `GameViewModel.kt:615:21 The function performJump is too long (61). The maximum length is 60. [LongMethod]`. detekt is a mandatory build gate per CLAUDE.md, so this blocked the task.
- **Fix:** Moved the post-jump terminal block (timer freeze, `finishedEmitted` latch, snapshot clear, Challenge advance, `_finished.trySend`) into a new `private fun freezeAndFinishIfTerminal(status, nextBoard)`, replacing the inline `if (status != GameStatus.Playing) { … }` with a single call. The nested `if`s became guard-clause early returns; the logic and its ordering are otherwise unchanged, and every comment travelled with the code it explains.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`
- **Verification:** `./gradlew detekt` BUILD SUCCESSFUL; `GameViewModelTest`, `GameFlowTest`, `GameViewModelChallengeAdvanceTest` and the rest of `:app:testDebugUnitTest` pass unedited, which is what proves the extraction is behaviour-preserving (the Won/Stuck/advance paths are heavily covered there).
- **Committed in:** `b6e6704`

**3. [Rule 1 - Bug] Replaced the plan's `movableButUnwinnableStrip` fixture**
- **Found during:** Task 1
- **Issue:** The plan specifies a 1x5 strip with pegs at cols 0, 1, 4. Its only legal jump (0→2) strands {2,4} with no legal move, so `performJump` sets `stuck = true` and `GameUiState.showRescueBanner` (`= stuck || noWinningMove`) is true regardless of the search. Test 4's `assertThat(state.showRescueBanner).isFalse()` would therefore fail against a *correct* fix, and the test could never distinguish a stale banner from an honest Stuck.
- **Fix:** `movableButUnwinnableBoard()` — two mutually unreachable 1x3 rows (rows 0 and 2, no shared edge), each with pegs at cols 0 and 1. Four pegs in two components can only ever reduce to two, so the hint on the start position is an honest `null`; the second component keeps the post-jump board `Playing`, so `stuck` is false and any banner is attributable to the stale search alone. The test asserts `stuck` is false explicitly before releasing the gate, so the fixture cannot silently regress into the plan's version.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt`
- **Verification:** The test fails at HEAD on `noWinningMove` (`expected to be false`) and passes after the fix — exactly the discrimination the plan intended.
- **Committed in:** `5c299eb`

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 bug)
**Impact on plan:** No scope creep. Two were forced by the plan's own internal inconsistencies (contradictory grep criteria; a fixture that contradicted its own assertion), one by a build gate the plan explicitly names as mandatory. Every stated behavioural goal of the plan is delivered unchanged.

## Issues Encountered

Two hangs, both diagnosed with `jstack` against the Gradle test worker rather than guessed at. Both are properties of this ViewModel that any future async spec on it will meet:

1. **`advanceUntilIdle()` never returns while the board is `Playing`.** The first run sat for 30 minutes with zero test results. The thread dump showed `TestCoroutineScheduler.advanceUntilIdle` spinning inside `aStaleNullResultNeverRaisesTheRescueBannerOnANewBoard`. `startTimerIfNeeded()` launches a ticker that re-`delay(1s)`s for as long as the game is `Playing`, so virtual time can be advanced forever and idle is never reached. These fixtures stay `Playing` deliberately, so every drain became `runCurrent()`.

2. **A *failing* assertion made the suite hang instead of reporting.** After fix 1, the run hung again — this time in `runTest`'s own end-of-test drain (`TestBuilders.kt:370`), which is not covered by the test timeout. The cause: `cancelTickers()` was the last statement of each test body, so a failed assertion threw past it, leaving the ticker live for the drain to spin on. This is a nasty failure mode for a RED-first plan — the spec would have been *unable to go red*. Fixed by a `hintTest { }` wrapper that runs each body in `try { … } finally { cancelTickers() }`. The `finally` is documented in the file as load-bearing.

Also worth recording for anyone running this worktree: `local.properties` is gitignored, so a fresh worktree has no `sdk.dir` and every Gradle task fails with "SDK location not found". Writing `sdk.dir=/Users/ermanergoz/Library/Android/sdk` unblocks it; the file is correctly not committed.

## User Setup Required

None — no external service configuration, no new dependency, no version bump. `gradle/libs.versions.toml` is untouched, so threat `T-08-12-SC` (package legitimacy) has nothing to gate.

## Threat Model Outcome

| Threat ID | Disposition | Outcome |
|-----------|-------------|---------|
| T-08-12-01 | mitigate | **Closed, twice over.** `cancelHintSearch()` at `performJump` and `onUndo` removes the writer; the `it.board != boardForHint` guard discards any result that still arrives. Pinned by tests 1, 3 and 4, each of which failed at HEAD. |
| T-08-12-02 | mitigate | **Closed.** `solverThinking = false` added to both mutation state updates (repo count 6). Pinned by test 2, which re-requests a hint after a cancelled search and asserts it resolves to a member of `RulesEngine.legalMoves(currentBoard)`. |
| T-08-12-03 | accept | Unchanged and still not applicable — no logging, persistence, IPC, permission or identity path was added; cross-thread reads were **removed**, not added. |
| T-08-12-SC | accept | Unchanged — no dependency added or bumped. |

No new threat surface found. No `threat_flag` raised: the change adds no network endpoint, no auth path, no file access and no schema change.

## Known Stubs

None. No hardcoded empty value, placeholder string or unwired data source was introduced.

## Next Phase Readiness

- **Phase 8's blocking gap is closed.** `08-VERIFICATION.md`'s single FAILED truth is now backed by a committed, deterministic test. Re-verification should flip truth #? ("Every hint the game shows is a legal move on the board the player is actually looking at") from FAILED to VERIFIED, taking the phase to 13/14 with the one remaining item the deliberately-deferred device audit.
- **Out of scope and still open, as planned:** the two non-blocking review warnings (WR-01 snapshot scope, WR-02 duplicated cue carry-forward), the three developer-approved overrides (Wiegleb-45 K=3, English-33's four proven-dead markers, the on-device checklist), and the 444 header-declared book gaps on Diamond-41/Wiegleb-45. None was touched.
- **`GatedDispatcher` is reusable.** Phase 9 (AdMob) will introduce genuinely slow, genuinely cancellable ad loads behind `AdGateway`; the same harness parks them deterministically without a real SDK.
- **Note for the orchestrator:** `STATE.md`, `ROADMAP.md` and `REQUIREMENTS.md` were deliberately NOT modified — this ran as a worktree agent. `REQUIREMENTS.md` still lists DEBT-01/02/03 as `Pending`, the bookkeeping gap `08-VERIFICATION.md` flagged as a WARNING; this plan completes DEBT-01's last open item.

## Self-Check: PASSED

- Files claimed created/modified all exist on disk: `GameViewModelHintStalenessTest.kt` (21351 B), `GameViewModel.kt` (55307 B), `08-12-SUMMARY.md` (20917 B).
- Commits claimed all exist in `git log`: `5c299eb` (RED), `b6e6704` (GREEN), `61d2c26` (this SUMMARY).
- Shared orchestrator artifacts untouched: `STATE.md`, `ROADMAP.md` and `REQUIREMENTS.md` appear in no commit of this plan.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-30*
