---
phase: 02-classic-mode-scoring-navigation
plan: 04
subsystem: data
tags: [repository, hilt, classic-catalog, board-geometry, tdd, fail-fast]

# Dependency graph
requires:
  - phase: 02-classic-mode-scoring-navigation
    plan: 02
    provides: "Boards.{English33,European37,Asymmetric39,Diamond41,Wiegleb45} specs + *Initial boards"
  - phase: 02-classic-mode-scoring-navigation
    plan: 03
    provides: "board_* string resources (display names) in strings.xml"
  - phase: 01-foundation
    provides: "BoardGeometry/BoardMetrics, BoardSpec/Board/Position, Hilt seam (DomainModule), test harness"
provides:
  - "LevelRepository seam (single source of truth): classicCatalog() + loadClassic(id) + loadChallenge(n)"
  - "Level / ClassicLevelDescriptor immutable models + ClassicLevelIds named constants"
  - "DefaultClassicLevelSource — real 5-board catalog in D-18 order, fail-fast load, names from R.string.board_*"
  - "StubChallengeLevelSource — throwing Phase-3 placeholder behind a stable interface"
  - "DataModule Hilt @Binds wiring for the repository + both sources"
  - "Per-board grid-dimension geometry: BoardGeometry.gridDimension derived from BoardSpec bounds (7/8/9)"
affects: [02-05-classic-catalog, 02-07-results, game-route, phase-3-generator]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Repository pattern in :app data layer: interface + Default impl + Hilt @Binds (abstract DataModule)"
    - "Fail-fast id validation against a named-constant catalog (requireNotNull → IllegalArgumentException)"
    - "Display names resolved from string resources in the data layer via @ApplicationContext Context (no magic strings, domain stays Android-free)"
    - "Geometry derives grid dimension per board from BoardSpec bounds instead of a hardcoded constant"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/LevelRepository.kt
    - app/src/main/java/com/app/pegsolitaire/data/ClassicLevelSource.kt
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeLevelSource.kt
    - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
    - app/src/test/java/com/app/pegsolitaire/data/LevelRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/components/BoardGeometryTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/BoardMetrics.kt
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardGeometry.kt
    - app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "Display names resolved in the data layer (ctx.getString(R.string.board_*)) and carried as resolved String on Level/ClassicLevelDescriptor — NOT @StringRes, to avoid rippling the type through 02-05/06/07"
  - "Catalog built once into an insertion-ordered Map keyed by id (LinkedHashMap via associateBy) → D-18 order preserved, O(1) fail-fast load"
  - "gridDimension = max(maxRow, maxCol)+1 from BoardSpec bounds; exposed as a public val on BoardGeometry so tests assert it directly"
  - "DomainModule.provideInitialBoard left UNTOUCHED this plan — gameplay keeps Phase-1 English-33 until 02-05 wires the levelId seam"

requirements-completed: [CLAS-01, CLAS-02]

# Metrics
duration: 5min
completed: 2026-06-28
---

# Phase 2 Plan 04: LevelRepository Seam + Per-Board Geometry Summary

**Stood up the `LevelRepository` single-source-of-truth seam (real Classic source serving the 5 boards as descriptors in D-18 order with fail-fast load-by-id, plus a throwing Challenge stub Phase 3 swaps in) and generalized board geometry to derive its grid dimension per footprint (7/8/9) from the `BoardSpec` bounds — so all five square boards render and hit-test in bounds while English-33 gameplay stays untouched.**

## Performance

- **Duration:** ~5 min
- **Tasks:** 2 (Task 1 via TDD RED→GREEN; Task 2 implementation)
- **Files:** 6 created, 4 modified

## Accomplishments
- `LevelRepository` interface + `DefaultLevelRepository` delegating to a Classic source and a Challenge source — the Repository-pattern seam shaped so Phase 3's generator drops in behind `loadChallenge` unchanged.
- `Level` / `ClassicLevelDescriptor` immutable models + `ClassicLevelIds` named constants (QUAL-03 — no string-literal ids at call sites).
- `DefaultClassicLevelSource`: maps the 5 `Boards.*` specs to levels in **D-18 ascending hole-count order** (english33→european37→asymmetric39→diamond41→wiegleb45), resolves display names from `R.string.board_*` in the data layer, and **fails fast** on an unknown/forged id (`requireNotNull` → `IllegalArgumentException`, mitigating T-02-04-ID).
- `StubChallengeLevelSource.load` throws `error(...)` — a stable Phase-3 placeholder never invoked this phase.
- `DataModule` (`@Binds` abstract class) binds the repository + both sources into the `SingletonComponent` graph.
- `BoardGeometry` now derives `gridDimension = max(maxRow, maxCol)+1` from the spec bounds (exposed as a public `val`), driving cell size, grid extent, and the per-instance `positionAt` bounds-range — replacing the hardcoded `BoardMetrics.GRID_DIMENSION = 7`. All five footprints (dims 7/8/9) render and hit-test in bounds (T-02-04-TAP preserved).
- New `BoardGeometryTest` asserts dimension 7/8/9 derivation, an in-play round-trip (`positionAt(center(p)) == p`) inside the canvas for every cell of all five boards, and that off-footprint / off-canvas offsets return null.

## Task Commits

1. **Task 1 RED — failing LevelRepository seam spec** — `9cd824d` (test)
2. **Task 1 GREEN — DefaultClassicLevelSource catalog implementation** — `ac8159c` (feat)
3. **Task 2 — per-board grid-dimension geometry + tests** — `7d2b1de` (feat)

_TDD gate sequence verified in git log: `test()` RED commit precedes the `feat()` GREEN commit._

## Files Created/Modified
- `data/LevelRepository.kt` (created) — interface + `DefaultLevelRepository` + `Level` / `ClassicLevelDescriptor` + `ClassicLevelIds`
- `data/ClassicLevelSource.kt` (created) — interface + `DefaultClassicLevelSource` (catalog map, fail-fast load, resource-resolved names)
- `data/ChallengeLevelSource.kt` (created) — interface + throwing `StubChallengeLevelSource`
- `di/DataModule.kt` (created) — `@Binds` wiring for repository + both sources
- `data/LevelRepositoryTest.kt` (created) — Robolectric spec: catalog order, hole counts, resolved names, fail-fast id, throwing stub
- `ui/components/BoardGeometryTest.kt` (created) — dims 7/8/9 derivation + round-trip + off-bounds null
- `ui/game/BoardMetrics.kt` (modified) — removed `GRID_DIMENSION`; KDoc points to the derived per-board dimension
- `ui/components/BoardGeometry.kt` (modified) — `gridDimensionOf(spec)`, public `gridDimension` val, per-instance `gridRange`
- `ui/components/BoardCanvasTest.kt` (modified) — assert against `geometry.gridDimension` (removed constant ref + unused import)
- `ui/game/GameFlowTest.kt` (modified) — local `ENGLISH33_GRID_DIMENSION` constant in place of the removed `BoardMetrics.GRID_DIMENSION`

## Decisions Made
- **Resolved-String display names, not `@StringRes`:** names are looked up via `ctx.getString(R.string.board_*)` in the data layer and carried as plain `String` on `Level`/`ClassicLevelDescriptor`. This keeps domain-pure board data in `:domain`, satisfies QUAL-03 (no magic strings), and avoids rippling a `@StringRes Int` type through the downstream catalog/results UI (02-05/06/07).
- **Insertion-ordered catalog map:** `associateBy { it.id }` over a D-18-ordered list yields a `LinkedHashMap`, so `descriptors()` preserves ascending hole-count order and `load()` is an O(1) keyed lookup with a single fail-fast `requireNotNull`.
- **`gridDimension` as a public val** on `BoardGeometry`: lets tests assert the derived dimension directly and lets `BoardCanvasTest` express `cellSize` without a magic number.
- **`provideInitialBoard` left in place:** the levelId→board wiring (replacing the injected initial board) is explicitly deferred to 02-05; this plan only stands up the seam, so existing English-33 gameplay and all Phase-1 tests stay green.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Retargeted two existing tests off the removed `BoardMetrics.GRID_DIMENSION`**
- **Found during:** Task 2 (removing the hardcoded constant)
- **Issue:** `BoardCanvasTest.kt` and `GameFlowTest.kt` referenced `BoardMetrics.GRID_DIMENSION`; deleting the constant broke their compilation (and would fail the verification suite the plan requires to stay green).
- **Fix:** `BoardCanvasTest` now asserts against the new public `geometry.gridDimension` (and drops the now-unused `BoardMetrics` import); `GameFlowTest` (an English-33-specific flow) gains a local `private const val ENGLISH33_GRID_DIMENSION = 7` used by its cell-offset helper.
- **Files modified:** `BoardCanvasTest.kt`, `GameFlowTest.kt`
- **Verification:** `:app:testDebugUnitTest` (all suites), `ktlintCheck`, `detekt` → BUILD SUCCESSFUL.
- **Committed in:** `7d2b1de` (Task 2 commit)

**2. [Rule 3 - Blocking] Wrapped `level(...)` factory calls to satisfy detekt MaxLineLength**
- **Found during:** Task 1 GREEN
- **Issue:** Two single-line `level(id, R.string.*, Boards.*, Boards.*Initial)` calls exceeded the configured max line length (detekt `MaxLineLength`, a mandatory gate).
- **Fix:** Reformatted all five catalog entries to one argument per line (consistent style).
- **Files modified:** `ClassicLevelSource.kt`
- **Verification:** `detekt` exit 0.
- **Committed in:** `ac8159c` (Task 1 GREEN commit)

**Total deviations:** 2 auto-fixed (both blocking gate/compile fixes). No behavioral or scope change.

## Threat Model Coverage
- **T-02-04-ID (Tampering/DoS on `loadClassic`)** — mitigated: `DefaultClassicLevelSource.load` validates the id against the named-constant catalog and `requireNotNull`s, failing fast on an unknown/forged id; `LevelRepositoryTest.loadClassicWithUnknownIdFailsFast` covers it.
- **T-02-04-TAP (Tampering on `positionAt` across dims 7/8/9)** — mitigated: the per-cell inscribed-hit-radius + per-instance in-grid bounds check is preserved for every footprint; `BoardGeometryTest` asserts off-footprint and off-canvas offsets return null. RulesEngine still re-decides legality (unchanged).
- No new security surface beyond the plan's `<threat_model>`.

## Known Stubs
- `StubChallengeLevelSource.load` throws `error("Challenge generation arrives in Phase 3")` — **intentional** Phase-3 placeholder. There is no Challenge route this phase (D-09), so it is never invoked; the interface (`loadChallenge`) is stable and Phase 3 swaps in the real generator behind the same seam. Documented in the source and the plan.

## Verification
- `./gradlew :app:testDebugUnitTest --tests *LevelRepositoryTest* --tests *DomainPurityTest*` — green (repository lives in `:app`, domain purity intact).
- `./gradlew :app:testDebugUnitTest --tests *BoardGeometryTest* --tests *BoardCanvasTest* --tests *GameViewModelTest* --tests *GameFlowTest*` — green.
- `./gradlew :app:testDebugUnitTest` (full suite) + `ktlintCheck` + `detekt` — BUILD SUCCESSFUL (exit 0).

## Next Phase Readiness
- CLAS-01 catalog data is ready for 02-05's Classic browse screen (`ClassicCatalogViewModel` injects `LevelRepository.classicCatalog()`).
- CLAS-02 load-by-id is ready: 02-05 navigates with a `levelId`, and `GameViewModel` will resolve `loadClassic(levelId).initialBoard` (replacing the injected `provideInitialBoard`).
- Geometry renders every footprint (dims 7/8/9), unblocking the catalog mini-previews and gameplay for all five boards.
- Challenge seam is stable; Phase 3 implements `ChallengeLevelSource.load` with zero changes to the repository or its callers.

## Self-Check: PASSED

- Files: all 6 created files present on disk (LevelRepository.kt, ClassicLevelSource.kt, ChallengeLevelSource.kt, DataModule.kt, LevelRepositoryTest.kt, BoardGeometryTest.kt); 4 modified files updated.
- Commits: `9cd824d` (test/RED), `ac8159c` (feat/GREEN), `7d2b1de` (feat) all present in git log.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-28*
