---
phase: 02-classic-mode-scoring-navigation
plan: 07
subsystem: ui
tags: [compose, material3, jetpack-compose, lazyverticalgrid, hilt-viewmodel, stateflow, canvas]

# Dependency graph
requires:
  - phase: 02-04
    provides: LevelRepository.classicCatalog() + ClassicLevelDescriptor (D-18-ordered), BoardGeometry per-footprint grid dimension
  - phase: 02-03
    provides: catalog_grid_min_cell_width / catalog_preview_size / content_max_width dimens, catalog_title + holes_count plural + navigate_back strings
  - phase: 01
    provides: BoardCanvas stateless Canvas leaf, GameUiState, Route -> Screen -> Leaf + UDF convention
provides:
  - ClassicCatalogViewModel + ClassicCatalogUiState (immutable pass-through of the D-18 catalog over StateFlow)
  - ClassicCatalogScreen (adaptive LazyVerticalGrid of 5 board cards) + ClassicCatalogRoute (hiltViewModel seam)
  - BoardPreview read-only catalog mini-preview
  - BoardCanvas read-only mode (interactive flag) reused for non-interactive footprint rendering
affects: [02-08 AppNavHost, results, home]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Read-only BoardCanvas variant via an interactive: Boolean = true flag (suppresses tap handler, selection ring, destination dots, animation)"
    - "LazyVerticalGrid(GridCells.Adaptive) catalog grid capped to content_max_width and centered"
    - "Clickable Material3 Card(onClick) as whole-card tap target forwarding descriptor.id"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/BoardPreview.kt
    - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt

key-decisions:
  - "BoardCanvas read-only mode is an interactive flag (default true keeps Phase-1 gameplay verbatim) rather than a forked composable, so the catalog preview and gameplay share one render path"
  - "ClassicCatalogViewModel seeds state synchronously in the constructor and is a pure pass-through (no reordering) since LevelRepository already enforces D-18 order"
  - "drawBoard's per-cell rendering extracted into drawCell to stay under the detekt CyclomaticComplexMethod gate after adding the read-only guards"

patterns-established:
  - "Pattern: read-only Canvas leaf via a boolean flag that conditionally attaches the pointerInput modifier and gates draw overlays"

requirements-completed: [CLAS-01]

# Metrics
duration: 14min
completed: 2026-06-30
---

# Phase 2 Plan 07: Classic Catalog Screen Summary

**Browsable Classic catalog — an adaptive grid of 5 tappable board cards (read-only BoardCanvas footprint preview + name + plural hole count) in D-18 ascending order, backed by a pass-through ClassicCatalogViewModel over LevelRepository.classicCatalog().**

## Performance

- **Duration:** ~14 min
- **Completed:** 2026-06-30
- **Tasks:** 2
- **Files modified:** 7 (6 created, 1 modified)

## Accomplishments
- `ClassicCatalogViewModel` + `@Immutable ClassicCatalogUiState` expose the 5 D-18-ordered descriptors over a read-only `StateFlow` with no reordering, unit-tested with Turbine + Truth.
- `BoardCanvas` gained an `interactive: Boolean = true` read-only mode that suppresses tap emission, the selection ring, legal-destination dots and all animation — the Phase-1 gameplay default is unchanged (`BoardCanvasTest` still green).
- `BoardPreview` renders a descriptor's footprint read-only at `catalog_preview_size` on a `surfaceContainerLowest` backing, purely decorative.
- `ClassicCatalogScreen` shows a `LazyVerticalGrid(GridCells.Adaptive(catalog_grid_min_cell_width))` of `surfaceContainerHigh` cards (preview + `titleMedium` name + plural `holes_count`); whole-card tap forwards `descriptor.id`. `ClassicCatalogRoute` mirrors the `GameRoute` `hiltViewModel` + `collectAsStateWithLifecycle` seam.

## Task Commits

Each task was committed atomically:

1. **Task 1: ClassicCatalogViewModel + UiState over LevelRepository** - `9a8d8cb` (feat)
2. **Task 2: read-only BoardCanvas + Classic catalog screen + route** - `2b050d0` (feat)

**Plan metadata:** see final docs commit.

_Note: Task 1 was authored test-first (RED run confirmed the test failed to compile before the production classes existed); RED and GREEN were committed together because the test references the production types and cannot compile standalone._

## Files Created/Modified
- `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogUiState.kt` - `@Immutable` catalog UI state (`boards: List<ClassicLevelDescriptor>`).
- `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModel.kt` - `@HiltViewModel` pass-through of `classicCatalog()` over `StateFlow`.
- `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt` - stateless catalog screen: adaptive grid + tappable board cards.
- `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogRoute.kt` - stateful route seam (`hiltViewModel` + lifecycle collection).
- `app/src/main/java/com/app/pegsolitaire/ui/classic/BoardPreview.kt` - read-only `BoardCanvas` wrapper for the mini-preview.
- `app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt` - Turbine + Truth pass-through test (5 boards, D-18 id order, fields intact).
- `app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt` - added `interactive` read-only mode; extracted `drawCell`.

## Decisions Made
- Read-only rendering is a flag on the existing `BoardCanvas`, not a separate composable — one render path for gameplay and preview, with the flag conditionally attaching `pointerInput` and gating the draw overlays.
- The catalog grid is width-capped to `content_max_width` and centered (`Box(TopCenter)` + `widthIn`), consistent with the Home/Results layout intent; the adaptive grid already handles column count on wide screens.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Extracted `drawCell` to satisfy the detekt complexity gate**
- **Found during:** Task 2 (BoardCanvas read-only mode)
- **Issue:** Adding the `interactive &&` guards to `drawBoard` pushed its cyclomatic complexity from 14 to 15, failing the detekt `CyclomaticComplexMethod` gate (max 14).
- **Fix:** Extracted the per-cell `when (cellAt(position))` render into a private `DrawScope.drawCell(...)` helper; `drawBoard` now loops calling it.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
- **Verification:** `./gradlew :app:detekt :app:ktlintCheck :app:compileDebugKotlin :app:testDebugUnitTest --tests *BoardCanvasTest*` all green.
- **Committed in:** `2b050d0` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Pure refactor to pass an existing quality gate; no behavior change, no scope creep.

## Issues Encountered
None beyond the deviation above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- `ClassicCatalogRoute(onSelect, onNavigateBack)` is ready to wire into `AppNavHost` (02-08): `onSelect(levelId)` → `Gameplay(levelId)`, back → `Home`. The route contract from 02-05 is reused unchanged.
- No blockers. CLAS-01 is satisfied at the UI layer.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-30*

## Self-Check: PASSED
All 6 created files + SUMMARY.md present on disk; both task commits (9a8d8cb, 2b050d0) exist in history.
