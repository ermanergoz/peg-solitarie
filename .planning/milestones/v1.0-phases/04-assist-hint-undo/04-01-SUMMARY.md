---
phase: 04-assist-hint-undo
plan: 01
subsystem: ui
tags: [undo, adgateway, hilt, jetpack-compose, mvvm, stateflow, robolectric]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: GameViewModel move authority (moveHistory, initialBoard, statusFor), stateless GameScreen/top-bar leaf pattern (RestartControl), Robolectric Compose test harness
  - phase: 03-challenge-generator
    provides: unified LevelRef Gameplay route + level-scaled score wiring in GameViewModel
provides:
  - "Undo rescue end-to-end (ASST-02): replay-from-initialBoard undo, repeatable to start, timer-safe"
  - "AdGateway/AdReason/NoOpAdGateway platform seam + PlatformModule @Binds (D-11) — the sole Slice-2 rewarded-ad swap point, reused by 04-03 Hint"
  - "GameUiState.canUndo drives Undo control enablement"
  - "UndoControl stateless leaf + local auto-mirrored UndoIcon vector"
affects: [04-03-hint-logic, 04-03b-hint-rendering, 04-05, slice-2-ads]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Platform seam: interface + no-op auto-grant stub + @Binds @Singleton (PlatformModule) as the single Slice-2 swap point"
    - "Undo via pure replay: fold RulesEngine.applyMove over remaining moveHistory from initialBoard (no serialized/trusted state, T-04-01-U)"
    - "Local ImageVector for a single missing glyph instead of pulling material-icons-extended"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdReason.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/UndoControl.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/UndoIcon.kt
    - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/UndoControlTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "onUndo() folds RulesEngine.applyMove over the remaining moveHistory from initialBoard — undo trusts only recorded in-memory moves (T-04-01-U), and is naturally multi-step to the start."
  - "Undo deliberately never touches startMark/tickerJob/elapsedSeconds (D-07): real elapsed time keeps counting across an undo; undo resets finishedEmitted when the rebuilt board is Playing so a rescued Stuck game can re-emit a later terminal result."
  - "AdGateway is a suspend-returning-Boolean grant seam; NoOpAdGateway auto-grants (Slice 1). onUndo requests AdReason.UNDO and only proceeds on true — a denied grant is a no-op (proven by test)."
  - "Undo glyph declared as a local auto-mirrored UndoIcon ImageVector rather than adding material-icons-extended, honoring the explicit 'extended set intentionally avoided' decision in libs.versions.toml (core icon set has no undo)."
  - "GameViewModel annotated @Suppress(TooManyFunctions): the ViewModel is the UI event-handler hub (12 small public handlers); splitting the single unidirectional-data-flow authority to satisfy detekt's 11-fn limit would harm cohesion."

patterns-established:
  - "Platform gateway seam (interface + no-op stub + @Binds @Singleton) mirroring DataModule — the one Slice-2 integration point."
  - "Undo-by-replay over recorded moveHistory; canUndo mirrors moveHistory.isNotEmpty() at every write site."
  - "Single-glyph local ImageVector to avoid the heavy material-icons-extended dependency."

requirements-completed: [ASST-02]

# Metrics
duration: 60min
completed: 2026-07-07
---

# Phase 4 Plan 01: Undo Rescue + AdGateway Seam Summary

**Replay-from-initial Undo (repeatable to the start, timer-untouched, AdGateway-gated) plus the D-11 AdGateway/NoOpAdGateway platform seam that Hint (04-03) reuses unchanged.**

## Performance

- **Duration:** ~60 min (resumed a partially-complete plan)
- **Started:** 2026-07-07T01:07:09Z (Task 1, prior executor) — resumed ~02:00Z
- **Completed:** 2026-07-07T02:08:00Z
- **Tasks:** 3
- **Files modified:** 15 (8 created, 7 modified)

## Accomplishments
- Player can undo the last move — repeatedly, all the way back to the initial board — with the elapsed timer left running (D-07); the Undo control disables when there is nothing to undo.
- A rescued Stuck game returns to Playing (finishedEmitted cleared) so it can reach a later terminal result and re-navigate to Results.
- `AdGateway`/`AdReason`/`NoOpAdGateway` seam + `PlatformModule` @Binds singleton established (D-11); undo requests `AdReason.UNDO` and auto-grants for free in Slice 1. This is the single swap point 04-03 Hint reuses.
- Stateless `UndoControl` leaf wired into the gameplay top bar (order: timer, Undo, hint toggle, Restart), driven by `GameUiState.canUndo`.

## Task Commits

Each task was committed atomically:

1. **Task 1: AdGateway platform seam + Hilt binding (D-11)** - `25a5857` (feat) — committed by the prior executor
2. **Task 2: GameViewModel.onUndo() replay + GameUiState.canUndo (D-05/D-07)** - `20d5da1` (feat)
3. **Task 3: UndoControl leaf wired into top bar (ASST-02)** - `3566312` (feat)

_Note: Task 2 is a `tdd="true"` task; because it was resumed mid-flight (tests + a RED onUndo stub were already uncommitted in the working tree), RED and GREEN were not split into separate commits — the failing tests and their passing implementation landed together in `20d5da1`. See TDD Gate Compliance below._

## Files Created/Modified
- `platform/ads/AdGateway.kt` - grant seam: `suspend fun requestRewarded(reason): Boolean` + `showInterstitial()` (Task 1)
- `platform/ads/AdReason.kt` - `enum { HINT, UNDO }` (RETRY reserved for Slice 2) (Task 1)
- `platform/ads/NoOpAdGateway.kt` - Slice-1 auto-grant stub (Task 1)
- `di/PlatformModule.kt` - `@Binds @Singleton AdGateway -> NoOpAdGateway` (Task 1)
- `ui/game/GameViewModel.kt` - `onUndo()` replay, `adGateway` ctor param, `canUndo` write sites, `@Suppress(TooManyFunctions)`
- `ui/game/GameUiState.kt` - `canUndo` field + both factories
- `ui/game/UndoControl.kt` - stateless top-bar Undo IconButton
- `ui/game/UndoIcon.kt` - local auto-mirrored `undo` ImageVector
- `ui/game/GameScreen.kt` / `GameRoute.kt` - forward `canUndo`/`onUndo` through the screen and route
- `res/values/strings.xml` - `undo_action`
- test: `NoOpAdGatewayTest`, `UndoControlTest`, extended `GameViewModelTest` (6 undo cases), `GameFlowTest` (ctor/param updates)

## Decisions Made
See `key-decisions` in frontmatter. Highlights: undo is a pure replay over `moveHistory` from `initialBoard`; the timer is deliberately never touched by undo (D-07); the Undo glyph is a local `ImageVector` rather than a new dependency.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `Icons.AutoMirrored.Rounded.Undo` does not exist in the core icon set**
- **Found during:** Task 3 (UndoControl)
- **Issue:** The plan specified `Icons.AutoMirrored.Rounded.Undo`, but this project ships only `material-icons-core` — whose auto-mirrored/rounded set contains just `ArrowBack`, no `Undo`. `libs.versions.toml` explicitly documents that `material-icons-extended` is "intentionally avoided," so adding it to obtain the glyph would violate a standing codebase decision (which takes precedence over the plan).
- **Fix:** Declared a local auto-mirrored `UndoIcon` `ImageVector` (`UndoIcon.kt`) using the standard 24dp Material `undo` path — zero new dependencies. `UndoControl` references `UndoIcon` instead of the non-existent core symbol.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/game/UndoIcon.kt (new), app/src/main/java/com/app/pegsolitaire/ui/game/UndoControl.kt
- **Verification:** `:app:testDebugUnitTest` (UndoControlTest asserts enabled+clickable / disabled), ktlint, detekt all green.
- **Committed in:** `3566312` (Task 3 commit)
- **Acceptance-criteria note:** the plan's `grep -q "Icons.AutoMirrored.Rounded.Undo"` check is intentionally not satisfied (that symbol is unavailable); the equivalent behavior is met via `UndoIcon`.

**2. [Rule 3 - Blocking] detekt `TooManyFunctions` on GameViewModel**
- **Found during:** Task 2 (onUndo)
- **Issue:** Adding `onUndo()` pushed `GameViewModel` to 12 public/private functions, over detekt's default limit of 11 — failing the mandatory detekt gate.
- **Fix:** Added `@Suppress("TooManyFunctions")` to the class with an inline rationale (the ViewModel is the UI event hub; each gesture is one small handler). No threshold change to the shared detekt config.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
- **Verification:** `:app:detekt` green.
- **Committed in:** `20d5da1` (Task 2 commit)

**3. [Rule 3 - Blocking] GameScreen/GameFlowTest signature updates for the new param**
- **Found during:** Tasks 2-3
- **Issue:** Adding the `adGateway` constructor param and the `onUndo` GameScreen param broke compilation of `GameFlowTest` (constructs the VM and calls `GameScreen` directly).
- **Fix:** Passed `adGateway = NoOpAdGateway()` and `onUndo = viewModel::onUndo` in `GameFlowTest`. (This mirrors the plan's own instruction to update the test `viewModelFor` factory.)
- **Files modified:** app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
- **Verification:** full `:app:testDebugUnitTest` green.
- **Committed in:** `20d5da1` (adGateway) and `3566312` (onUndo param)

---

**Total deviations:** 3 auto-fixed (all Rule 3 - blocking). One (the Undo glyph) also honored an explicit codebase decision over the plan's assumption.
**Impact on plan:** No scope creep. All fixes were required to compile and pass the mandatory ktlint/detekt/test gates. The Undo affordance and behavior match the plan intent exactly; only the icon source differs.

## TDD Gate Compliance

Task 2 is `tdd="true"`. It was resumed after a prior executor had already written the undo tests and a RED `onUndo` stub into the working tree (uncommitted), then stalled. Rather than reconstruct an artificial separate RED commit, the completed GREEN implementation and its tests landed together in `20d5da1`. The RED→GREEN discipline still held during execution (tests existed and drove the implementation; the full suite is green), but there is no standalone `test(...)` commit preceding the `feat(...)`. Noting here for gate-sequence transparency.

## Issues Encountered
- The previous executor stalled after launching a background test. Resolved by running every build/test in the foreground (blocking) and completing the plan within a single turn.

## User Setup Required
None - no external service configuration required (the AdGateway is an in-process no-op stub in Slice 1).

## Next Phase Readiness
- The `AdGateway` seam is ready for 04-03 to reuse for `AdReason.HINT` with no changes; `PlatformModule` is the sole binding to swap in Slice 2.
- Undo is fully wired end-to-end and covered by VM + Compose tests.
- On-device manual sanity (undo repeatedly to start, timer keeps counting, Undo greys out at start) remains for the plan 04-05 checkpoint.

## Self-Check: PASSED
- All 8 created files exist on disk (verified).
- All 3 task commits present: `25a5857`, `20d5da1`, `3566312` (verified via `git log`).
- Acceptance greps pass: `moveHistory.fold`, `canUndo` (GameUiState), `bindAdGateway`, `onUndo = viewModel::onUndo`, `undo_action`.
- `:app:testDebugUnitTest :app:ktlintCheck :app:detekt` all green.

---
*Phase: 04-assist-hint-undo*
*Completed: 2026-07-07*
