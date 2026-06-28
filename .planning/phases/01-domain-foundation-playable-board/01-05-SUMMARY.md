---
phase: 01-domain-foundation-playable-board
plan: 05
subsystem: ui-game-flow
tags: [kotlin, compose, material3, mvvm, hilt, route-screen-leaf, robolectric, compose-ui-test, walking-skeleton]

# Dependency graph
requires:
  - 01-01 — :app Android/Compose module, standards gate (detekt/ktlint/dpSpLiteralCheck), Hilt + KSP applied
  - 01-02 — domain model (Board, Boards.English33/english33Initial, Position, GameStatus) + RulesEngine
  - 01-03 — GameViewModel (@HiltViewModel) + @Immutable GameUiState (status/confirmRestart/animating); DomainModule
  - 01-04 — stateless BoardCanvas(state, onCellTap, onAnimationFinished, modifier) + BOARD_CANVAS_TEST_TAG; dimens/strings/theme/BoardMetrics
provides:
  - "GameRoute() — stateful route: hiltViewModel() + collectAsStateWithLifecycle, wires VM callbacks to GameScreen"
  - "GameScreen(state, onCellTap, onRestart, onConfirmRestart, onDismissRestart, onAnimationFinished) — stateless M3 Scaffold + TopAppBar + board panel + dialogs"
  - "GameOverDialog(status, pegCount, onPlayAgain, onViewBoard) — status-tiered M3 AlertDialog (D-05)"
  - "RestartControl(onRestart) — top-bar Icons.Rounded.Refresh action (D-06)"
  - "PegCountKey/GameStatusKey/LegalTargetCountKey semantics on GAME_STATE_TEST_TAG for deterministic flow assertions"
  - "MainActivity hosts GameRoute inside AppTheme — the walking skeleton is end-to-end runnable"
affects: [Phase 2 — Results screen replaces the D-05 game-over dialog at the same seam; Home/Classic/Challenge navigation wraps GameRoute]

# Tech tracking
tech-stack:
  added:
    - Robolectric 4.16 (app test — JVM-side Android runtime for the Compose flow test)
    - androidx.compose.ui:ui-test-junit4 + ui-test-manifest (Compose UI test, consumed from the catalog)
    - androidx.compose.material:material-icons-core (curated Material Symbols — only Icons.Rounded.Refresh)
    - androidx.hilt:hilt-navigation-compose 1.3.0 (hiltViewModel) + androidx.lifecycle:lifecycle-runtime-compose (collectAsStateWithLifecycle)
  patterns:
    - "Route/Screen/Leaf: GameRoute (state collection) → GameScreen (stateless render + event hoisting) → BoardCanvas (pure leaf)"
    - "Lifecycle-aware collection: collectAsStateWithLifecycle in the route; no MutableStateFlow leaks (Pitfall 10)"
    - "Game-over as replayable GameUiState.status (survives rotation); 'View Board' hide is ephemeral local remember keyed on game identity, not VM state"
    - "Robolectric runtime SDK pinned via @Config(sdk=34) independent of compileSdk 36 (no android-all jar for Android 16 yet)"
    - "Deterministic Compose e2e: custom semantics (pegCount/status/legalTargetCount) + percentOffset cell taps on the square canvas — no pixel comparisons"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/RestartControl.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameOverDialog.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    - app/build.gradle.kts
    - gradle/libs.versions.toml
    - build-logic/src/main/kotlin/android-app.gradle.kts

key-decisions:
  - "hilt-navigation-compose pinned 1.4.0-rc01 → 1.3.0 + the whole androidx.lifecycle atomic group force-resolved to 2.9.4 — the rc line transitively requires compileSdk 37 (uninstallable here); same class of pin as Plans 01/03"
  - "Kept the deprecated hiltViewModel() from androidx.hilt.navigation.compose — its replacement package (androidx.hilt.lifecycle.viewmodel.compose) is exactly the compileSdk-37 artifact; the deprecated overload is the 36-safe path and satisfies the GameRoute acceptance check"
  - "End-to-end flow test drives the real GameScreen over a directly-constructed GameViewModel (skips only the hiltViewModel() lookup) — avoids a Hilt-test harness while still exercising VM→Screen→Canvas→dialogs end-to-end"
  - "Game-over 'View Board' dismissal is a local remember(state.status, state.board) flag (ephemeral view state), not a new VM field — game-over itself stays replayable status"

requirements-completed: [GAME-01, GAME-03, GAME-05]

# Metrics
duration: 18min
completed: 2026-06-28
---

# Phase 1 Plan 05: Playable Screen & Walking-Skeleton Closure Summary

**`MainActivity` → `GameRoute` (`hiltViewModel()` + `collectAsStateWithLifecycle`) → stateless `GameScreen` (M3 `Scaffold` + top-bar `RestartControl` + the `surfaceContainerHigh` board panel hosting `BoardCanvas` + the status-tiered `GameOverDialog` and the mid-game restart confirm) assembles the one fully-playable English-33 board end-to-end, and a Robolectric-backed Compose `GameFlowTest` proves the whole walking-skeleton loop on the JVM — tap a peg → its legal destination highlights → tap the hole → the jumped peg is removed (32→31), a played line reaches a terminal position and the "Perfect!/Play Again" dialog appears, and the top-bar restart shows the mid-game confirm then resets to the 32-peg start — with every CI gate (ktlint, detekt, dp/sp, ArchUnit purity, all tests, debug build) green.**

## Performance

- **Duration:** ~18 min
- **Tasks:** 2 automatable (both `auto`) + 1 human-verify checkpoint (emulator playtest — see Manual Verification Required)
- **Files:** 5 created, 4 modified
- **Commits:** 2 task commits + 1 docs commit

## Accomplishments

- **Route/Screen/Leaf boundary (Task 1).** `GameRoute()` is the stateful seam: `val viewModel: GameViewModel = hiltViewModel()` + `val state by viewModel.uiState.collectAsStateWithLifecycle()`, handing the immutable state plus the VM's method references to the stateless `GameScreen` — zero logic in the route. `GameScreen` owns no game state: an M3 `Scaffold` with a `TopAppBar` (title = `app_name`, `titleLarge`) hosting the `RestartControl` action, over a centered `surfaceContainerHigh` rounded board panel (`board_max_size` cap, `board_screen_margin` inset, square via `BOARD_ASPECT_RATIO`) containing the `BoardCanvas`.
- **D-05 game-over dialog.** `GameOverDialog` picks the status tier from `(status, pegCount)` per the Copywriting Contract — `Won` → "Perfect!"; `Stuck` with ≤3 pegs → "So close!" + `pegs_remaining`; `Stuck` with 4+ → "No moves left" + `pegs_remaining_retry` — with primary "Play Again" (`primary`)→`onConfirmRestart` and neutral "View Board" hiding the overlay. It is shown from replayable `status`; the "View Board" hide is a local `remember(status, board)` flag (ephemeral), so the dialog survives rotation but can be dismissed to inspect the final board.
- **D-06 restart.** `RestartControl` is a top-bar `IconButton` with `Icons.Rounded.Refresh` (`top_bar_icon_size`, `onSurface` tint, `contentDescription = restart_board`) → `onRestart`; mid-game it raises the confirm `AlertDialog` ("Restart this board?" / "Your current progress will be lost." / "Restart Board" → confirm, "Keep Playing" → dismiss), at an end-state "Play Again" resets with no extra confirm — the decision lives entirely in the VM.
- **MainActivity hosts the route.** `setContent { AppTheme { GameRoute() } }` — the walking skeleton is now a runnable debug app showing one playable English-33 board.
- **End-to-end Compose flow test (Task 2, GAME-01/03/05).** `GameFlowTest` runs JVM-side via Robolectric and drives the real `GameScreen` over a directly-constructed `GameViewModel`: (1) the English-33 start composes with 32 pegs; tapping the peg left-of-center highlights exactly one legal destination (the center hole) and tapping it drops `pegCount` to 31 — the jumped peg removed (GAME-01); (2) a seeded two-peg line resolved in one jump reaches `Won` and the "Perfect!"/"Play Again" dialog is displayed (D-05/GAME-02); (3) a mid-game top-bar restart shows the confirm dialog and "Restart Board" resets to the 32-peg center-empty board (GAME-05/D-06). Assertions read custom `PegCountKey`/`GameStatusKey`/`LegalTargetCountKey` semantics and taps use `percentOffset` on the square canvas, so the test is deterministic rather than pixel-based.
- **QUAL-03 held.** Every dp/sp in the three new composables flows through `dimensionResource`; every color a `MaterialTheme.colorScheme` role; the only tier threshold (`NEAR_MISS_MAX_PEGS`) is a named const. `:app:dpSpLiteralCheck` and `:app:detekt` are green.

## Task Commits

1. **Task 1 — GameScreen/GameRoute/GameOverDialog/RestartControl + MainActivity wiring** — `e40bea9` (feat)
2. **Task 2 — end-to-end Compose GameFlowTest** — `75ccb0e` (test)

## Decisions Made

- **compileSdk-37 transitive pin (Deviation #1).** `hilt-navigation-compose` 1.4.0-rc01 pulled `hilt-lifecycle-viewmodel-compose:1.4.0-rc01` and `lifecycle-*-compose:2.11.0-rc01`, all requiring compileSdk 37. Downgraded to androidx.hilt 1.3.0 (CLAUDE.md's pinned line) and added a `resolutionStrategy` forcing the whole `androidx.lifecycle` atomic group to 2.9.4 — the same compileSdk-36 guard Plans 01/03 used for androidx.core/lifecycle.
- **Deprecated `hiltViewModel()` kept on purpose.** The compiler flags it as moved to `androidx.hilt.lifecycle.viewmodel.compose` — but that package is the compileSdk-37 artifact. The deprecated overload from `androidx.hilt.navigation.compose` is the 36-safe path and satisfies the GameRoute acceptance check (`hiltViewModel()` present).
- **Direct-VM flow test over a Hilt-test harness.** `GameFlowTest` constructs `GameViewModel(RulesEngine, board)` directly and renders `GameScreen` (the only thing skipped vs. `GameRoute` is the `hiltViewModel()` lookup, which `:app:assembleDebug` already proves generates). This exercises VM→Screen→Canvas→dialogs end-to-end without standing up `@HiltAndroidTest`.
- **Robolectric runtime SDK pinned to 34.** Android 16 (SDK 36) has no Robolectric android-all jar yet; `@Config(sdk = 34)` emulates a supported framework independently of the app's compileSdk 36 (the app uses no >34 runtime APIs).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] hilt-navigation-compose 1.4.0-rc01 → 1.3.0 + lifecycle group force-pin 2.9.4**
- **Found during:** Task 1 (`:app:assembleDebug` failed `checkDebugAarMetadata` — 4 deps require compileSdk 37)
- **Issue:** the rc hilt-navigation-compose transitively forced `hilt-lifecycle-viewmodel-compose:1.4.0-rc01` + `lifecycle-*-compose:2.11.0-rc01`, all minCompileSdk 37 (uninstallable here).
- **Fix:** catalog `hiltNavigationCompose` 1.4.0-rc01 → 1.3.0; extended the existing `resolutionStrategy` to force `androidx.lifecycle:*` → 2.9.4 (the line the Compose BOM already resolves).
- **Files modified:** gradle/libs.versions.toml, app/build.gradle.kts
- **Committed in:** `e40bea9`

**2. [Rule 3 - Blocking] Added UI + test build wiring (catalog-declared official artifacts)**
- **Found during:** Task 1/2 (compile + test could not resolve required APIs)
- **Issue:** `hiltViewModel()`, `collectAsStateWithLifecycle`, `Icons.Rounded.Refresh`, and the Robolectric/Compose-UI-test runtime were not yet on the classpath.
- **Fix:** added `implementation` of `hilt-navigation-compose`, `lifecycle-runtime-compose`, `material-icons-core`; `testImplementation` of `ui-test-junit4` + `robolectric`; `debugImplementation` of `ui-test-manifest`; and `testOptions.unitTests.isIncludeAndroidResources = true` in the android-app convention plugin. All are catalog-declared official androidx/Robolectric artifacts (Robolectric is in CLAUDE.md's testing stack) — build wiring, not new package installs.
- **Files modified:** app/build.gradle.kts, gradle/libs.versions.toml, build-logic/src/main/kotlin/android-app.gradle.kts
- **Committed in:** `e40bea9`

**3. [Rule 2 - Missing affordance] Exposed minimal test semantics on the board-state node**
- **Found during:** Task 2 (deterministic e2e assertions needed observable state)
- **Issue:** the flow test needs to assert pegCount / status / legal-target count without pixel comparisons; the plan explicitly asked for "minimal test semantics".
- **Fix:** added `PegCountKey`/`GameStatusKey`/`LegalTargetCountKey` semantics on the `GAME_STATE_TEST_TAG` node in `GameScreen` (read-only mirror of `state`); no behavior change.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
- **Committed in:** `e40bea9`

**4. [Rule 3 - Style] ktlint chain-wrapping in GameFlowTest**
- **Found during:** Task 2 (`:app:ktlintTestSourceSetCheck` failed — "Expected newline before '.'")
- **Issue:** wrapped `composeRule.onNodeWith…().assert…()` chains mixed an unwrapped and a wrapped dot.
- **Fix:** led each wrapped chain with `composeRule` on its own line so every chain operator wraps consistently; no behavior change (3/3 tests still green).
- **Files modified:** app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
- **Committed in:** `75ccb0e`

**Total deviations:** 4 auto-fixed (2 blocking build wiring, 1 missing test affordance, 1 style). No scope change; no must-have weakened.

## Authentication Gates

None — offline, single-player, local-only UI; no I/O, network, or credentials.

## Known Stubs

None. The screen is fully wired (route → screen → canvas → dialogs), playable end-to-end, and the D-05 dialog is the deliberate Phase-2 Results seam (documented in CONTEXT, not a stub). The "View Board" overlay-hide uses ephemeral local state by design, not a placeholder.

## Threat Flags

None — no new security surface beyond the plan's `<threat_model>` (T-05-01 illegal/forged move; T-05-02 animation/state desync). The screen only forwards `onCellTap(Position)`; `RulesEngine` in the VM remains the sole move authority (T-03-01) and the tap is bounds-checked in `BoardCanvas` (T-04-01). Game-over + confirm are replayable `GameUiState` fields (survive rotation, T-05-02). The added semantics are read-only test affordances, not an input surface.

## Verification Evidence

- `./gradlew :app:assembleDebug check` — exit 0 (Task 1 acceptance: lint, detekt, ktlint, dp/sp, ArchUnit purity, all tests, debug build).
- `./gradlew :app:testDebugUnitTest --tests "*GameFlowTest"` — 3/3 green (`tests="3" skipped="0" failures="0"`): tap→jump→removal (GAME-01), terminal→game-over dialog (D-05/GAME-02), mid-game restart→confirm→reset (GAME-05/D-06).
- `./gradlew ktlintCheck detekt :domain:test test assembleDebug` — exit 0 (full phase gate; `:domain` golden-master + ArchUnit purity still green; GameViewModelTest + BoardCanvasTest unaffected).
- `./gradlew :app:dpSpLiteralCheck :app:detekt` — exit 0 (QUAL-03; no raw dp/sp in the new composables, only named consts).
- `GameRoute.kt` greps: `hiltViewModel()` ×1, `collectAsStateWithLifecycle` ×3; `MainActivity.kt` references `GameRoute` ×3 inside `AppTheme`.

## Manual Verification Required

The plan's Task 3 is a `checkpoint:human-verify` (gate="blocking") emulator playtest of the **jump/removal feel**, which the automated `GameFlowTest` cannot judge (it asserts state changes + dialog presence, not motion smoothness/clarity). This step is **pending** — it requires a human on an emulator/device and was not (and cannot be) faked here.

**How to launch:** with a Pixel AVD running, `./gradlew :app:installDebug` then launch "Peg Solitaire" (or `adb shell am start -n com.app.pegsolitaire/.MainActivity`); or run `app` from Android Studio. (`./gradlew :app:assembleDebug` is already green, so the debug APK builds.)

**What a human should confirm (D-02..D-08):**
1. Tapping a peg adjacent-to-adjacent toward the center hole highlights its legal destination(s) (`primary` dot) and shows the selected peg's ring + slight scale-up.
2. Tapping a different peg moves selection; re-tapping the selected peg deselects; tapping an empty non-legal cell while selected does nothing (no deselect).
3. Tapping a highlighted destination: the peg **slides straight** into the hole (~200ms, D-07) while the jumped peg **fades + shrinks** and is gone as the jumper lands (D-08) — the "satisfying jump" feel.
4. Playing down until no moves remain shows the correct game-over tier (Perfect / So close / No moves left) + pegs-remaining, with "Play Again".
5. Mid-game top-bar Restart shows "Restart this board?"; "Keep Playing" cancels, "Restart Board" resets to the full center-empty board.

**Resume signal:** type "approved" if the board plays and the jump/removal feel reads correctly, or describe what feels off (timing, highlight clarity, dialog copy).

## Next Plan Readiness

- Phase 2 replaces the D-05 `GameOverDialog` with the Results/stars screen at the same `status ∈ {Won, Stuck}` seam, and wraps `GameRoute` under Home/Classic/Challenge navigation. The Route/Screen/Leaf boundary, the `hiltViewModel()`/`collectAsStateWithLifecycle` wiring, the Robolectric + Compose-UI-test harness, and the `GAME_STATE_TEST_TAG` semantics are all in place to extend.

## Self-Check: PASSED

- All 5 created + 4 modified files exist on disk (verified above).
- Both task commits present (`e40bea9`, `75ccb0e`).
- `./gradlew ktlintCheck detekt :domain:test test assembleDebug`, `:app:assembleDebug check`, `:app:dpSpLiteralCheck`, and `:app:testDebugUnitTest --tests "*GameFlowTest"` (3/3) all exit 0.
- **All AUTOMATED work is green.** The human emulator playtest (Task 3, jump/removal feel) is the one remaining item — pending under "Manual Verification Required", not faked.

---
*Phase: 01-domain-foundation-playable-board*
*Completed: 2026-06-28*
