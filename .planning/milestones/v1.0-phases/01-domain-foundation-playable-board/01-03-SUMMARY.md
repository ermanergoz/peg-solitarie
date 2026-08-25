---
phase: 01-domain-foundation-playable-board
plan: 03
subsystem: ui-viewmodel
tags: [kotlin, mvvm, stateflow, hilt, compose-immutable, turbine, jvm-test, security-seam]

# Dependency graph
requires:
  - 01-01 — Hilt + KSP applied (@HiltViewModel needs no further build setup); :app test source set
  - 01-02 — RulesEngine (legalMoves/applyMove/isGameOver/pegCount), Boards.english33Initial, Position/Move/GameStatus/Cell/Board/BoardSpec
provides:
  - "@Immutable GameUiState (board, selected, legalTargets, status, animating, confirmRestart) + GameUiState.initial() factory"
  - "@Immutable MoveTransition (from, to, removed, id=moveHistory.size) — the Canvas animation key"
  - "GameViewModel: read-only StateFlow<GameUiState> with select/deselect/two-step-jump/restart/game-over logic; RulesEngine as sole move authority"
  - "Hilt DomainModule providing RulesEngine (singleton) + Boards.english33Initial (initial Board)"
affects: [01-04 BoardCanvas (renders GameUiState, emits onCellTap), 01-05 game-flow (GameRoute/GameScreen + dialogs)]

# Tech tracking
tech-stack:
  added:
    - Turbine 1.2.1 + kotlinx-coroutines-test 1.11.0 (app test deps — StateFlow emission assertions)
    - androidx.lifecycle:lifecycle-viewmodel 2.9.4 (non-Compose ViewModel base; pinned for compileSdk 36)
  patterns:
    - "Single immutable @Immutable UiState over a private MutableStateFlow exposed read-only (Pitfall 10)"
    - "Animate-from-committed-state: domain move applied FIRST, MoveTransition set after (RESEARCH Q5)"
    - "Game-over as replayable status, never a one-shot effect (T-03-02)"
    - "Security seam T-03-01: VM re-derives the Move from RulesEngine.legalMoves; only a member is ever applied"
    - "Custom tiny BoardSpec fixtures in tests to reach Won/Stuck terminal lines in one jump"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/MoveTransition.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/di/DomainModule.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  modified:
    - app/build.gradle.kts
    - gradle/libs.versions.toml

key-decisions:
  - "GameViewModel is the sole move authority — every selection rebuilds legalTargets from RulesEngine.legalMoves and a jump only applies a Move that is a member of that set (T-03-01); an illegal/forged destination is a silent no-op"
  - "Game-over modeled as GameUiState.status ∈ {Won, Stuck} (replayable state surviving rotation), not a Channel effect (T-03-02)"
  - "Two-step jump always (D-03): tapping a highlighted destination performs the jump; there is no auto-jump branch even for a single legal option"
  - "Pinned androidx.lifecycle to 2.9.4 (atomic group) — 2.11.0's -compose variants require compileSdk 37 (uninstallable here); 2.9.4 matches what the Compose BOM 2026.06.00 already resolves transitively"

requirements-completed: [GAME-02, GAME-04, GAME-05]

# Metrics
duration: 9min
completed: 2026-06-28
---

# Phase 1 Plan 03: GameViewModel & Immutable GameUiState Summary

**The `GameViewModel` turns untrusted taps into validated domain moves over an immutable `StateFlow<GameUiState>` — tap-to-select with engine-derived legal-destination highlighting (GAME-04), always-two-step jumps (D-03), mid-game-confirmed restart (GAME-05), and state-driven Won/Stuck game-over (GAME-02) — with `RulesEngine` as the sole move authority, fully Turbine-tested on the JVM and wired through a Hilt `DomainModule`.**

## Performance

- **Duration:** ~9 min
- **Tasks:** 2 (1 plain + 1 TDD)
- **Files:** 5 created, 2 modified
- **Commits:** 3 task commits (Task 1 feat; Task 2 RED test + GREEN feat) + final docs commit

## Accomplishments

- **Immutable UiState contract (GAME-04):** `GameUiState` (`@Immutable`, all `val`) holds `board`, `selected`, `legalTargets`, `status`, `animating`, `confirmRestart`, with a `GameUiState.initial(board)` factory that builds the deterministic English-33 start (D-01). `MoveTransition(from, to, removed, id)` keys the jump/removal animation on `id = moveHistory.size` (D-07/D-08). No game logic in either file — pure state shape.
- **GameViewModel (GAME-02/04/05):** `@HiltViewModel`, constructor-injected `RulesEngine` + initial `Board`. A private `MutableStateFlow<GameUiState>` is exposed only as read-only `val uiState: StateFlow<GameUiState>` (Pitfall 10). `onCellTap` implements the exact locked tap table — deselect-on-re-tap before jump-on-legal-destination before (re)select, so empty/blocked/illegal cells fall through to a no-op (D-04). Restart raises `confirmRestart` only mid-game and resets immediately at an end-state or with no moves (D-06).
- **Security seam (T-03-01):** `performJump` never trusts the tap-asserted destination — it re-derives the `Move` from `RulesEngine.legalMoves(board)` and applies it only if present; otherwise no state change. Proved by a test that forges a non-legal destination while a peg is selected and asserts the state is unchanged.
- **Animate-from-committed-state (RESEARCH Q5):** the domain move is applied (`applyMove`) and `status` recomputed BEFORE `animating` is set, so the board in `GameUiState` is always the post-move board and the Canvas is a pure render.
- **Hilt wiring:** `DomainModule` (`@InstallIn(SingletonComponent)`) `@Provides` `RulesEngine` (singleton) and `Boards.english33Initial`. `assembleDebug` generates `GameViewModel_Factory` + `GameViewModel_HiltModules` end-to-end and dexes — the VM is injectable via `hiltViewModel()` in Plans 04/05 with zero further setup.
- **JVM test coverage:** 12 Turbine tests over `uiState` emissions cover selection highlighting, re-tap deselect, different-peg reselect, jump (pegCount −1 + `animating` set + transition fields), `onAnimationFinished` clearing, blocked-cell no-op, the forged-move no-op, Won/Stuck terminal status via custom tiny boards, mid-game restart confirm→reset, dismiss-keeps-game, and end-state restart-without-confirm.

## Task Commits

1. **Task 1 — GameUiState + MoveTransition** — `ac11524` (feat)
2. **Task 2 — GameViewModel + DomainModule (TDD)** — RED `a92d2d6` (test + test/runtime wiring), GREEN `b665c54` (feat)

## Decisions Made

- **RulesEngine as sole authority / no-trust taps (T-03-01):** the VM rebuilds legal targets from the domain on every selection and re-derives the `Move` before applying — the UI can never submit a forged legal move.
- **Game-over is replayable state, not an effect (T-03-02):** `status` lives in the immutable UiState so the Phase-1 dialog (D-05) survives rotation and re-renders idempotently; effects are reserved for nav/snackbar (none in Phase 1).
- **Always two-step (D-03):** no auto-jump branch — selecting a peg with a single legal target still requires a second tap, identical to the multi-target case.
- **lifecycle 2.9.4 pin:** see Deviation #2 — an atomic-group + compileSdk-36 constraint, not a feature choice.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added Turbine + coroutines-test + ViewModel deps to `:app`**
- **Found during:** Task 2 (RED — test could not compile/run without them)
- **Issue:** The plan's `GameViewModelTest` requires Turbine + `kotlinx-coroutines-test`, and `GameViewModel` extends `androidx.lifecycle.ViewModel`; none were declared in `app/build.gradle.kts` (catalog had Turbine/coroutines-test but `:app` didn't consume them, and no lifecycle-viewmodel artifact was wired).
- **Fix:** Added `testImplementation(libs.turbine)`, `testImplementation(libs.kotlinx.coroutines.test)`, and `implementation(libs.androidx.lifecycle.viewmodel)` (new catalog entry for the non-Compose ViewModel base). These are catalog-declared, official artifacts — not a new package install.
- **Files modified:** app/build.gradle.kts, gradle/libs.versions.toml
- **Committed in:** `a92d2d6` (RED)

**2. [Rule 3 - Blocking] Pinned androidx.lifecycle 2.11.0 → 2.9.4 (atomic group vs compileSdk 36)**
- **Found during:** Task 2 (RED — `:app:checkDebugAarMetadata` failed)
- **Issue:** `androidx.lifecycle` is an **atomic group**: depending on `lifecycle-viewmodel` at the catalog's `androidxLifecycle = 2.11.0` forced the whole group — including `lifecycle-runtime-compose-android:2.11.0` and `lifecycle-viewmodel-compose-android:2.11.0` — to 2.11.0, whose `-compose` variants require `compileSdk 37` (uninstallable in this environment; the same class of issue Plan 01 hit with androidx.core). Plan 01's build passed only because the Compose BOM resolved lifecycle to 2.9.4 transitively.
- **Fix:** Lowered the catalog `androidxLifecycle` pin to **2.9.4** (highest line whose `-compose` variants keep `minCompileSdk 36`, and exactly what Compose BOM 2026.06.00 already resolves). This also pre-empts the same break for Plan 04's `collectAsStateWithLifecycle` (`lifecycle-runtime-compose`).
- **Files modified:** gradle/libs.versions.toml
- **Committed in:** `a92d2d6` (RED)

**Total deviations:** 2 auto-fixed (both blocking build wiring). No scope change; no must-have weakened — both were prerequisites to make the planned ViewModel + its tests build under the pinned compileSdk 36.

## Authentication Gates

None — pure in-memory ViewModel + domain logic, no I/O, no network, no credentials.

## Known Stubs

None. `GameViewModel` is fully implemented and tested; `DomainModule` provides real domain instances. (The Canvas render and game-flow screen are out of this plan's scope — Plans 04/05.)

## Threat Flags

None — no new security surface beyond the single trust boundary the plan's `<threat_model>` already names (UI tap → VM). Both dispositions are mitigated in code and by test: T-03-01 (forged move never applied) and T-03-02 (game-over as replayable status).

## Verification Evidence

- `./gradlew :app:compileDebugKotlin` — exit 0 (Task 1 acceptance).
- `./gradlew :app:testDebugUnitTest --tests "*GameViewModelTest"` — 12 tests green (Task 2 acceptance).
- `./gradlew ktlintCheck detekt :domain:test test` — exits 0 (full wave gate; no regressions; `:domain` golden-master + ArchUnit purity still green).
- `./gradlew :app:assembleDebug` — exits 0; Hilt generated `GameViewModel_Factory` + `GameViewModel_HiltModules` (KSP, no KAPT); APK dexes — DI wiring verified end-to-end.
- `:app:dpSpLiteralCheck` — green (no raw dp/sp literals; `ui/game` holds only state/logic).

## Next Plan Readiness

- Plan 04 (`BoardCanvas`) renders `GameUiState` and emits `onCellTap(Position)` upward; geometry/animation read `board`/`legalTargets`/`selected`/`animating`. `MoveTransition.id` is the `Animatable` key. The lifecycle 2.9.4 pin keeps `lifecycle-runtime-compose` (`collectAsStateWithLifecycle`) on a compileSdk-36-compatible line.
- Plan 05 (`GameRoute`/`GameScreen`) injects `GameViewModel` via `hiltViewModel()` (graph generates) and renders the D-05 game-over dialog from `status` and the D-06 restart confirm from `confirmRestart`.

## Self-Check: PASSED

- All 5 created files + 2 modified files exist on disk (verified below).
- All 3 task commits present (`ac11524`, `a92d2d6`, `b665c54`).
- `./gradlew ktlintCheck detekt :domain:test test` and `:app:assembleDebug` exit 0.

---
*Phase: 01-domain-foundation-playable-board*
*Completed: 2026-06-28*
