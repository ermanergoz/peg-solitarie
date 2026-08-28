---
phase: quick-260828-tnw
plan: 01
subsystem: navigation
tags: [navigation, back-stack, popUpTo, results, bugfix]
requires:
  - AppNavHost type-safe nav graph (NAV-02, Phase 02-08)
  - Routes.Results.challengeLevel mode discriminator (Phase 03-05)
provides:
  - "private NavHostController.toResults — the win/finish hop, back-stack managed"
affects:
  - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
  - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
tech-stack:
  added: []
  patterns:
    - "Third popUpTo extension helper on NavHostController, matching toBoardFromCatalog / toChallengeBoard"
    - "previousBackStackEntry (public API) as the launched-from probe instead of currentBackStack (@RestrictTo)"
key-files:
  created: []
  modified:
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
decisions:
  - "The launched-from probe is previousBackStackEntry, not currentBackStack — NavController.getCurrentBackStack is @RestrictTo(LIBRARY_GROUP) and Android Lint fails :app:check on it in app code"
  - "Helper named toResults, not toResultsFromWin — GameViewModel.finished also fires for a stuck board (the Challenge Retry path)"
metrics:
  duration: ~20 min
  tasks: 2
  files: 2
  completed: 2026-08-28
---

# Quick Task 260828-tnw: Fix Results Back Navigation Summary

System Back on Results now pops the finished board and lands on the destination the level
was launched from — the catalog for Classic boards entered from it, Home for Challenge
levels and for Classic games resumed via Home Continue.

## What Changed

**`AppNavHost.kt`** — the win/finish hop in `composable<Gameplay>` was the only navigation
in the file that did not manage the back stack: `{ results -> navController.navigate(results) }`
pushed `Results` on top of a still-live `Gameplay` entry, so Back re-showed the board the
player had just finished. It now routes through a new private extension helper:

```kotlin
private fun NavHostController.toResults(results: Results) {
    val launchedFromCatalog =
        results.challengeLevel == null &&
            previousBackStackEntry?.destination?.hasRoute<ClassicCatalog>() == true
    if (launchedFromCatalog) {
        navigate(results) { popUpTo<ClassicCatalog>() }
    } else {
        navigate(results) { popUpTo<Home>() }
    }
}
```

Both pops are non-inclusive, mirroring the two sibling helpers, so the landing destination
survives and only the completed `Gameplay` entry above it is removed. The helper KDoc and
the `AppNavHost` function KDoc were both updated to state the new back-stack behaviour.

**`NavFlowTest.kt`** — three new Robolectric tests over the real captured `NavHostController`,
plus `previousRoute()`, `backStackRoutes()` and `assertCompletedBoardIsOffTheBackStack()`
helpers. The class KDoc now records that the file also pins the post-finish back stack.

## Task Commits

| Task | Name | Commit |
|------|------|--------|
| 1 | RED — assert the post-finish back stack | `2957ebb` |
| 1b | ktlint chain-wrap fix on the new helper | `e277f46` |
| 2 | GREEN — `toResults` helper wired into the win hop | `4fa6833` |

## TDD Gate Compliance

RED (`test(...)` @ `2957ebb`) → GREEN (`fix(...)` @ `4fa6833`). RED was verified genuinely
failing before implementation: all three new tests failed with
`value of: previousRoute() … but was: com.app.pegsolitaire.navigation.Gameplay?levelId=…`,
i.e. failing for the exact reason the bug exists (the finished board is still
`previousBackStackEntry`), not for a compile or setup error. No REFACTOR commit was needed.

## Verification

- `./gradlew :app:testDebugUnitTest --tests "…NavFlowTest"` — 11/11 pass (3 new + 8 pre-existing).
  The pre-existing `drivesHomeToCatalogToGameplayToResultsReachingEveryRoute` and
  `challengeRetryReloadsSameBoardWithoutGrowingBackStack` still pass unchanged, so the six
  Results action buttons keep their existing destinations and stack behaviour.
- `./gradlew :app:check --offline` — **BUILD SUCCESSFUL** (full unit tests, ktlint, detekt, Android Lint).
- Structural acceptance checks from the plan, all satisfied:
  `toResults` declared once; `navController.toResults(results)` once; `navController.navigate(results)`
  zero; `popUpTo<ClassicCatalog>()` twice; `popUpTo<Home>()` twice; zero `BackHandler` in `app/src/main/java`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `currentBackStack` is `@RestrictTo` and fails Android Lint**

- **Found during:** Task 2, on the first `./gradlew :app:check --offline`.
- **Issue:** The plan's `<interfaces>` block asserted `NavController.currentBackStack` was a
  usable public API on navigation 2.9.8. It compiles, but it carries
  `@RestrictTo(LIBRARY_GROUP)`, and Android Lint fails the build:
  `NavController.getCurrentBackStack can only be called from within the same library group
  (referenced groupId=androidx.navigation from groupId=peg-solitaire) [RestrictedApi]`.
  Since `:app:check --offline` passing is a hard constraint, the planned helper body could
  not ship as written.
- **Fix:** The `launchedFromCatalog` probe now reads `previousBackStackEntry?.destination
  ?.hasRoute<ClassicCatalog>()`. This is public, unrestricted API, and is a *tighter*
  expression of the intent: when the finished `Gameplay` is current, the entry directly
  beneath it **is** the destination it was launched from. It is equivalent for every
  reachable path in this graph — `Gameplay` is only ever entered from `ClassicCatalog`
  (catalog select, Results Replay) or from `Home` (Challenge tile, Next/Retry, Continue),
  and both entry helpers `popUpTo` so nothing else can be interposed. Suppressing the lint
  was rejected: the branch structure the user locked is preserved either way, so there was
  no reason to take on an androidx-internal API. The KDoc records why.
- **Not changed:** the locked decision itself. Classic still pops to `ClassicCatalog`,
  Challenge still pops to `Home`, no `BackHandler` was added, and no `inclusive`/
  `launchSingleTop` was introduced.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt`
- **Commit:** `4fa6833`

**2. [Rule 3 - Blocking] ktlint chain-continuation violation in the new test helper**

- **Found during:** Task 2 verification.
- **Issue:** `previousRoute()` was written as a single-line `?.destination?.route.orEmpty()`
  chain; `ktlint_official` requires a newline before each chain link, and
  `:app:ktlintTestSourceSetCheck` failed on three counts at that line.
- **Fix:** Wrapped the chain one link per line.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt`
- **Commit:** `e277f46`

### Discretionary Addition (pre-approved)

The plan's flagged addition — falling back to `Home` when the Classic finish has no
`ClassicCatalog` beneath it — was implemented as planned and confirmed approved before
execution. Its test, `resumedClassicWinBacksOutToHomeWhenNoCatalogWasVisited`, is guarded
against being a tautology: before firing the finish it asserts
`backStackRoutes().filter { it.contains(CATALOG_ROUTE) }` is empty, proving the catalog
really is absent, so the test exercises the fallback branch and not an incidental
`popUpTo<ClassicCatalog>` hit. The finish itself uses `sampleResults()`
(`challengeLevel == null`), so it is genuinely the Classic path.

## Out of Scope — Untouched as Instructed

Undo behaviour was not changed: no status guard was added to `GameViewModel.onUndo()`,
`UndoControl` was not disabled on a won board, and neither file was opened. The `Results`
destination's six action lambdas, `toBoardFromCatalog`, `toChallengeBoard`, `Routes.kt`,
`GameRoute.kt` and everything under `ui/game/` are unmodified.

## Pre-existing Issues Noted, Not Fixed

`./gradlew :app:detektDebugUnitTest` (a type-resolution task that is **not** part of
`:app:check`) reports 8 pre-existing violations in unrelated test files —
`SoundPoolPlayerTest`, `ButtonSoundTest`, `GameFlowTest`, `GameScreenSoundTest`,
`GameViewModelTest`, `ResultsViewModelTest` (`InjectDispatcher`, `LongParameterList`,
`UnusedVariable`). None are in `NavFlowTest` and none were introduced here. Out of scope
per the executor scope boundary; logged for whoever owns test-source detekt.

## Known Stubs

None.

## Self-Check: PASSED

All modified files exist on disk; all three commit hashes (`2957ebb`, `e277f46`, `4fa6833`)
resolve in `git log`. Working tree clean apart from this SUMMARY (left uncommitted for the
orchestrator's docs commit) and the pre-existing untracked `.claude/`.

## Threat Flags

None. No new trust boundary, no new dependency, no route surface change —
`gradle/libs.versions.toml` is untouched and `Results` still carries the same
in-game-computed args by value. The plan's `T-TNW-02` (DoS via `popUpTo` against an absent
route) is mitigated as designed: the absent-catalog case branches to `popUpTo<Home>()`, and
`Home` is the graph's `startDestination` so the pop can never no-op.
