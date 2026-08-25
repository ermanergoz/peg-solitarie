---
phase: 02-classic-mode-scoring-navigation
plan: 06
subsystem: ui
tags: [compose, material3, results, navigation, scoring, hilt, robolectric]

# Dependency graph
requires:
  - phase: 02-01
    provides: ScoreCalculator + ScoreParams (stars/score bands consumed for tier mapping)
  - phase: 02-04
    provides: LevelRepository.classicCatalog() D-18 order (consumed for the next-board wrap)
  - phase: 02-05
    provides: Routes.kt @Serializable Results route + Gameplay finish that navigates to it
provides:
  - Results destination quadruple (Route/Screen/ViewModel/UiState) under ui/results
  - "@Immutable ResultsUiState(stars/pegsRemaining/elapsedSeconds/score/headlineTier/replayId/nextId)"
  - "ResultsViewModel: typed toRoute<Results>() args, wrapping next-board id (D-06), stars->headline tier (D-08)"
  - "ResultsScreen: tiered headline, 3-star row, SCORE/TIME/PEGS LEFT metric block, 4 actions"
  - "EarnedStarCountKey + RESULTS_STAR_ROW_TEST_TAG semantics for decorative star-row testing"
affects: [02-08, navigation, navhost, results]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Display-only ViewModel: passes ScoreCalculator route-arg values through verbatim, computes only the wrapping next-board id"
    - "Decorative icon row exposes a single row-level semantics count (EarnedStarCountKey) instead of per-icon contentDescription"
    - "Scrollable results content (verticalScroll + Arrangement.Center) so all actions are reachable on small screens"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
  modified: []

key-decisions:
  - "Star icons substituted to material-icons-core (Icons.Filled.Star earned / Icons.Outlined.Star aliased StarBorder unearned) because the UI-SPEC's Icons.Rounded.Star/StarBorder live only in the intentionally-avoided material-icons-extended set"
  - "Headline tier thresholds reuse ScoreParams.THREE_STARS/TWO_STARS (domain single source of truth) rather than redeclaring star-band magic numbers"
  - "mm:ss reuses the internal GameTimerFormat from ui.game (no duplicate clock-format logic)"
  - "Results content is verticalScroll + Arrangement.Center so the four stacked actions stay reachable on short viewports (Rule 2 correctness)"

patterns-established:
  - "Display-only ViewModel passes domain-computed route args through unchanged; only navigation-target derivation (wrapping nextId) lives in the VM"
  - "Decorative icon rows assert via a row-level semantics count rather than per-icon labels"

requirements-completed: [SCOR-01, SCOR-02, SCOR-03]

# Metrics
duration: 18min
completed: 2026-06-30
---

# Phase 2 Plan 06: Results Destination Summary

**Full-screen Results payoff (Route/Screen/ViewModel/UiState) surfacing score, 1-3 star tier, pegs and mm:ss time from typed route args, with a star-tiered headline (D-08) and four actions including a wrapping "Next board" (D-06).**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-06-30T00:10:00Z (approx)
- **Completed:** 2026-06-30
- **Tasks:** 2
- **Files modified:** 6 created

## Accomplishments
- `ResultsViewModel` reads `toRoute<Results>()` and passes the four `ScoreCalculator`-computed values through verbatim (T-02-06-ARG); it computes only the wrapping next-board id from `classicCatalog()` D-18 order — an unknown id resolves to index 0 deterministically (T-02-06-NEXT) — and maps stars to the PERFECT/SOLVED/STUCK headline tier (D-08).
- `ResultsScreen` renders the `displaySmall` tiered headline, a 3-star row (filled `primary` earned / outline `outlineVariant` unearned), the SCORE / TIME / PEGS LEFT metric block (mm:ss via the reused `GameTimerFormat`), and the four actions with exactly one filled `primary` "Next board" Button carrying `nextId`.
- `ResultsRoute` is the stateless `hiltViewModel` + `collectAsStateWithLifecycle` seam forwarding hoisted nav lambdas (wired by `AppNavHost` in 02-08).
- Unit + Compose UI coverage: `ResultsViewModelTest` (wrap, tier, pass-through) and `ResultsScreenTest` (Robolectric `createComposeRule`: headline, star count, captions, mm:ss, four actions).

## Task Commits

1. **Task 1 (TDD): ResultsViewModel + ResultsUiState** — `74a40f0` (test, RED) → `8837dd1` (feat, GREEN)
2. **Task 2: ResultsScreen + ResultsRoute** — `df8e9f4` (feat)

_TDD gate sequence present: a `test(...)` commit precedes the `feat(...)` implementation commit._

## Files Created/Modified
- `app/.../ui/results/ResultsUiState.kt` - `@Immutable` state + `ResultsHeadlineTier` enum (PERFECT/SOLVED/STUCK)
- `app/.../ui/results/ResultsViewModel.kt` - typed-arg pass-through, wrapping nextId, tier mapping
- `app/.../ui/results/ResultsScreen.kt` - stateless headline/stars/metrics/actions + star-row semantics
- `app/.../ui/results/ResultsRoute.kt` - hiltViewModel + collectAsStateWithLifecycle seam
- `app/src/test/.../ui/results/ResultsViewModelTest.kt` - Robolectric/Truth VM spec
- `app/src/test/.../ui/results/ResultsScreenTest.kt` - Robolectric Compose UI spec

## Decisions Made
- Star icons drawn from `material-icons-core` (`Icons.Filled.Star` / `Icons.Outlined.Star` aliased `StarBorder`) since the spec's `Icons.Rounded.*` star pair is extended-set only and the project deliberately avoids `material-icons-extended`.
- Tier thresholds reuse `ScoreParams.THREE_STARS`/`TWO_STARS` (domain) to keep the star-band counts single-sourced and satisfy the no-magic-number gate.
- mm:ss reuses the internal `GameTimerFormat`; results content is scrollable so actions stay reachable on small screens.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Star icons substituted to the curated icon set**
- **Found during:** Task 2 (ResultsScreen)
- **Issue:** The plan/UI-SPEC specify `Icons.Rounded.Star` and `Icons.Rounded.StarBorder`, which exist only in `material-icons-extended`. The project's version catalog intentionally ships only `material-icons-core` ("the heavy `material-icons-extended` set is intentionally avoided"); those symbols do not resolve.
- **Fix:** Used the core-set `Icons.Filled.Star` (earned, `primary`) and `Icons.Outlined.Star` aliased `StarBorder` (unearned, `outlineVariant`) — same filled/outline tier semantics, no new dependency added.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
- **Verification:** `:app:compileDebugKotlin` + `ResultsScreenTest` (star-row earned-count semantics) green.
- **Committed in:** `df8e9f4`

**2. [Rule 2 - Missing Critical] Made the Results content scrollable**
- **Found during:** Task 2 (ResultsScreen test)
- **Issue:** The stacked headline + 48dp stars + metrics + four full-width buttons exceed a short viewport; the bottom "Home" action was laid out below the fold and unreachable (test confirmed it was not displayed).
- **Fix:** Wrapped the content column in `verticalScroll(rememberScrollState())` with `Arrangement.Center` (centers when it fits, scrolls when it doesn't); the test scrolls nodes into view before asserting.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt, app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
- **Verification:** `ResultsScreenTest` green (all four actions reachable + displayed).
- **Committed in:** `df8e9f4`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 missing critical)
**Impact on plan:** Both necessary for correctness; no scope creep. The icon substitution preserves the documented filled/outline star contract without adding the intentionally-avoided dependency.

## Issues Encountered
- ktlint import-ordering required the aliased `Star as StarBorder` import to sit at the end of the import block; resolved by reordering. Pre-existing `createComposeRule` deprecation warning is shared with `GameFlowTest` and out of scope.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Results destination is complete and ready for `AppNavHost` (02-08) to wire `composable<Results> { ResultsRoute(...) }` with the back-stack-driven nav lambdas (Next board → `Gameplay(nextId)`, Replay → `Gameplay(replayId)`, Catalog/Home → popBackStack).
- No blockers introduced.

## Threat Flags

None - no new security surface beyond the display-only Results route args already in the plan's threat model.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-30*

## Self-Check: PASSED

All 6 created source files and the SUMMARY exist on disk; all three task commits (`74a40f0`, `8837dd1`, `df8e9f4`) are present in git history. Verification suite (`ResultsViewModelTest`, `ResultsScreenTest`, `DomainPurityTest`, `compileDebugKotlin`, `ktlintCheck`, `detekt`) is green.
