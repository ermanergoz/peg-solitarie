---
phase: 02-classic-mode-scoring-navigation
plan: 05
subsystem: navigation
tags: [routes, serialization, savedstatehandle, timer, one-shot-event, tdd, mvvm]

# Dependency graph
requires:
  - phase: 02-classic-mode-scoring-navigation
    plan: 01
    provides: "ScoreCalculator.stars/score + ScoreResult (pure-domain scoring)"
  - phase: 02-classic-mode-scoring-navigation
    plan: 03
    provides: "kotlin-serialization plugin on :app; navigate_back/elapsed_time strings; top_bar_icon_size dimen"
  - phase: 02-classic-mode-scoring-navigation
    plan: 04
    provides: "LevelRepository.loadClassic(levelId) seam + per-board BoardGeometry"
  - phase: 01-foundation
    provides: "GameViewModel/GameUiState/GameScreen/GameRoute, RulesEngine, Hilt DomainModule, Robolectric+Turbine harness"
provides:
  - "Type-safe @Serializable nav contract: Home, ClassicCatalog, Gameplay(levelId), Results(levelId,stars,pegsRemaining,elapsedSeconds,score)"
  - "GameViewModel loads the played board from the Gameplay(levelId) route via SavedStateHandle.toRoute + LevelRepository.loadClassic (CLAS-02)"
  - "Visible mm:ss elapsed timer that starts on the first move, ticks once/sec against an injected TimeSource, and freezes at terminal status (D-04/SCOR-02)"
  - "One-shot finished ScoreResult event (CONFLATED Channel + finishedEmitted guard) consumed once by GameRoute → Results navigation (D-05)"
  - "GameTopBar with back nav + live timer; GameOverDialog removed (Phase-1 overlay replaced by Results hop)"
affects: [02-06-results, 02-07-classic-catalog, 02-08-navhost]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Type-safe @Serializable routes as the single nav contract; args read back via toRoute<Route>()"
    - "SavedStateHandle.toRoute<Gameplay>() in the ViewModel to resolve a nav arg → repository lookup (fail-fast, no catch)"
    - "Injectable kotlin.time.TimeSource (Monotonic in prod, TestTimeSource in tests) backing a viewModelScope per-second ticker"
    - "One-shot side effect via Channel(CONFLATED).receiveAsFlow() + boolean guard, collected in a GameRoute LaunchedEffect(Unit)"
    - "Computed mm:ss display string in a scoped formatter object (named constants), not a string resource"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameTimerFormat.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/di/DomainModule.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  deleted:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameOverDialog.kt

key-decisions:
  - "Routes carry only identifying state (levelId) + already-computed Results values — never a Board/bitmask; the board is LevelRepository's single source of truth"
  - "Finish is a one-shot Channel(CONFLATED) + finishedEmitted guard, not replayable state — GameRoute collects it in LaunchedEffect(Unit) so recomposition can never re-navigate (T-02-05-NAV)"
  - "TimeSource injected (Monotonic prod / TestTimeSource test) so the ticker is deterministic under virtual time; ticker stops when status leaves Playing so the displayed time freezes at terminal"
  - "Dropped the optional Icons.Rounded.Schedule timer glyph rather than add the material-icons-extended dependency (not on the classpath); elapsed_time content description moved onto the timer Text"
  - "MainActivity passes interim stub nav lambdas — real wiring + Gameplay(levelId) route arg arrive with AppNavHost (02-08)"

requirements-completed: [CLAS-02, SCOR-02, NAV-02]

# Metrics
duration: 20min
completed: 2026-06-29
---

# Phase 2 Plan 05: Type-Safe Routes + levelId Board Load + Timer + One-Shot Results Summary

**Defined the five `@Serializable` routes the whole Phase-2 nav graph builds on, rewired `GameViewModel` to load the played board from the `Gameplay(levelId)` route arg through `LevelRepository` (killing the hardcoded English-33 inject), added a visible `mm:ss` elapsed timer that feeds `ScoreCalculator`, and replaced the Phase-1 game-over dialog with a single `finished` event that `GameRoute` turns into a one-shot navigation to Results.**

## Resume Note

This plan was completed as a **resume** after a previous executor crashed mid-flight. On resume, Task 1 (`feat 7128a85`) and the Task 2 RED test (`test ef1179f`) were already committed, and a compiling Task 2 GREEN was uncommitted in the working tree. The GREEN WIP was reviewed against the plan, found correct (board-by-levelId + timer + one-shot finish, all four behaviors covered by `GameViewModelTest`), and committed as-is; Task 3 was then implemented from scratch.

## Performance

- **Duration:** ~20 min (resume session)
- **Tasks:** 3 (Task 2 via TDD RED→GREEN)
- **Files:** 2 created, 8 modified, 1 deleted

## Accomplishments
- **`Routes.kt` nav contract:** `data object Home`, `data object ClassicCatalog`, `data class Gameplay(levelId)`, `data class Results(levelId, stars, pegsRemaining, elapsedSeconds, score)` — each `@Serializable`, KDoc'd to its requirement. This is the fixed contract 02-06/02-07/02-08 build against.
- **`DomainModule` rewire:** `provideInitialBoard()` deleted; `provideScoreCalculator()` and `provideTimeSource() = TimeSource.Monotonic` added (`provideRulesEngine` unchanged).
- **`GameViewModel` (CLAS-02/SCOR-02/D-04):** constructor now injects `LevelRepository`, `ScoreCalculator`, `TimeSource`, `SavedStateHandle`; `levelId = savedStateHandle.toRoute<Gameplay>().levelId`; `initialBoard = levelRepository.loadClassic(levelId).initialBoard` (fail-fast, no catch — T-02-05-ID). All Phase-1 select/jump/restart/statusFor logic preserved verbatim. A `viewModelScope` ticker writes whole elapsed seconds once/sec while `Playing` and stops at terminal. On the first Won/Stuck transition a single `ScoreResult` is emitted via a CONFLATED `Channel` guarded by `finishedEmitted`.
- **`GameUiState`:** gained `val elapsedSeconds: Int = 0`; the mutable flow is still only exposed via `asStateFlow()` (UDF/state-hoisting intact).
- **`GameRoute`:** gained `onFinished: (Results) -> Unit` + `onNavigateBack: () -> Unit`; a single `LaunchedEffect(Unit)` collects `viewModel.finished` and navigates once to `Results(...)` (T-02-05-NAV — no per-recomposition re-fire).
- **`GameScreen`:** extracted a stateless `GameTopBar` (back `IconButton` → `navigate_back`, live `mm:ss` timer → `elapsed_time`, `RestartControl`); removed the `GameOverOverlay` block and **deleted `GameOverDialog.kt`** (D-05). Mid-game `RestartConfirmDialog` (D-06) kept unchanged. Every dp via `dimensionResource`, every string via `stringResource`, every type/color via `MaterialTheme.*`.
- **`GameTimerFormat`:** scoped formatter rendering whole seconds as zero-padded `mm:ss` with named constants (no inline magic), satisfying QUAL-03.
- **Tests:** `GameViewModelTest` (Turbine + `TestTimeSource` + virtual time) asserts board-by-levelId, elapsedSeconds 0-until-first-move-then-increments, timer-frozen-at-terminal, and exactly-one ScoreCalculator-consistent `ScoreResult`. `GameFlowTest` retargeted off the removed dialog: it drives a board to Won and asserts the `finished` flow emits + the timer is displayed.

## Task Commits

1. **Task 1 — type-safe Routes contract + DomainModule rewire** — `7128a85` (feat, pre-resume)
2. **Task 2 RED — failing levelId/timer/one-shot spec** — `ef1179f` (test, pre-resume)
3. **Task 2 GREEN — load board by levelId + visible timer + one-shot finish** — `c0d35d5` (feat)
4. **Task 3 — top-bar timer + back nav, one-shot Results hop, drop dialog** — `8f39fe6` (feat)

_TDD gate sequence verified in git log: `test()` RED (`ef1179f`) precedes the `feat()` GREEN (`c0d35d5`)._

## Decisions Made
- **Routes carry minimal state:** `levelId` for Gameplay, already-computed values for Results — never a `Board`; the board stays `LevelRepository`'s single source of truth (re-derived from `levelId`).
- **One-shot finish, not replayable state:** a `Channel(CONFLATED)` + `finishedEmitted` boolean, collected in a `LaunchedEffect(Unit)`, so reaching terminal navigates to Results exactly once and a recomposition can never re-fire it or pollute the back stack (Pitfall 4 / T-02-05-NAV).
- **Injectable `TimeSource`:** `TimeSource.Monotonic` in prod, `kotlin.time.TestTimeSource` in tests, making the per-second ticker deterministic under `runTest` virtual time; the ticker halts when status leaves `Playing` so the on-screen time freezes at Won/Stuck.
- **No `material-icons-extended` dependency:** the spec'd `Icons.Rounded.Schedule` prefix is in the extended icon set (not on the classpath, and the plan marks it *optional*). Dropped the glyph and moved the `elapsed_time` content description onto the timer `Text` rather than pull in a heavy dependency outside this plan's scope.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated `MainActivity` to the new `GameRoute` signature**
- **Found during:** Task 3 (adding required `onFinished`/`onNavigateBack` params to `GameRoute`)
- **Issue:** `MainActivity` hosts `GameRoute()` directly (the `AppNavHost` arrives in 02-08); the new required params broke its compilation.
- **Fix:** `MainActivity` now passes interim stub lambdas (`onFinished = {}`, `onNavigateBack = {}`) with a KDoc noting 02-08's `AppNavHost` replaces this direct host and supplies the `Gameplay(levelId)` route arg. `MainActivity` was not in the plan's `files_modified` — this is a compile-unblock only.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/MainActivity.kt`
- **Committed in:** `8f39fe6`

**2. [Rule 3 - Blocking] Substituted the optional timer glyph to avoid a new dependency**
- **Found during:** Task 3 verification (`:app:compileDebugKotlin` — `Unresolved reference 'Schedule'`)
- **Issue:** `Icons.Rounded.Schedule` lives in `material-icons-extended`, which is not a project dependency; adding it was out of scope (deps are set in 02-03) and a per-rule package add is not auto-permitted.
- **Fix:** Removed the optional clock glyph (the plan marks it "optional prefix") and attached the `elapsed_time` content description to the timer `Text` via `semantics { contentDescription = ... }`, preserving the accessibility contract and the `GameFlowTest` assertion.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt`
- **Committed in:** `8f39fe6`

**3. [Rule 3 - Blocking] Extracted `GameTopBar` to satisfy detekt `LongMethod`**
- **Found during:** Task 3 verification (`:app:detekt` — `GameScreen is too long (66 > 60)`)
- **Issue:** Adding the back nav + timer to the `TopAppBar` pushed `GameScreen` past the configured 60-line `LongMethod` gate.
- **Fix:** Extracted a stateless `GameTopBar` composable; `GameScreen` delegates to it.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt`
- **Committed in:** `8f39fe6`

**Total deviations:** 3 auto-fixed (all blocking compile/gate fixes). No behavioral or scope change.

## Threat Model Coverage
- **T-02-05-ID (Tampering/DoS on `loadClassic(levelId)`)** — mitigated: the VM reads `levelId` via `toRoute<Gameplay>()` and calls `loadClassic` with **no catch**; the repository (02-04) fails fast on a forged id. The route is only ever constructed from a catalog-emitted id (02-07).
- **T-02-05-ARG (Tampering on Results args)** — mitigated: `stars/pegsRemaining/elapsedSeconds/score` are computed in-VM from the validated `Board.pegCount` and the monotonic timer, `ScoreCalculator`-clamped (stars 1–3, time term floored at 0), then passed by the typed `@Serializable` route; Results re-derives nothing trust-sensitive.
- **T-02-05-NAV (Tampering on terminal→Results re-navigation)** — mitigated: one-shot `Channel(CONFLATED)` + `finishedEmitted` guard, collected in a `LaunchedEffect(Unit)`; recomposition cannot re-fire Results.
- No new security surface beyond the plan's `<threat_model>`.

## Known Stubs
- `MainActivity` passes `onFinished = {}` / `onNavigateBack = {}` and there is **no NavHost yet**, so the running app cannot supply a `Gameplay(levelId)` route arg — gameplay is not launchable end-to-end until **02-08** wires `AppNavHost` (and **02-06** adds the Results screen the finish hop targets). This is the planned wave-3 interim state: this plan's contract (routes, VM board-load, timer, one-shot finish) is fully unit-tested; runtime wiring is 02-08's scope. Not a blocking stub for this plan's goal.

## Verification
- `./gradlew :app:testDebugUnitTest` — green (`GameViewModelTest`, `GameFlowTest`, `DomainPurityTest`, all existing suites).
- `./gradlew :app:compileDebugKotlin` — succeeds (routes + serialization resolve).
- `./gradlew :app:ktlintCheck :app:detekt` — exit 0 (no magic numbers/strings; UDF/state-hoisting preserved).
- Full combined gate `:app:testDebugUnitTest :app:compileDebugKotlin :app:ktlintCheck :app:detekt` — **BUILD SUCCESSFUL**.

## Next Phase Readiness
- **02-06 Results:** consumes the `Results` route (reads its args via `toRoute<Results>()`); the field names/types are now frozen.
- **02-07 Classic catalog:** constructs `Gameplay(levelId)` from catalog-emitted ids; `levelId` arg contract is fixed.
- **02-08 AppNavHost:** wires `GameRoute(onFinished, onNavigateBack)` into the typed back stack, supplies the `Gameplay(levelId)` SavedStateHandle arg, and replaces the interim `MainActivity` direct host.

## Self-Check: PASSED

- Files: `navigation/Routes.kt` and `ui/game/GameTimerFormat.kt` present on disk; `ui/game/GameOverDialog.kt` removed; 8 modified files updated.
- Commits: `7128a85`, `ef1179f`, `c0d35d5`, `8f39fe6` all present in git log.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-29*
