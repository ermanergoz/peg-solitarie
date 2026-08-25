---
phase: 05-persistence-resume-settings
plan: 04
subsystem: ui
tags: [compose, material3, settings, navigation, haptics, mvvm, state-hoisting]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    plan: 02
    provides: SettingsRepository.soundOn / hapticsOn fail-open Flows + setters
  - phase: 02-core-gameplay-loop
    provides: Home screen + type-safe AppNavHost + GameUiState/GameViewModel settings-collect pattern
provides:
  - Dedicated Settings destination (SET-01/SET-02, D-14) reached by a Home top-bar gear
  - SettingsViewModel/SettingsUiState/SettingsScreen/SettingsRoute (state-hoisted MVVM slice)
  - "@Serializable data object Settings route + composable<Settings> NavHost destination"
  - GameUiState.hapticsOn + a real light haptic fired per jump, gated by the setting (D-15)
  - "AppNavHost settingsContent slot (test-injectable, like the other destination slots)"
affects: [05-05-results-pips, 05-06-challenge-continue-wiring, 05-07-phase-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Settings toggle row: stateless Card + M3 Switch whose checked/onCheckedChange are hoisted params"
    - "Haptic emission as a UI concern: LaunchedEffect keyed on the MoveTransition move ordinal, gated by a UiState flag"
    - "AppNavHost destination bodies stay default-valued slots so NavFlowTest drives real nav lambdas without Hilt"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/res/values/dimens.xml
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt

key-decisions:
  - "HapticFeedbackType.ContextClick (not LongPress) — lightest constant in Compose BOM 2026.06.00 that maps to a platform constant available from API 23 (≤ minSdk 24); SegmentTick/GestureEnd are API-34-only and would be silent on most devices (resolves RESEARCH assumption A2)"
  - "The haptic fires from GameScreen keyed on MoveTransition.id (per jump, never per recomposition); the composable reads state.hapticsOn and never touches SettingsRepository"
  - "HomeRoute/HomeScreen take a hoisted onOpenSettings lambda — no NavController reaches a screen"
  - "AppNavHost gained a settingsContent slot mirroring the existing test-injectable destination-slot convention, so NavFlowTest proves the gear→Settings→back path against the real nav lambdas"
  - "The sound row's supporting copy states the honest D-16 truth ('Saved for the upcoming sound effects') rather than implying playback that does not exist yet"
  - "The in-game 'Show possible moves' toggle was NOT mirrored into Settings (RESEARCH Open Question 3 default — it stays a live gameplay affordance)"

patterns-established:
  - "ui/settings/* mirrors ui/home/* exactly: @Immutable UiState, @HiltViewModel collecting repo Flows, stateless Screen, hiltViewModel() Route"
  - "Compose haptics are verified JVM-side by injecting a recording HapticFeedback through LocalHapticFeedback (no device needed for the gate; only the physical feel is manual)"

requirements-completed: [SET-01, SET-02]

# Metrics
duration: 24min
completed: 2026-08-25
---

# Phase 05 Plan 04: Settings Screen & Real Haptics Summary

**A Home gear now opens a dedicated Settings destination whose sound and haptics switches read and write the persisted DataStore flags, and the haptics flag is honest: a light `ContextClick` tick fires on every peg jump while it is on and nothing fires when it is off.**

## Performance

- **Duration:** ~24 min
- **Tasks:** 3 (2 TDD: RED → GREEN; 1 straight wiring task)
- **Files modified:** 16 (5 created, 11 modified)

## Accomplishments

- **Settings slice (SET-01, SET-02, D-14):** `ui/settings/` now holds an `@Immutable SettingsUiState(soundOn, hapticsOn)`, a `@HiltViewModel SettingsViewModel` that collects both flags from `SettingsRepository` and writes toggles back through it, a fully stateless `SettingsScreen` (Scaffold + back nav + two hoisted `Switch` rows capped at `content_max_width`), and a `SettingsRoute` doing the `hiltViewModel()` + `collectAsStateWithLifecycle` hand-off. No ViewModel or repository reference exists below the route.
- **Reachable (Pitfall 7 "invisible feature" avoided):** a gear `IconButton` in `HomeTopBar` invokes a hoisted `onOpenSettings`, `AppNavHost` navigates to a new `@Serializable data object Settings`, and its back action pops to Home. `NavFlowTest` drives the real nav lambdas through gear → Settings → back → Home.
- **Haptics wired for real (SET-02, D-15):** `GameUiState` carries `hapticsOn`, collected in `GameViewModel` exactly like `showHints` (and carried across restart / the async Challenge load). `GameScreen.JumpHaptic` fires one `HapticFeedbackType.ContextClick` per jump, keyed on the `MoveTransition.id` move ordinal and gated by `state.hapticsOn`.
- **The gate is proven, not assumed:** `GameFlowTest` injects a recording `HapticFeedback` through `LocalHapticFeedback`, then asserts exactly one tick after a jump with haptics on and **zero** ticks with haptics off — while still asserting the jump itself happened (the switch gates feedback, never gameplay).
- **T-05-02 mitigated end-to-end:** the screen paints ON defaults before the store's first emission and consumes the 05-02 fail-open reads, so a corrupt store degrades to "everything on" and can never blank or crash Settings. A `NeverEmittingSettingsRepository` test locks that behavior in.
- **QUAL-02/QUAL-03 kept clean:** every new dp goes through `settings_row_min_height` / existing spacing tokens via `dimensionResource`, every new string through `strings.xml`; `dpSpLiteralCheck`, `ktlintCheck` and `detekt` are green. No `material-icons-extended` dependency was added — the gear is the core-set `Icons.Rounded.Settings` (threat T-05-SC holds: zero new packages).

## Task Commits

1. **Task 1: Settings screen, VM, UiState, Route** (TDD)
   - `70dd8fd` (test) — RED: defaults-ON-before-emission, mirroring, Turbine re-emission, toggle-through, flag independence
   - `143c2f7` (feat) — GREEN: `ui/settings/*` + `settings_row_min_height` + Settings strings
2. **Task 2: Wire Settings into nav (Home gear + Settings destination)**
   - `de07bc1` (feat) — `Settings` route, `composable<Settings>` destination + `settingsContent` slot, hoisted `onOpenSettings`, NavFlowTest reachability case
3. **Task 3: Fire haptic on peg jump gated by hapticsOn** (TDD)
   - `0fef1bb` (test) — RED: recording `HapticFeedback` via `LocalHapticFeedback`; one tick on / none off
   - `a63f767` (feat) — GREEN: `GameUiState.hapticsOn`, the VM collect, `GameScreen.JumpHaptic`

No REFACTOR commit was needed — both TDD implementations landed at their final shape (direct copies of in-repo analogs).

## Files Created/Modified

- `ui/settings/SettingsUiState.kt` — **created**; `@Immutable` two-flag state, both defaulting `true` (D-17).
- `ui/settings/SettingsViewModel.kt` — **created**; `@HiltViewModel`, two `init` collects, two `viewModelScope` write-through toggles.
- `ui/settings/SettingsScreen.kt` — **created**; stateless Scaffold + top bar (title/back) + two `SettingsToggleRow`s (Card + label/supporting + `Switch` with the label as its content description).
- `ui/settings/SettingsRoute.kt` — **created**; `hiltViewModel()` + `collectAsStateWithLifecycle` + forwarded method refs.
- `ui/settings/SettingsViewModelTest.kt` — **created**; 6 cases over a settable fake repository.
- `navigation/Routes.kt` — added `@Serializable data object Settings` (no args → nothing untrusted crosses the new surface, T-05-08).
- `navigation/AppNavHost.kt` — `homeContent` slot gained `onOpenSettings`; new `settingsContent` slot + `composable<Settings>` destination popping back to Home.
- `ui/home/HomeScreen.kt` / `HomeRoute.kt` — gear action in `HomeTopBar.actions` (`top_bar_icon_size`, `settings_action` description) fed by a hoisted `onOpenSettings`.
- `ui/game/GameUiState.kt` — added `hapticsOn` (+ documented) and wired it through `initial()`/`loading()`.
- `ui/game/GameViewModel.kt` — parallel `hapticsOn` collect; restart and the async Challenge load now carry the flag forward alongside `showHints`.
- `ui/game/GameScreen.kt` — new private `JumpHaptic` composable (the sole fire site).
- `res/values/dimens.xml` — `settings_row_min_height` (72dp).
- `res/values/strings.xml` — Phase-5 Settings copy block (title, gear description, two labels + two supporting lines).
- `ui/game/GameFlowTest.kt` — recording haptics + parameterized settings; 2 new cases (7 total green).
- `navigation/NavFlowTest.kt` — updated for the 3-arg `homeContent` slot; new gear→Settings→back case.

## Decisions Made

See `key-decisions` in the frontmatter. The load-bearing one is the haptic constant: RESEARCH assumption **A2** is now resolved against the pinned BOM by inspecting `androidx.compose.ui:ui-android:1.11.3` directly. The exposed set is `Confirm, ContextClick, GestureEnd, GestureThresholdActivate, KeyboardTap, LongPress, Reject, SegmentFrequentTick, SegmentTick, TextHandleMove, ToggleOff, ToggleOn, VirtualKey`. `SegmentTick` is the "lightest" but maps to an API-34 platform constant, so on the project's `minSdk 24` floor it would silently do nothing for most users; `LongPress` is far too heavy for a peg hop. `ContextClick` (API 23) is the lightest tick that actually fires across the supported range.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created the worktree's missing `local.properties`**
- **Found during:** Task 1 (first Gradle run)
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every Gradle invocation with "SDK location not found" (the same blocker 05-02 hit).
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree root.
- **Files modified:** `local.properties` (gitignored — **not committed**, by design).
- **Committed in:** n/a (intentionally untracked).

**2. [Rule 3 - Blocking] Updated `NavFlowTest` for the widened `homeContent` slot**
- **Found during:** Task 2
- **Issue:** Adding `onOpenSettings` to `AppNavHost`'s `homeContent` slot is a source-breaking change for the two existing `NavFlowTest` call sites (`{ onPlayClassic, _ -> … }`), so the test source set no longer compiled.
- **Fix:** Widened both lambdas to three params and, rather than a bare compile fix, added a `settingsContent` slot plus a `homeGearOpensSettingsAndBackReturnsToHome` case so the new nav edge is actually covered.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt`, `app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt`
- **Verification:** `:app:testDebugUnitTest --tests "*NavFlowTest"` green (5 cases).
- **Committed in:** `de07bc1`

**3. [Rule 2 - Missing Critical] Carry `hapticsOn` across restart and the Challenge load**
- **Found during:** Task 3 (GREEN)
- **Issue:** The plan specified only the collect. But `resetToInitial()` and the async Challenge-load branch both rebuild state from `GameUiState.initial(...)` and re-apply `showHints` explicitly — because the settings flows only re-emit on *change*. Copying only `showHints` would silently reset `hapticsOn` to `true` after a restart, so a player with haptics OFF would start feeling ticks again mid-session (a dishonest switch, exactly what D-15 forbids).
- **Fix:** Both sites now carry `hapticsOn` forward alongside `showHints`.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`
- **Verification:** full `:app:testDebugUnitTest` green; the off-case flow test proves the gate.
- **Committed in:** `a63f767`

**4. [Rule 2 - Missing Critical] Strengthened the Task-3 verification beyond a VM assertion**
- **Found during:** Task 3 (RED)
- **Issue:** The plan asked only that `GameUiState` reflect the injected `hapticsOn`. That would pass even if nothing ever called `performHapticFeedback` — i.e. it cannot distinguish a wired setting from a dead one.
- **Fix:** Injected a recording `HapticFeedback` through `LocalHapticFeedback` in `GameFlowTest` and asserted the actual fire count (exactly 1 on, 0 off) in addition to the state assertions.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt`
- **Verification:** `:app:testDebugUnitTest --tests "*GameFlowTest"` — 7 tests, 0 failures.
- **Committed in:** `0fef1bb` (RED) / `a63f767` (GREEN)

---

**Total deviations:** 4 auto-fixed (2 blocking, 2 missing-critical)
**Impact on plan:** All four served the plan's own acceptance criteria (build, compile, an honest non-dead switch, a verification that can actually fail). No scope creep — no results/pips, resume or Challenge-continue work was pulled in, and the concurrently-owned `data/ProgressRepository.kt`, `data/resume/*` and `di/DataModule.kt` were never touched.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*SettingsViewModelTest"` | PASS (6 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*GameFlowTest"` | PASS (7 tests, incl. the 2 new haptic cases) |
| `./gradlew :app:testDebugUnitTest --tests "*NavFlowTest"` | PASS (5 tests, incl. gear→Settings→back) |
| `./gradlew :app:testDebugUnitTest` (full suite) | PASS |
| `./gradlew :domain:test` | PASS |
| `./gradlew :app:dpSpLiteralCheck :app:ktlintCheck :app:detekt` | PASS |
| `./gradlew :app:compileDebugKotlin` | PASS |
| `./gradlew :app:assembleDebug` (Hilt graph resolves `SettingsViewModel`) | PASS |
| No `material-icons-extended` dependency added | PASS (core icon set only) |

## Issues Encountered

- None beyond the known fresh-worktree `local.properties` gap (deviation 1) and the expected `homeContent`-slot compile break (deviation 2).

## Known Stubs

- **`soundOn` persists a flag only — there is no audio engine or sound assets (D-16, explicitly out of scope this phase).** The switch is honest about it: the row's supporting copy reads "Saved for the upcoming sound effects" rather than implying playback. The later audio-polish pass consumes the already-wired flag; nothing further is needed from this plan.
- No other stubs: haptics is fully wired end-to-end (persisted → collected → fired), and the Settings destination is reachable from Home in the shipping graph (not just in tests).

## Threat Flags

None. The one new surface (the `Settings` route) carries no arguments and no new dependency was introduced — matching the plan's `accept` dispositions for T-05-08 and T-05-SC. T-05-02 is mitigated as planned (fail-open reads + ON defaults, test-locked).

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **05-06 (Challenge continue wiring)** will re-touch `AppNavHost`'s Home destination: note the `homeContent` slot is now a **3-arg** lambda (`onPlayClassic, onPlayChallenge, onOpenSettings`) and there is a `settingsContent` slot; any new Continue callback should be appended in the same style, and `NavFlowTest`'s four `homeContent` call sites widened accordingly.
- **05-07 (phase verification)** should include the device checkpoint for the physical haptic feel (VALIDATION Manual-Only): the JVM tests prove *whether* a tick fires and that the toggle gates it, but not how it feels — confirm `ContextClick` reads as a light hop on hardware, and swap the constant in the single `JumpHaptic` fire site if it feels wrong.
- Any future setting is a two-line addition: a flag on `SettingsUiState` + a `SettingsToggleRow` in `SettingsScreen`.

## Self-Check: PASSED

All 5 created files verified present on disk; all 5 task commits (`70dd8fd`, `143c2f7`, `de07bc1`, `0fef1bb`, `a63f767`) verified in `git log`. `git log --diff-filter=D` over the plan's commit range reports no deleted files, and the working tree is clean.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
