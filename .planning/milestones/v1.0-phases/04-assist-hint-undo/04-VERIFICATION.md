---
phase: 04-assist-hint-undo
verified: 2026-07-13T00:00:00Z
status: passed
score: 4/4 must-haves verified
overrides_applied: 0
re_verification: false
---

# Phase 4: Assist — Hint + Undo Verification Report

**Phase Goal:** Players can request a hint that reveals a genuine next winning move and can undo their last move, with both affordances free in Slice 1 via the no-op AdGateway auto-grant stub. Hint logic is state-relative (never blind solution[moveCount] indexing): canonical line first, off-thread budgeted Solver fallback on deviation, and an explicit 'no winning move' state when the position is genuinely unsolvable.
**Verified:** 2026-07-13
**Status:** PASSED
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | While on the recorded solution line, the player can tap "hint" and see a valid next winning move highlighted | VERIFIED | `GetHintUseCase.invoke`: returns `solution[movesPlayed]` when in range AND legal. `GameViewModel.onHintRequested` writes `hintSource`/`hintTarget`. `BoardCanvas.drawHintHighlight` draws source glow ring + target ring + A→B arrow in tertiary when `interactive && hintSource != null && hintTarget != null`. `HintControl` is wired via `GameRoute` (`onHint = viewModel::onHintRequested`). VM test `hintOnTheRecordedLineReturnsTheRecordedMoveWithoutSolverLatency` locks this path. |
| 2 | After deviating from the canonical line, the hint falls back to the Solver (Dispatchers.Default, node/time-budgeted) and still returns a legal, win-preserving move (within documented performance envelope) | VERIFIED (within documented envelope) | `GetHintUseCase` falls through to `Solver.firstWinningMove(board, budget, shouldCancel)` when `solution == null` (off-line/Classic). `onHintRequested` runs it in `withContext(defaultDispatcher)` with `shouldCancel = { !isActive || deadline.hasPassedNow() }`. `SolverBudget.DEFAULT` caps at 40M nodes / 1.5s deadline. `onRecordedLine` tracked per-move and recomputed on undo; never blind-indexed (`solution.takeIf { onRecordedLine }`). ENVELOPE CAVEAT (documented at 04-02, accepted by user at 04-05 playtest): on hard early positions of larger Classic boards (European-37 / Asymmetric-39 / Wiegleb-45) and a few English-33 mid-positions the Solver exceeds the 1.5s deadline and D-04 folds to "no winning move". The recorded-line path (criterion 1) and endgame/easy positions always resolve. This degradation is by design and was explicitly accepted. |
| 3 | When the position is genuinely unsolvable, the player sees a clear "no winning move — undo or restart" state rather than a crash, illegal move, or silent no-op | VERIFIED | `Solver.firstWinningMove` returns `null` when pagoda-rejected, budget-missed, cancelled, or provably exhausted. `onHintRequested` folds `null` to `noWinningMove = true`. `GameUiState.showRescueBanner = stuck || noWinningMove`. `GameScreen` renders `NoWinBanner` inline over the still-interactive board when `showRescueBanner`. Banner text: "No winning move — undo or restart" (`nowin_message`) / "No moves left — undo or restart" (`stuck_message`). Banner offers Undo · Restart · Home. `GameFlowTest.reachingAStuckPositionSurfacesTheRescueBannerOverTheStillPresentBoard` validates board remains present beneath banner. Stuck boards no longer auto-navigate to Results — `finished` emission narrowed to `GameStatus.Won` only (line 327, `GameViewModel`). |
| 4 | Player can undo the last move, restoring the exact prior board state | VERIFIED | `onUndo()` pops from `moveHistory`, replays via `moveHistory.fold(initialBoard) { b, m -> rulesEngine.applyMove(b, m) }` (line 433, `GameViewModel`). Multi-step to initial board: repeatable. Timer untouched: `startMark`/`tickerJob`/`elapsedSeconds` not touched in `onUndo` (inline comment D-07). `UndoControl` disabled when `canUndo == false`. `GameRoute` wires `onUndo = viewModel::onUndo`. VM tests: `undoRestoresTheExactPriorBoardAndDropsTheLastMove`, `repeatedUndoWalksAllTheWayBackToTheInitialBoardAndRescuesAStuckGame`, `undoDoesNotTouchTheElapsedTimer`, `undoWithNoMovesIsANoOp`. |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/.../platform/ads/AdGateway.kt` | AdGateway interface with `suspend requestRewarded` + `showInterstitial` | VERIFIED | 25 lines; interface declares both methods with KDoc |
| `app/.../platform/ads/AdReason.kt` | Enum `HINT`, `UNDO` (no `RETRY`) | VERIFIED | Exactly two members; RETRY reserved noted in KDoc |
| `app/.../platform/ads/NoOpAdGateway.kt` | Slice-1 auto-grant stub returning `true` | VERIFIED | `requestRewarded` returns `true`; `showInterstitial` returns `Unit` |
| `app/.../di/PlatformModule.kt` | `@Binds @Singleton AdGateway -> NoOpAdGateway` | VERIFIED | `bindAdGateway` present, `@InstallIn(SingletonComponent::class)` |
| `domain/.../solver/Solver.kt` | `firstWinningMove(board, budget, shouldCancel): Move?` pure domain | VERIFIED | 322 lines; memoised symmetry-canonical DFS + VisitedSet + jittered restarts; no coroutines; uses `RulesEngine.legalMoves`/`applyMove` |
| `domain/.../solver/SolverBudget.kt` | `maxNodes` + `deadline` budget + `DEFAULT` | VERIFIED | `DEFAULT_MAX_NODES = 40_000_000L`, `DEFAULT_DEADLINE = 1_500.milliseconds`, `CANCEL_MASK = 4_095L`; no magic numbers |
| `domain/.../solver/Pagoda.kt` | `mayBeSolvable(board): Boolean` fast-reject | VERIFIED | 68 lines; mod-3 colour-parity necessary condition on two colourings; O(cells); board-agnostic |
| `domain/.../usecase/GetHintUseCase.kt` | Recorded-line-first -> Solver -> null | VERIFIED | 59 lines; `solution.getOrNull(movesPlayed)` with legality guard before returning; no Android/coroutines |
| `app/.../ui/game/UndoControl.kt` | Stateless Undo IconButton (enabled by `canUndo`) | VERIFIED | 47 lines; uses `UndoIcon`, `undo_action`, `top_bar_icon_size`; `enabled = canUndo` |
| `app/.../ui/game/HintControl.kt` | Hint IconButton + thinking `CircularProgressIndicator` | VERIFIED | 69 lines; `Icons.Rounded.Search` resting; disabled `CircularProgressIndicator` + description on button during thinking |
| `app/.../ui/game/NoWinBanner.kt` | Inline rescue banner (M3 Surface, Undo/Restart/Home) | VERIFIED | 95 lines; `errorContainer` + `onErrorContainer`; `shapes.medium`; three `TextButton`s; `nowin_banner_min_height` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GameViewModel.onUndo` | Board replay from `initialBoard` | `moveHistory.fold(base) { b, m -> rulesEngine.applyMove(b, m) }` | WIRED | Line 433, `GameViewModel.kt` |
| `PlatformModule` | `NoOpAdGateway` | `@Binds @Singleton bindAdGateway(impl: NoOpAdGateway): AdGateway` | WIRED | Line 23, `PlatformModule.kt` |
| `Solver` | `RulesEngine.legalMoves` / `applyMove` | DFS expansion uses both (lines 152 + 160) | WIRED | No custom jump logic in Solver |
| `Solver` | `budget + shouldCancel` | `seen.size < budget.maxNodes` + periodic `shouldCancel()` via `CANCEL_MASK` | WIRED | Lines 144, 155, `Solver.kt` |
| `GameViewModel.onHintRequested` | `GetHintUseCase` on `defaultDispatcher` | `withContext(defaultDispatcher) { getHintUseCase(...) }` | WIRED | Line 499, `GameViewModel.kt` |
| `GameViewModel.onHintRequested` | `GameUiState.hintSource/hintTarget/solverThinking/noWinningMove` | State update after use-case resolution | WIRED | Lines 507-519, `GameViewModel.kt` |
| `BoardCanvas.drawBoard` | `hintSource`/`hintTarget` | `if (interactive && hintSource != null && hintTarget != null) drawHintHighlight(...)` | WIRED | Lines 194-197, `BoardCanvas.kt` |
| `GameRoute` | `GameViewModel.onHintRequested` | `onHint = viewModel::onHintRequested` | WIRED | Line 62, `GameRoute.kt` |
| `GameRoute` | `GameViewModel.onUndo` | `onUndo = viewModel::onUndo` | WIRED | Line 61, `GameRoute.kt` |
| `GameScreen` | `NoWinBanner` | `if (state.showRescueBanner) NoWinBanner(...)` | WIRED | Line 164, `GameScreen.kt` |
| `GameViewModel` | `finished` emission narrowed to Won | `if (status == GameStatus.Won && !finishedEmitted)` | WIRED | Line 327, `GameViewModel.kt`; Stuck sets `stuck = true` instead |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `BoardCanvas` | `hintSource` / `hintTarget` | `GameUiState` set by `onHintRequested` after `GetHintUseCase` resolves | Yes — use case returns recorded move or Solver result | FLOWING |
| `GameScreen` / `NoWinBanner` | `showRescueBanner` | Derived: `stuck || noWinningMove`; `stuck` set when `GameStatus.Stuck`; `noWinningMove` set when hint folds to null | Yes — both paths write real state | FLOWING |
| `UndoControl` | `canUndo` | `GameUiState.canUndo = moveHistory.isNotEmpty()` updated at every write site in VM | Yes — reflects live move history | FLOWING |

### Behavioral Spot-Checks

Step 7b is not applicable here: the project is an Android app with no runnable JVM entry points outside the test suite. The instrumented Compose tests (`GameFlowTest`, `HintControlTest`, `UndoControlTest`, `NoWinBannerTest`) serve as the behavioral spot-checks and were run as part of the `./gradlew check` gate confirmed green at the 04-05 checkpoint. Manual on-device verification was performed and approved (04-05 playtest).

### Probe Execution

No conventional `scripts/*/tests/probe-*.sh` probes declared or applicable to this phase. The `./gradlew check` gate (`:domain:test`, `:app:testDebugUnitTest`, `DomainPurityTest`, detekt, ktlint, dp/sp gate) was confirmed green in 04-05-SUMMARY.md ("BUILD SUCCESSFUL in ~33s"). Targeted re-run was not performed per verification-method instructions (build is heavy; green-build evidence is current session).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| ASST-01 | 04-02, 04-03, 04-03b, 04-05 | Player can use a hint that reveals the next winning move (free in Slice 1) | SATISFIED | `GetHintUseCase` returns recorded move on-line; `Solver.firstWinningMove` on deviation/Classic. `BoardCanvas` renders source glow + target ring + A→B arrow. `HintControl` wired. Human playtest approved. |
| ASST-02 | 04-01, 04-05 | Player can undo the last move | SATISFIED | `onUndo()` replay from `initialBoard` via `moveHistory.fold`; `UndoControl` wired; timer untouched (D-07); `canUndo` drives enablement. Human playtest approved. |
| ASST-03 | 04-02, 04-03, 04-04, 04-05 | Hint handles deviation — falls back to the solver, and shows a clear "no winning move" state when the position is unsolvable | SATISFIED | Solver fallback on deviation/Classic; `noWinningMove = true` when null; `NoWinBanner` renders over still-interactive board; stuck boards raise same banner; `finished` narrowed to Won only. Human playtest approved. |

### Anti-Patterns Found

No unreferenced debt markers (`TBD`, `FIXME`, `XXX`) found in any phase-modified file.

One `return null` in `Solver.kt` (line 71) is an intentional, documented fast-path for already-solved boards and pagoda-rejected positions — not a stub. It feeds the correct "no winning move" state.

`AdReason.kt` mentions RETRY "reserved for Slice-2" in KDoc — this is forward-looking documentation in a comment, not a `TODO` or `FIXME`, and is not a debt marker.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | — | — | — | No anti-patterns found |

### Human Verification Required

All human verification items were satisfied at the 04-05 on-device playtest checkpoint, which was explicitly marked "approved" by the user. Items verified:

1. **Challenge hint on recorded line** — source glow + target ring + faint A→B arrow visible; hint never auto-plays the move; player performs it themselves.
2. **Solver-fallback thinking state** — brief `CircularProgressIndicator` on `HintControl` while Solver runs on Classic and post-deviation boards; legal win-preserving move highlighted afterward (within performance envelope).
3. **No-win / stuck rescue banner** — inline non-blocking banner over still-tappable board with Undo · Restart · Home; board does not auto-navigate to Results on stuck/no-win.
4. **Multi-step Undo rescue** — board backs out step by step; timer keeps counting; banner clears on undo to a playable position.
5. **Won unchanged** — solving to one peg still proceeds to the Results screen.

All items approved. No outstanding human verification items remain.

### Gaps Summary

No gaps. All four success criteria are verified:
- Criterion 1 (recorded-line hint): fully verified in code and tests.
- Criterion 2 (Solver fallback): verified within the documented performance envelope; the envelope (hard early positions on large boards fold to "no winning move" under the 1.5s deadline) is by design (D-04), measured (04-02), documented, and explicitly accepted by the user at the 04-05 playtest.
- Criterion 3 (no winning move state): fully verified in code, tests, and on-device playtest.
- Criterion 4 (undo): fully verified in code and tests.

---

_Verified: 2026-07-13_
_Verifier: Claude (gsd-verifier)_
