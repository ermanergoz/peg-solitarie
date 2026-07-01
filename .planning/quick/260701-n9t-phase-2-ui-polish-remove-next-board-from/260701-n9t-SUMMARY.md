---
phase: quick-260701-n9t
plan: 01
subsystem: ui-polish
tags: [settings, datastore, hints, results, catalog, compose]
requires:
  - OnboardingRepository / app_prefs DataStore (Phase 02-08)
  - ResultsViewModel / AppNavHost (Phase 02-06, 02-08)
  - ClassicCatalogScreen (Phase 02-07)
provides:
  - SettingsRepository (persisted hintsEnabled, fail-open default ON)
  - Shared Hilt-provided app_prefs DataStore<Preferences>
  - GameUiState.showHints wired end-to-end + top-bar HintToggleControl
  - Trimmed Classic Results (Replay-only primary action)
  - Fixed-height uniform catalog cards
affects:
  - Every future preferences-backed repository (shares the one app_prefs store)
tech-stack:
  added: []
  patterns:
    - "Shared DataStore<Preferences> provided once via DataModule companion @Provides"
    - "Persisted setting is the single source of truth for UI state (collect → showHints)"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/AppPreferences.kt
    - app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/HintToggleControl.kt
    - app/src/test/java/com/app/pegsolitaire/data/SettingsRepositoryTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt
    - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/dimens.xml
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
decisions:
  - "Single shared app_prefs DataStore lives in AppPreferences.kt and is Hilt-provided; OnboardingRepository now injects it instead of owning a duplicate delegate (two delegates for the same file crash at runtime)."
  - "Persisted hintsEnabled is the single source of truth for GameUiState.showHints — onToggleHints() only writes; the collected flow drives state (no optimistic local flip). showHints is preserved across restart since the settings flow only re-emits on change."
  - "Icons.Rounded.Info conveys the hint toggle (core set has no lightbulb/visibility); on/off is shown via tint (primary vs onSurfaceVariant) + content description."
  - "Inlined the single-use clearSelection() into onCellTap to stay within detekt's TooManyFunctions budget (11) after adding onToggleHints — behavior unchanged."
metrics:
  duration_minutes: 30
  completed: 2026-07-01
---

# Phase Quick 260701-n9t: Phase-2 UI Polish (Hint Toggle, Remove Next Board, Uniform Cards) Summary

Persisted hint toggle over a shared `app_prefs` DataStore, a Replay-only Classic Results screen, and uniform fixed-height catalog cards — all behind the CLAUDE.md gates (dimens/strings, state hoisting, ktlint + detekt).

## What Was Built

**Task 1 — Persisted hint toggle.** New `SettingsRepository` (interface + `SettingsRepositoryImpl`) exposing `hintsEnabled: Flow<Boolean>` (fail-open default `true`) and `setHintsEnabled`. The `app_prefs` DataStore delegate was extracted into `AppPreferences.kt` as the single app-wide delegate and Hilt-provided from `DataModule`'s companion; `OnboardingRepository` was refactored to inject the shared store (its old private duplicate delegate deleted). `GameViewModel` collects `hintsEnabled` into `GameUiState.showHints` and `onToggleHints()` persists the flip (the flow remains the single source of truth; `showHints` is carried across restart). `BoardCanvas` gates the destination dots on `state.showHints`. A stateless `HintToggleControl` (Info IconButton, tint reflects on/off) sits in the gameplay top bar.

**Task 2 — Remove "Next board" from Classic Results.** Replay is now the single filled primary `Button`. Dropped `ResultsUiState.nextId`, `ResultsViewModel`'s `nextIdFrom`/`NEXT_OFFSET` and its now-unused `levelRepository` dependency, the `onNextBoard` param threaded through `ResultsScreen`/`ResultsRoute`, the 5th `ResultsContent` lambda in `AppNavHost`, and the `action_next_board` string.

**Task 3 — Uniform catalog cards.** New `catalog_card_height` dimen (216dp); every `BoardCard` uses `.height(...)` so equal grid-row cards align even when a name wraps. The board name allows up to 2 lines with ellipsis (never truncated to one), so "Asymmetric 3-3-2-2" wraps.

## Verification

- `./gradlew :app:testDebugUnitTest :app:ktlintCheck :app:detekt` — all pass.
- `./gradlew :app:assembleDebug` — packages cleanly.
- `grep 'preferencesDataStore(' app/src/main` — one real delegate (in `AppPreferences.kt`).
- `grep 'nextId\|onNextBoard\|nextIdFrom\|action_next_board' app/src` — no matches.

## Commits

- `fa67bc3` feat(260701-n9t): add persisted hint toggle to gameplay top bar
- `75c2db7` refactor(260701-n9t): remove Next board from Classic Results
- `cca6cb3` feat(260701-n9t): uniform fixed-height catalog cards

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Single `@Inject` constructor instead of a delegating pair for `SettingsRepositoryImpl`/`OnboardingRepository`**
- **Found during:** Task 1
- **Issue:** The plan described a delegating `@Inject constructor(dataStore)` alongside an `internal constructor(dataStore)`; two constructors with the identical `DataStore<Preferences>` signature do not compile.
- **Fix:** Since the shared store is now Hilt-provided, the single primary constructor is annotated `@Inject` (for `OnboardingRepository`, `@Inject internal constructor`). Tests construct the impls directly over the same seam.
- **Files:** SettingsRepository.kt, OnboardingRepository.kt

**2. [Rule 3 - Blocking] Updated additional VM/screen construction sites**
- **Found during:** Task 1
- **Issue:** `GameFlowTest` also builds `GameViewModel` and calls `GameScreen`; it needed the new `settingsRepository` arg and `onToggleHints` param (not listed in the plan's files).
- **Fix:** Added a fixed hints-on fake `SettingsRepository` and wired `onToggleHints` in `GameFlowTest`.
- **Files:** GameFlowTest.kt

**3. [Rule 3 - Blocking] Inlined `clearSelection()` to satisfy detekt**
- **Found during:** Task 3 (full gate)
- **Issue:** Adding `onToggleHints()` pushed `GameViewModel` to 12 functions; detekt `TooManyFunctions` max is 11.
- **Fix:** Inlined the single-use `clearSelection()` into `onCellTap` (identical behavior).
- **Files:** GameViewModel.kt

## Known Stubs

None.

## Self-Check: PASSED

- All 4 created files present on disk.
- All 3 task commits present in git history.
