---
phase: 04-assist-hint-undo
plan: 04
subsystem: ui
tags: [no-winning-move, stuck-rescue, banner, jetpack-compose, mvvm, robolectric]

# Dependency graph
requires:
  - phase: 04-assist-hint-undo
    provides: "04-03 GameUiState.noWinningMove written by the VM hint fold; 04-01 undo replay + AdGateway seam; 04-03b hint rendering"
provides:
  - "No-winning-move / stuck-rescue banner (ASST-03): NoWinBanner leaf surfaced when the hint folds to no-win, with undo/restart affordances — no crash, no illegal move, no silent no-op"
  - "GameUiState.noWinningMove + derived showRescueBanner; stuck-rescue (narrow finished->Won, timer-resume) wired VM->screen->route"
affects: [04-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Derived UI flag (showRescueBanner) computed from noWinningMove/stuck state rather than stored separately"
    - "Stuck-rescue: a Playing-but-unwinnable board raises the rescue banner and keeps the timer running; undo clears the no-win flag and resumes normal play"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/NoWinBanner.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/dimens.xml
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "showRescueBanner is a DERIVED UiState flag (from noWinningMove + stuck), not a separately-stored field — one source of truth, cleared automatically when the underlying no-win/stuck state clears on undo/jump/restart (D-08)."
  - "A Playing-but-unwinnable board raises the rescue banner while STILL Playing (timer keeps running); undo restores a playable, rescue-free board — the 'no winning move — undo or restart' path (ASST-03) rather than a terminal state."
  - "All banner dp/sp in dimens.xml, copy in strings.xml (D-09) — no hardcoded UI values."

patterns-established:
  - "Rescue banner as a stateless leaf over derived UiState; undo/restart handlers shared with 04-01."

requirements-completed: [ASST-03]

# Metrics
duration: 90min
completed: 2026-07-13
---

# Phase 4 Plan 04: No-Winning-Move + Stuck-Rescue Banner Summary

**Surfaces 04-03's `noWinningMove` fold as a clear "no winning move — undo or restart" rescue banner (ASST-03): a Playing-but-unwinnable board raises the banner with the timer still running, and undo restores a playable, rescue-free board — never a crash, illegal move, or silent no-op.**

## Performance
- **Duration:** ~90 min (spanned a session-limit interruption + a test-harness hang diagnosis; see Issues)
- **Completed:** 2026-07-13
- **Tasks:** 3
- **Files:** 1 created (NoWinBanner.kt), 8 modified

## Accomplishments
- `NoWinBanner` stateless rescue leaf (strings/dimens-driven) surfaced when `showRescueBanner` is set.
- `GameUiState.noWinningMove` + derived `showRescueBanner`; VM raises them when the hint folds to no-win and clears them on undo/jump/restart.
- Stuck-rescue: narrow `finished -> Won` handling + timer-resume so a rescued board reaches a real terminal result.
- Wired VM -> GameScreen -> GameRoute; VM + Robolectric flow tests cover the rescue path.

## Task Commits
1. **Task 1: NoWinBanner shared rescue leaf + strings/dimens (D-08/D-09)** — `4d950e4` (feat) — prior executor
2. **Tasks 2-3: VM/state wiring + screen/route wiring + test-hang fix** — `6035840` (feat)

_Tasks 2 and 3 landed together on resume (the first executor hit a session limit mid-Task-2, and the continuation completed Task 2 + Task 3 in the working tree). The RED/GREEN discipline held during execution; no standalone split commit for the resumed portion._

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Test-harness hang: `advanceUntilIdle()` spinning on a live game ticker**
- **Found during:** verification of Task 2/3 (the `:app:testDebugUnitTest` run hung for tens of minutes at 100% CPU).
- **Root cause:** `noWinningMoveIsUndoneBackToAPlayableRescueFreeBoard` makes a move (board stays **Playing**, so the 1-second `viewModelScope` timer ticker is live), then called `advanceUntilIdle()`. With an infinite repeating ticker, `advanceUntilIdle()` advances virtual time forever and never becomes idle — a CPU spin, not a solver problem. (The solver on that tiny board returns null instantly.)
- **Fix:** switched that call to `runCurrent()` — the file's dominant pattern for Playing-board tests — which drains the queued off-thread hint job (folding to no-win) without touching the pending ticker delay. `cancelTickers()` at the test end unchanged.
- **Verification:** `GameViewModelTest` now completes in ~13s; full targeted `:app` test + ktlint + detekt green in <15s.
- **Committed in:** `6035840`.

**Total deviations:** 1 blocking auto-fix (a test-only harness bug; no production behavior change).

## Issues Encountered
- The first executor hit a **session limit** mid-plan; a continuation finished the code, but every full-suite verification then **hung** because of the ticker/`advanceUntilIdle` bug above — compounded by the harness auto-backgrounding long Gradle runs and, once, a stale orphaned Gradle test-JVM wedging the build for a day. Resolved by force-clearing Gradle, running targeted (`--tests`) `--no-daemon` verifications, per-test "started" logging to pinpoint the hanging method, and the one-line `runCurrent()` fix.

## User Setup Required
None.

## Next Phase Readiness
- All three assist affordances (hint / undo / no-winning-move) are now implemented and unit/flow-test covered end-to-end.
- **04-05 is the on-device playtest checkpoint (autonomous: false)** — the human step. Evaluate: hint highlights a real winning move (on-line and after a small deviation); undo restores the exact prior board; the no-winning-move banner shows cleanly on a genuinely stuck board.
- Carry into 04-05: 04-02's documented **solver performance envelope** — after deviation on the large Classic boards, a hint may show "no winning move" even on a solvable board (deadline-governed). Confirm this is acceptable for Slice 1 or flag a follow-up solver-improvement task.

## Self-Check: PASSED
- NoWinBanner.kt exists; 8 files modified — verified.
- Task commits present: `4d950e4`, `6035840` — verified via `git log`.
- `:app:testDebugUnitTest --tests '*NoWinBannerTest*' '*GameViewModelTest*' '*GameFlowTest*' :app:ktlintCheck :app:detekt` — BUILD SUCCESSFUL (~9-13s), no hangs.
- No scratch/diagnostic files or `/tmp` init scripts left in the repo (verified).

---
*Phase: 04-assist-hint-undo*
*Completed: 2026-07-13*
